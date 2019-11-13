package top.codings.websiphon.core.context.event;

import top.codings.websiphon.bean.WebRequest;
import top.codings.websiphon.core.context.CrawlerContext;
import lombok.Data;

@Data
public class WebSyncEvent<T extends WebRequest> {
    protected CrawlerContext context;
    protected T request;

    public WebSyncEvent setContext(CrawlerContext context) {
        this.context = context;
        return this;
    }
}
