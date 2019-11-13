package top.codings.websiphon.core.parser;

import top.codings.websiphon.bean.WebProcessorDefinition;
import top.codings.websiphon.bean.WebRequest;
import top.codings.websiphon.core.context.CrawlerContext;
import top.codings.websiphon.core.processor.WebProcessor;
import top.codings.websiphon.exception.WebParseException;

import java.util.*;

public class BasicWebParser implements WebParser {
    private List<WebProcessorDefinition> processorDefinitions = new LinkedList<>();

    @Override
    public void parse(WebRequest param, CrawlerContext context) throws WebParseException {
        Iterator<WebProcessorDefinition> iterator = processorDefinitions.iterator();
        while (iterator.hasNext()) {
            WebProcessorDefinition definition = iterator.next();
            if (definition.getType().isAssignableFrom(param.getClass())) {
                definition.getProcessor().process(param, context);
            } else {
                definition.getProcessor().fireProcess(param, context);
            }
            if (!WebProcessor.BOOLEAN_THREAD_LOCAL.get()) {
                break;
            }
            WebProcessor.BOOLEAN_THREAD_LOCAL.set(false);
        }
    }

    @Override
    public WebParser addProcessor(WebProcessorDefinition definition) {
        processorDefinitions.add(definition);
        return this;
    }

    @Override
    public List<WebProcessor> getProcessors() {
        List<WebProcessor> list = new ArrayList<>(processorDefinitions.size());
        processorDefinitions.forEach(definition -> list.add(definition.getProcessor()));
        return Collections.unmodifiableList(list);
    }
}
