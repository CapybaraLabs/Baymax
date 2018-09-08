package space.npstr.baymax.helpdesk;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

/**
 * Created by napster on 05.09.18.
 */
public class NodeModel implements Node {

    private String id = "";

    private String title = "";

    private Long roleId = null;

    private List<BranchModel> branches = Collections.emptyList();

    public NodeModel() {}

    @Override
    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getTitle() {
        return this.title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @Override
    @Nullable
    public Long getRoleId() {
        return this.roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    @Override
    public List<BranchModel> getBranches() {
        return this.branches;
    }

    public void setBranches(List<BranchModel> branches) {
        this.branches = branches;
    }
}
