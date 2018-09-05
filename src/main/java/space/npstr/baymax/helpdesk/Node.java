package space.npstr.baymax.helpdesk;

import java.util.List;

/**
 * Created by napster on 05.09.18.
 */
public interface Node {

    String getId();

    String getTitle();

    List<? extends Branch> getBranches();
}
