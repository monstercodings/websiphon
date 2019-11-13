package top.codings.websiphon.factory.support;

import top.codings.websiphon.bean.WebProcessorDefinition;
import top.codings.websiphon.core.Crawler;
import top.codings.websiphon.core.context.BasicCrawlerContext;
import top.codings.websiphon.core.context.CrawlerContext;
import top.codings.websiphon.core.context.WebType;
import top.codings.websiphon.core.context.event.listener.WebAsyncEventListener;
import top.codings.websiphon.core.context.event.listener.WebSyncEventListener;
import top.codings.websiphon.core.parser.BasicWebParser;
import top.codings.websiphon.core.parser.WebParser;
import top.codings.websiphon.core.pipeline.BasicReadWritePipeline;
import top.codings.websiphon.core.pipeline.ReadWritePipeline;
import top.codings.websiphon.core.plugins.PluginFactory;
import top.codings.websiphon.core.plugins.WebPlugin;
import top.codings.websiphon.core.processor.WebProcessor;
import top.codings.websiphon.core.proxy.manager.ProxyManager;
import top.codings.websiphon.core.requester.NettyWebRequester;
import top.codings.websiphon.core.requester.WebRequester;
import top.codings.websiphon.core.support.BasicCrawler;
import top.codings.websiphon.factory.WebFactory;
import top.codings.websiphon.factory.bean.WebHandler;
import top.codings.websiphon.operation.QueueMonitor;
import top.codings.websiphon.util.ParameterizedTypeUtils;
import lombok.Data;

import java.util.LinkedList;
import java.util.List;

@Data
public class BasicWebFactory implements WebFactory {
    private WebRequester requester;
    private List<WebPlugin> plugins = new LinkedList<>();
    private List<WebProcessorDefinition> processorDefinitions = new LinkedList<>();
    private WebParser webParser;
    private ReadWritePipeline readWritePipeline;

    private BasicCrawler basicCrawler;
    private BasicCrawlerContext basicCrawlerContext;
    private BasicWebHandler webHandler = new BasicWebHandler();

    public static WebFactory create() {
        BasicWebFactory factory = new BasicWebFactory();
        factory.basicCrawler = new BasicCrawler();
        factory.basicCrawlerContext = factory.basicCrawler.getContext();
        factory.basicCrawlerContext.setWebHandler(factory.webHandler);
        return factory;
    }

    @Override
    public WebFactory setNetworkThread(int max) {
        basicCrawlerContext.setNetworkThreadSize(max);
        return this;
    }

    @Override
    public WebFactory setParseThread(int max) {
        basicCrawlerContext.setParseThreadSize(max);
        return this;
    }

    @Override
    public WebFactory addLast(WebType type) {
        if (type instanceof WebRequester) {
            requester = (WebRequester) type;
        } else if (type instanceof WebParser) {
            webParser = (WebParser) type;
        } else if ((type instanceof WebPlugin)) {
            plugins.add((WebPlugin) type);
        } else if (type instanceof ReadWritePipeline) {
            readWritePipeline = (ReadWritePipeline) type;
        } else if (type instanceof WebProcessor) {
            WebProcessor processor = (WebProcessor) type;
            WebProcessorDefinition definition = new WebProcessorDefinition();
            definition.setType(ParameterizedTypeUtils.getType(processor));
            definition.setProcessor(processor);
            processorDefinitions.add(definition);
        } else if (type instanceof WebSyncEventListener) {
            this.webHandler.getSyncMap().put(ParameterizedTypeUtils.getType(type), (WebSyncEventListener) type);
        } else if (type instanceof WebAsyncEventListener) {
            this.webHandler.getAsyncMap().put(ParameterizedTypeUtils.getType(type), (WebAsyncEventListener) type);
        }
        return this;
    }

    @Override
    public WebFactory enableProxy(ProxyManager manager) {
        webHandler.enableProxy(manager);
        return this;
    }

    @Override
    public WebFactory queueMonitor(QueueMonitor.TaskHandler monitor) {
        webHandler.getQueueMonitor().setMonitor(monitor);
        return this;
    }

    @Override
    public Crawler build() {
        if (requester == null) {
            requester = new NettyWebRequester();
        }
        if (readWritePipeline == null) {
            readWritePipeline = new BasicReadWritePipeline();
        }
        readWritePipeline.init();
        if (webParser == null) {
            webParser = new BasicWebParser();
        }
        for (WebPlugin plugin : plugins) {
            plugin.setWebFactory(this);
            for (Class targetInterface : plugin.getTargetInterface()) {
                if (targetInterface.isAssignableFrom(WebHandler.class)) {
                    webHandler = PluginFactory.create(plugin, webHandler);
                }
                if (targetInterface.isAssignableFrom(requester.getClass())) {
                    requester = PluginFactory.create(plugin, requester);
                }
                if (targetInterface.isAssignableFrom(WebParser.class)) {
                    webParser = PluginFactory.create(plugin, webParser);
                }
                if (targetInterface.isAssignableFrom(ReadWritePipeline.class)) {
                    readWritePipeline = PluginFactory.create(plugin, readWritePipeline);
                }
                if (targetInterface.isAssignableFrom(CrawlerContext.class)) {
                    basicCrawlerContext = PluginFactory.create(plugin, basicCrawlerContext);
                }
                for (WebProcessorDefinition processorDefinition : processorDefinitions) {
                    WebProcessor processor = processorDefinition.getProcessor();
                    if (targetInterface.isAssignableFrom(processor.getClass())) {
                        processor = PluginFactory.create(plugin, processor);
                    }
                    processorDefinition.setProcessor(processor);
                }
            }
        }
        processorDefinitions.forEach(definition -> webParser.addProcessor(definition));
        webHandler.setWebRequester(requester);
        webHandler.setWebParser(webParser);
        webHandler.setReadWritePipeline(readWritePipeline);
        webHandler.getQueueMonitor().setContext(basicCrawlerContext);
        basicCrawlerContext.setWebHandler(webHandler);
        basicCrawler.setContext(basicCrawlerContext);
        return basicCrawler;
    }
}
