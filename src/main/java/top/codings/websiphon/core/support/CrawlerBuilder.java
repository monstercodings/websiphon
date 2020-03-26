package top.codings.websiphon.core.support;

import top.codings.websiphon.core.Crawler;
import top.codings.websiphon.core.context.WebType;
import top.codings.websiphon.core.context.event.listener.WebAsyncEventListener;
import top.codings.websiphon.core.context.event.listener.WebSyncEventListener;
import top.codings.websiphon.factory.support.BasicWebFactory;

import java.util.List;

public class CrawlerBuilder {

    private BasicWebFactory webFactory = (BasicWebFactory) BasicWebFactory.create();

    private CrawlerBuilder() {
    }

    public final static CrawlerBuilder create() {
        return new CrawlerBuilder();
    }

    public CrawlerBuilder addLast(WebType type) {
        if (null == type) {
            return this;
        }
        webFactory.addLast(type);
        return this;
    }

    public CrawlerBuilder addLast(WebType... types) {
        for (WebType type : types) {
            addLast(type);
        }
        return this;
    }

    public CrawlerBuilder addLast(List<WebType> types) {
        if (null != types) {
            for (WebType type : types) {
                addLast(type);
            }
        }
        return this;
    }

    public CrawlerBuilder setNetworkThread(int size) {
        webFactory.setNetworkThread(size);
        return this;
    }

    public CrawlerBuilder setPermitForHost(int permitForHost) {
        webFactory.setPermitForHost(permitForHost);
        return this;
    }

    public CrawlerBuilder addListener(WebSyncEventListener listener) {
        addLast(listener);
        return this;
    }

    public CrawlerBuilder addListener(WebAsyncEventListener listener) {
        addLast(listener);
        return this;
    }

    public CrawlerBuilder setParseThread(int size) {
        webFactory.setParseThread(size);
        return this;
    }

    public Crawler build() {
        return webFactory.build();
    }
}
