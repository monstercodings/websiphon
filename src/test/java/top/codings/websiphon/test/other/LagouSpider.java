package top.codings.websiphon.test.other;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import top.codings.websiphon.bean.WebRequest;
import top.codings.websiphon.core.Crawler;
import top.codings.websiphon.core.context.CrawlerContext;
import top.codings.websiphon.core.context.event.async.WebNetworkExceptionEvent;
import top.codings.websiphon.core.context.event.listener.WebAsyncEventListener;
import top.codings.websiphon.core.plugins.RateLimiterPlugin;
import top.codings.websiphon.core.plugins.UrlFilterPlugin;
import top.codings.websiphon.core.processor.WebProcessorAdapter;
import top.codings.websiphon.core.requester.Cdp4jWebRequester;
import top.codings.websiphon.core.requester.NettyWebRequester;
import top.codings.websiphon.core.support.CrawlerBuilder;
import top.codings.websiphon.exception.WebParseException;
import top.codings.websiphon.util.HttpOperator;

import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class LagouSpider {
    private static AtomicInteger pageCount = new AtomicInteger(0);

    public static void main(String[] args) {
        Crawler crawler = CrawlerBuilder.create()
                .addLast(new Cdp4jWebRequester<>(false))
                // 配置文档处理器，用于解析返回的html并抽取你想要的信息
                .addLast(new WebProcessorAdapter<WebRequest>() {
                    @Override
                    public void process(WebRequest request, CrawlerContext context) throws WebParseException {
                        pageCount.getAndIncrement();
                        if (!request.getResponse().getContentType().startsWith("text")) {
                            return;
                        }
                        Document document = Jsoup.parse(request.getResponse().getHtml());
                        Elements lis = document.select("ul.item_con_list>li");
                        for (Element li : lis) {
                            String positionid = li.attr("data-positionid");
                            String positionname = li.attr("data-positionname");
                            String company = li.attr("data-company");
                            String salary = li.attr("data-salary");
                            log.debug("公司：{} | 职位：{} | 薪资：{} | 职位id：{}", company, positionname, salary, positionid);
                        }
                        Element page = document.selectFirst(".pager_container");
                        if (null != page) {
                            String href = page.select("a").last().attr("href");
                            String url = HttpOperator.recombineLink(href, request.getUrl());
                            WebRequest newReq = WebRequest.simple(url);
                            context.getCrawler().push(newReq);
                        } else {
                            log.debug("无下一页，当前页数 {}", pageCount.get());
                        }
//                        log.debug("收到响应 -> {} | {}", Jsoup.parse(request.getResponse().getHtml()).title(), request.getUrl());
//                        log.debug("{}", request.getResponse().getHtml());
                        // 显式调用该方法才会将处理事件传递到下一个处理器中继续处理
                        fireProcess(request, context);
                    }
                })
                .addLast(new UrlFilterPlugin())
                .addLast(new RateLimiterPlugin(0.5))
                .addListener(new WebAsyncEventListener<WebNetworkExceptionEvent>() {
                    @Override
                    public void listen(WebNetworkExceptionEvent event) {
                        log.error("请求网络异常", event.getThrowable());
                    }
                })
                .queueMonitor((ctx, requestHolder, force) -> {
                    log.debug("采集完成");
                    System.exit(0);
//                    urlFilterPlugin.clear();
                })
//                .enableProxy(manager)
                // 设置网络请求最大并发数
                .setNetworkThread(1)
                // 设置最大处理线程数
                .setParseThread(1)
                .build();
        // 设置爬虫的名字(必须)
        crawler.getContext().setId("测试爬虫");
        // 启动爬虫(异步)
        crawler.start();
        // 构建爬取任务
        WebRequest request = new WebRequest();
        // 设置需要爬取的入口URL
        request.setUrl("https://www.lagou.com/zhaopin/");
        // 设置超时
        request.setTimeout(60000);
        // 将任务推送给爬虫
        crawler.push(request);
    }
}
