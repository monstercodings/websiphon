package top.codings.websiphon.operation;

import top.codings.websiphon.bean.WebRequest;
import top.codings.websiphon.core.context.BasicCrawlerContext;
import top.codings.websiphon.core.context.CrawlerContext;
import top.codings.websiphon.core.context.event.async.WebMonitorExceptionEvent;
import com.google.common.collect.Sets;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;

@Slf4j
public class QueueMonitor {
    //    private AtomicLong taskCount = new AtomicLong(0);
    @Setter
    private TaskHandler monitor;
    @Setter
    private CrawlerContext context;

    private Set<WebRequest> requestHolder = Sets.newConcurrentHashSet();
    //    private AtomicInteger size = new AtomicInteger(0);
    private long maxTime = 5 * 60 * 1000l;
    private long lastPushTime = System.currentTimeMillis();
    private long lastPullTime = System.currentTimeMillis();

    public void increment(WebRequest request) {
        if (monitor == null) {
            return;
        }
//        size.getAndIncrement();
        boolean add = requestHolder.add(request);
        if (!add) {
            WebMonitorExceptionEvent event = new WebMonitorExceptionEvent(request);
            event.setThrowable(new IllegalMonitorStateException("新增的队列监视对象已存在"));
            event.setSize(requestHolder.size());
            context.postAsyncEvent(event);
            log.trace("新增的队列监视对象已存在");
            return;
        }
        lastPushTime = System.currentTimeMillis();
    }

    public void decrement(WebRequest request) {
        if (monitor == null) {
            return;
        }
//        size.getAndDecrement();
        boolean remove = requestHolder.remove(request);
        int size = requestHolder.size();
        if (!remove) {
            WebMonitorExceptionEvent event = new WebMonitorExceptionEvent(request);
            event.setThrowable(new IllegalMonitorStateException("移除的队列监视对象不存在"));
            event.setSize(size);
            context.postAsyncEvent(event);
            log.trace("移除的队列监视对象不存在");
            return;
        }
        lastPullTime = System.currentTimeMillis();
        if (size == 0) {
            log.trace("队列任务已全部执行完毕");
            if (null != monitor) {
                ((BasicCrawlerContext) context).getWebHandler().getAsyncEventExecutor().submit(() -> monitor.handle(context, requestHolder, false));
            } else {
                log.trace("监视器为空，无触发任务");
            }
        } else if (size < 0) {
            log.error("队列任务统计发生异常[重复计数] -> 当前[{}] | {}", requestHolder.size(), request.getUrl());
        }
    }

    public void init() {
        if (null == monitor) {
            return;
        }
        ((BasicCrawlerContext) context).getWebHandler().getAsyncEventExecutor().submit(() -> {
            while (!Thread.currentThread().isInterrupted() && monitor != null) {
                try {
                    Thread.sleep(maxTime + 500l);
                    long now = System.currentTimeMillis();
                    int pipeSize = ((BasicCrawlerContext) context).getWebHandler().getReadWritePipeline().size();
                    int reqSize = ((BasicCrawlerContext) context).getWebHandler().getWebRequester().size();
                    log.trace("队列内任务数量 -> {} | 正在请求的数量 -> {} | 监视器对象数量 -> {}", pipeSize, reqSize, requestHolder.size());
                    if (!requestHolder.isEmpty() && (now - lastPushTime) > maxTime && (now - lastPullTime) > maxTime) {
                        if (pipeSize == 0) {
                            log.trace("超过时限，强制执行清理任务");
                            monitor.handle(context, requestHolder, true);
                        }
                    }
                } catch (InterruptedException e) {
                    break;
                } catch (Exception e) {
                    log.error("监视队列发生未知异常", e);
                }
            }
            log.debug("结束监控队列任务");
        });
    }

    public interface TaskHandler {
        void handle(CrawlerContext ctx, Set<WebRequest> requestHolder, boolean force);
    }

}
