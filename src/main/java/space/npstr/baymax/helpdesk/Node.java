package space.npstr.baymax.helpdesk;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Created by napster on 05.09.18.
 */
public interface Node {

    /**
     * @return The id of this node
     */
    String getId();

    /**
     * @return The title of this node.
     */
    String getTitle();

    /**
     * @return A possible role associated with reaching this node that will be assigned to the user.
     * <p>
     * NOTE: We can't use Optional due to SnakeYaml aiming for Java 6 compatibility :clap:
     */
    @Nullable
    Long getRoleId();

    /**
     * @return A list of branches connecting this node to 0-n other nodes in a unidirectional way
     */
    List<? extends Branch> getBranches();
}
