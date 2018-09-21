/*
 * Copyright (C) 2018 Dennis Neufeld
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
import net.dv8tion.jda.bot.sharding.ShardManager;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.ReadyEvent;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import org.springframework.stereotype.Component;
import space.npstr.baymax.config.properties.BaymaxConfig;
import space.npstr.baymax.db.TemporaryRoleService;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Created by napster on 05.09.18.
 */
@Component
public class HelpDeskListener extends ListenerAdapter {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(HelpDeskListener.class);

    public static final int EXPIRE_MINUTES = 2;

    private final EventWaiter eventWaiter;
    private final Models models;
    private final BaymaxConfig baymaxConfig;
    private final RestActions restActions;
    private final TemporaryRoleService temporaryRoleService;

    //channel id of the helpdesk <-> user id <-> ongoing dialogue
    private Map<Long, Cache<Long, UserDialogue>> helpDesksDialogues = new ConcurrentHashMap<>();

    public HelpDeskListener(EventWaiter eventWaiter, Models models, BaymaxConfig baymaxConfig,
                            RestActions restActions, TemporaryRoleService temporaryRoleService) {

        this.eventWaiter = eventWaiter;
        this.models = models;
        this.baymaxConfig = baymaxConfig;
        this.restActions = restActions;
        this.temporaryRoleService = temporaryRoleService;
    }

    @Override
    public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
        if (event.getAuthor().isBot()) {
            return;
        }
        if (!event.getChannel().canTalk()) {
            return;
        }

        var helpDeskOpt = this.baymaxConfig.getHelpDesks().stream()
                .filter(helpDesk -> helpDesk.getChannelId() == event.getChannel().getIdLong())
                .findAny();

        if (!helpDeskOpt.isPresent()) {
            return;
        }

        var helpDesk = helpDeskOpt.get();
        var userDialogues = this.helpDesksDialogues.computeIfAbsent(
                helpDesk.getChannelId(), channelId -> this.createUserDialogueCache()
        );
        if (isStaff(event.getMember())) {
            if (event.getMessage().isMentioned(event.getJDA().getSelfUser())) {
                if (event.getMessage().getContentRaw().toLowerCase().contains("init")) {
                    userDialogues.invalidateAll();
                    userDialogues.cleanUp();
                    init(event.getChannel(), helpDesk.getModelName());
                    return;
                }
            }
        }

        userDialogues.get(event.getAuthor().getIdLong(),
                userId -> {
                    var model = this.models.getModelByName(helpDesk.getModelName());
                    return new UserDialogue(this.eventWaiter, model, event, this.restActions, this.temporaryRoleService);
                });
    }

    // revisit for when there is more than one shard
    @Override
    public void onReady(ReadyEvent event) {
        //1. Clean up the channel
        //2. Post the root message

        ShardManager shardManager = event.getJDA().asBot().getShardManager();
        for (BaymaxConfig.HelpDesk helpDesk : this.baymaxConfig.getHelpDesks()) {
            TextChannel channel = shardManager.getTextChannelById(helpDesk.getChannelId());
            if (channel == null) {
                log.warn("Failed to find and setup configured help desk channel {}", helpDesk.getChannelId());
                return;
            }
            init(channel, helpDesk.getModelName());
        }
    }

    private void init(TextChannel channel, String modelName) {
        try {
            this.restActions.purgeChannel(channel)
                    .exceptionally(t -> {
                        log.error("Failed to purge messages for init in channel {} for model {}", channel, modelName, t);
                        return null; //Void
                    })
                    .thenCompose(__ -> {
                            var model = this.models.getModelByName(modelName);
                        return this.restActions.sendMessage(channel, UserDialogue.asMessage(model.get("root")));
                    })
                    .whenComplete((__, t) -> {
                        if (t != null) {
                            log.error("Failed to send init message in channel {} for model {}", channel, modelName, t);
                        }
                    });
        } catch (Exception e) {
            log.error("Failed to purge channel {}", channel, e);
        }
    }

    private boolean isStaff(Member member) {
        return member.getRoles().stream()
                .anyMatch(role -> role.getIdLong() == this.baymaxConfig.getStaffRoleId());
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
