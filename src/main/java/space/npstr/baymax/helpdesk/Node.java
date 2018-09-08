package space.npstr.baymax.helpdesk;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Created by napster on 05.09.18.
 */
public interface Node {

    String getId();

    String getTitle();

    @Nullable
    Long getRoleId();

    List<? extends Branch> getBranches();
}
