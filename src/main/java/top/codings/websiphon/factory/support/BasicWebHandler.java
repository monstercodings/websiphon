package top.codings.websiphon.factory.support;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import top.codings.websiphon.bean.BasicWebRequest;
import top.codings.websiphon.bean.PushResult;
import top.codings.websiphon.bean.RateResult;
import top.codings.websiphon.bean.WebRequest;
import top.codings.websiphon.core.context.CrawlerContext;
import top.codings.websiphon.core.context.event.WebAsyncEvent;
import top.codings.websiphon.core.context.event.WebSyncEvent;
import top.codings.websiphon.core.context.event.async.*;
import top.codings.websiphon.core.context.event.listener.WebAsyncEventListener;
import top.codings.websiphon.core.context.event.listener.WebSyncEventListener;
import top.codings.websiphon.core.context.event.sync.WebAfterParseEvent;
import top.codings.websiphon.core.context.event.sync.WebBeforeParseEvent;
import top.codings.websiphon.core.context.event.sync.WebBeforeRequestEvent;
import top.codings.websiphon.core.parser.WebParser;
import top.codings.websiphon.core.pipeline.ReadWritePipeline;
import top.codings.websiphon.core.plugins.WebPlugin;
import top.codings.websiphon.core.requester.WebRequester;
import top.codings.websiphon.core.schedule.support.BasicRequestScheduler;
import top.codings.websiphon.exception.StopWebRequestException;
import top.codings.websiphon.exception.WebException;
import top.codings.websiphon.exception.WebNetworkException;
import top.codings.websiphon.factory.bean.CrawlThreadPoolExecutor;
import top.codings.websiphon.factory.bean.WebHandler;
import top.codings.websiphon.operation.QueueMonitor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 调度器
 */
