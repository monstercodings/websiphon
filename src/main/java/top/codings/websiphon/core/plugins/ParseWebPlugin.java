package top.codings.websiphon.core.plugins;/*
package top.codings.websiphon.core.plugins;

import top.codings.websiphon.bean.MethodDesc;
import top.codings.websiphon.bean.ReturnPoint;
import top.codings.websiphon.bean.WebProcessorDefinition;
import top.codings.websiphon.bean.WebRequest;
import top.codings.websiphon.core.context.CrawlerContext;
import top.codings.websiphon.core.parser.WebParser;
import top.codings.websiphon.core.processor.WebProcessor;
import top.codings.websiphon.exception.WebException;
import top.codings.websiphon.factory.WebFactory;
import top.codings.websiphon.factory.support.BasicWebFactory;

import java.util.Iterator;
import java.util.List;

public class ParseWebPlugin implements WebPlugin {
    private List<WebProcessorDefinition> processorDefinitions;

    @Override
    public Object[] before(Object[] params, ReturnPoint point) throws WebException {
        return params;
    }

    @Override
    public Object after(Object proxy, Object[] params, Object result, MethodDesc methodDesc, ReturnPoint point) throws WebException {
        Object obj = null;
        switch (methodDesc.getName()) {
            case "parse":
                parse((WebRequest) params[0], (CrawlerContext) params[1]);
                break;
            case "addProcessor":
                addProcessor((WebProcessorDefinition) params[0]);
                obj = proxy;
                break;
            case "getProcessors":
                obj = getProcessors();
                break;
        }
        return obj;
    }

    @Override
    public Class[] getTargetInterface() {
        return new Class[]{WebParser.class};
    }

    @Override
    public MethodDesc[] getMethods() {
        return new MethodDesc[]{
                new MethodDesc("parse", new Class[]{WebRequest.class, CrawlerContext.class}),
                new MethodDesc("addProcessor", new Class[]{WebProcessorDefinition.class}),
                new MethodDesc("getProcessors", new Class[]{List.class})
        };
    }

    @Override
    public void setWebFactory(WebFactory webFactory) {
        processorDefinitions = ((BasicWebFactory) webFactory).getProcessorDefinitions();
    }

    private void parse(WebRequest param, CrawlerContext context) throws WebException {
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
        */
/*WebProcessor.ITERATOR_THREAD_LOCAL.set(processorDefinitions.iterator());
        processorDefinitions.get(0).getProcessor().fireProcess(param, context);
        WebProcessor.ITERATOR_THREAD_LOCAL.set(null);*//*

    }

    private void addProcessor(WebProcessorDefinition processor) {
        if (processorDefinitions == null) {
            throw new RuntimeException("处理器链为空");
        }
        processorDefinitions.add(processor);
    }

    private List<WebProcessorDefinition> getProcessors() {
        return processorDefinitions;
    }
}
*/
