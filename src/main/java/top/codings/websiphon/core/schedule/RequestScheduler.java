package top.codings.websiphon.core.schedule;

import top.codings.websiphon.bean.WebRequest;
import top.codings.websiphon.operation.QueueMonitor;

public interface RequestScheduler {
    void init(QueueMonitor queueMonitor);

    void handle(WebRequest request);

    /**
     * 释放占用的token
     *
     * @param request
     */
    void release(WebRequest request);

    WebRequest take() throws InterruptedException;

    void close() throws Exception;
}
