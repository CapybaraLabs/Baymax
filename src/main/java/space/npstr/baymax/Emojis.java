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
