package top.codings.websiphon.core.context;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import top.codings.websiphon.core.context.event.WebAsyncEvent;
import top.codings.websiphon.core.context.event.WebSyncEvent;
import top.codings.websiphon.core.support.BasicCrawler;
import top.codings.websiphon.exception.WebException;
import top.codings.websiphon.factory.support.BasicWebHandler;

@NoArgsConstructor
@Slf4j
@Data
public class BasicCrawlerContext implements CrawlerContext {
    private BasicCrawler crawler;
    private long beginAt;
    private long endAt;
    private int networkThreadSize = 10;
    private int parseThreadSize = networkThreadSize * 10;
    private BasicWebHandler webHandler;
    private volatile boolean running = false;
    private String id;

    public BasicCrawlerContext(BasicCrawler crawler) {
        this.crawler = crawler;
    }

    @Override
    public boolean postSyncEvent(WebSyncEvent event) throws WebException {
        return webHandler.postSyncEvent(event);
    }

    @Override
    public boolean postAsyncEvent(WebAsyncEvent event) {
        return webHandler.postAsyncEvent(event);
    }

    @Override
    public void clearTask() {
        throw new NotImplementedException("暂未实现");
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public CrawlerContext setId(String id) {
        this.id = id;
        return this;
    }

    @Override
    public void doOnFinished(Object data) {
        webHandler.doOnFinished(data);
    }
}
