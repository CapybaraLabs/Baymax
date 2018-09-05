package space.npstr.baymax.helpdesk.exception;

/**
 * Created by napster on 05.09.18.
 */
public class NoRootNodeException extends MalformedModelException {

    public NoRootNodeException() {
        super();
    }

    @Override
    public String getMessage() {
        return "No root node found";
    }
}
