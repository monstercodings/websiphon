package top.codings.websiphon.core.context.event.async;

import lombok.Getter;
import lombok.Setter;
import top.codings.websiphon.bean.WebRequest;
import top.codings.websiphon.core.context.event.WebAsyncEvent;

@Getter
@Setter
public class WebErrorAsyncEvent<T extends WebRequest> extends WebAsyncEvent<T> {
    protected Throwable throwable;
}
