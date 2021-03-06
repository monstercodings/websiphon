package top.codings.websiphon.core.schedule.support;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import top.codings.websiphon.bean.WebRequest;
import top.codings.websiphon.core.schedule.RequestScheduler;
import top.codings.websiphon.util.HttpOperator;

import java.util.Map;
import java.util.Optional;
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

    public BasicRequestScheduler(int maxRequestCount) {
        this.maxRequestCount = maxRequestCount;
    }

    @Override
    public void init() {
        executorService.submit(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    schedule();
                } catch (InterruptedException e) {
                    log.error("爬虫调度器被中断 -> {}", e.getLocalizedMessage());
                    return;
                } catch (Exception e) {
                    log.error("爬虫调度器出现异常", e);
                }
            }
            log.debug("爬虫调度器结束运行");
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
            hostAndTask.take().ifPresent(tasks::offer);
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

        private Optional<WebRequest> take() {
            if (queue.peek() == null || !token.tryAcquire()) {
                return Optional.empty();
            }
            return Optional.ofNullable(queue.poll());
        }
    }

}
