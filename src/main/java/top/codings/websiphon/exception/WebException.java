package top.codings.websiphon.exception;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WebException extends Exception {
    protected Object accessory;

    public WebException() {
    }

    public WebException(String message) {
        super(message);
    }

    public WebException(String message, Throwable cause) {
        super(message, cause);
    }

    public WebException(Throwable cause) {
        super(cause);
    }

    public WebException(Object accessory) {
        this.accessory = accessory;
    }

    public WebException(String message, Object accessory) {
        super(message);
        this.accessory = accessory;
    }

    public WebException(String message, Throwable cause, Object accessory) {
        super(message, cause);
        this.accessory = accessory;
    }

    public WebException(Throwable cause, Object accessory) {
        super(cause);
        this.accessory = accessory;
    }
}
