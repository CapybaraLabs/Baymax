package space.npstr.baymax.helpdesk.exception;

import space.npstr.baymax.helpdesk.Node;

/**
 * Created by napster on 05.09.18.
 */
public class MissingTargetNodeException extends MalformedModelException {

    private final Node sourceNode;
    private final String targetNodeId;

    public MissingTargetNodeException(Node sourceNode, String targetNodeId) {
        super();
        this.sourceNode = sourceNode;
        this.targetNodeId = targetNodeId;
    }

    @Override
    public String getMessage() {
        return "Node " + this.sourceNode.getId() + "  has a branch referencing a non-existent node " + this.targetNodeId;
    }
}
