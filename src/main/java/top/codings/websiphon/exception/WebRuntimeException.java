package top.codings.websiphon.exception;

public class WebRuntimeException extends RuntimeException {
    public WebRuntimeException() {
    }

    public WebRuntimeException(String message) {
        super(message);
    }

    public WebRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }

    public WebRuntimeException(Throwable cause) {
        super(cause);
    }

    public WebRuntimeException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
