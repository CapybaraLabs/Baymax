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

package space.npstr.baymax.helpdesk;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import space.npstr.baymax.helpdesk.exception.MissingTargetNodeException;
import space.npstr.baymax.helpdesk.exception.NoRootNodeException;
import space.npstr.baymax.helpdesk.exception.UnreferencedNodesException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by napster on 05.09.18.
 */
public class ModelParser {

    public Map<String, Node> parse(String yaml) {
        Yaml snakeYaml = new Yaml(new Constructor(NodeModel.class, new LoaderOptions()));
        Iterable<Object> objects = snakeYaml.loadAll(yaml);
        Map<String, Node> model = new HashMap<>();
        objects.forEach(o -> model.put(((Node) o).getId(), (Node) o));

        sanityCheck(model);

        return model;
    }

    private void sanityCheck(Map<String, Node> model) {
        // A model is considered sane when:
        //      0) There is a root node
        //      1) Each node referenced by a branch exists
        //      2) All nodes are referenced from the root node

        Map<String, Node> allNodes = Map.copyOf(model);
        Map<String, Node> unreferencedNodes = new HashMap<>(model);

        Node rootNode = model.get("root");
        if (rootNode == null) {
            throw new NoRootNodeException();
        }

        traverse(rootNode, allNodes, unreferencedNodes);

        if (!unreferencedNodes.isEmpty()) {
            throw new UnreferencedNodesException(unreferencedNodes.values());
        }
    }


    private void traverse(Node node, Map<String, Node> allNodes, Map<String, Node> unreferencedNodes) {
        List<? extends Branch> branches = node.getBranches();

        Node remove = unreferencedNodes.remove(node.getId());
        if (remove == null) {
            return; //already visited this node
        }

        for (Branch branch : branches) {
            String targetId = branch.getTargetId();
            Node targetNode = allNodes.get(targetId);
            if (targetNode == null) {
                throw new MissingTargetNodeException(node, targetId);
            }
            if ("root".equals(targetId)) {
                continue;
            }
            traverse(targetNode, allNodes, unreferencedNodes);
        }
    }
}
