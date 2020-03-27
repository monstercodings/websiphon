package top.codings.websiphon.test;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;
import top.codings.websiphon.bean.BasicWebRequest;
import top.codings.websiphon.bean.WebRequest;
import top.codings.websiphon.bean.WebRequestDoc;
import top.codings.websiphon.core.Crawler;
import top.codings.websiphon.core.context.event.async.WebNetworkExceptionEvent;
import top.codings.websiphon.core.context.event.listener.WebAsyncEventListener;
import top.codings.websiphon.core.context.event.listener.WebSyncEventListener;
import top.codings.websiphon.core.context.event.sync.WebBeforeRequestEvent;
import top.codings.websiphon.core.context.event.sync.WebLinkEvent;
import top.codings.websiphon.core.pipeline.FilePipeline;
import top.codings.websiphon.core.plugins.support.*;
import top.codings.websiphon.core.processor.WebProcessorAdapter;
import top.codings.websiphon.core.proxy.bean.WebProxy;
import top.codings.websiphon.core.proxy.pool.BasicProxyPool;
import top.codings.websiphon.core.proxy.pool.ProxyPool;
import top.codings.websiphon.core.support.CrawlerBuilder;
import top.codings.websiphon.exception.WebException;
import top.codings.websiphon.exception.WebParseException;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class NewDemo {
    @Test
    public void test() throws InterruptedException {
        AtomicBoolean stop = new AtomicBoolean(false);
        QpsPlugin qpsPlugin = new QpsPlugin(qpsStats -> {
            if (!stop.get()) {
                log.debug("\n总QPS -> {}/s\n{}", qpsStats.getQps(), JSON.toJSONString(qpsStats.getHostQpsMap(), true));
            }
        });
        WebsiphonStatsPlugin statsPlugin = new WebsiphonStatsPlugin();
        WebProxy proxy = new WebProxy("127.0.0.1", 1080);
        ProxyPool pool = new BasicProxyPool()
                .add(proxy);
        Crawler crawler = CrawlerBuilder
                .create()
//                .setNetworkThread(10)
//                .setPermitForHost(10)
//                .addLast(new SuperWebRequester())
                .addLast(new FilePipeline("list.properties", "utf-8"))
                .addListener(new WebSyncEventListener<WebBeforeRequestEvent>() {
                    @Override
                    public void listen(WebBeforeRequestEvent event) throws WebException {
//                        log.debug("请求 -> {}", event.getRequest().uri());
                        /*if (event.getRequest().uri().equals("https://www.ip.cn")) {
                            log.debug("停止请求 -> {}", event.getRequest().uri());
                            event.getRequest().stop();
                        }*/
                    }
                })
                .addListener(new WebSyncEventListener<WebLinkEvent>() {
                    private AtomicInteger count = new AtomicInteger();

                    @Override
                    public void listen(WebLinkEvent event) throws WebException {
                        String url = event.getNewUrl();
                        if (count.incrementAndGet() > 50) {
                            return;
                        }
                        BasicWebRequest request = new BasicWebRequest();
                        request.setUri(url);
                        event.getOut().add(request);
                    }
                })
                .addLast(new WebProcessorAdapter<WebRequestDoc>() {
                    @Override
                    public void process(WebRequestDoc request) throws WebParseException {
                        log.debug("微博 -> {}", Jsoup.parse(request.response().getHtml()).title());
                        fireProcess(request);
                    }
                })
                .addListener(new WebAsyncEventListener<WebNetworkExceptionEvent>() {
                    @Override
                    public void listen(WebNetworkExceptionEvent event) {

                    }
                })
                .addLast(new WebProcessorAdapter() {
                    @Override
                    public void process(WebRequest request) throws WebParseException {
                        Document document = Jsoup.parse(request.response().getHtml());
                        log.debug("{} | {}", document.title(), request.uri());
                        /*Elements elements = document.select("code");
                        StringBuilder sb = new StringBuilder();
                        for (Element element : elements) {
                            sb.append(element.text()).append("\n");
                        }
                        log.debug("请求完成 -> {}\n{}", request.uri(), sb.toString());*/
                    }
                })
//                .addLast(new ProxyPlugin(pool))
                .addLast(new CookiePlugin(
                        CookiePlugin.ReadFromFile.from("cookie.txt"),
                        CookiePlugin.WriteToFile.to("cookie.txt")))
                .addLast(new ExtractUrlPlugin(true, false))
                .addLast(new MissionOverAlertPlugin((MissionOverAlertPlugin.MissionOverHandler<WebRequest>) request -> {
                    stop.set(true);
//                    log.debug("最后的URL -> {}", request.uri());
                    log.debug("统计指标\n{}", JSON.toJSONString(statsPlugin.stats(), true));
                }))
                .addLast(new UrlFilterPlugin())
                .addLast(statsPlugin)
                .addLast(qpsPlugin)
//                .addLast(new PermitsPerSecondWebPlugin(3))
//                .queueMonitor((ctx, requestHolder, force) -> log.debug("完结"))
                .build();
        crawler.getContext().setId("test");
        crawler.start();
        WebRequestDoc request = new WebRequestDoc();
//        request.setUri("https://weibo.com/u/5869826499?profile_ftype=1&is_ori=1#_0");
//        request.setUri("https://www.ipp.cn");
//        TimeUnit.SECONDS.sleep(5);
//        crawler.push(request);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> crawler.close()));
        Thread.currentThread().join();
    }
}