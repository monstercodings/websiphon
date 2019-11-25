package top.codings.websiphon.factory.bean;

import java.util.concurrent.*;

public class CrawlThreadPoolExecutor extends ThreadPoolExecutor {
    private CrawlThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
    }

    private CrawlThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory);
    }

    private CrawlThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, RejectedExecutionHandler handler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, handler);
    }

    private CrawlThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory, RejectedExecutionHandler handler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
    }

    public CrawlThreadPoolExecutor(int corePoolSize, String poolName) {
//        this(corePoolSize, corePoolSize, 60, TimeUnit.SECONDS, new SynchronousQueue<>(), new CrawlThreadFactory(poolName));
        this(0, Integer.MAX_VALUE, 60, TimeUnit.SECONDS, new SynchronousQueue<>(), new CrawlThreadFactory(poolName));
    }
}
