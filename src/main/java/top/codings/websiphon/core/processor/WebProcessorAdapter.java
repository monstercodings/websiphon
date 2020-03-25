package top.codings.websiphon.core.processor;

import top.codings.websiphon.bean.WebRequest;
import top.codings.websiphon.core.context.CrawlerContext;
import top.codings.websiphon.exception.WebParseException;

public abstract class WebProcessorAdapter<IN extends WebRequest> implements WebProcessor<IN> {

    @Override
    public void fireProcess(IN request) throws WebParseException {
        WebProcessor.BOOLEAN_THREAD_LOCAL.set(true);
    }
}
