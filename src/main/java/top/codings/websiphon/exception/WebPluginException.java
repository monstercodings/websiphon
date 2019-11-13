package top.codings.websiphon.exception;

public class WebPluginException extends RuntimeException {
    public WebPluginException() {
    }

    public WebPluginException(String message) {
        super(message);
    }

    public WebPluginException(String message, Throwable cause) {
        super(message, cause);
    }

    public WebPluginException(Throwable cause) {
        super(cause);
    }

    public WebPluginException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
