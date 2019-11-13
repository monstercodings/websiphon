package top.codings.websiphon.core.context;

import top.codings.websiphon.bean.RateResult;
import top.codings.websiphon.bean.WebRequest;
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

    void postSyncEvent(WebSyncEvent event) throws WebException;

    void postAsyncEvent(WebAsyncEvent event);

    Crawler getCrawler();

    RateResult getRateResult();

    void clearTask();

    String getId();

    CrawlerContext setId(String id);

    void finishRequest(WebRequest webRequest);

//    void close();

}