@Slf4j
public class BasicWebHandler implements WebHandler {
    private LinkedTransferQueue<RespRunner> respQueue = new LinkedTransferQueue<>();
    @Getter
    private QueueMonitor queueMonitor = new QueueMonitor();
    @Getter
    @Setter
    private WebRequester webRequester;
    @Getter
    @Setter
    private WebParser webParser;
    @Getter
    @Setter
    private List<ReadWritePipeline> readWritePipelines;
    private Semaphore networkToken;
    private Semaphore parseToken;
    @Getter
    private Map<Class<? extends WebAsyncEvent>, WebAsyncEventListener> asyncMap = new HashMap<>();
    @Getter
    private Map<Class<? extends WebSyncEvent>, WebSyncEventListener> syncMap = new HashMap<>();
    private Thread runThread;
    @Setter
    private BasicRequestScheduler scheduler;
    @Setter
    private List<WebPlugin> plugins;
    /**
     * 异步事件池
     */
    @Getter
    private ExecutorService asyncEventExecutor = Executors.newCachedThreadPool(new ThreadFactory() {
        private final AtomicInteger threadNumber = new AtomicInteger(1);

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(Thread.currentThread().getThreadGroup(), r,
                    "async-event-" + threadNumber.getAndIncrement(),
                    0);
            // 设置为守护线程
            t.setDaemon(true);
            t.setPriority(Thread.NORM_PRIORITY);
            return t;
        }
    });
    /**
     * 解析线程池
     */
    private ExecutorService parseExecutor;
    /**
     * 速率统计
     */
    @Getter
    private RateResult rateResult = new RateResult();

    @Override
    public void request(CrawlerContext context) throws InterruptedException {
        networkToken.acquire();
        WebRequest request = scheduler.take();
        request.context(context);
        try {
            if (request instanceof BasicWebRequest) {
                BasicWebRequest basicWebRequest = (BasicWebRequest) request;
//                basicWebRequest.setProxy(Optional.ofNullable(basicWebRequest.getProxy()).orElse(proxyPool.select()));
                basicWebRequest.setBeginAt(System.currentTimeMillis());
            }
            WebBeforeRequestEvent event = new WebBeforeRequestEvent();
            event.setRequest(request);
            postSyncEvent(event);
            webRequester.execute(request);
            rateResult.addTotal();
            return;
        } catch (StopWebRequestException e) {
            // 停止处理该请求
        } catch (WebNetworkException e) {
            WebNetworkExceptionEvent exceptionEvent = new WebNetworkExceptionEvent();
            exceptionEvent.setRequest(request);
            postAsyncEvent(exceptionEvent);
        } catch (WebException e) {
            WebExceptionEvent event = new WebExceptionEvent();
            event.setRequest(request);
            event.setThrowable(e);
            postAsyncEvent(event);
        } catch (Exception e) {
            log.error("警告!执行网络请求发生未知异常，该异常不应该出现，请仔细排查相关代码");
            WebExceptionEvent event = new WebExceptionEvent();
            event.setRequest(request);
            event.setThrowable(e);
            postAsyncEvent(event);
        }
        scheduler.release(request);
        networkToken.release();
        queueMonitor.decrement(request);
    }

    @Override
    public PushResult write(WebRequest request) {
        scheduler.handle(request);
        return PushResult.SUCCESS;
        /*PushResult result;
        try {
            result = readWritePipeline.write(request);
        } catch (InterruptedException e) {
            return PushResult.BLOCK_FAILED;
        }
        if (result == PushResult.SUCCESS) {
            queueMonitor.increment(request);
        }
        return result;*/
    }

    public void handleSuccessd(WebRequest request) {
        /*if (readWritePipeline instanceof BasicReadWritePipeline) {
            ((BasicReadWritePipeline) readWritePipeline).eliminateForRequest(request);
        }
        WebResponse response = request.response();
        if (request == null) {
            log.warn("任务尚未进行 -> {}", request.getUrl());
            return;
        } else if (response == null) {
            queueMonitor.decrement(request);
            return;
        } else if (response.isRedirect() && response.getResult() == null) {
            queueMonitor.decrement(request);
            return;
        } else if (response.getErrorEvent() != null) {
            queueMonitor.decrement(request);
            postAsyncEvent(request.getResponse().getErrorEvent());
            return;
        }*/
        respQueue.offer(new RespRunner(request, parseToken));
    }

    public void handleFailed(WebErrorAsyncEvent event) {
        WebRequest request = event.getRequest();
        queueMonitor.decrement(request);
        postAsyncEvent(event);
    }

    @Override
    public void doOnFinished(Object data) {
        networkToken.release();
        if (data instanceof WebRequest) {
            scheduler.release((WebRequest) data);
            rateResult.incrementResult((WebRequest) data);
            handleSuccessd((WebRequest) data);
        } else if (data instanceof WebErrorAsyncEvent) {
            scheduler.release(((WebErrorAsyncEvent) data).getRequest());
            rateResult.incrementResult(((WebErrorAsyncEvent) data).getRequest());
            handleFailed((WebErrorAsyncEvent) data);
        } else {
            throw new IllegalArgumentException("处理参数只能为WebRequest或WebErrorAsyncEvent的子类");
        }
    }

    @Override
    public void init(int networkCount, int parseCount) {
        queueMonitor.init();
        scheduler.init(queueMonitor);
        rateResult.start();
        networkToken = new Semaphore(networkCount);
        parseToken = new Semaphore(parseCount);
        parseExecutor = new CrawlThreadPoolExecutor(parseCount, "crawler-parse");
        runThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    parseToken.acquire();
                    parseExecutor.submit(respQueue.take());
                } catch (InterruptedException e) {
                    break;
                } catch (Exception e) {
                    log.error("执行解析响应失败", e);
                }
            }
            log.info("WebHandler核心执行终止");
        });
        runThread.setName("webHandler");
        // 设置为守护线程
        runThread.setDaemon(true);
        runThread.start();
        if (readWritePipelines != null && !readWritePipelines.isEmpty()) {
            readWritePipelines.forEach(readWritePipeline -> readWritePipeline.init());
            Thread transfer = new Thread(() -> {
                try {
                    while (!Thread.currentThread().isInterrupted()) {
                        for (ReadWritePipeline readWritePipeline : readWritePipelines) {
                            Optional.ofNullable(readWritePipeline.read()).ifPresent(scheduler::handle);
                        }
                    }
                } catch (InterruptedException e) {
                    return;
                }
            });
            transfer.setName("transfer");
            transfer.setDaemon(true);
            transfer.start();
        }
    }

    @Override
    public boolean postAsyncEvent(WebAsyncEvent event) {
        if (asyncMap.containsKey(event.getClass())) {
            asyncEventExecutor.submit(() -> asyncMap.get(event.getClass()).listen(event));
            return true;
        } else {
            if (event instanceof WebErrorAsyncEvent) {
                WebErrorAsyncEvent webErrorAsyncEvent = (WebErrorAsyncEvent) event;
                if (asyncMap.containsKey(AllExceptionEvent.class)) {
                    asyncEventExecutor.submit(() -> {
                        AllExceptionEvent allExceptionEvent = new AllExceptionEvent();
                        allExceptionEvent.setThrowable(webErrorAsyncEvent.getThrowable());
                        allExceptionEvent.setRequest(webErrorAsyncEvent.getRequest());
                        asyncMap.get(AllExceptionEvent.class).listen(allExceptionEvent);
                    });
                    return true;
                } else {
                    log.error("未捕获异常", webErrorAsyncEvent.getThrowable());
                }
            }
        }
        return false;
    }

    @Override
    public boolean postSyncEvent(WebSyncEvent event) throws WebException {
        if (syncMap.containsKey(event.getClass())) {
            try {
                syncMap.get(event.getClass()).listen(event);
            } catch (StopWebRequestException e) {
                throw e;
            } catch (WebException e) {
                throw e;
            } catch (Exception e) {
                WebException exception = new WebException(e);
                throw exception;
            }
            return true;
        }
        return false;
    }

    @Override
    public void close() throws Exception {
        rateResult.close();
        if (null != readWritePipelines) {
            readWritePipelines.forEach(readWritePipeline -> {
                try {
                    readWritePipeline.close();
                } catch (Exception e) {
                    log.error("关闭管道异常", e);
                }
            });
        }
        asyncEventExecutor.shutdownNow();
        parseExecutor.shutdownNow();
        if (CollectionUtils.isNotEmpty(plugins)) {
            plugins.forEach(webPlugin -> webPlugin.close());
        }
        if (webRequester != null) {
            webRequester.close();
        }
    }

    @AllArgsConstructor
    private class RespRunner implements Runnable {
        private WebRequest request;
        private Semaphore parseToken;

        @Override
        public void run() {
            try {
                WebBeforeParseEvent event = new WebBeforeParseEvent();
                event.setRequest(request);
                postSyncEvent(event);
                webParser.parse(request);
                if (request instanceof BasicWebRequest) {
                    ((BasicWebRequest) request).setEndAt(System.currentTimeMillis());

                }
//                rateResult.addSuccess(request.getEndAt() - request.getBeginAt());
                WebAfterParseEvent afterParseEvent = new WebAfterParseEvent();
                afterParseEvent.setRequest(request);
                postSyncEvent(afterParseEvent);
            } catch (StopWebRequestException e) {
                // 停止异常不做处理
            } catch (WebException e) {
                WebParseExceptionEvent exceptionEvent = new WebParseExceptionEvent();
                exceptionEvent.setRequest(request);
                exceptionEvent.setThrowable(e);
                postAsyncEvent(exceptionEvent);
            } catch (Exception e) {
                AllExceptionEvent exceptionEvent = new AllExceptionEvent();
                exceptionEvent.setRequest(request);
                exceptionEvent.setThrowable(e);
                postAsyncEvent(exceptionEvent);
            } finally {
                queueMonitor.decrement(request);
                parseToken.release();
            }
        }
    }
}
