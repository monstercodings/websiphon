package top.codings.websiphon.core.schedule.support;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import top.codings.websiphon.bean.WebRequest;
import top.codings.websiphon.core.pipeline.ReadWritePipeline;
import top.codings.websiphon.core.schedule.RequestScheduler;
import top.codings.websiphon.operation.QueueMonitor;
import top.codings.websiphon.util.HttpOperator;

import java.util.Map;
import java.util.concurrent.*;

/**
 * 爬虫任务调度器
 */
@NoArgsConstructor
@Slf4j
public class BasicRequestScheduler implements RequestScheduler {
    private volatile boolean empty = true;
    private int maxRequestCount;
    private Map<String, HostAndTask> hostAndTasks = new ConcurrentHashMap<>();
    private LinkedTransferQueue<WebRequest> tasks = new LinkedTransferQueue<>();
    private ExecutorService executorService = Executors.newCachedThreadPool();
    private QueueMonitor queueMonitor;

    public BasicRequestScheduler(int maxRequestCount) {
        this.maxRequestCount = maxRequestCount;
    }

    @Override
    public void init(QueueMonitor queueMonitor) {
        this.queueMonitor = queueMonitor;
        executorService.submit(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    schedule();
                } catch (InterruptedException e) {
                    return;
                }
            }
        });
    }

    @Override
    public void handle(WebRequest request) {
        HttpOperator.HttpProtocol protocol = HttpOperator.resolve(request.uri());
        HostAndTask hostAndTask = hostAndTasks.get(protocol.getHost());
        if (null == hostAndTask) {
            synchronized (this) {
                hostAndTask = hostAndTasks.get(protocol.getHost());
                if (null == hostAndTask) {
                    hostAndTask = new HostAndTask();
                    hostAndTasks.put(protocol.getHost(), hostAndTask);
                }
            }
        }
        hostAndTask.queue.offer(request);
        queueMonitor.increment(request);
        empty = false;
    }

    @Override
    public void release(WebRequest request) {
        HttpOperator.HttpProtocol protocol = HttpOperator.resolve(request.uri());
        HostAndTask hostAndTask = hostAndTasks.get(protocol.getHost());
        if (null != hostAndTask) {
            hostAndTask.token.release();
        }
    }

    @Override
    public WebRequest take() throws InterruptedException {
        return tasks.take();
    }

    private void schedule() throws InterruptedException {
        boolean next = false;
        for (Map.Entry<String, HostAndTask> entry : hostAndTasks.entrySet()) {
            HostAndTask hostAndTask = entry.getValue();
            if (hostAndTask.queue.isEmpty()) {
                continue;
            }
            if (!hostAndTask.token.tryAcquire()) {
                continue;
            }
            WebRequest request = hostAndTask.queue.poll();
            tasks.offer(request);
            next = true;
        }
        if (!next) {
            TimeUnit.SECONDS.sleep(1);
        }
    }

    @Override
    public void close() throws Exception {
        executorService.shutdownNow();
    }

    private class HostAndTask {
        private Semaphore token = new Semaphore(maxRequestCount);
        private LinkedTransferQueue<WebRequest> queue = new LinkedTransferQueue<>();
    }

}
