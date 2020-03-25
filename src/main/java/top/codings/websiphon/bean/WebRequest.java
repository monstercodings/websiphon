package top.codings.websiphon.bean;

import top.codings.websiphon.core.context.CrawlerContext;
import top.codings.websiphon.exception.StopWebRequestException;

import java.util.Map;

public interface WebRequest {
    String uri();

    Method method();

    Object body();

    Map<String, String> headers();

    WebResponse response();

    CrawlerContext context();

    void context(CrawlerContext context);

    /**
     * 完成一次请求后由请求器主动调用
     */
    void succeed();

    /**
     * 完成一次请求后由请求器主动调用
     */
    void failed(Throwable throwable);

    default void stop() {
        throw new StopWebRequestException();
    }

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
