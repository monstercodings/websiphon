package top.codings.websiphon.test;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import top.codings.websiphon.bean.WebRequest;
import top.codings.websiphon.core.Crawler;
import top.codings.websiphon.core.context.CrawlerContext;
import top.codings.websiphon.core.processor.WebProcessorAdapter;
import top.codings.websiphon.core.support.CrawlerBuilder;
import top.codings.websiphon.exception.WebParseException;

import java.util.HashMap;

@Slf4j
public class SpiderDemo {
    @Test
    public void test() throws InterruptedException {
        // 构建爬虫对象
        Crawler crawler = CrawlerBuilder.create()
                // 配置文档处理器，用于解析返回的html并抽取你想要的信息
                .addLast(new WebProcessorAdapter<WebRequest>() {
                    @Override
                    public void process(WebRequest request, CrawlerContext context) throws WebParseException {
                        if (!request.getResponse().getContentType().startsWith("text")) {
                            return;
                        }
                        log.debug("收到响应 -> {} | {}", Jsoup.parse(request.getResponse().getHtml()).title(), request.getUrl());
                        // 显式调用该方法才会将处理事件传递到下一个处理器中继续处理
                        fireProcess(request, context);
                    }
                })
                // 设置网络请求最大并发数
                .setNetworkThread(5)
                // 设置最大处理线程数
                .setParseThread(10)
                .build();
        // 设置爬虫的名字(必须)
        crawler.getContext().setId("测试爬虫");
        // 启动爬虫(异步)
        crawler.start();
        // 构建爬取任务
        WebRequest request = new WebRequest();
        // 设置需要爬取的入口URL
        request.setUrl("https://www.163.com/");
        // 使用扩散插件的情况下，最大的扩散深度
        request.setMaxDepth(1);
        // 设置超时
        request.setTimeout(6000);
        request.setHeaders(new HashMap<>());
        // 将任务推送给爬虫
        crawler.push(request);
        Thread.currentThread().join();
    }
}