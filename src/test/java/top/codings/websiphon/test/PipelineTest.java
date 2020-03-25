package top.codings.websiphon.test;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.Test;
import top.codings.websiphon.bean.RateResult;
import top.codings.websiphon.bean.WebRequest;
import top.codings.websiphon.core.Crawler;
import top.codings.websiphon.core.context.CrawlerContext;
import top.codings.websiphon.core.context.event.listener.WebSyncEventListener;
import top.codings.websiphon.core.context.event.sync.WebBeforeParseEvent;
import top.codings.websiphon.core.context.event.sync.WebBeforeRequestEvent;
import top.codings.websiphon.core.pipeline.FilePipeline;
import top.codings.websiphon.core.plugins.CookiePlugin;
import top.codings.websiphon.core.plugins.UrlFilterPlugin;
import top.codings.websiphon.core.processor.WebProcessorAdapter;
import top.codings.websiphon.core.proxy.bean.WebProxy;
import top.codings.websiphon.core.proxy.pool.BasicProxyPool;
import top.codings.websiphon.core.proxy.pool.ProxyPool;
import top.codings.websiphon.core.requester.SuperWebRequester;
import top.codings.websiphon.core.support.CrawlerBuilder;
import top.codings.websiphon.exception.WebException;
import top.codings.websiphon.exception.WebParseException;

import java.util.concurrent.TimeUnit;

@Slf4j
public class PipelineTest {
    @Test
    public void test() throws InterruptedException {
        WebProxy proxy = new WebProxy("127.0.0.1", 1081);
//        proxy.setEnabled(false);
//        proxy.setHealthy(false);
        ProxyPool pool = new BasicProxyPool()
                .add(proxy);
        Crawler crawler = CrawlerBuilder
                .create()
                .setNetworkThread(1)
                .setPermitForHost(2)
                .addLast(new SuperWebRequester())
                .addLast(new FilePipeline("list.txt", "utf-8"))
                .addListener(new WebSyncEventListener<WebBeforeRequestEvent>() {
                    @Override
                    public void listen(WebBeforeRequestEvent event) throws WebException {
                        if (event.getRequest().uri().equals("https://www.runoob.com/swift/swift-optionals.html")) {
                            log.debug("停止请求 -> {}", event.getRequest().uri());
                            event.getRequest().stop();
                        }
                    }
                })
                .addListener(new WebSyncEventListener<WebBeforeParseEvent>() {
                    @Override
                    public void listen(WebBeforeParseEvent event) throws WebException {
                        if (event.getRequest().uri().equals("https://news.163.com/20/0324/07/F8FGV2TR00019B3E.html")) {
                            log.debug("停止解析 -> {}", event.getRequest().uri());
                            event.getRequest().stop();
                        }
                    }
                })
                .addLast(new WebProcessorAdapter() {
                    @Override
                    public void process(WebRequest request, CrawlerContext context) throws WebParseException {
                        Document document = Jsoup.parse(request.response().getHtml());
                        Elements elements = document.select("code");
                        StringBuilder sb = new StringBuilder();
                        for (Element element : elements) {
                            sb.append(element.text()).append("\n");
                        }
                        log.debug("请求完成 -> {}\n{}", request.uri(), sb.toString());
                    }
                })
//                .addLast(new ProxyPlugin(pool))
                .addLast(new CookiePlugin())
//                .addLast(new ExtractUrlPlugin(true, false))
//                .addLast(new UrlFilterPlugin())
                .queueMonitor((ctx, requestHolder, force) -> log.debug("完结"))
                .build();
        crawler.getContext().setId("test");
        crawler.start();
        /*BasicWebRequest request = new BasicWebRequest();
        request.setUri("https://www.ip.cn");
        crawler.push(request);*/
        Runtime.getRuntime().addShutdownHook(new Thread(() -> crawler.close()));
        RateResult rateResult = crawler.getContext().getRateResult();
        StringBuilder stringBuilder = new StringBuilder();
        while (true) {
            TimeUnit.SECONDS.sleep(1);
            log.trace("QPS {}/s", rateResult.getEverySecondMessage());
            stringBuilder.delete(0, stringBuilder.length());
        }
    }
}
