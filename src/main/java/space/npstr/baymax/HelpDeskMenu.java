/*
 * Copyright (C) 2018-2026 Dennis Neufeld
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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import space.npstr.baymax.helpdesk.Branch;
import space.npstr.baymax.helpdesk.ModelParser;
import space.npstr.baymax.helpdesk.Node;

/**
 * Renders help desk nodes as messages with buttons.
 * <p>
 * The navigation state is stored in the button ids as the path of branch indices taken from the root node,
 * so no state needs to be kept around between interactions.
 */
public final class HelpDeskMenu {

    public static final String BUTTON_PREFIX = "baymax:";

    private static final int MAX_CUSTOM_ID_LENGTH = 100;
    // button ids have to be unique within a message, but different buttons may point to the same path
    private static final String SLOT_SEPARATOR = "#";
    private static final String KEYCAP = "️⃣";

    private HelpDeskMenu() {}

    public static MessageCreateData render(Map<String, Node> model, List<Integer> path) {
        Node node = resolve(model, path).orElseThrow(() -> new IllegalArgumentException("Path " + path + " does not resolve"));
        MessageCreateBuilder mb = new MessageCreateBuilder();
        List<Button> buttons = new ArrayList<>();

        mb.addContent("**").addContent(node.getTitle()).addContent("**\n\n");
        List<? extends Branch> branches = node.getBranches();
        for (int i = 0; i < branches.size(); i++) {
            String number = Integer.toString(i + 1);
            mb.addContent(asKeycapEmojis(number)).addContent(" ").addContent(branches.get(i).getMessage()).addContent("\n");
            buttons.add(Button.primary(buttonId(childPath(model, path, i), buttons.size()), number));
        }
        if (path.isEmpty()) {
            mb.addContent("\nPick a number to start.\n");
        } else {
            buttons.add(Button.secondary(buttonId(path.subList(0, path.size() - 1), buttons.size()), "Back"));
        }

        if (!buttons.isEmpty()) {
            mb.addComponents(ActionRow.partitionOf(buttons));
        }
        return mb.build();
    }

    /**
     * @return the path encoded in the button id, or empty if the button id is not ours or malformed
     */
    public static Optional<List<Integer>> parseButtonId(String buttonId) {
        if (!buttonId.startsWith(BUTTON_PREFIX) || !buttonId.contains(SLOT_SEPARATOR)) {
            return Optional.empty();
        }
        String rawPath = buttonId.substring(BUTTON_PREFIX.length(), buttonId.indexOf(SLOT_SEPARATOR));
        if (rawPath.isEmpty()) {
            return Optional.of(List.of());
        }
        try {
            List<Integer> path = new ArrayList<>();
            for (String index : rawPath.split("\\.")) {
                path.add(Integer.parseInt(index));
            }
            return Optional.of(path);
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    /**
     * @return the node at the end of the path, or empty if the path does not fit the model (e.g. it changed)
     */
    public static Optional<Node> resolve(Map<String, Node> model, List<Integer> path) {
        Node node = model.get("root");
        for (int index : path) {
            if (index < 0 || index >= node.getBranches().size()) {
                return Optional.empty();
            }
            node = model.get(node.getBranches().get(index).getTargetId());
        }
        return Optional.of(node);
    }

    private static List<Integer> childPath(Map<String, Node> model, List<Integer> path, int branchIndex) {
        String targetId = resolve(model, path).orElseThrow().getBranches().get(branchIndex).getTargetId();
        if ("root".equals(targetId)) {
            return List.of();
        }
        List<Integer> childPath = new ArrayList<>(path);
        childPath.add(branchIndex);
        if (buttonId(childPath, ModelParser.MAX_BRANCHES).length() > MAX_CUSTOM_ID_LENGTH) {
            // models with loops can grow the path indefinitely, start over with the shortest path to the target
            return shortestPath(model, targetId);
        }
        return childPath;
    }

    private static List<Integer> shortestPath(Map<String, Node> model, String targetId) {
        Map<String, List<Integer>> paths = new HashMap<>();
        paths.put("root", List.of());
        ArrayDeque<String> queue = new ArrayDeque<>(List.of("root"));
        while (!queue.isEmpty()) {
            String nodeId = queue.poll();
            List<Integer> path = paths.get(nodeId);
            if (nodeId.equals(targetId)) {
                return path;
            }
            List<? extends Branch> branches = model.get(nodeId).getBranches();
            for (int i = 0; i < branches.size(); i++) {
                String nextId = branches.get(i).getTargetId();
                if (!paths.containsKey(nextId)) {
                    List<Integer> nextPath = new ArrayList<>(path);
                    nextPath.add(i);
                    paths.put(nextId, nextPath);
                    queue.add(nextId);
                }
            }
        }
        throw new IllegalArgumentException("Node " + targetId + " is not reachable from root");
    }

    private static String buttonId(List<Integer> path, int slot) {
        return BUTTON_PREFIX + path.stream().map(String::valueOf).collect(Collectors.joining(".")) + SLOT_SEPARATOR + slot;
    }

    private static String asKeycapEmojis(String number) {
        StringBuilder sb = new StringBuilder();
        number.chars().forEach(digit -> sb.append((char) digit).append(KEYCAP));
        return sb.toString();
    }
}
