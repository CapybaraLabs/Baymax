package space.npstr.baymax.helpdesk;

/**
 * Created by napster on 05.09.18.
 */
public interface Branch {

    /**
     * @return The message describing this branch.
     */
    String getMessage();

    /**
     * @return The id of the node which this branch is pointing at.
     */
    String getTargetId();
}
