package top.codings.websiphon.core;

import top.codings.websiphon.bean.PushResult;
import top.codings.websiphon.bean.WebRequest;
import top.codings.websiphon.core.context.CrawlerContext;

public interface Crawler {
    CrawlerContext start();

    PushResult push(WebRequest requeset);

    String getId();

    CrawlerContext getContext();

    void close();
}
