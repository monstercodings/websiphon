package top.codings.websiphon.core.requester;

import top.codings.websiphon.bean.PushResult;
import top.codings.websiphon.bean.WebRequest;
import top.codings.websiphon.bean.WebResponse;
import top.codings.websiphon.core.context.CrawlerContext;
import top.codings.websiphon.core.context.event.async.WebNetworkExceptionEvent;
import top.codings.websiphon.exception.WebNetworkException;
import top.codings.websiphon.util.HeadersUtils;
import top.codings.websiphon.util.HttpDecodeUtils;
import top.codings.websiphon.util.HttpOperator;
import com.alibaba.fastjson.JSON;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.*;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.nio.DefaultHttpClientIODispatch;
import org.apache.http.impl.nio.DefaultNHttpClientConnectionFactory;
import org.apache.http.impl.nio.SSLNHttpClientConnectionFactory;
import org.apache.http.impl.nio.pool.BasicNIOConnFactory;
import org.apache.http.impl.nio.pool.BasicNIOConnPool;
import org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.nio.protocol.BasicAsyncRequestProducer;
import org.apache.http.nio.protocol.BasicAsyncResponseConsumer;
import org.apache.http.nio.protocol.HttpAsyncRequestExecutor;
import org.apache.http.nio.protocol.HttpAsyncRequester;
import org.apache.http.nio.reactor.ConnectingIOReactor;
import org.apache.http.nio.reactor.IOEventDispatch;
import org.apache.http.protocol.*;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.nio.charset.Charset;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 已作废
 * 请使用{@link BasicAsyncWebRequester}代替
 * @param <W>
 */
@Deprecated
@Slf4j
public class ApacheWebRequester<W extends WebRequest> implements WebRequester<W> {
    @Getter
    private volatile boolean health = true;
    private ConnectingIOReactor ioReactor;
    private HttpAsyncRequester requester;
    private BasicNIOConnPool pool;
    private Thread reactorThread;
    private boolean redirect;
    private AtomicInteger size = new AtomicInteger(0);

    public ApacheWebRequester() {
        this(false);
    }

    public ApacheWebRequester(boolean redirect) {
        this.redirect = redirect;
    }

