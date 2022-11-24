/*
 * Copyright (C) 2018-2022 Dennis Neufeld
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package space.npstr.baymax;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.error.YAMLException;
import space.npstr.baymax.config.properties.BaymaxConfig;
import space.npstr.baymax.db.TemporaryRoleService;
import space.npstr.baymax.helpdesk.Node;
import space.npstr.baymax.helpdesk.exception.MalformedModelException;

/**
 * Created by napster on 05.09.18.
 */
@Component
public class HelpDeskListener extends ListenerAdapter {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(HelpDeskListener.class);

    public static final int EXPIRE_MINUTES = 2;

    private final EventWaiter eventWaiter;
    private final ModelLoader modelLoader;
    private final BaymaxConfig baymaxConfig;
    private final RestActions restActions;
    private final TemporaryRoleService temporaryRoleService;

    //channel id of the helpdesk <-> user id <-> ongoing dialogue
    private Map<Long, Cache<Long, UserDialogue>> helpDesksDialogues = new ConcurrentHashMap<>();

    public HelpDeskListener(EventWaiter eventWaiter, ModelLoader modelLoader, BaymaxConfig baymaxConfig,
                            RestActions restActions, TemporaryRoleService temporaryRoleService) {

        this.eventWaiter = eventWaiter;
        this.modelLoader = modelLoader;
        this.baymaxConfig = baymaxConfig;
        this.restActions = restActions;
        this.temporaryRoleService = temporaryRoleService;
    }

    @Override
    public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
        TextChannel channel = event.getChannel();
        if (!channel.canTalk()) {
            return;
        }

        var helpDeskOpt = this.baymaxConfig.getHelpDesks().stream()
                .filter(helpDesk -> helpDesk.getChannelId() == channel.getIdLong())
                .findAny();

        if (helpDeskOpt.isEmpty()) {
            return;
        }
        if (event.getAuthor().isBot()) {
            if (event.getAuthor().getIdLong() == event.getJDA().getSelfUser().getIdLong()) {
                return;
            }

            restActions.deleteMessageAfter(event.getMessage(), Duration.ofSeconds(5))
                .whenComplete((__, t) -> {
                    if (t != null) {
                        log.error("Failed to delete bot message in channel {}", channel, t);
                    }
                });
            return;
        }

        var helpDesk = helpDeskOpt.get();
        var userDialogues = this.helpDesksDialogues.computeIfAbsent(
                helpDesk.getChannelId(), channelId -> this.createUserDialogueCache()
        );
        Member member = event.getMember();
        if (member == null) {
            return;
        }
        if (isStaff(member)) {
            if (event.getMessage().isMentioned(event.getJDA().getSelfUser())) {
                String content = event.getMessage().getContentRaw().toLowerCase();
                if (content.contains("init")) {
                    userDialogues.invalidateAll();
                    userDialogues.cleanUp();
                    init(channel, helpDesk.getModelName(), helpDesk.getModelUri());
                    return;
                } else if (content.contains("reload")) {
                    try {
                        var reloadedModel = this.modelLoader.attemptReload(helpDesk.getModelName(), helpDesk.getModelUri());
                        userDialogues.invalidateAll();
                        userDialogues.cleanUp();
                        init(channel, reloadedModel);
                    } catch (MalformedModelException | YAMLException e) {
                        Message message = new MessageBuilder().append("Failed to load model due to: **")
                            .append(e.getMessage())
                            .append("**")
                            .build();
                        this.restActions.sendMessage(channel, message)
                            .whenComplete((__, t) -> {
                                if (t != null) {
                                    log.error("Failed to reply in channel {}", channel, t);
                                }
                            });
                    }
                    return;
                }
            }
        }

        userDialogues.get(event.getAuthor().getIdLong(),
                userId -> {
                    var model = this.modelLoader.getModel(helpDesk.getModelName(), helpDesk.getModelUri());
                    return new UserDialogue(this.eventWaiter, model, event, this.restActions, this.temporaryRoleService);
                });
    }

    // revisit for when there is more than one shard
    @Override
    public void onReady(ReadyEvent event) {
        //1. Clean up the channel
        //2. Post the root message

        ShardManager shardManager = Objects.requireNonNull(event.getJDA().getShardManager(), "Shard manager required");
        for (BaymaxConfig.HelpDesk helpDesk : this.baymaxConfig.getHelpDesks()) {
            TextChannel channel = shardManager.getTextChannelById(helpDesk.getChannelId());
            if (channel == null) {
                log.warn("Failed to find and setup configured help desk channel {}", helpDesk.getChannelId());
                return;
            }
            init(channel, helpDesk.getModelName(), helpDesk.getModelUri());
        }
    }

    private void init(TextChannel channel, String modelName, Optional<URI> modelUri) {
        init(channel, this.modelLoader.getModel(modelName, modelUri));
    }

    private void init(TextChannel channel, Map<String, Node> model) {
        try {
            this.restActions.purgeChannel(channel)
                    .exceptionally(t -> {
                        log.error("Failed to purge messages for init in channel {}", channel, t);
                        return null; //Void
                    })
                    .thenCompose(__ -> {
                        NodeContext nodeContext = new NodeContext(model.get("root"), Optional.empty());
                        Message message = UserDialogue.asMessage(nodeContext);
                        return this.restActions.sendMessage(channel, message);
                    })
                    .whenComplete((__, t) -> {
                        if (t != null) {
                            log.error("Failed to send init message in channel {}", channel, t);
                        }
                    });
        } catch (Exception e) {
            log.error("Failed to purge channel {}", channel, e);
        }
    }

    private boolean isStaff(Member member) {
        return member.getRoles().stream()
                .anyMatch(role -> this.baymaxConfig.getStaffRoleIds().contains(role.getIdLong()));
    }

    private Cache<Long, UserDialogue> createUserDialogueCache() {
        return Caffeine.newBuilder()
                .expireAfterAccess(EXPIRE_MINUTES, TimeUnit.MINUTES)
                .removalListener((@Nullable Long userId, @Nullable UserDialogue userDialogue, RemovalCause cause) -> {
                    if (userDialogue != null) {
                        userDialogue.done();
                    }
                })
                .build();
    }
}
