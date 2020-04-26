package top.codings.websiphon.factory.support;

import lombok.Data;
import top.codings.websiphon.bean.WebProcessorDefinition;
import top.codings.websiphon.core.Crawler;
import top.codings.websiphon.core.context.BasicCrawlerContext;
import top.codings.websiphon.core.context.CrawlerContext;
import top.codings.websiphon.core.context.WebType;
import top.codings.websiphon.core.context.event.listener.WebAsyncEventListener;
import top.codings.websiphon.core.context.event.listener.WebSyncEventListener;
import top.codings.websiphon.core.parser.BasicWebParser;
import top.codings.websiphon.core.parser.WebParser;
import top.codings.websiphon.core.pipeline.ReadWritePipeline;
import top.codings.websiphon.core.plugins.AspectInfo;
import top.codings.websiphon.core.plugins.PluginFactory;
import top.codings.websiphon.core.plugins.WebPlugin;
import top.codings.websiphon.core.plugins.WebPluginPro;
import top.codings.websiphon.core.processor.WebProcessor;
import top.codings.websiphon.core.requester.BasicWebRequester;
import top.codings.websiphon.core.requester.WebRequester;
import top.codings.websiphon.core.schedule.RequestScheduler;
import top.codings.websiphon.core.schedule.support.BasicRequestScheduler;
import top.codings.websiphon.core.support.BasicCrawler;
import top.codings.websiphon.factory.WebFactory;
import top.codings.websiphon.factory.bean.WebHandler;
import top.codings.websiphon.util.ParameterizedTypeUtils;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

@Data
public class BasicWebFactory implements WebFactory {
    private WebRequester requester;
    private List<WebPluginPro> plugins = new LinkedList<>();
    private List<WebProcessorDefinition> processorDefinitions = new LinkedList<>();
    private WebParser webParser;
    private List<ReadWritePipeline> readWritePipelines = new LinkedList<>();
    private BasicRequestScheduler scheduler;
    private int permitForHost = 2;

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
    public WebFactory setPermitForHost(int permitForHost) {
        this.permitForHost = permitForHost;
        return this;
    }

    @Override
    public WebFactory addLast(WebType type) {
        if (type instanceof WebRequester) {
            requester = (WebRequester) type;
        } else if (type instanceof WebParser) {
            webParser = (WebParser) type;
        } else if ((type instanceof WebPluginPro)) {
            WebPluginPro plugin = (WebPluginPro) type;
            plugins.add(plugin);
        } else if (type instanceof ReadWritePipeline) {
            readWritePipelines.add((ReadWritePipeline) type);
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
    public Crawler build() {
        plugins.sort(Comparator.comparingInt(WebPluginPro::index).reversed());
        if (requester == null) {
            requester = new BasicWebRequester();
        }
        if (webParser == null) {
            webParser = new BasicWebParser();
        }
        scheduler = new BasicRequestScheduler(permitForHost);
        for (WebPluginPro plugin : plugins) {
//            plugin.setWebFactory(this);
            plugin.init();
            AspectInfo[] aspectInfos = plugin.aspectInfos();
            if (null == aspectInfos) {
                continue;
            }
            for (AspectInfo aspectInfo : aspectInfos) {
                Class clazz = aspectInfo.getClazz();
                if (WebHandler.class.isAssignableFrom(clazz)) {
                    webHandler = PluginFactory.create0(plugin, webHandler);
                }
                if (WebRequester.class.isAssignableFrom(clazz)) {
                    requester = PluginFactory.create0(plugin, requester);
                }
                if (WebParser.class.isAssignableFrom(clazz)) {
                    webParser = PluginFactory.create0(plugin, webParser);
                }
                if (ReadWritePipeline.class.isAssignableFrom(clazz)) {
                    readWritePipelines.replaceAll(pipeline -> {
                        if (clazz.isAssignableFrom(pipeline.getClass())) {
                            return PluginFactory.create0(plugin, pipeline);
                        }
                        return pipeline;
                    });
                }
                if (CrawlerContext.class.isAssignableFrom(clazz)) {
                    basicCrawlerContext = PluginFactory.create0(plugin, basicCrawlerContext);
                }
                if (RequestScheduler.class.isAssignableFrom(clazz)) {
                    scheduler = PluginFactory.create0(plugin, scheduler);
                }
                for (WebProcessorDefinition processorDefinition : processorDefinitions) {
                    WebProcessor processor = processorDefinition.getProcessor();
                    if (clazz.isAssignableFrom(processor.getClass())) {
                        processor = PluginFactory.create0(plugin, processor);
                    }
                    processorDefinition.setProcessor(processor);
                }
            }
            /*for (Class targetInterface : plugin.getTargetInterface()) {
                if (WebHandler.class.isAssignableFrom(targetInterface)) {
                    webHandler = PluginFactory.create(plugin, webHandler);
                }
                if (WebRequester.class.isAssignableFrom(targetInterface)) {
                    requester = PluginFactory.create(plugin, requester);
                }
                if (WebParser.class.isAssignableFrom(targetInterface)) {
                    webParser = PluginFactory.create(plugin, webParser);
                }
                if (ReadWritePipeline.class.isAssignableFrom(targetInterface)) {
                    readWritePipelines.replaceAll(pipeline -> {
                        if (targetInterface.isAssignableFrom(pipeline.getClass())) {
                            return PluginFactory.create(plugin, pipeline);
                        }
                        return pipeline;
                    });
                }
                if (CrawlerContext.class.isAssignableFrom(targetInterface)) {
                    basicCrawlerContext = PluginFactory.create(plugin, basicCrawlerContext);
                }
                if (RequestScheduler.class.isAssignableFrom(targetInterface)) {
                    scheduler = PluginFactory.create(plugin, scheduler);
                }
                for (WebProcessorDefinition processorDefinition : processorDefinitions) {
                    WebProcessor processor = processorDefinition.getProcessor();
                    if (targetInterface.isAssignableFrom(processor.getClass())) {
                        processor = PluginFactory.create(plugin, processor);
                    }
                    processorDefinition.setProcessor(processor);
                }
            }*/
        }
        processorDefinitions.forEach(definition -> webParser.addProcessor(definition));
        webHandler.setWebRequester(requester);
        webHandler.setWebParser(webParser);
        webHandler.setReadWritePipelines(readWritePipelines);
        webHandler.setScheduler(scheduler);
        webHandler.setPlugins(plugins);
        basicCrawlerContext.setWebHandler(webHandler);
        basicCrawler.setContext(basicCrawlerContext);
        return basicCrawler;
    }
}
