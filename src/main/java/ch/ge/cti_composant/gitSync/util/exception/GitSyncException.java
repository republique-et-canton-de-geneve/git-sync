package ch.ge.cti_composant.gitSync.util.exception;

public class GitSyncException extends RuntimeException {

    public GitSyncException(String msg, Exception cause) {
        super(msg, cause);
    }

}
