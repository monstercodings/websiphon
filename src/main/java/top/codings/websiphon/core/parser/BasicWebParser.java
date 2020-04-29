package top.codings.websiphon.core.parser;

import lombok.Setter;
import top.codings.websiphon.bean.WebProcessorDefinition;
import top.codings.websiphon.bean.WebRequest;
import top.codings.websiphon.core.context.event.async.AllExceptionEvent;
import top.codings.websiphon.core.context.event.async.WebParseExceptionEvent;
import top.codings.websiphon.core.context.event.sync.WebBeforeParseEvent;
import top.codings.websiphon.core.processor.WebProcessor;
import top.codings.websiphon.exception.StopWebRequestException;
import top.codings.websiphon.exception.WebException;
import top.codings.websiphon.exception.WebParseException;
import top.codings.websiphon.factory.support.BasicWebHandler;

import java.util.*;

public class BasicWebParser implements WebParser {
    private List<WebProcessorDefinition> processorDefinitions = new LinkedList<>();
    @Setter
    private BasicWebHandler handler;

    @Override
    public void parse(WebRequest request) throws WebParseException {
        WebBeforeParseEvent event = new WebBeforeParseEvent();
        event.setRequest(request);
        handler.postSyncEvent(event);
        Iterator<WebProcessorDefinition> iterator = processorDefinitions.iterator();
        while (iterator.hasNext()) {
            WebProcessorDefinition definition = iterator.next();
            if (definition.getType().isAssignableFrom(request.getClass())) {
                definition.getProcessor().process(request);
            } else {
                definition.getProcessor().fireProcess(request);
            }
            if (!WebProcessor.BOOLEAN_THREAD_LOCAL.get()) {
                break;
            }
            WebProcessor.BOOLEAN_THREAD_LOCAL.set(false);
        }
        request.status(WebRequest.Status.FINISH);
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
