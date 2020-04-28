package top.codings.websiphon.exception;

import top.codings.websiphon.bean.WebResponse;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WebNetworkException extends WebException {
    protected int statusCode;

    public WebNetworkException() {
    }

    public WebNetworkException(String message) {
        super(message);
    }

    public WebNetworkException(String message, Throwable cause) {
        super(message, cause);
    }

    public WebNetworkException(Throwable cause) {
        super(cause);
    }

    public WebNetworkException(Object accessory) {
        super(accessory);
    }

    public WebNetworkException(String message, Object accessory) {
        super(message, accessory);
    }

    public WebNetworkException(String message, Throwable cause, Object accessory) {
        super(message, cause, accessory);
    }

    public WebNetworkException(Throwable cause, Object accessory) {
        super(cause, accessory);
    }

    public WebNetworkException(int statusCode) {
        this.statusCode = statusCode;
    }

    public WebNetworkException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public WebNetworkException(String message, Throwable cause, int statusCode) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    public WebNetworkException(Throwable cause, int statusCode) {
        super(cause);
        this.statusCode = statusCode;
    }

    public WebNetworkException(Object accessory, int statusCode) {
        super(accessory);
        this.statusCode = statusCode;
    }

    public WebNetworkException(String message, Object accessory, int statusCode) {
        super(message, accessory);
        this.statusCode = statusCode;
    }

    public WebNetworkException(String message, Throwable cause, Object accessory, int statusCode) {
        super(message, cause, accessory);
        this.statusCode = statusCode;
    }

    public WebNetworkException(Throwable cause, Object accessory, int statusCode) {
        super(cause, accessory);
        this.statusCode = statusCode;
    }
}
