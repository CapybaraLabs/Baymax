package space.npstr.baymax;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import java.util.concurrent.TimeUnit;

/**
 * Created by napster on 05.09.18.
 */
@Component
public class HelpDeskListener extends ListenerAdapter {

    public static final int EXPIRE_MINUTES = 5;

    private final EventWaiter eventWaiter;
    private final Models models;

    private Cache<Long, UserDialogue> userDialogues = Caffeine.newBuilder()
            .expireAfterAccess(EXPIRE_MINUTES, TimeUnit.MINUTES)
            .removalListener((@Nullable Long userId, @Nullable UserDialogue userDialogue, RemovalCause cause) -> {
                if (userDialogue != null) {
                    userDialogue.done();
                }
            })
            .build();

    public HelpDeskListener(EventWaiter eventWaiter, Models models) {
        this.eventWaiter = eventWaiter;
        this.models = models;
    }

    @Override
    public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
        if (event.getAuthor().isBot()) {
            return;
        }
        if (!event.getChannel().canTalk()) {
            return;
        }

        this.userDialogues.get(event.getAuthor().getIdLong(),
                userId -> new UserDialogue(this.eventWaiter, this.models.getAkiModel(), event));
    }
}
