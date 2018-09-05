package space.npstr.baymax.helpdesk;

/**
 * Created by napster on 05.09.18.
 */
public class BranchModel implements Branch {

    private String message = "";

    private String targetId = "";

    public BranchModel() {}

    @Override
    public String getMessage() {
        return this.message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String getTargetId() {
        return this.targetId;
    }

    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }
}
