package top.codings.websiphon.core.support;

import top.codings.websiphon.bean.PushResult;
import top.codings.websiphon.bean.WebRequest;
import top.codings.websiphon.core.Crawler;
import top.codings.websiphon.core.context.BasicCrawlerContext;
import top.codings.websiphon.core.context.CrawlerContext;
import top.codings.websiphon.core.context.event.async.WebExceptionEvent;
import top.codings.websiphon.core.context.event.sync.WebCrawlShutdownEvent;
import top.codings.websiphon.core.context.event.sync.WebCrawlStartEvent;
import top.codings.websiphon.factory.bean.CrawlThreadPoolExecutor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import top.codings.websiphon.util.HttpOperator;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class BasicCrawler implements Crawler {
    @Getter
    @Setter
    private ExecutorService executorService;
    @Getter
    @Setter
    private BasicCrawlerContext context;

    public BasicCrawler() {
        context = new BasicCrawlerContext(this);
    }

    @Override
    public synchronized CrawlerContext start() {
        if (context.isRunning()) {
            return context;
        }
        context.setRunning(true);
        try {
            // 初始化请求器
            context.getWebHandler().getWebRequester().init();
            WebCrawlStartEvent event = new WebCrawlStartEvent();
            event.setContext(context);
            context.postSyncEvent(event);
        } catch (Exception e) {
            context.setRunning(false);
            WebExceptionEvent event = new WebExceptionEvent();
            event.setThrowable(e);
            context.postAsyncEvent(event);
            return context;
        }
        context.getWebHandler().init(context.getNetworkThreadSize(), context.getParseThreadSize());
        initThreadPool();
        context.setBeginAt(System.currentTimeMillis());
        executorService.submit(() -> loop());
        return context;
    }

    /**
     * 初始化执行线程池
     */
    private void initThreadPool() {
        executorService = new CrawlThreadPoolExecutor(1, "crawler-main");
        /*Executors.newCachedThreadPool(new ThreadFactory() {
            private final AtomicInteger threadNumber = new AtomicInteger(1);

            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(Thread.currentThread().getThreadGroup(), r,
                        "crawler-" + context.getId().replaceAll("\\s*", "") + "-" + threadNumber.getAndIncrement(),
                        0);
                if (t.isDaemon())
                    t.setDaemon(false);
                if (t.getPriority() != Thread.NORM_PRIORITY)
                    t.setPriority(Thread.NORM_PRIORITY);
                return t;
            }
        });*/
    }

    @Override
    public PushResult push(WebRequest request) {
        if (!context.isRunning()) {
            return PushResult.CRAWLER_STOP;
        }
        if (null == request) {
            return PushResult.TASK_EMPTY;
        }
        try {
            HttpOperator.resolve(request.uri());
        } catch (Exception e) {
            return PushResult.URL_ERROR;
        }
        return context.getWebHandler().write(request);
    }

    @Override
    public String getId() {
        return context.getId();
    }

    /**
     * 爬虫主线程循环
     */
    private void loop() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                context.getWebHandler().request(context);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("爬虫主线程发生异常", e);
        }
        log.info("关闭爬虫[{}]主线程", context.getId());
    }

    @Override
    public synchronized void close() {
        if (!context.isRunning()) {
            return;
        }
        context.setEndAt(System.currentTimeMillis());
        try {
            WebCrawlShutdownEvent event = new WebCrawlShutdownEvent();
            event.setContext(context);
            context.postSyncEvent(event);
        } catch (Exception e) {
            WebExceptionEvent event = new WebExceptionEvent();
            event.setThrowable(e);
            context.postAsyncEvent(event);
        }
        context.setRunning(false);
        executorService.shutdownNow();
        try {
            context.getWebHandler().close();
        } catch (Exception e) {
            log.error("关闭爬虫时发生异常", e);
        }
    }
}
