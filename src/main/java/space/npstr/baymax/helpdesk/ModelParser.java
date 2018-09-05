package space.npstr.baymax.helpdesk;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import space.npstr.baymax.helpdesk.exception.MissingTargetNodeException;
import space.npstr.baymax.helpdesk.exception.NoRootNodeException;
import space.npstr.baymax.helpdesk.exception.UnreferencedNodesException;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by napster on 05.09.18.
 */
public class ModelParser {

    public ModelParser() {
    }

    public Map<String, Node> parse(String yaml) {
        Yaml snakeYaml = new Yaml(new Constructor(NodeModel.class));
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

        Map<String, Node> allNodes = Collections.unmodifiableMap(new HashMap<>(model));
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

        for (Branch branch : branches) {
            String targetId = branch.getTargetId();
            Node targetNode = allNodes.get(targetId);
            if (targetNode == null) {
                throw new MissingTargetNodeException(node, targetId);
            }
            traverse(targetNode, allNodes, unreferencedNodes);
        }

        unreferencedNodes.remove(node.getId());
    }
}
