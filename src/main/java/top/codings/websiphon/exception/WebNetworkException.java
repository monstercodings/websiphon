package top.codings.websiphon.exception;

import top.codings.websiphon.bean.WebResponse;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WebNetworkException extends WebException {
    protected WebResponse.Result result;

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

    public WebNetworkException(WebResponse.Result result) {
        this.result = result;
    }

    public WebNetworkException(String message, WebResponse.Result result) {
        super(message);
        this.result = result;
    }

    public WebNetworkException(String message, Throwable cause, WebResponse.Result result) {
        super(message, cause);
        this.result = result;
    }

    public WebNetworkException(Throwable cause, WebResponse.Result result) {
        super(cause);
        this.result = result;
    }

    public WebNetworkException(Object accessory, WebResponse.Result result) {
        super(accessory);
        this.result = result;
    }

    public WebNetworkException(String message, Object accessory, WebResponse.Result result) {
        super(message, accessory);
        this.result = result;
    }

    public WebNetworkException(String message, Throwable cause, Object accessory, WebResponse.Result result) {
        super(message, cause, accessory);
        this.result = result;
    }

    public WebNetworkException(Throwable cause, Object accessory, WebResponse.Result result) {
        super(cause, accessory);
        this.result = result;
    }
}
