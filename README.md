# 爬虫框架

`Designed By Jian.He`

### Maven坐标

```
<dependency>
    <groupId>top.codings</groupId>
    <artifactId>websiphon</artifactId>
    <version>0.0.1</version>
</dependency>
```

1. [框架简介](#框架简介)
1. [快速开始](#快速开始)
1. [框架概念](#框架概念)
1. 组件开发
1. 最佳实践
1. 内部原理

### 框架简介

* 设计目的

    > 本框架为简化各类网络爬虫开发工作而设计，使用框架可实现快速开发定制爬虫。  
      框架实现了线程管理，使用者无需关心线程生命周期，从而屏蔽大部分线程概念，让使用者可专注于业务实现。
* 基础核心

    > 框架基于插件、事件两大核心概念驱动：  
      `事件`——即触发回调，框架内置一系列默认事件，可自由选择需要监听的事件以及同/异步监听方式。  
      `插件`——框架内最核心的组件，框架内的大部分组件都是基于插件开发的，所以在有特殊化定制需求的时候，可以灵活的替换各类自定义组件。  
* 使用方法

    > 框架内置一套默认爬取流程，一般情况下使用默认流程即可，若有特殊需求使用另行开发的一套插件替换默认流程即可。  
      爬虫使用方式目前有两种：  
        1. 开发为可使用java -jar形式直接启动的爬虫。 `推荐使用`  
        2. 使用本框架搭建好爬虫平台，该平台可提供各种资源通道，之后开发非独立运行模式的爬虫，所需资源均从平台获取。  

### 快速开始

1. 简单示例

    > 使用默认爬取流程，均使用内置请求器、读写双向管道、操作句柄、解析器、处理器作为示例进行演示开发。
    
    ```java
    import cn.szkedun.kdew.collector.document.bean.WebRequest;
    import cn.szkedun.kdew.collector.document.core.Crawler;
    import cn.szkedun.kdew.collector.document.core.context.CrawlerContext;
    import cn.szkedun.kdew.collector.document.core.context.event.async.AllExceptionEvent;
    import cn.szkedun.kdew.collector.document.core.context.event.async.WebNetworkExceptionEvent;
    import cn.szkedun.kdew.collector.document.core.context.event.listener.WebAsyncEventListener;
    import cn.szkedun.kdew.collector.document.core.context.event.listener.WebSyncEventListener;
    import cn.szkedun.kdew.collector.document.core.context.event.sync.*;
    import cn.szkedun.kdew.collector.document.core.pipeline.BasicReadWritePipeline;
    import cn.szkedun.kdew.collector.document.core.plugins.ExtractUrlPlugin;
    import cn.szkedun.kdew.collector.document.core.plugins.RateLimiterPlugin;
    import cn.szkedun.kdew.collector.document.core.plugins.RequestMaxParallelismWebPlugin;
    import cn.szkedun.kdew.collector.document.core.plugins.UrlFilterPlugin;
    import cn.szkedun.kdew.collector.document.core.processor.WebProcessorAdapter;
    import cn.szkedun.kdew.collector.document.core.proxy.ProxyBuilder;
    import cn.szkedun.kdew.collector.document.core.proxy.bean.ProxyExtension;
    import cn.szkedun.kdew.collector.document.core.proxy.manager.ProxyManager;
    import cn.szkedun.kdew.collector.document.core.requester.NettyWebRequester;
    import cn.szkedun.kdew.collector.document.core.support.CrawlerBuilder;
    import cn.szkedun.kdew.collector.document.exception.WebException;
    import cn.szkedun.kdew.collector.document.exception.WebParseException;
    import lombok.extern.slf4j.Slf4j;
    import org.apache.commons.lang3.StringUtils;
    import org.jsoup.nodes.Document;
    
    import java.text.ParseException;
    import java.text.SimpleDateFormat;
    import java.util.Date;
    import java.util.concurrent.TimeUnit;
    
    @Slf4j
    public class CrawlerDemo {
        public static void main(String[] args) throws Exception {
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
                    .addLast(new WebProcessorAdapter<TestWebRequest>() {
                        @Override
                        public void process(TestWebRequest request, CrawlerContext context) throws WebParseException {
                            Document document = request.getResponse().getDocument();
                            String title = document.select(".article-title").text();
                            if (StringUtils.isBlank(title)) {
                                title = document.select(".piccontext>h2").text();
                            }
                            String createdAtStr = document.select(".date.updated").attr("datetime");
                            String author = document.select(".author").text()
                                    .replaceAll("[：:]|作者", "");
                            if (StringUtils.isBlank(author)) {
                                author = document.select(".laiyuan").text()
                                        .replaceAll("[：:]|来源", "");
                            }
                            String content = document.select(".article-entry").text();
                            if (StringUtils.isAnyBlank(title, createdAtStr, author, content)) {
                                return;
                            }
                            request.setTitle(title);
                            request.setAuthor(author);
                            request.setContent(content);
                            try {
                                request.setCreatedAt(new SimpleDateFormat("yyyy-MM-dd")
                                        .parse(createdAtStr).getTime());
                            } catch (ParseException e) {
                                throw new WebParseException("日期解析异常");
                            }
                            request.setResolve(true);
                            // 显式调用该方法才会将处理事件传递到下一个处理器中继续处理
                            fireProcess(request, context);
                        }
                    })
                    // 添加页面链接扩散抓取插件(需要配合监听WebLinkEvent事件获取扩散出来的新链接)
                    // 第一个入参是sameDomain——是否强制限定同域名
                    // 第二个参数是allowHomepage——是否允许将首页纳入扩散链接中
                    // 第三个入参是自定义过滤链接规则
                    .addLast(new ExtractUrlPlugin(true, false, s -> {
                        if (s.contains("/video/") ||
                                s.contains("/shipin/") ||
                                s.contains("/weixin") ||
                                s.contains("/ClickAd") ||
                                s.endsWith(".pdf") ||
                                s.endsWith(".docx")) {
                            return false;
                        }
                        return true;
                    }))
                    // 添加URL去重插件，第一个入参表示预期最大容量，第二个参数表示去重误差率
                    .addLast(new UrlFilterPlugin(2000000, 0.0001D))
                    // 添加请求速率控制插件，入参表示允许每秒发起多少个请求
                    .addLast(new RateLimiterPlugin(2))
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
                            log.info("准备请求 -> {}", event.getRequest().getUrl());
                        }
                    })
                    // 监听响应解析前事件
                    .addListener(new WebSyncEventListener<WebBeforeParseEvent>() {
                        @Override
                        public void listen(WebBeforeParseEvent event) throws WebException {
                            log.info("准备解析 -> {}", event.getRequest().getUrl());
                        }
                    })
                    // 监听解析后事件
                    .addListener(new WebSyncEventListener<WebAfterParseEvent<TestWebRequest>>() {
                        @Override
                        public void listen(WebAfterParseEvent<TestWebRequest> event) throws WebException {
                            TestWebRequest request = event.getRequest();
                            if (request.isResolve()) {
                                log.info("爬取结果 -> {}\r\n标题：{}\r\n作者：{}\r\n时间：{}\r\n内容：{}",
                                        request.getResponse().getUrl(),
                                        request.getTitle(),
                                        request.getAuthor(),
                                        new SimpleDateFormat("yyyy-MM-dd")
                                                .format(new Date(request.getCreatedAt())),
                                        request.getContent().substring(0,
                                                request.getContent().length() > 10 ? 10 : request.getContent().length()));
                            } else {
                                log.error("解析失败 -> {}", request.getUrl());
                            }
                        }
                    })
                    // 监听扩散链接事件
                    .addListener(new WebSyncEventListener<WebLinkEvent>() {
                        @Override
                        public void listen(WebLinkEvent event) throws WebException {
                            // 推送新爬取任务给爬虫
                            event.getContext().getCrawler().push(event.getNewRequest());
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
                            log.error("异常 -> {}", event.getThrowable());
                        }
                    })
                    // 启用代理模式，若不需要代理则无需调用该方法
                    .enableProxy(manager)
                    // 设置网络请求最大并发数
                    .setNetworkThread(5)
                    // 设置最大处理线程数
                    .setParseThread(20)
                    .build();
            // 设置爬虫的名字(必须)
            crawler.getContext().setId("测试爬虫");
            // 设置爬虫内统计爬取QPS的频率
            crawler.getContext().getRateResult().setSecond(1);
            // 启动爬虫(非阻塞方法)
            crawler.start();
            // 构建爬取任务
            TestWebRequest request = new TestWebRequest();
            // 设置需要爬取的入口URL
            request.setUrl("http://www.qhlingwang.com/");
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
                        TimeUnit.SECONDS.sleep(5);
                    }
                } catch (InterruptedException e) {
                    return;
                }
            });
            thread.start();
        }
    }
    ```  
    
1. 内置组件一览
    * 爬虫构建器  
        **CrawlerBuilder**  
        
        ```java
        // 生成一个构建器
        public final static CrawlerBuilder create()
        // 添加请求器、管道、处理器、插件
        public CrawlerBuilder addLast(WebType type)
        // 添加同步事件的监听器
        public CrawlerBuilder addListener(WebSyncEventListener listener)
        // 添加异步事件的监听器
        public CrawlerBuilder addListener(WebAsyncEventListener listener)
        // 启用自定义的代理管理器
        public CrawlerBuilder enableProxy(ProxyManager manager)
        // 设置网络请求最大并发数
        public CrawlerBuilder setNetworkThread(int size)
        // 设置处理响应最大线程数
        public CrawlerBuilder setParseThread(int size)
        // 构建爬虫对象
        public Crawler build()
        ```  
    * 网络请求器  
        1. **NettyWebRequester** `推荐使用`  
            基于netty4打造，可使用Proxy代理的HTTP Client，专为该框架而设计，使用nio实现高性能和降低资源消耗的目的。
        1. **ApacheWebRequester**  
            基于apache的阻塞异步http client封装而来，具备高性能、低资源消耗的有点，缺点是目前在该请求器中尚未进行代理封装。
    * 读写管道  
        * **BasicReadWritePipeline**  
        
            ```java
            public BasicReadWritePipeline()
            public BasicReadWritePipeline(int maxTask)
            ```
            maxTask代表最大允许积压的爬取任务数量，超过该值时继续添加任务会返回添加失败的结果。
    * 文档处理器  
        > 如果要使用以下处理器，均要求任务对象是`WebRequestDoc`或其子类。  
        1. **DmicNewsExtractorProcessor**  
            基于DMIC算法实现正文内容自动提取的功能。
        1. **BasicDocTitleProcessor**  
            基于标签提取的方式自动提取文章标题。
        1. **BasicDocPubdateProcessor**  
            基于标签提取的方式自动提取文章发布时间。
        1. **BasicDocDenoiseProcessor**  
            基于预设标签的方式去除html中的噪音数据，放在BasicDocMarkScoreProcessor之前，提高打分准确性。
        1. **BasicDocMarkScoreProcessor**  
            基于计算词频的方式为每一个html标签打分，需要配合BasicDocContentProcessor根据分值提取正文文本。
        1. **BasicDocContentProcessor**  
            根据BasicDocMarkScoreProcessor计算出来的各标签分值，提取分值最大的标签作为正文文本。
        1. **BasicDocCommentsProcessor**  
            根据html标签的重复程度提取评论内容(准确度低)。  
            
        **`注意`** 使用内置通用处理器的提取准确度并不是非常理想，对于精度要求不高的爬取任务，可以按需搭配使用各处理器，但是如果需要精确提取，则建议自行编写相应的文档处理器。
    * 同步事件
        1. **WebCrawlStartEvent**  
            爬虫启动事件  
            爬虫启动之前会触发，事件处理完成后才会开始初始化爬虫。在爬虫的生命周期中，启动事件仅发生一次。
        1. **WebCrawlShutdownEvent**  
            爬虫终止事件  
            在爬虫接收到停止指令后触发，事件处理完成后才会开始停止爬虫。在爬虫的生命周期中，停止事件仅发生一次。
        1. **WebBeforeRequestEvent**  
            网络请求前事件
            爬取任务的网络请求发起前会触发该事件，若在该事件中想阻止请求的发生，只需要抛出WebException异常或其子类异常即可。在爬虫的生命周期中，如果监听了该事件，则每次请求前都会触发。  
            `注意` 该事件的处理线程是**network网络请求线程池**里的线程。
        1. **WebBeforeParseEvent**  
            文档解析前事件  
            爬虫获取请求响应后，在调用WebParser执行处理器前会触发该事件，若想阻止解析该文档，只需要抛出WebException异常或其子类异常即可。在爬虫的生命周期中，如果监听了该事件，则每次解析前都会触发。 
            `注意` 该事件的处理线程是**parse解析线程池**里的线程。
        1. **WebAfterParseEvent**  
            文档解析后事件
            解析器调用完处理器链之后会触发该事件。在爬虫的生命周期中，如果监听了该事件，则每次解析后都会触发。 
            `注意` 该事件的处理线程是**parse解析线程池**里的线程。
        1. **WebLinkEvent**  
            链接扩散事件
            配置了`ExtractUrlPlugin`插件的情况下，当扩散出链接时会触发该事件。
            `注意` 该事件的处理线程是**parse解析线程池**里的线程。
    * 异步事件
        1. **AllExceptionEvent**  
            所有未监听异常事件  
            当监听了该异常时，一旦有其他未监听异常发生时，异常会冒泡至该事件处。
        1. **WebExceptionEvent**  
            爬虫执行异常监听事件  
            该异常事件是`WebNetworkExceptionEvent`/`WebParseExceptionEvent`/`WebRejectedExecutionExceptionEvent`的父类事件。
        1. **WebNetworkExceptionEvent**  
            网络异常事件  
            当发生连接失败/超时/响应码非2xx(若不允许3xx自动跳转)的情况时会触发该事件。
        1. **WebParseExceptionEvent**  
            解析异常事件  
            当解析器调用处理器链进行处理时发生异常，则会触发该事件。
        1. **WebRejectedExecutionExceptionEvent**  
            任务拒绝事件  
            当任务池饱和无法接收新任务时会触发该事件。
    * 爬虫上下文
        ```java
        // 获取爬虫启动时间
        public long getBeginAt()
        // 获取爬虫终止时间
        public long getEndAt()
        // 爬虫运行状态
        public boolean isRunning()
        // 网络请求最大并发数
        public int getNetworkThreadSize()
        // 解析最大并发数
        public int getParseThreadSize()
        // 推送同步事件
        public void postSyncEvent(WebSyncEvent event) throws WebException
        // 推送异步事件
        public void postAsyncEvent(WebAsyncEvent event)
        // 获取爬虫对象
        public Crawler getCrawler()
        // 获取QPS统计对象
        public RateResult getRateResult()
        // 获取爬虫名称
        public String getId()
        // 设置爬虫名称
        public CrawlerContext setId(String id)
        // 终止爬虫
        public void close()
        ```  
        
### 框架概念

* **请求器**  

    ```java
    // 顶级接口
    cn.szkedun.kdew.collector.document.core.requester.WebRequester
    ```
    用于真正与网页交互的对象，发出请求、接收响应结果并将结果状态封装到请求对象`WebRequest`里的成员变量`WebResponse`。

* **爬虫**  

    ```java
    // 顶级接口
    cn.szkedun.kdew.collector.document.core.Crawler
    ```
    该接口只有三个职责：  
    1. 启动爬虫；
    1. 推送需要爬取的任务；
    1. 关闭爬虫。  

    另外还可利用该接口获取爬虫的id和对应的爬虫上下文`CrawlerContext`。

* **爬虫上下文**  

    ```java
    // 顶级接口
    cn.szkedun.kdew.collector.document.core.context.CrawlerContext
    ```
    该接口包含对应爬虫的所有资料， 并且可获取爬虫的内部组件，以及注册监听事件也是使用上下文进行的。

* **操作句柄**  

    ```java
    // 顶级接口
    cn.szkedun.kdew.collector.document.factory.bean.WebHandler
    ```
    它只有一个职责，就是获取任务，经过请求之后返回响应对象并交由解析器做后续处理。

* **解析器**  

    ```java
    // 顶级接口
    cn.szkedun.kdew.collector.document.core.parser.WebParser
    ```
    它负责注册各种处理器，并在获得请求返回结果之后按注册顺序调用处理器对结果进行处理。

* **处理器**  

    ```java
    // 顶级接口
    cn.szkedun.kdew.collector.document.core.processor.WebProcessor
    ```
    实现该接口并注册到解析器里就可以对返回结果进行数据处理。

* **管道**  
    目前管道有两种类型：
    1. 读写双向管道  
    
        ```java
        // 顶级接口
        cn.szkedun.kdew.collector.document.core.pipeline.ReadWritePipeline
        ```  
    1. 只写单向管道  
    
        ```java
        // 顶级接口
        cn.szkedun.kdew.collector.document.core.pipeline.WritePipeline
        ```  
    管道意味着解耦来源和去向，接入管道后爬虫并不关心它的任务从哪里来，处理结果输出到哪里去，这些均交由使用者自行定制信息源和数据池。

* **事件**  
    1. 异步事件  
    
        ```java
        cn.szkedun.kdew.collector.document.core.context.event.listener.WebAsyncEventListener
        ```
    1. 同步事件  
    
        ```java
        cn.szkedun.kdew.collector.document.core.context.event.listener.WebSyncEventListener
        ```  
        
    重写方法`listen(E event)`并且注册到爬虫上下文里就可以实现对应事件的监听。  
    同步事件只能注册为同步事件，异步事件只能注册为异步事件。 
     
    内置`异步`事件(基本均为异常回调事件)：  
    > 1. AllExceptionEvent  
    > 1. WebExceptionEvent  
    > 1. WebNetworkExceptionEvent  
    > 1. WebParseExceptionEvent  
    > 1. WebRejectedExecutionExceptionEvent  

    内置`同步`事件：
    > 1. WebCrawlStartEvent 爬虫启动事件  
    > 1. WebCrawlShutdownEvent 爬虫关闭事件(可用于清理资源、保存爬虫状态等)  
    > 1. WebBeforeRequestEvent 爬虫真正发起网络请求前事件(若要阻止请求可使用抛出异常的方式中断流程)  
    > 1. WebBeforeParseEvent 调用解析器之前事件    
    > 1. WebAfterParseEvent 调用解析器之后事件  
    > 1. WebLinksEvent 页面链接扩散事件

* **插件**  

    ```java
    // 顶级接口
    cn.szkedun.kdew.collector.document.core.plugins.WebPlugin
    ```  
    
    插件可用于替换爬虫的解析器、操作句柄，增强所有爬虫内部组件。  
    ps：欢迎探索更多有趣的插件用法。