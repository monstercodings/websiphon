package top.codings.websiphon.factory.bean;

import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

@RequiredArgsConstructor
public class CrawlThreadFactory implements ThreadFactory {
    @NonNull
    private String name;
    private AtomicInteger threadNumber = new AtomicInteger(1);

    @Override
    public Thread newThread(Runnable r) {
        Thread t = new Thread(Thread.currentThread().getThreadGroup(), r,
                String.format("%s-%d", name, threadNumber.getAndIncrement()),
                0);
        // 设置为守护线程
        t.setDaemon(true);
        t.setPriority(Thread.NORM_PRIORITY);
        return t;
    }
}
