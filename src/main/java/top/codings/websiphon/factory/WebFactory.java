package top.codings.websiphon.factory;

import top.codings.websiphon.bean.WebRequest;
import top.codings.websiphon.core.Crawler;
import top.codings.websiphon.core.context.CrawlerContext;
import top.codings.websiphon.core.context.WebType;
import top.codings.websiphon.core.pipeline.ReadWritePipeline;
import top.codings.websiphon.core.proxy.manager.ProxyManager;
import top.codings.websiphon.factory.bean.WebHandler;
import top.codings.websiphon.operation.QueueMonitor;

import java.util.function.Consumer;

public interface WebFactory<T extends WebRequest> {
    WebFactory<T> setNetworkThread(int max);

    WebFactory<T> setParseThread(int max);

    WebFactory<T> setPermitForHost(int permitForHost);

    WebFactory<T> addLast(WebType type);

    WebFactory<T> enableProxy(ProxyManager manager);

    WebFactory<T> queueMonitor(QueueMonitor.TaskHandler monitor);

    Crawler build();

}
