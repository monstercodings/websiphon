package top.codings.websiphon.test;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;
import top.codings.websiphon.bean.BasicWebRequest;
import top.codings.websiphon.bean.WebRequest;
import top.codings.websiphon.core.Crawler;
import top.codings.websiphon.core.context.event.async.WebNetworkExceptionEvent;
import top.codings.websiphon.core.context.event.listener.WebAsyncEventListener;
import top.codings.websiphon.core.context.event.listener.WebSyncEventListener;
import top.codings.websiphon.core.context.event.sync.WebBeforeRequestEvent;
import top.codings.websiphon.core.context.event.sync.WebDownloadEvent;
import top.codings.websiphon.core.context.event.sync.WebLinkEvent;
import top.codings.websiphon.core.pipeline.FilePipeline;
import top.codings.websiphon.core.plugins.support.*;
import top.codings.websiphon.core.processor.WebProcessorAdapter;
import top.codings.websiphon.core.proxy.bean.WebProxy;
import top.codings.websiphon.core.proxy.pool.BasicProxyPool;
import top.codings.websiphon.core.proxy.pool.ProxyPool;
import top.codings.websiphon.core.requester.HttpWebRequester;
import top.codings.websiphon.core.support.CrawlerBuilder;
import top.codings.websiphon.exception.WebException;
import top.codings.websiphon.exception.WebParseException;

@Slf4j
public class Demo {
    @Test
    public void test() throws InterruptedException {
        // QPS插件 - 每隔一秒会回调一次用户自定义的函数，入参为统计结果
        QpsPlugin qpsPlugin = new QpsPlugin(qpsStats -> {
            log.debug("\n总QPS -> {}/s\n{}", qpsStats.getQps(), JSON.toJSONString(qpsStats.getHostQpsMap(), true));
        });
        WebsiphonStatsPlugin statsPlugin = new WebsiphonStatsPlugin();
        WebProxy proxy = new WebProxy("127.0.0.1", 1080);
        ProxyPool pool = new BasicProxyPool()
                .add(proxy);
        // 构建爬虫对象
        Crawler crawler = CrawlerBuilder
                .create()
                // 配置请求器，默认请求器为HttpWebRequester，若非自行编写请求器的话可以不需要配置
                .addLast(new HttpWebRequester(true))
                // 配置数据来源管道，非必须，如果有持续任务输入来源，可自行实现管道接口来对接爬虫
                .addLast(new FilePipeline("config/list.properties", "utf-8"))
                // 配置文档处理器，用于解析返回的html并抽取想要的结构化信息
                .addLast(new WebProcessorAdapter<BasicWebRequest>() {
                    @Override
                    public void process(BasicWebRequest request) throws WebParseException {
                        Document document = Jsoup.parse(request.response().getHtml());
                        log.debug("{} | {}", document.title(), request.uri());
                        // 显式调用该方法才会将处理事件传递到下一个处理器继续处理
                        fireProcess(request);
                    }
                })
                // 代理池插件 - 为爬虫提供代理功能
//                .addLast(new ProxyPlugin(pool))
                // Cookie插件 - 提供cookie维护功能，主要用于需要维护登录状态的场景
                .addLast(new CookiePlugin(
                        CookiePlugin.ReadFromFile.from("config/cookie.txt"),
                        CookiePlugin.WriteToFile.to("config/cookie.txt")))
                // URL提取插件 - 自动抓取页面上的所有链接，可自定义各种提取规则
//                .addLast(new ExtractUrlPlugin(true, false))
                // 任务完成监控通知插件 - 当爬虫内的爬取任务都完成后会执行该回调
                .addLast(new MissionOverAlertPlugin((MissionOverAlertPlugin.MissionOverHandler<WebRequest>) request -> {
//                    log.debug("最后的URL -> {}", request.uri());
                    log.debug("统计指标\n{}", JSON.toJSONString(statsPlugin.stats(), true));
                }))
                // 请求速率限制插件 - 设置整体爬虫最多允许的QPS
                .addLast(new RateLimiterPlugin(3))
                // URL过滤插件 - 对URL进行去重
                .addLast(new UrlFilterPlugin())
                // 爬取结果统计插件 - 可以实时看到爬取的完成情况
                .addLast(statsPlugin)
                // QPS统计插件 - 统计总实时QPS以及各域名对应的实时QPS
//                .addLast(qpsPlugin)
                .addListener(new WebSyncEventListener<WebDownloadEvent>() {
                    @Override
                    public void listen(WebDownloadEvent event) throws WebException {
                        log.debug("请求类型 -> {} | 长度 -> {}", event.getContentType(), event.getContentLength());
//                        event.getRequest().stop();
                    }
                })
                // 发起请求前事件监听器
                .addListener(new WebSyncEventListener<WebBeforeRequestEvent>() {
                    @Override
                    public void listen(WebBeforeRequestEvent event) throws WebException {
                        log.debug("请求 -> {}", event.getRequest().uri());
                        // 如果需要终止该请求，可直接调用stop()方法
                        // event.getRequest().stop();
                    }
                })
                // 链接抽取事件监听器，唯一作用是配合ExtractUrlPlugin使用
                // 如果不添加该监听器，所有符合要求的链接都会直接进入爬虫任务队列
                .addListener(new WebSyncEventListener<WebLinkEvent>() {
                    @Override
                    public void listen(WebLinkEvent event) throws WebException {
                        // 获取URL提取插件提取出来的新URL
                        String url = event.getNewUrl();
                        BasicWebRequest request = new BasicWebRequest();
                        request.setUri(url);
                        // 将构建的新爬取任务添加到List里，爬虫会从该List读取所有的任务
                        event.getOut().add(request);
                    }
                })
                // 网络异常监听器
                .addListener(new WebAsyncEventListener<WebNetworkExceptionEvent>() {
                    @Override
                    public void listen(WebNetworkExceptionEvent event) {
                        log.error("网络请求异常", event.getThrowable());
                    }
                })
                // 限制网络最大并发数
                .setNetworkThread(10)
                // 限制单个域名最大并发数，如不设置则默认每个域名只允许2个并发
                .setPermitForHost(2)
                .build();
        // 设置爬虫的名字
        crawler.getContext().setId("test");
        // 启动爬虫(异步)
        crawler.start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            // 优雅停机必须调用爬虫关闭接口
            // 否则可能造成不可知的数据丢失
            crawler.close();
        }));
        BasicWebRequest request = new BasicWebRequest();
        request.setProxy(WebProxy.NO_PROXY);
        request.setUri("http://www.codings.top");
        crawler.push(request);
        Thread.currentThread().join();
    }
}