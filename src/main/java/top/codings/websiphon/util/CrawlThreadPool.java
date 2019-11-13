package top.codings.websiphon.util;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class CrawlThreadPool {
    private final static ExecutorService EXECUTOR = Executors.newCachedThreadPool(new ThreadFactory() {
        private final AtomicInteger threadNumber = new AtomicInteger(1);

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(Thread.currentThread().getThreadGroup(), r,
                    "爬虫簇池-" + threadNumber.getAndIncrement(),
                    0);
            if (t.isDaemon())
                t.setDaemon(false);
            if (t.getPriority() != Thread.NORM_PRIORITY)
                t.setPriority(Thread.NORM_PRIORITY);
            return t;
        }
    });

    private final static Map<String, Future> FUTURE_MAP = new ConcurrentHashMap<>();

    public static String newTask(Runnable task) {
        String id = UUID.randomUUID().toString().replace("-", "").toLowerCase();
        newTask(id, task);
        return id;
    }

    public static String newTask(Callable task) {
        String id = UUID.randomUUID().toString().replace("-", "").toLowerCase();
        newTask(id, task);
        return id;
    }

    public static void newTask(String id, Runnable task) {
        FUTURE_MAP.put(id, EXECUTOR.submit(task));
    }

    public static void newTask(String id, Callable task) {
        FUTURE_MAP.put(id, EXECUTOR.submit(task));
    }

    public static void interrupt(String id) {
        Optional.ofNullable(FUTURE_MAP.get(id)).ifPresent(future -> future.cancel(true));
    }

    public static void shutdownNow() {
        EXECUTOR.shutdownNow();
    }
}
