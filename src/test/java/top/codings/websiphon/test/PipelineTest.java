package top.codings.websiphon.test;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import top.codings.websiphon.bean.BasicWebRequest;
import top.codings.websiphon.bean.RateResult;
import top.codings.websiphon.bean.WebRequest;
import top.codings.websiphon.bean.WebResponse;
import top.codings.websiphon.core.Crawler;
import top.codings.websiphon.core.context.CrawlerContext;
import top.codings.websiphon.core.pipeline.FilePipeline;
import top.codings.websiphon.core.plugins.ExtractUrlPlugin;
import top.codings.websiphon.core.plugins.UrlFilterPlugin;
import top.codings.websiphon.core.processor.WebProcessorAdapter;
import top.codings.websiphon.core.proxy.bean.ProxyExtension;
import top.codings.websiphon.core.proxy.manager.BasicProxyManager;
import top.codings.websiphon.core.proxy.manager.ProxyManager;
import top.codings.websiphon.core.requester.SuperWebRequester;
import top.codings.websiphon.core.support.CrawlerBuilder;
import top.codings.websiphon.exception.WebParseException;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
public class PipelineTest {
    @Test
    public void test() throws InterruptedException {
        ProxyManager proxyManager=new BasicProxyManager();
        proxyManager.addProxy(new ProxyExtension("127.0.0.1", 1080));
        Crawler crawler = CrawlerBuilder
                .create()
//                .enableProxy(proxyManager)
                .setNetworkThread(3)
                .setPermitForHost(10)
                .addLast(new SuperWebRequester())
//                .addLast(new FilePipeline("list.txt", "utf-8"))
                .addLast(new WebProcessorAdapter() {
                    @Override
                    public void process(WebRequest request, CrawlerContext context) throws WebParseException {
                        log.debug("请求完成 -> {}", request.uri(), request.response().getHtml());
                    }
                })
//                .addLast(new ExtractUrlPlugin(true, false))
                .addLast(new UrlFilterPlugin())
                .queueMonitor((ctx, requestHolder, force) -> log.debug("完结"))
                .build();
        crawler.getContext().setId("test");
        crawler.start();
        BasicWebRequest request = new BasicWebRequest();
        request.setUri("https://news.163.com/20/0324/07/F8FGV2TR00019B3E.html");
        crawler.push(request);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> crawler.close()));
        RateResult rateResult = crawler.getContext().getRateResult();
        StringBuilder stringBuilder = new StringBuilder();
        while (true) {
            TimeUnit.SECONDS.sleep(1);
//            log.debug("{}/s", rateResult.getEverySecondMessage());
            stringBuilder.delete(0, stringBuilder.length());
        }
    }
}
