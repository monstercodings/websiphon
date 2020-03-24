package top.codings.websiphon.core.pipeline;

import top.codings.websiphon.bean.PushResult;
import top.codings.websiphon.bean.WebRequest;
import top.codings.websiphon.core.context.WebType;

import java.util.Collection;
import java.util.List;

public interface ReadWritePipeline<W extends WebRequest, P> extends WebType {
    void init();

    W read() throws InterruptedException;

    PushResult write(W webRequest) throws InterruptedException;

    void conversion(P param, List<W> out);

    void close() throws Exception;

    void clear();
}
