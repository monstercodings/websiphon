package top.codings.websiphon.core.context.event.async;

import lombok.Getter;
import lombok.Setter;
import top.codings.websiphon.bean.WebRequest;
import top.codings.websiphon.core.context.CrawlerContext;

public class WebExceptionEvent<T extends WebRequest> extends WebErrorAsyncEvent<T> {
}
