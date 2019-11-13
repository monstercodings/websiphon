package top.codings.websiphon.bean;

import top.codings.websiphon.core.context.CrawlerContext;
import top.codings.websiphon.util.HeadersUtils;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.net.Proxy;
import java.util.Map;

@NoArgsConstructor
public class WebRequest {
//    protected String serialId = UUID.randomUUID().toString().replace("-", "");
@Getter
@Setter
    protected String url;
    @Getter
    @Setter
    protected Method method = Method.GET;
    @Getter
    @Setter
    protected Object body;
    @Getter
    @Setter
    protected Map<String, String> headers;
    @Getter
    @Setter
    protected int timeout;
    @Getter
    @Setter
    protected short depth;
    @Getter
    @Setter
    protected short maxDepth;
    @Getter
    @Setter
    protected WebResponse response;
    @Getter
    @Setter
    protected Proxy proxy;
    @Setter
    private CrawlerContext context;
    @Getter
    @Setter
    protected long beginAt;
    @Getter
    @Setter
    protected long endAt;

    public WebRequest(String url, Map<String, String> headers, int timeout) {
        this.url = url;
        this.headers = headers;
        this.timeout = timeout;
    }

    public WebRequest(String url) {
        this.url = url;
    }

    public CrawlerContext context() {
        return context;
    }
    public static WebRequest simple(String url) {
        return new WebRequest(url, HeadersUtils.getHeaders(), 30 * 1000);
    }

    public enum Method {
        GET(),
        POST(),
        PUT(),
        DELETE(),
        PATCH(),
        HEAD(),
        CONNECT()
    }

}
