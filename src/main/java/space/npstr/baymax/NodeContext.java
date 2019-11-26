package space.npstr.baymax;

import space.npstr.baymax.helpdesk.Node;

import java.util.Optional;

/**
 * Provide some context around a node, for example the node that was previously visited.
 */
public class NodeContext {

    private final Node node;
    private final Optional<Node> previousNode;

    public NodeContext(Node node, Optional<Node> previousNode) {
        this.node = node;
        this.previousNode = previousNode;
    }

    public Node getNode() {
        return this.node;
    }

    public Optional<Node> getPreviousNode() {
        return this.previousNode;
    }
}
