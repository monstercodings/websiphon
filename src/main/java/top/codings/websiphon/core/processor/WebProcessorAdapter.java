package top.codings.websiphon.core.processor;

import top.codings.websiphon.bean.WebRequest;
import top.codings.websiphon.core.context.CrawlerContext;
import top.codings.websiphon.exception.WebParseException;

public abstract class WebProcessorAdapter<IN extends WebRequest> implements WebProcessor<IN> {

    @Override
    public void fireProcess(IN request, CrawlerContext context) throws WebParseException {
        WebProcessor.BOOLEAN_THREAD_LOCAL.set(true);
        /*if (!ITERATOR_THREAD_LOCAL.get().hasNext()) {
            return;
        }
        WebProcessorDefinition definition = ITERATOR_THREAD_LOCAL.get().next();
        if (definition.getType().isAssignableFrom(request.getClass())) {
            definition.getProcessor().process(request, context);
        } else {
            definition.getProcessor().fireProcess(request, context);
        }*/

    }
}
