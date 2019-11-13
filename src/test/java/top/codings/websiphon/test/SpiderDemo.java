package top.codings.websiphon.test;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import top.codings.websiphon.bean.PushResult;
import top.codings.websiphon.bean.WebRequest;
import top.codings.websiphon.core.Crawler;
import top.codings.websiphon.core.context.CrawlerContext;
import top.codings.websiphon.core.context.event.async.AllExceptionEvent;
import top.codings.websiphon.core.context.event.async.WebNetworkExceptionEvent;
import top.codings.websiphon.core.context.event.listener.WebAsyncEventListener;
import top.codings.websiphon.core.context.event.listener.WebSyncEventListener;
import top.codings.websiphon.core.context.event.sync.*;
import top.codings.websiphon.core.plugins.ExtractUrlPlugin;
import top.codings.websiphon.core.plugins.UrlFilterPlugin;
import top.codings.websiphon.core.processor.WebProcessorAdapter;
import top.codings.websiphon.core.proxy.ProxyBuilder;
import top.codings.websiphon.core.proxy.bean.ProxyExtension;
import top.codings.websiphon.core.proxy.manager.ProxyManager;
import top.codings.websiphon.core.support.CrawlerBuilder;
import top.codings.websiphon.exception.WebException;
import top.codings.websiphon.exception.WebParseException;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Slf4j
public class SpiderDemo {
    @Test
    public void test() throws InterruptedException {
        // 构建网络请求并发控制插件，入参为允许的请求最大并发数
        // 构建代理管理器(可选)
        ProxyManager manager = ProxyBuilder.create()
                // 添加代理地址
                .addProxy(new ProxyExtension("127.0.0.1", 1080))
                // 设置代理校验地址(用于检测代理节点活性)
                .setVaildUrl("https://www.google.com")
                .build();
        // 构建爬虫对象
        Crawler crawler = CrawlerBuilder.create()
                // 配置文档处理器，用于解析返回的html并抽取你想要的信息(基于Jsoup)
                .addLast(new WebProcessorAdapter<WebRequest>() {
                    @Override
                    public void process(WebRequest request, CrawlerContext context) throws WebParseException {
                        if (!request.getResponse().getContentType().startsWith("text")) {
                            try {
                                FileUtils.writeByteArrayToFile(new File("./log/img.png"), request.getResponse().getBytes());
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            return;
                        }
                        log.debug("收到响应 -> {} | {}", Jsoup.parse(request.getResponse().getHtml()).title(), request.getUrl());
                        // 显式调用该方法才会将处理事件传递到下一个处理器中继续处理
                        fireProcess(request, context);
                    }
                })
                // 添加页面链接扩散抓取插件(需要配合监听WebLinkEvent事件获取扩散出来的新链接)
                // 第一个入参是sameDomain——是否强制限定同域名
                // 第二个参数是allowHomepage——是否允许将首页纳入扩散链接中
                // 第三个入参是自定义过滤链接规则
                .addLast(new ExtractUrlPlugin(false, true))
                // 添加URL去重插件，第一个入参表示预期最大容量，第二个参数表示去重误差率
                .addLast(new UrlFilterPlugin(2000000, 0.0001D))
                // 添加请求速率控制插件，入参表示允许每秒发起多少个请求
//                .addLast(new RateLimiterPlugin(2))
                // 监听爬虫启动事件
                .addListener(new WebSyncEventListener<WebCrawlStartEvent>() {
                    @Override
                    public void listen(WebCrawlStartEvent event) throws WebException {
                        log.info("爬虫[{}]启动了", event.getContext().getId());
                    }
                })
                // 监听爬虫关闭事件
                .addListener(new WebSyncEventListener<WebCrawlShutdownEvent>() {
                    @Override
                    public void listen(WebCrawlShutdownEvent event) throws WebException {
                        log.info("爬虫[{}]关闭了", event.getContext().getId());
                    }
                })
                // 监听请求发起前事件
                .addListener(new WebSyncEventListener<WebBeforeRequestEvent>() {
                    @Override
                    public void listen(WebBeforeRequestEvent event) throws WebException {
                        log.trace("准备请求 -> {}", event.getRequest().getUrl());
                    }
                })
                // 监听响应解析前事件
                .addListener(new WebSyncEventListener<WebBeforeParseEvent>() {
                    @Override
                    public void listen(WebBeforeParseEvent event) throws WebException {
                        log.trace("准备解析 -> {}", event.getRequest().getUrl());
                    }
                })
                // 监听解析后事件
                .addListener(new WebSyncEventListener<WebAfterParseEvent<WebRequest>>() {
                    @Override
                    public void listen(WebAfterParseEvent<WebRequest> event) throws WebException {
                        log.trace("解析后事件 -> {}", event.getRequest().getUrl());
                    }
                })
                // 监听扩散链接事件
                .addListener(new WebSyncEventListener<WebLinkEvent>() {
                    @Override
                    public void listen(WebLinkEvent event) throws WebException {
                        // 推送新爬取任务给爬虫
                        if (event.getNewRequest().getDepth() > event.getNewRequest().getMaxDepth()) {
                            return;
                        }
                        PushResult pushResult = event.getContext().getCrawler().push(event.getNewRequest());
                        if (pushResult == PushResult.SUCCESS || pushResult == PushResult.URL_REPEAT) {
                            return;
                        }
                        log.debug("推送失败 [{}]", pushResult.value, event.getNewRequest().getUrl());
                    }
                })
                // 监听请求网络异常类事件
                .addListener(new WebAsyncEventListener<WebNetworkExceptionEvent>() {
                    @Override
                    public void listen(WebNetworkExceptionEvent event) {
                        WebRequest request = event.getRequest();
                        if (request.getResponse() == null) {
                            return;
                        }
                        log.error("网络异常 -> {} | 原因：{}",
                                event.getRequest().getUrl(),
                                event.getThrowable().getLocalizedMessage());
                    }
                })
                // 监听所有未处理的异常事件
                .addListener(new WebAsyncEventListener<AllExceptionEvent>() {
                    @Override
                    public void listen(AllExceptionEvent event) {
                        Throwable throwable = event.getThrowable();
                        if (throwable instanceof IllegalMonitorStateException) {

                            return;
                        }
                        log.error("异常 -> {}", event.getThrowable());
                    }
                })
                // 启用代理模式，若不需要代理则无需调用该方法
//                .enableProxy(manager)
                // 设置网络请求最大并发数
                .setNetworkThread(50)
                // 设置最大处理线程数
                .setParseThread(200)
                .queueMonitor((crawlerContext, requestHolder, force) -> crawlerContext.getCrawler().close())
                .build();
        // 设置爬虫的名字(必须)
        crawler.getContext().setId("测试爬虫");
        // 设置爬虫内统计爬取QPS的频率
        crawler.getContext().getRateResult().setSecond(1);
        // 启动爬虫(非阻塞方法)
        crawler.start();
        // 构建爬取任务
        WebRequest request = new WebRequest();
        // 设置需要爬取的入口URL
        request.setUrl("https://www.163.com/");
        // 使用扩散插件的情况下，最大的扩散深度
        request.setMaxDepth(1);
        request.setTimeout(30000);
        // 将任务推送给爬虫
        crawler.push(request);
        // 打印QPS相关信息
        Thread thread = new Thread(() -> {
            CrawlerContext context = crawler.getContext();
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    log.info("当前总QPS：{}/s | 当前成功QPS：{}/s | 消费总数：{} | 成功总数：{}",
                            context.getRateResult().getEverySecondMessage(),
                            context.getRateResult().getEverySecondCount(),
                            context.getRateResult().getTotalMessage().get(),
                            context.getRateResult().getSuccessCount().get());
                    TimeUnit.SECONDS.sleep(1);
                }
            } catch (InterruptedException e) {
                return;
            }
        });
        thread.setDaemon(true);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> crawler.close()));
        thread.start();
        Thread.currentThread().join();
    }
}
