package top.codings.websiphon.core.context.event.async;

import lombok.Getter;
import lombok.Setter;
import top.codings.websiphon.bean.WebRequest;

@Getter
@Setter
@Deprecated
public class WebMonitorExceptionEvent<T extends WebRequest> extends WebExceptionEvent<T> {
    protected int size;

    public WebMonitorExceptionEvent(T webRequest) {
        this.request = webRequest;
    }
}
