package top.codings.websiphon.core.pipeline;

import lombok.extern.slf4j.Slf4j;
import top.codings.websiphon.bean.PushResult;
import top.codings.websiphon.bean.WebRequest;

import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.Semaphore;

@Slf4j
public abstract class ReadWritePipelineAdapter<T extends WebRequest, P> implements ReadWritePipeline<T, P> {
    protected static Semaphore taskToken = new Semaphore(0);
    protected LinkedTransferQueue<T> queue = new LinkedTransferQueue();

    @Override
    public T read() throws InterruptedException {
        taskToken.acquire();
        return queue.poll();
    }

    @Override
    public PushResult write(T webRequest) throws InterruptedException {
        /*queue.transfer(webRequest);
        return PushResult.SUCCESS;*/
        boolean success = queue.offer(webRequest);
        if (success) {
            taskToken.release();
            return PushResult.SUCCESS;
        }
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
