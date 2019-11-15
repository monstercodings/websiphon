package top.codings.websiphon.factory.support;

import top.codings.websiphon.bean.PushResult;
import top.codings.websiphon.bean.RateResult;
import top.codings.websiphon.bean.WebRequest;
import top.codings.websiphon.bean.WebResponse;
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
import top.codings.websiphon.core.pipeline.BasicReadWritePipeline;
import top.codings.websiphon.core.pipeline.ReadWritePipeline;
import top.codings.websiphon.core.proxy.ProxyPool;
import top.codings.websiphon.core.proxy.manager.ProxyManager;
import top.codings.websiphon.core.requester.WebRequester;
import top.codings.websiphon.exception.WebException;
import top.codings.websiphon.exception.WebNetworkException;
import top.codings.websiphon.factory.bean.CrawlThreadPoolExecutor;
import top.codings.websiphon.factory.bean.WebHandler;
import top.codings.websiphon.operation.QueueMonitor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class BasicWebHandler implements WebHandler {
    //    private LinkedTransferQueue<WebRequest> responseList = new LinkedTransferQueue<>();
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
    private ReadWritePipeline readWritePipeline;
    private Semaphore networkToken;
    //    private Semaphore parseToken;
    @Getter
    private Map<Class<? extends WebAsyncEvent>, WebAsyncEventListener> asyncMap = new HashMap<>();
    @Getter
    private Map<Class<? extends WebSyncEvent>, WebSyncEventListener> syncMap = new HashMap<>();
//    private Thread runThread;
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
     * 代理池
     */
    private ProxyPool proxyPool = new ProxyPool();
    /**
     * 速率统计
     */
    @Getter
    private RateResult rateResult = new RateResult();

    @Override
    public void request() throws InterruptedException {
        networkToken.acquire();
        WebRequest request = readWritePipeline.read();
        // TODO 校验URL正确性
        CrawlerContext context = request.context();
        try {
            request.setProxy(Optional.ofNullable(request.getProxy()).orElse(proxyPool.select()));
            WebBeforeRequestEvent event = new WebBeforeRequestEvent();
            event.setContext(context);
            event.setRequest(request);
            postSyncEvent(event);
            request.setBeginAt(System.currentTimeMillis());
            webRequester.execute(request);
            rateResult.addTotal();
            return;
        } catch (WebNetworkException e) {
            WebNetworkExceptionEvent exceptionEvent = new WebNetworkExceptionEvent();
            exceptionEvent.setContext(context);
            exceptionEvent.setRequest(request);
            postAsyncEvent(exceptionEvent);
        } catch (WebException e) {
            WebExceptionEvent event = new WebExceptionEvent();
            event.setRequest(request);
            event.setContext(context);
            event.setThrowable(e);
            postAsyncEvent(event);
        } catch (Exception e) {
            log.error("警告!执行网络请求发生未知异常，该异常不应该出现，请仔细排查相关代码");
            WebExceptionEvent event = new WebExceptionEvent();
            event.setRequest(request);
            event.setContext(context);
            event.setThrowable(e);
            postAsyncEvent(event);
        }
        networkToken.release();
        queueMonitor.decrement(request);
    }

    @Override
    public PushResult write(WebRequest request) {
        PushResult result = readWritePipeline.write(request);
        if (result == PushResult.SUCCESS) {
            queueMonitor.increment(request);
        }
        return result;
    }

    @Override
    public void handleResponse(WebRequest request) {
        networkToken.release();
        /*if (readWritePipeline instanceof BasicReadWritePipeline) {
            ((BasicReadWritePipeline) readWritePipeline).eliminateForRequest(request);
        }*/
        WebResponse response = request.getResponse();
        if (request == null) {
//            log.warn("任务尚未进行 -> {}", request.getUrl());
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
        }
        /*try {
            parseToken.acquire();
        } catch (InterruptedException e) {
            queueMonitor.decrement(request);
            return;
        }*/
//        responseList.offer(request);
//        parseToken.release();

        parseExecutor.submit(() -> {
            try {
                WebBeforeParseEvent event = new WebBeforeParseEvent();
                event.setContext(request.context());
                event.setRequest(request);
                postSyncEvent(event);
                webParser.parse(request, request.context());
                request.setEndAt(System.currentTimeMillis());
                rateResult.addSuccess(request.getEndAt() - request.getBeginAt());
                WebAfterParseEvent afterParseEvent = new WebAfterParseEvent();
                afterParseEvent.setContext(request.context());
                afterParseEvent.setRequest(request);
                postSyncEvent(afterParseEvent);
            } catch (WebException e) {
                WebParseExceptionEvent exceptionEvent = new WebParseExceptionEvent();
                exceptionEvent.setContext(request.context());
                exceptionEvent.setRequest(request);
                exceptionEvent.setThrowable(e);
                postAsyncEvent(exceptionEvent);
            } catch (Exception e) {
                AllExceptionEvent exceptionEvent = new AllExceptionEvent();
                exceptionEvent.setContext(request.context());
                exceptionEvent.setThrowable(e);
                postAsyncEvent(exceptionEvent);
            } finally {
                queueMonitor.decrement(request);
//                            parseToken.release();
            }
        });
    }

    @Override
    public void enableProxy(ProxyManager manager) {
        proxyPool.setManager(manager);
    }

    @Override
    public void init(int networkCount, int parseCount) {
        proxyPool.init();
        rateResult.start();
        networkToken = new Semaphore(networkCount);
//        parseToken = new Semaphore(crawlerContext.getParseThreadSize());
        parseExecutor = new CrawlThreadPoolExecutor(parseCount, "crawler-parse");
        /*runThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    WebRequest webRequest = responseList.take();
//                    parseToken.acquire();
                    parseExecutor.submit(() -> {
                        try {
                            WebBeforeParseEvent event = new WebBeforeParseEvent();
                            event.setContext(crawlerContext);
                            event.setRequest(webRequest);
                            postSyncEvent(event);
                            webParser.parse(webRequest, crawlerContext);
                            webRequest.setEndAt(System.currentTimeMillis());
                            rateResult.addSuccess(webRequest.getEndAt() - webRequest.getBeginAt());
                            WebAfterParseEvent afterParseEvent = new WebAfterParseEvent();
                            afterParseEvent.setContext(crawlerContext);
                            afterParseEvent.setRequest(webRequest);
                            postSyncEvent(afterParseEvent);
                        } catch (WebException e) {
                            WebParseExceptionEvent exceptionEvent = new WebParseExceptionEvent();
                            exceptionEvent.setContext(crawlerContext);
                            exceptionEvent.setRequest(webRequest);
                            exceptionEvent.setThrowable(e);
                            postAsyncEvent(exceptionEvent);
                        } catch (Exception e) {
                            AllExceptionEvent exceptionEvent = new AllExceptionEvent();
                            exceptionEvent.setContext(crawlerContext);
                            exceptionEvent.setThrowable(e);
                            postAsyncEvent(exceptionEvent);
                        } finally {
                            queueMonitor.decrement(webRequest);
//                            parseToken.release();
                        }
                    });
                } catch (InterruptedException e) {
                    break;
                }
            }
            log.warn("WebHandler执行终止");
        });
        runThread.setName("webHandler-run");
        // 设置为守护线程
        runThread.setDaemon(true);
        runThread.start();*/
        queueMonitor.init();
    }

    @Override
    public void postAsyncEvent(WebAsyncEvent event) {
        if (asyncMap.containsKey(event.getClass())) {
            asyncEventExecutor.submit(() -> asyncMap.get(event.getClass()).listen(event));
        } else {
            if (event instanceof WebErrorAsyncEvent) {
                WebErrorAsyncEvent webErrorAsyncEvent = (WebErrorAsyncEvent) event;
                if (asyncMap.containsKey(AllExceptionEvent.class)) {
                    asyncEventExecutor.submit(() -> {
                        AllExceptionEvent event1 = new AllExceptionEvent();
                        event1.setThrowable(webErrorAsyncEvent.getThrowable());
                        event1.setContext(webErrorAsyncEvent.getContext());
                        event1.setRequest(webErrorAsyncEvent.getRequest());
                        asyncMap.get(AllExceptionEvent.class).listen(event1);
                    });
                } else {
                    log.error("未捕获异常", webErrorAsyncEvent.getThrowable());
                }
            }
        }
    }

    @Override
    public void postSyncEvent(WebSyncEvent event) throws WebException {
        if (syncMap.containsKey(event.getClass())) {
            try {
                syncMap.get(event.getClass()).listen(event);
            } catch (WebException e) {
                throw e;
            } catch (Exception e) {
                WebException exception = new WebException(e);
                throw exception;
            }
        }
    }

    @Override
    public void close() {
        rateResult.close();
        /*if (runThread != null) {
            runThread.interrupt();
        }*/
        if (null != readWritePipeline) {
            readWritePipeline.close();
        }
        proxyPool.shutdownNow();
        asyncEventExecutor.shutdownNow();
        parseExecutor.shutdownNow();
        if (webRequester != null) {
            webRequester.close();
        }
    }
}