    @Override
    public void init() throws Exception {
        // 创建HTTP协议处理
        //RequestTargetHost，添加http host头
        // 添加UserAgent
        //RequestContent，发起请求最重要的拦截器
        // 添加请求头伪装
        // 响应拦截器，处理非2xx响应
        // 装填响应头信息
        // 判断响应码是否正常
        HttpProcessor httpproc = HttpProcessorBuilder.create()
                //RequestTargetHost，添加http host头
                .addLast(new RequestTargetHost())
                .addLast(new RequestDate())
                .addLast(new RequestConnControl())
                // 添加UserAgent
                .addLast(new RequestUserAgent("Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/76.0.3809.132 Safari/537.36"))
                .addLast(new RequestExpectContinue(true))
                //RequestContent，发起请求最重要的拦截器
                .addLast(new RequestContent())
                .addLast((HttpRequestInterceptor) (httpRequest, httpContext) -> {
                    // 添加请求头伪装
                    W webRequest = (W) httpContext.getAttribute("webRequest");
                    if (MapUtils.isEmpty(webRequest.getHeaders())) {
                        HeadersUtils.getHeaders().forEach((key, value) -> {
                            if (httpRequest.containsHeader(key)) {
                                return;
                            }
                            httpRequest.addHeader(key, value);
                        });
                    } else {
                        webRequest.getHeaders().forEach((key, value) -> {
                            if (httpRequest.containsHeader(key)) {
                                return;
                            }
                            httpRequest.addHeader(key, value);
                        });
                    }
                    if (!httpRequest.containsHeader("Referer")) {
                        httpRequest.addHeader("Referer", webRequest.getUrl());
                    }
                })
                // 响应拦截器，处理非2xx响应
                .addLast((HttpResponseInterceptor) (response, context) -> {
                    try {
                        W webRequest = (W) context.getAttribute("webRequest");
                        CrawlerContext crawlerContext = (CrawlerContext) context.getAttribute("crawlerContext");
                        WebResponse webResponse = new WebResponse();
                        // 装填响应头信息
                        for (Header header : response.getAllHeaders()) {
                            if (webResponse.getHeaders().containsKey(header.getName())) {
                                String value = webResponse.getHeaders().get(header.getName());
                                value = value + ";" + header.getValue();
                                webResponse.getHeaders().put(header.getName(), value);
                            } else {
                                webResponse.getHeaders().put(header.getName(), header.getValue());
                            }
                        }
                        webRequest.setResponse(webResponse);
                        int respCode = response.getStatusLine().getStatusCode();
                        // 判断响应码是否正常
                        if (respCode >= 300 && respCode < 400) {
                            if (redirect) {
                                String location = Optional.ofNullable(webRequest.getResponse().getHeaders().get("Location")).orElse(webRequest.getResponse().getHeaders().get("location"));
                                if (StringUtils.isNotBlank(location)) {
                                    location = location.replace(":80", "").replace(":443", "");
                                    W redirectRequest;
                                    try {
                                        redirectRequest = (W) BeanUtils.cloneBean(webRequest);
                                    } catch (Exception e) {
                                        throw new HttpException("", new WebNetworkException("克隆跳转对象失败", e));
                                    }
                                    redirectRequest.setUrl(HttpOperator.recombineLink(location, webRequest.getUrl()));
                                    webResponse.setRedirect(true);
                                    redirectRequest.setResponse(webResponse);
                                    PushResult pushResult = crawlerContext.getCrawler().push(redirectRequest);
                                    if (pushResult != PushResult.SUCCESS && pushResult != PushResult.URL_REPEAT) {
                                        log.debug("跳转失败 [{}] | 原链接:{} | 跳转链接:{}", pushResult.value, webRequest.getUrl(), redirectRequest.getUrl());
                                    }
                                    webRequest.setResponse(null);
                                }
                            } else {
                                throw new HttpException("", new WebNetworkException(String.format("响应码不是2xx [%d]", response.getStatusLine().getStatusCode())));
                            }
                        } else if (!(respCode >= 200 && respCode < 300)) {
                            throw new HttpException("", new WebNetworkException(String.format("响应码不是2xx [%d]", response.getStatusLine().getStatusCode())));
                        }
                    } catch (HttpException e) {
                        throw e;
                    } catch (Exception e) {
                        throw new HttpException("发生未知异常", e);
                    }
                })
                .build();
        // 创建客户端HTTP协议处理器
        HttpAsyncRequestExecutor protocolHandler = new HttpAsyncRequestExecutor();
        // 创建客户端I/0事件分发
        IOEventDispatch ioEventDispatch = new DefaultHttpClientIODispatch(protocolHandler, ConnectionConfig.DEFAULT);
        // 创建客户端I/O反应器
        ioReactor = new DefaultConnectingIOReactor();
        // 创建HTTP连接池
        pool = new BasicNIOConnPool(
                ioReactor,
                new BasicNIOConnFactory(
                        new DefaultNHttpClientConnectionFactory(ConnectionConfig.DEFAULT),
                        new SSLNHttpClientConnectionFactory(ConnectionConfig.DEFAULT)),
                30000);
        // 数限2个连接总数
        pool.setDefaultMaxPerRoute(Integer.MAX_VALUE);
        pool.setMaxTotal(Integer.MAX_VALUE);
        requester = new HttpAsyncRequester(httpproc);
        reactorThread = new Thread(() -> {
            try {
                // Ready to go!
                ioReactor.execute(ioEventDispatch);
            } catch (InterruptedIOException ex) {
                log.warn("Nio反应器终止执行");
            } catch (Exception e) {
                log.error("Nio反应器发生异常", e);
            }
            log.warn("Nio反应器终止执行");
        });
        reactorThread.setName("Nio请求器线程");
        reactorThread.start();
    }

    @Override
    public void execute(W w) {
        CrawlerContext crawlerContext = w.context();
        try {
            _execute(w, crawlerContext);
            size.getAndIncrement();
        } catch (Exception e) {
            WebNetworkExceptionEvent event = new WebNetworkExceptionEvent();
            WebResponse response = new WebResponse();
            w.setResponse(response);
            event.setThrowable(e);
            event.setRequest(w);
            response.setErrorEvent(event);
            crawlerContext.finishRequest(w);
        }
    }

