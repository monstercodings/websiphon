package top.codings.websiphon.core.context;

import top.codings.websiphon.core.Crawler;
import top.codings.websiphon.core.context.event.WebAsyncEvent;
import top.codings.websiphon.core.context.event.WebSyncEvent;
import top.codings.websiphon.exception.WebException;

public interface CrawlerContext {
    long getBeginAt();

    long getEndAt();

    boolean isRunning();

    int getNetworkThreadSize();

    int getParseThreadSize();

//    WebHandler getWebHandler();

    boolean postSyncEvent(WebSyncEvent event) throws WebException;

    boolean postAsyncEvent(WebAsyncEvent event);

    Crawler getCrawler();

    void clearTask();

    String getId();

    CrawlerContext setId(String id);

    void doOnFinished(Object data);

//    void close();

}
