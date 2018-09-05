package space.npstr.baymax;

import com.vdurmont.emoji.Emoji;
import com.vdurmont.emoji.EmojiManager;

/**
 * Created by napster on 05.09.18.
 */
public class Emojis {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(Emojis.class);

    private Emojis() {}

    public static String get(String alias) {
        Emoji emoji = EmojiManager.getForAlias(alias);
        if (emoji == null) {
            log.warn("Emoji not found for alias {}, falling back to :thinking:", alias);
            emoji = EmojiManager.getForAlias("thinking");
        }
        return emoji.getUnicode();
    }
}
