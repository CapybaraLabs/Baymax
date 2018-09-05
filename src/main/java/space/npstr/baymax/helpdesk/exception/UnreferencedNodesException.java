package space.npstr.baymax.helpdesk.exception;

import space.npstr.baymax.helpdesk.Node;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Created by napster on 05.09.18.
 */
public class UnreferencedNodesException extends MalformedModelException {

    private final Collection<Node> unreferencedNodes;

    public UnreferencedNodesException(Collection<Node> unreferencedNodes) {
        super();
        this.unreferencedNodes = unreferencedNodes;
    }

    @Override
    public String getMessage() {
        String nodes = String.join(", ",
                this.unreferencedNodes.stream().map(Node::getId).collect(Collectors.toList())
        );
        return "Nodes " + nodes + " have no reference leading to them from the root node.";
    }
}
