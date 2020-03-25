package top.codings.websiphon.bean;

import lombok.Getter;
import lombok.Setter;
import top.codings.websiphon.core.context.CrawlerContext;
import top.codings.websiphon.core.context.event.async.WebNetworkExceptionEvent;

import java.net.Proxy;
import java.util.Map;

@Setter
public class BasicWebRequest implements WebRequest {
    protected String uri;
    protected Method method = Method.GET;
    protected Object body;
    protected Map<String, String> headers;
    protected int timeout;
    protected int depth;
    protected int maxDepth;
    protected WebResponse response = new WebResponse();
    @Getter
    protected Proxy proxy;
    protected CrawlerContext context;
    protected long beginAt;
    protected long endAt;
    @Getter
    protected String charset;

    @Override
    public String uri() {
        return uri;
    }

    @Override
    public Method method() {
        return method;
    }

    @Override
    public Object body() {
        return body;
    }

    @Override
    public Map<String, String> headers() {
        return headers;
    }

    @Override
    public WebResponse response() {
        return response;
    }

    @Override
    public CrawlerContext context() {
        return context;
    }

    @Override
    public void context(CrawlerContext context) {
        this.context = context;
    }

    @Override
    public void succeed() {
        context.doOnFinished(this);
    }

    @Override
    public void failed(Throwable throwable) {
        WebNetworkExceptionEvent event = new WebNetworkExceptionEvent();
        event.setThrowable(throwable);
        event.setRequest(this);
        context.doOnFinished(event);
    }
}
