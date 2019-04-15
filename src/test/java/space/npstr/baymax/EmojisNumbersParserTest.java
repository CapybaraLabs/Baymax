package space.npstr.baymax;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class EmojisNumbersParserTest {

    private final EmojisNumbersParser parser = new EmojisNumbersParser();

    @Test
    void fortyTwo() {
        String fortyTwo = this.parser.numberAsEmojis(42);
        assertEquals(EmojisNumbersParser.FOUR + EmojisNumbersParser.TWO, fortyTwo);

        Optional<Integer> result = this.parser.emojisToNumber(fortyTwo);
        assertTrue(result.isPresent());
        assertEquals(42, result.get());
    }

    @Test
    void maxInt() {
        String value = this.parser.numberAsEmojis(Integer.MAX_VALUE);

        Optional<Integer> result = this.parser.emojisToNumber(value);
        assertTrue(result.isPresent());
        assertEquals(Integer.MAX_VALUE, result.get());
    }

    @Test
    void zero() {
        String value = this.parser.numberAsEmojis(0);

        Optional<Integer> result = this.parser.emojisToNumber(value);
        assertTrue(result.isPresent());
        assertEquals(0, result.get());
    }

    @Test
    void negative_shouldThrow() {
        assertThrows(NumberFormatException.class, () -> this.parser.numberAsEmojis(-1));
    }

    @Test
    void notANumberEmoji_shouldBeEmpty() {
        Optional<Integer> poop = this.parser.emojisToNumber(Emojis.get("poop"));
        assertTrue(poop.isEmpty());
    }
}
