package top.codings.websiphon.core.pipeline;

import top.codings.websiphon.bean.PushResult;
import top.codings.websiphon.bean.WebRequest;
import top.codings.websiphon.util.HttpOperator;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class BasicReadWritePipeline implements ReadWritePipeline {
    public static final String ID = "BasicReadWritePipeline";

    private LinkedTransferQueue<WebRequest> requests = new LinkedTransferQueue<>();
    private Map<String, RequestCompose> hostStat = new ConcurrentHashMap<>();
    private Semaphore token;
    //    private int maxTask;
    private int currentRequestByHost;
    private AtomicInteger size = new AtomicInteger(0);

    public BasicReadWritePipeline() {
        this(2);
    }

    public BasicReadWritePipeline(int currentRequestByHost) {
        if (currentRequestByHost > Integer.MAX_VALUE) {
            currentRequestByHost = Integer.MAX_VALUE;
        }
        this.currentRequestByHost = currentRequestByHost;
    }

    @Override
    public void init() {
        token = new Semaphore(Integer.MAX_VALUE);
        Thread thread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                hostStat.forEach((host, compose) -> {
                    if (compose.at.get() < currentRequestByHost) {
                        WebRequest request = compose.queue.poll();
                        if (request != null) {
                            compose.at.getAndIncrement();
                            requests.offer(request);
                        }
                    }
                });
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * 消除一个对应域名的并发数
     *
     * @param request
     */
    public void eliminateForRequest(WebRequest request) {
        HttpOperator.HttpProtocol protocol = HttpOperator.resolve(request.getUrl());
        hostStat.get(protocol.getHost()).at.getAndDecrement();
    }

    @Override
    public WebRequest read() throws InterruptedException {
        WebRequest request = requests.take();
        size.getAndDecrement();
        token.release();
        return request;
    }

    @Override
    public PushResult write(WebRequest webRequest) {
        if (token.tryAcquire()) {
            HttpOperator.HttpProtocol protocol = HttpOperator.resolve(webRequest.getUrl());
            RequestCompose rc = hostStat.get(protocol.getHost());
            if (rc == null) {
                synchronized (this) {
                    rc = hostStat.get(protocol.getHost());
                    if (rc == null) {
                        rc = new RequestCompose();
                        hostStat.put(protocol.getHost(), rc);
                    }
                }
            }
            rc.queue.offer(webRequest);
//            queue.offer(webRequest);
            size.getAndIncrement();
            return PushResult.SUCCESS;
        }
        return PushResult.FULL_QUEUE;
    }

    @Override
    public int size() {
        return size.get();
    }

    @Override
    public void close() {
        clear();
        log.warn("管道关闭");
    }

    @Override
    public void clear() {
        requests.clear();
        token = new Semaphore(Integer.MAX_VALUE);
    }

    @Override
    public String getId() {
        return ID;
    }

    private class RequestCompose {
        AtomicInteger at = new AtomicInteger(0);
        LinkedTransferQueue<WebRequest> queue = new LinkedTransferQueue<>();
    }
}