    @Override
    public int size() {
        return size.get();
    }

    protected void _execute(W webRequest, CrawlerContext crawlerContext) {
        HttpOperator.HttpProtocol httpProtocol = HttpOperator.resolve(webRequest.getUrl());
        HttpHost target = new HttpHost(httpProtocol.getHost(), httpProtocol.getPort(), httpProtocol.getScheme());
        BasicHttpRequest request = new BasicHttpRequest(webRequest.getMethod().name(), httpProtocol.getPath());
        HttpCoreContext coreContext = HttpCoreContext.create();
        coreContext.setAttribute("webRequest", webRequest);
        coreContext.setAttribute("crawlerContext", crawlerContext);
        requester.execute(new BasicAsyncRequestProducer(target, request), new BasicAsyncResponseConsumer(), pool, coreContext,
                new FutureCallback<HttpResponse>() {
                    @Override
                    public void completed(HttpResponse response) {
                        size.getAndDecrement();
                        WebResponse webResponse = webRequest.getResponse();
                        if (null == webResponse) {
                            crawlerContext.finishRequest(webRequest);
                            return;
                        }
                        try {
                            // 装填响应状态
                            webResponse.setResult(WebResponse.Result.valueOf(response.getStatusLine().getStatusCode()));
                            // 装填响应码
                            webResponse.setStatusCode(response.getStatusLine().getStatusCode());
                            // 装填URL
                            webResponse.setUrl(webRequest.getUrl());
                            // 装填响应内容
                            webResponse.setBytes(EntityUtils.toByteArray(response.getEntity()));
                            ContentType contentType;
                            Charset charset;
                            String encoding;
                            if ((contentType = ContentType.get(response.getEntity())) != null && (charset = contentType.getCharset()) != null) {
                                encoding = charset.name();
                            } else {
                                // 查找编码
                                if (contentType != null && contentType.getMimeType().startsWith("text")) {
                                    encoding = HttpDecodeUtils.findCharset(webResponse.getBytes());
                                } else {
                                    encoding = "utf-8";
                                }
                            }
                            if (null != contentType) {
                                // 装填响应类型
                                webResponse.setContentType(contentType.getMimeType());
                                if (contentType.getMimeType().contains("json")) {
                                    webResponse.setJson((JSON) JSON.parse(new String(webResponse.getBytes(), encoding)));
                                } else if (contentType.getMimeType().startsWith("text")) {
                                    webResponse.setHtml(new String(webResponse.getBytes(), encoding));
                                }
                            }
                        } catch (Exception e) {
                            WebNetworkExceptionEvent event = new WebNetworkExceptionEvent();
                            event.setThrowable(e);
                            event.setRequest(webRequest);
                            webRequest.getResponse().setErrorEvent(event);
                        } finally {
                            crawlerContext.finishRequest(webRequest);
                        }
                    }

                    @Override
                    public void failed(final Exception e) {
                        size.getAndDecrement();
                        try {
                            Throwable throwable = e;
                            if (e instanceof HttpException) {
                                throwable = e.getCause();
                            }
                            webRequest.setResponse(new WebResponse());
                            WebNetworkExceptionEvent event = new WebNetworkExceptionEvent();
                            event.setThrowable(throwable);
                            event.setRequest(webRequest);
                            webRequest.getResponse().setErrorEvent(event);
                        } finally {
                            crawlerContext.finishRequest(webRequest);
                        }
                    }

                    @Override
                    public void cancelled() {
                        size.getAndDecrement();
                        try {
                            webRequest.setResponse(new WebResponse());
                            WebNetworkExceptionEvent event = new WebNetworkExceptionEvent();
                            event.setThrowable(new CancellationException("请求被强制取消"));
                            event.setRequest(webRequest);
                            webRequest.getResponse().setErrorEvent(event);
                        } finally {
                            crawlerContext.finishRequest(webRequest);
                        }
                    }
                });
    }

    @Override
    public void close() {
        if (null != ioReactor) {
            try {
                ioReactor.shutdown();
            } catch (IOException e) {
                log.error("关闭Nio反应器发生异常", e);
            }
        }
    }
}
