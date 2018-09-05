package space.npstr.baymax;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import net.dv8tion.jda.bot.sharding.ShardManager;
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
                continue;
            }

            try {
                purgeChannel(channel)//todo listen for failures
                        .whenComplete((__, ___) -> {
                            var model = this.models.getModelByName(helpDesk.getModelName());
                            channel.sendMessage(UserDialogue.asMessage(model.get("root"))).queue();
                        }); //todo listen for failures
            } catch (Exception e) {
                log.error("Failed to purge channel {}", channel, e);
            }
        }
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
