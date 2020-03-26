package top.codings.websiphon.test;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import top.codings.websiphon.bean.BasicWebRequest;
import top.codings.websiphon.bean.RateResult;
import top.codings.websiphon.bean.WebRequest;
import top.codings.websiphon.bean.WebResponse;
import top.codings.websiphon.core.Crawler;
import top.codings.websiphon.core.context.event.async.WebNetworkExceptionEvent;
import top.codings.websiphon.core.context.event.listener.WebAsyncEventListener;
import top.codings.websiphon.core.plugins.support.ExtractUrlPlugin;
import top.codings.websiphon.core.plugins.support.UrlFilterPlugin;
import top.codings.websiphon.core.processor.WebProcessorAdapter;
import top.codings.websiphon.core.support.CrawlerBuilder;
import top.codings.websiphon.exception.WebParseException;
import top.codings.websiphon.test.feign.CrawlManager;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
public class SpiderDemo {
    @Test
    public void test() throws InterruptedException {
        CrawlManager crawlManager = CrawlManager.create();
        UrlFilterPlugin urlFilterPlugin = new UrlFilterPlugin();
//        WebRequester requester = new SeimiAgentWebRequest("http://121.201.107.77:51000");
//        requester.setIgnoreHttpError(true);
        // 构建爬虫对象
        Crawler crawler = CrawlerBuilder.create()
//                .addLast(requester)
                // 配置文档处理器，用于解析返回的html并抽取你想要的信息
                .addLast(new WebProcessorAdapter<WebRequest>() {
                    @Override
                    public void process(WebRequest request) throws WebParseException {
                        if (!request.response().getContentType().startsWith("text")) {
                            return;
                        }
                        if (request.response().isRedirect()) {
                            log.debug("[{}] [跳转] 原链：{} | 转链：{}", request.response().getResult().getKey(), request.response().getUrl(), request.response().getRedirectUrl());
                        } else {
                            log.debug("[{}] [正常] 链接：{}", request.response().getResult().getKey(), request.response().getUrl());
                        }
//                        log.debug("收到响应 -> {} | {}", Jsoup.parse(request.getResponse().getHtml()).title(), request.getUrl());
//                        log.debug("{}", request.getResponse().getHtml());
                        // 显式调用该方法才会将处理事件传递到下一个处理器中继续处理
                        fireProcess(request);
                    }
                })
                .addLast(new ExtractUrlPlugin(false, false))
                .addLast(urlFilterPlugin)
                .addListener(new WebAsyncEventListener<WebNetworkExceptionEvent>() {
                    @Override
                    public void listen(WebNetworkExceptionEvent event) {
//                        log.error("请求网络异常", event.getThrowable());
                    }
                })
//                .enableProxy(manager)
                // 设置网络请求最大并发数
                .setNetworkThread(10)
                // 设置最大处理线程数
                .setParseThread(20)
                .build();
        // 设置爬虫的名字(必须)
        crawler.getContext().setId("测试爬虫");
        // 启动爬虫(异步)
        crawler.start();
        /*CrawlManager.CrawlTask task;
        do {
            log.debug("尝试拉取爬虫任务");
            task = crawlManager.getTask();
        } while (task.getCode() != 0);*/

        // 构建爬取任务
        BasicWebRequest request = new BasicWebRequest();
        // 设置需要爬取的入口URL
        request.setUri("https://www.163.com");
//        request.setUrl("http://2000019.ip138.com/");
        // 使用扩散插件的情况下，最大的扩散深度
        // 设置超时
        request.setTimeout(60000);
        // 将任务推送给爬虫
        crawler.push(request);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> crawler.close()));
        RateResult rateResult = crawler.getContext().getRateResult();
        StringBuilder stringBuilder = new StringBuilder();
        while (true) {
            TimeUnit.SECONDS.sleep(1);
            stringBuilder.append("\n");
            for (Map.Entry<WebResponse.Result, AtomicLong> entry : rateResult.getResultStat().entrySet()) {
                stringBuilder.append(entry.getKey().getKey()).append(":").append(entry.getValue().get()).append("\n");
            }
            log.debug("\n{}", stringBuilder.toString());
            stringBuilder.delete(0, stringBuilder.length());
        }
//        Thread.currentThread().join();
    }
}