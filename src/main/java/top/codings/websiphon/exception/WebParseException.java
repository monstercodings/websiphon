package top.codings.websiphon.exception;

public class WebParseException extends WebException {
    public WebParseException() {
    }

    public WebParseException(String message) {
        super(message);
    }

    public WebParseException(String message, Throwable cause) {
        super(message, cause);
    }

    public WebParseException(Throwable cause) {
        super(cause);
    }

    public WebParseException(Object accessory) {
        super(accessory);
    }

    public WebParseException(String message, Object accessory) {
        super(message, accessory);
    }

    public WebParseException(String message, Throwable cause, Object accessory) {
        super(message, cause, accessory);
    }

    public WebParseException(Throwable cause, Object accessory) {
        super(cause, accessory);
    }
}
