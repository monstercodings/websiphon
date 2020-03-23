package top.codings.websiphon.bean;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Data
@Slf4j
public class RateResult {
    protected int second;
    /**
     * 总消费消息数量
     */
    protected AtomicLong totalMessage = new AtomicLong(0);

    /**
     * 总消费QPS（无论成败）
     */
    protected volatile long everySecondMessage;
    /**
     * 一条消息成功消费耗时
     */
    protected volatile double timeConsuming;

    /**
     * 请求成功总花费时间
     */
    protected AtomicLong successTotalTime = new AtomicLong(0);

    /**
     * 成功完成次数
     */
    protected AtomicLong successCount = new AtomicLong(0);

//    private Semaphore token;

    /**
     * 成功消费消息的QPS
     */
    protected volatile long everySecondCount;

    protected Map<WebResponse.Result, AtomicLong> resultStat = new ConcurrentHashMap<>();

    protected Thread thread;

    public void incrementResult(WebRequest webRequest) {
        if (null == webRequest) {
            return;
        } else if (webRequest.response() == null) {
            return;
        } else if (webRequest.response().getResult() == null) {
            return;
        }
        WebResponse.Result result = webRequest.response().getResult();
        if (result == null) {
            return;
        }
        AtomicLong stat = resultStat.get(result);
        if (null == stat) {
            synchronized (resultStat) {
                stat = resultStat.get(result);
                if (null == stat) {
                    stat = new AtomicLong();
                    resultStat.put(result, stat);
                }
            }
        }
        stat.incrementAndGet();
    }

    public void addSuccess(long time) {
        successTotalTime.addAndGet(time);
        successCount.incrementAndGet();
        if (successCount.get() > 0) {
            double timeSecond = successTotalTime.get() * 1.0 / 1000;
            timeConsuming = timeSecond / successCount.get();
        }
    }

    public void addTotal() {
        totalMessage.incrementAndGet();
    }

    public void start() {
        thread = new Thread(() -> {
            long previousSecondCount = 0;
            long previousSecondMessage = 0;
            synchronized (this) {
                try {
                    while (!Thread.currentThread().isInterrupted()) {
                        if (second <= 0) {
                            second = 5;
                        }
                        this.wait(second * 1000);
                        everySecondCount = (successCount.get() - previousSecondCount) / second;
                        previousSecondCount = successCount.get();
                        everySecondMessage = (totalMessage.get() - previousSecondMessage) / second;
                        previousSecondMessage = totalMessage.get();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            log.warn("速率统计功能关闭");
        });
        thread.setName("QPS统计");
        thread.start();
    }

    public void close() {
        if (null != thread) {
            thread.interrupt();
        }
    }
}
