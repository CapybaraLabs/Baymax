package space.npstr.baymax;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import org.springframework.stereotype.Component;
import space.npstr.baymax.config.properties.BaymaxConfig;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Created by napster on 05.09.18.
 */
@Component
public class HelpDeskListener extends ListenerAdapter {

    public static final int EXPIRE_MINUTES = 5;

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
