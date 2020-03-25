package top.codings.websiphon.test;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import top.codings.websiphon.bean.*;
import top.codings.websiphon.core.Crawler;
import top.codings.websiphon.core.context.CrawlerContext;
import top.codings.websiphon.core.context.event.WebSyncEvent;
import top.codings.websiphon.core.context.event.listener.WebSyncEventListener;
import top.codings.websiphon.core.context.event.sync.WebBeforeParseEvent;
import top.codings.websiphon.core.context.event.sync.WebBeforeRequestEvent;
import top.codings.websiphon.core.pipeline.FilePipeline;
import top.codings.websiphon.core.pipeline.ReadWritePipeline;
import top.codings.websiphon.core.plugins.ExtractUrlPlugin;
import top.codings.websiphon.core.plugins.UrlFilterPlugin;
import top.codings.websiphon.core.plugins.WebPlugin;
import top.codings.websiphon.core.processor.WebProcessorAdapter;
import top.codings.websiphon.core.proxy.bean.ProxyExtension;
import top.codings.websiphon.core.proxy.manager.BasicProxyManager;
import top.codings.websiphon.core.proxy.manager.ProxyManager;
import top.codings.websiphon.core.requester.SuperWebRequester;
import top.codings.websiphon.core.support.CrawlerBuilder;
import top.codings.websiphon.exception.WebException;
import top.codings.websiphon.exception.WebParseException;

import java.util.concurrent.TimeUnit;

@Slf4j
public class PipelineTest {
    @Test
    public void test() throws InterruptedException {
        ProxyManager proxyManager = new BasicProxyManager();
        proxyManager.addProxy(new ProxyExtension("127.0.0.1", 1080));
        Crawler crawler = CrawlerBuilder
                .create()
//                .enableProxy(proxyManager)
                .setNetworkThread(300)
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
                        log.debug("请求完成 -> {}", request.uri(), request.response().getHtml());
                    }
                })
                .addLast(new WebPlugin() {
                    @Override
                    public Object[] before(Object[] params, ReturnPoint point) throws WebException {
//                        log.debug("任务来了");
                        return params;
                    }

                    @Override
                    public Object after(Object proxy, Object[] params, Object result, MethodDesc methodDesc, ReturnPoint point) throws WebException {
                        return result;
                    }

                    @Override
                    public Class[] getTargetInterface() {
                        return new Class[]{FilePipeline.class};
                    }

                    @Override
                    public MethodDesc[] getMethods() {
                        return new MethodDesc[]{new MethodDesc("read", new Class[0])};
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
            log.trace("QPS {}/s", rateResult.getEverySecondMessage());
            stringBuilder.delete(0, stringBuilder.length());
        }
    }
}
