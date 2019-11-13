package top.codings.websiphon.core.context.event.async;

import lombok.Getter;
import lombok.Setter;
import top.codings.websiphon.bean.WebRequest;

@Getter
@Setter
public class WebExceptionEvent<T extends WebRequest> extends WebErrorAsyncEvent<T> {
}
