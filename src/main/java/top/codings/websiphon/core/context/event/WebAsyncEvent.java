package top.codings.websiphon.core.context.event;

import top.codings.websiphon.bean.WebRequest;
import top.codings.websiphon.core.context.CrawlerContext;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WebAsyncEvent<T extends WebRequest> {
    protected T request;

    /*public WebAsyncEvent setContext(CrawlerContext context) {
        this.context = context;
        return this;
    }

    public WebAsyncEvent setRequest(T request) {
        this.request = request;
        return this;
    }*/
}
