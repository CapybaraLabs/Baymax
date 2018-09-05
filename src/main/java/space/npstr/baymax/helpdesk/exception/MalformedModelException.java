package space.npstr.baymax.helpdesk.exception;

/**
 * Created by napster on 05.09.18.
 */
public abstract class MalformedModelException extends RuntimeException {

    public MalformedModelException() {
        super();
    }

    @Override
    public abstract String getMessage();
}
