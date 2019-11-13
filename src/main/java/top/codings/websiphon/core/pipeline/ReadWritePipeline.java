package top.codings.websiphon.core.pipeline;

import top.codings.websiphon.bean.PushResult;
import top.codings.websiphon.bean.WebRequest;
import top.codings.websiphon.core.context.WebType;

public interface ReadWritePipeline<IN extends WebRequest> extends WebType {
    void init();

    IN read() throws InterruptedException;

    PushResult write(IN webRequest);

    int size();

    void close();

    void clear();
}
