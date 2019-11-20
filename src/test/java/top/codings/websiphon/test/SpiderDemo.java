package top.codings.websiphon.test;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import top.codings.websiphon.bean.PushResult;
import top.codings.websiphon.bean.WebRequest;
import top.codings.websiphon.core.Crawler;
import top.codings.websiphon.core.context.CrawlerContext;
import top.codings.websiphon.core.context.event.WebSyncEvent;
import top.codings.websiphon.core.context.event.async.WebNetworkExceptionEvent;
import top.codings.websiphon.core.context.event.listener.WebAsyncEventListener;
import top.codings.websiphon.core.context.event.listener.WebSyncEventListener;
import top.codings.websiphon.core.context.event.sync.WebLinkEvent;
import top.codings.websiphon.core.plugins.ExtractUrlPlugin;
import top.codings.websiphon.core.plugins.UrlFilterPlugin;
import top.codings.websiphon.core.processor.WebProcessorAdapter;
import top.codings.websiphon.core.proxy.bean.ProxyExtension;
import top.codings.websiphon.core.proxy.manager.BasicProxyManager;
import top.codings.websiphon.core.proxy.manager.ProxyManager;
import top.codings.websiphon.core.requester.BasicAsyncWebRequester;
import top.codings.websiphon.core.support.CrawlerBuilder;
import top.codings.websiphon.exception.WebException;
import top.codings.websiphon.exception.WebParseException;
import top.codings.websiphon.test.feign.CrawlManager;

import java.util.HashMap;

@Slf4j
public class SpiderDemo {
    @Test
    public void test() throws InterruptedException {
        CrawlManager crawlManager = CrawlManager.create();
        UrlFilterPlugin urlFilterPlugin = new UrlFilterPlugin();
        BasicAsyncWebRequester requester = new BasicAsyncWebRequester();
//        requester.setIgnoreHttpError(true);
        // 构建爬虫对象
        ProxyManager manager = new BasicProxyManager()
                .addProxy(new ProxyExtension("127.0.0.1", 1080));
        Crawler crawler = CrawlerBuilder.create()
                .addLast(requester)
                // 配置文档处理器，用于解析返回的html并抽取你想要的信息
                .addLast(new WebProcessorAdapter<WebRequest>() {
                    @Override
                    public void process(WebRequest request, CrawlerContext context) throws WebParseException {
                        if (!request.getResponse().getContentType().startsWith("text")) {
                            return;
                        }
                        if (request.getResponse().isRedirect()) {
                            log.debug("[{}] [跳转] 原链：{} | 转链：{}", request.getResponse().getStatusCode(), request.getResponse().getUrl(), request.getResponse().getRedirectUrl());
                        } else {
                            log.debug("[{}] [正常] 链接：{}", request.getResponse().getStatusCode(), request.getResponse().getUrl());
                        }
//                        log.debug("收到响应 -> {} | {}", Jsoup.parse(request.getResponse().getHtml()).title(), request.getUrl());
//                        log.debug("{}", request.getResponse().getHtml());
                        // 显式调用该方法才会将处理事件传递到下一个处理器中继续处理
                        fireProcess(request, context);
                    }
                })
                .addLast(new ExtractUrlPlugin(true, false))
                .addLast(urlFilterPlugin)
                .addListener(new WebAsyncEventListener<WebNetworkExceptionEvent>() {
                    @Override
                    public void listen(WebNetworkExceptionEvent event) {
                    }
                })
                .queueMonitor((ctx, requestHolder, force) -> {
                    log.debug("采集完成");
                    urlFilterPlugin.clear();
                    CrawlManager.CrawlTask task;
                    do {
                        log.debug("尝试拉取爬虫任务");
                        task = crawlManager.getTask();
                    } while (task.getCode() != 0);
                    WebRequest request = new WebRequest();
                    request.setUrl(task.getData().toString());
                    request.setMaxDepth(1);
                    // 设置超时
                    request.setTimeout(6000);
                    // 将任务推送给爬虫
                    PushResult pushResult = ctx.getCrawler().push(request);
                    log.debug("推送结果 -> {}", pushResult.value);
                })
//                .enableProxy(manager)
                // 设置网络请求最大并发数
                .setNetworkThread(5)
                // 设置最大处理线程数
                .setParseThread(10)
                .build();
        // 设置爬虫的名字(必须)
        crawler.getContext().setId("测试爬虫");
        // 启动爬虫(异步)
        crawler.start();
        CrawlManager.CrawlTask task;
        do {
            log.debug("尝试拉取爬虫任务");
            task = crawlManager.getTask();
        } while (task.getCode() != 0);

        // 构建爬取任务
        WebRequest request = new WebRequest();
        // 设置需要爬取的入口URL
        request.setUrl(task.getData().toString());
//        request.setUrl("http://2000019.ip138.com/");
        // 使用扩散插件的情况下，最大的扩散深度
        request.setMaxDepth(1);
        // 设置超时
        request.setTimeout(6000);
        // 将任务推送给爬虫
        crawler.push(request);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> crawler.close()));
        Thread.currentThread().join();
    }
}