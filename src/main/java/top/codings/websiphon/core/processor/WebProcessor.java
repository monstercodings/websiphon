package top.codings.websiphon.core.processor;

import top.codings.websiphon.bean.WebProcessorDefinition;
import top.codings.websiphon.bean.WebRequest;
import top.codings.websiphon.core.context.CrawlerContext;
import top.codings.websiphon.core.context.WebType;
import top.codings.websiphon.exception.WebParseException;

import java.util.Iterator;

public interface WebProcessor<IN extends WebRequest> extends WebType {
    ThreadLocal<Iterator<WebProcessorDefinition>> ITERATOR_THREAD_LOCAL = new ThreadLocal<>();
    ThreadLocal<Boolean> BOOLEAN_THREAD_LOCAL = ThreadLocal.withInitial(() -> false);

    void process(IN request, CrawlerContext context) throws WebParseException;

    void fireProcess(IN request, CrawlerContext context) throws WebParseException;

    /*static void process(Iterator<WebProcessorDefinition> iterator, WebRequest param, CrawlerContext context) throws WebParseException {
        if (iterator.hasNext()) {
            WebProcessorDefinition definition = iterator.next();
            WebProcessor processor = definition.getProcessor();
            processor.setIterator(iterator);
            if (definition.getType().isAssignableFrom(param.getClass())) {
                processor.process(param, context);
            } else {
                processor.fireProcess(param, context);
            }
        }
    }*/
}
