package top.codings.websiphon.core.pipeline;

import lombok.extern.slf4j.Slf4j;
import top.codings.websiphon.bean.PushResult;
import top.codings.websiphon.bean.WebRequest;

import java.util.concurrent.LinkedTransferQueue;

@Slf4j
public abstract class ReadWritePipelineAdapter<T extends WebRequest, P> implements ReadWritePipeline<T, P> {
    protected LinkedTransferQueue<T> queue = new LinkedTransferQueue();

    @Override
    public T read() throws InterruptedException {
        return queue.take();
    }

    @Override
    public PushResult write(T webRequest) throws InterruptedException {
        boolean success = queue.offer(webRequest);
        if (success) {
            log.debug("推送完成");
            return PushResult.SUCCESS;
        }
        log.debug("推送失败");
        return PushResult.FULL_QUEUE;
    }

    @Override
    public void close() throws Exception {
        clear();
    }

    @Override
    public void clear() {
        queue.clear();
    }
}
