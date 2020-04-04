package top.codings.websiphon.bean;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import top.codings.websiphon.core.context.CrawlerContext;
import top.codings.websiphon.core.context.event.async.WebNetworkExceptionEvent;
import top.codings.websiphon.core.proxy.bean.WebProxy;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ToString
@Setter
public class BasicWebRequest implements WebRequest {
    protected String uri;
    protected Method method = Method.GET;
    protected Object body;
    protected Map<String, String> headers = new ConcurrentHashMap<>();
    @Getter
    protected Map<String, String> formData = new ConcurrentHashMap<>();
    protected int timeout;
    protected WebResponse response = new WebResponse();
    @Getter
    protected WebProxy proxy;
    protected CrawlerContext context;
    @Getter
    protected long beginAt;
    @Getter
    protected long endAt;
    @Getter
    protected String charset;
    protected Status status = Status.WAIT;

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
        status = Status.SUCCEED;
        context.doOnFinished(this);
    }

    @Override
    public void failed(Throwable throwable) {
        status = Status.ERROR;
        WebNetworkExceptionEvent event = new WebNetworkExceptionEvent();
        event.setThrowable(throwable);
        event.setRequest(this);
        context.doOnFinished(event);
    }

    @Override
    public Status status() {
        return status;
    }

    @Override
    public void status(Status status) {
        this.status = status;
    }
}
