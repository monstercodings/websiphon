package top.codings.websiphon.bean;

import java.util.Map;

public interface WebRequest {
    String uri();

    Method method();

    Object body();

    Map<String, String> headers();

    WebResponse response();

    /*@Getter
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
    protected int depth;
    @Getter
    @Setter
    protected int maxDepth;
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
    @Getter
    @Setter
    protected String charset;

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
    }*/

    /**
     * 完成一次请求后由请求器主动调用
     */
    void succeed();

    void failed(Throwable throwable);

    /*public static WebRequest simple(String url) {
        return new WebRequest(url, HeadersUtils.getHeaders(), 30 * 1000);
    }*/

    enum Method {
        GET(),
        POST(),
        PUT(),
        DELETE(),
        PATCH(),
        HEAD(),
        CONNECT()
    }

}
