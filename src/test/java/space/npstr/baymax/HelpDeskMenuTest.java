/*
 * Copyright (C) 2026 Dennis Neufeld
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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import org.junit.jupiter.api.Test;
import space.npstr.baymax.helpdesk.ModelParser;
import space.npstr.baymax.helpdesk.Node;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class HelpDeskMenuTest {

    private final ModelParser modelParser = new ModelParser();

    @Test
    void render_Root_NumberedButtonsWithoutBack() throws IOException {
        Map<String, Node> model = loadModel("models/sane.yaml");

        MessageCreateData message = HelpDeskMenu.render(model, List.of());

        assertTrue(message.getContent().startsWith("**How may I help you?**"));
        List<Button> buttons = buttons(message);
        assertEquals(List.of("1", "2"), buttons.stream().map(Button::getLabel).toList());
        assertEquals(List.of(List.of(0), List.of(1)), paths(buttons));
    }

    @Test
    void render_Child_BackLeadsToParent() throws IOException {
        Map<String, Node> model = loadModel("models/sane.yaml");

        List<Button> buttons = buttons(HelpDeskMenu.render(model, List.of(0)));

        assertEquals("Back", buttons.getLast().getLabel());
        assertEquals(List.of(List.of(0, 0), List.of(0, 1), List.of(0, 2), List.of(0, 3), List.of()), paths(buttons));
    }

    @Test
    void render_ButtonIdsAreUnique() throws IOException {
        Map<String, Node> model = loadModel("models/sane.yaml");

        List<Button> buttons = buttons(HelpDeskMenu.render(model, List.of(0, 1)));

        assertEquals(buttons.size(), buttons.stream().map(Button::getCustomId).distinct().count());
    }

    @Test
    void render_LoopingModel_PathStaysWithinDiscordLimits() throws IOException {
        Map<String, Node> model = loadModel("models/branch_loop.yaml");
        List<Integer> path = List.of();

        for (int i = 0; i < 200; i++) {
            Button next = buttons(HelpDeskMenu.render(model, path)).getFirst();
            assertTrue(Objects.requireNonNull(next.getCustomId()).length() <= 100);
            path = HelpDeskMenu.parseButtonId(next.getCustomId()).orElseThrow();
        }
        assertTrue(HelpDeskMenu.resolve(model, path).isPresent());
    }

    @Test
    void resolve_StalePath_Empty() throws IOException {
        Map<String, Node> model = loadModel("models/sane.yaml");

        assertEquals(Optional.empty(), HelpDeskMenu.resolve(model, List.of(0, 42)));
    }

    @Test
    void parseButtonId_ForeignId_Empty() {
        assertEquals(Optional.empty(), HelpDeskMenu.parseButtonId("something-else"));
        assertEquals(Optional.empty(), HelpDeskMenu.parseButtonId(HelpDeskMenu.BUTTON_PREFIX + "x#0"));
    }

    private static List<Button> buttons(MessageCreateData message) {
        List<Button> buttons = new ArrayList<>();
        message.getComponentTree().findAll(Button.class).forEach(buttons::add);
        return buttons;
    }

    private static List<List<Integer>> paths(List<Button> buttons) {
        return buttons.stream()
            .map(button -> HelpDeskMenu.parseButtonId(Objects.requireNonNull(button.getCustomId())).orElseThrow())
            .toList();
    }

    private Map<String, Node> loadModel(String fileName) throws IOException {
        try (InputStream is = Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream(fileName))) {
            return this.modelParser.parse(new String(is.readAllBytes()));
        }
    }
}
