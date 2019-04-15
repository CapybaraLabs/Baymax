/*
 * Copyright (C) 2018-2019 Dennis Neufeld
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

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Parse Emojis into a number
 */
public class EmojisNumbersParser {

    public static final String ZERO = Emojis.get("zero");
    public static final String ONE = Emojis.get("one");
    public static final String TWO = Emojis.get("two");
    public static final String THREE = Emojis.get("three");
    public static final String FOUR = Emojis.get("four");
    public static final String FIVE = Emojis.get("five");
    public static final String SIX = Emojis.get("six");
    public static final String SEVEN = Emojis.get("seven");
    public static final String EIGHT = Emojis.get("eight");
    public static final String NINE = Emojis.get("nine");

    private static final Map<String, Character> EMOJI_TO_DIGIT = Map.of(
            ZERO, '0',
            ONE, '1',
            TWO, '2',
            THREE, '3',
            FOUR, '4',
            FIVE, '5',
            SIX, '6',
            SEVEN, '7',
            EIGHT, '8',
            NINE, '9'
    );

    private static final Map<Character, String> DIGIT_TO_EMOJI = EMOJI_TO_DIGIT.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));


    public String numberAsEmojis(int number) {
        String numberAsString = Integer.toString(number);
        return numberAsString.chars()
                .mapToObj(c -> digitToEmoji((char) c))
                .collect(StringBuilder::new, StringBuilder::append, StringBuilder::append)
                .toString();
    }

    public String digitToEmoji(char digit) {
        String result = DIGIT_TO_EMOJI.get(digit);
        if (result == null || result.isEmpty()) {
            throw new NumberFormatException(digit + " is not a digit");
        }
        return result;
    }


    public Optional<Integer> emojisToNumber(String input) {
        try {
            return Optional.of(Integer.parseInt(parse(input)));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private String parse(String input) {
        if (input.isEmpty()) return "";
        EmojiMapping found = startsWithNumberEmoji(input);
        String shortened = input.replaceFirst(found.emoji, "");
        return found.digit + parse(shortened);
    }

    private EmojiMapping startsWithNumberEmoji(String input) {
        return EMOJI_TO_DIGIT.entrySet().stream()
                .filter(entry -> input.startsWith(entry.getKey()))
                .findAny()
                .map(entry -> new EmojiMapping(entry.getKey(), entry.getValue()))
                .orElseThrow(() -> new NumberFormatException("Input " + input + " does not start with a known emoji."));
    }

    private static class EmojiMapping {
        public final String emoji;
        public final char digit;

        public EmojiMapping(String emoji, char digit) {
            this.emoji = emoji;
            this.digit = digit;
        }
    }

}
