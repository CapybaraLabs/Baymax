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
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageHistory;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.ReadyEvent;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import org.springframework.stereotype.Component;
import space.npstr.baymax.config.properties.BaymaxConfig;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
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

    //channel id of the helpdesk <-> user id <-> ongoing dialogue
    private Map<Long, Cache<Long, UserDialogue>> helpDesksDialogues = new ConcurrentHashMap<>();

    public HelpDeskListener(EventWaiter eventWaiter, Models models, BaymaxConfig baymaxConfig) {
        this.eventWaiter = eventWaiter;
        this.models = models;
        this.baymaxConfig = baymaxConfig;
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
                    return new UserDialogue(this.eventWaiter, model, event);
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
            purgeChannel(channel)
                    .whenComplete((__, t) -> {
                        if (t != null) {
                            log.error("Failed to purge messages for init in channel {} for model {}", channel, modelName, t);
                        }
                        try {
                            var model = this.models.getModelByName(modelName);
                            channel.sendMessage(UserDialogue.asMessage(model.get("root"))).queue();
                        } catch (Exception e) {
                            log.error("Failed to send init message in channel {} for model {}", channel, modelName, e);
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

    @CheckReturnValue
    private CompletionStage<Void> purgeChannel(TextChannel channel) {
        //noinspection SuspiciousToArrayCall
        return fetchAllMessages(channel.getHistory())
                .thenApply(channel::purgeMessages)
                .thenCompose(requestFutures -> CompletableFuture.allOf(requestFutures.toArray(new CompletableFuture[0])));
    }

    @CheckReturnValue
    private CompletionStage<List<Message>> fetchAllMessages(MessageHistory history) {
        return history.retrievePast(100).submit()
                .thenCompose(
                        messages -> {
                            if (!messages.isEmpty()) {
                                return fetchAllMessages(history);
                            }
                            return CompletableFuture.completedStage(history.getRetrievedHistory());
                        });
    }
}
