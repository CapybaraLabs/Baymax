package space.npstr.baymax;

import space.npstr.baymax.helpdesk.Node;

import java.util.Optional;

/**
 * Provide some context around a node, for example the node that was previously visited.
 */
public class NodeContext {

    private final Node node;
    private final Optional<NodeContext> previousNodeContext;

    public NodeContext(Node node, Optional<NodeContext> previousNodeContext) {
        this.node = node;
        this.previousNodeContext = previousNodeContext;
    }

    public Node getNode() {
        return this.node;
    }

    public Optional<NodeContext> getPreviousNodeContext() {
        return this.previousNodeContext;
    }
}
