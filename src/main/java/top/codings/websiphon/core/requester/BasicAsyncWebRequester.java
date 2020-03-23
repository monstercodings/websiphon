package top.codings.websiphon.core.requester;

import com.alibaba.fastjson.JSON;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.MapUtils;
import org.apache.http.*;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.*;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.protocol.ResponseContentEncoding;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.impl.nio.conn.PoolingNHttpClientConnectionManager;
import org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.http.nio.conn.NoopIOSessionStrategy;
import org.apache.http.nio.conn.SchemeIOSessionStrategy;
import org.apache.http.nio.conn.ssl.SSLIOSessionStrategy;
import org.apache.http.nio.reactor.ConnectingIOReactor;
import org.apache.http.protocol.RequestConnControl;
import org.apache.http.protocol.RequestContent;
import org.apache.http.protocol.RequestTargetHost;
import org.apache.http.ssl.SSLContextBuilder;
import top.codings.websiphon.bean.WebRequest;
import top.codings.websiphon.bean.WebResponse;
import top.codings.websiphon.core.context.CrawlerContext;
import top.codings.websiphon.core.context.event.async.WebNetworkExceptionEvent;
import top.codings.websiphon.exception.WebException;
import top.codings.websiphon.exception.WebNetworkException;
import top.codings.websiphon.util.ByteUtils;
import top.codings.websiphon.util.HeadersUtils;
import top.codings.websiphon.util.HttpDecodeUtils;
import top.codings.websiphon.util.HttpOperator;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.nio.charset.Charset;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicInteger;

@NoArgsConstructor
@Slf4j
public class BasicAsyncWebRequester implements WebRequester<WebRequest> {
    @Getter
    private boolean health = false;

    @Setter
    @Getter
    private boolean redirect = false;
    @Setter
    @Getter
    private boolean ignoreHttpError = false;
    @Setter
    @Getter
    private int maxRedirects = 3;
    @Setter
    @Getter
    private int maxPerRoute = 2;
    private CloseableHttpAsyncClient httpAsyncClient;
//    private RequestConfig requestConfig;
    private AtomicInteger size = new AtomicInteger(0);

    public BasicAsyncWebRequester(boolean redirect) {
        this.redirect = redirect;
    }

    @Override
    public void init() throws Exception {
        int ioThreadCount = Runtime.getRuntime().availableProcessors();
        int maxConnTotal = Integer.MAX_VALUE;
        try {
            /*requestConfig = RequestConfig.custom()
                    .setMaxRedirects(2)
                    .setContentCompressionEnabled(false)
                    .setExpectContinueEnabled(true)
                    .setRedirectsEnabled(redirect)
                    .build();*/

            IOReactorConfig ioReactorConfig = IOReactorConfig.custom()
//                    .setBacklogSize(10240)
                    .setSoTimeout(30000)
                    .setTcpNoDelay(true)
                    .setIoThreadCount(ioThreadCount)
                    .setSoKeepAlive(true)
                    .build();

            ConnectingIOReactor ioReactor = new DefaultConnectingIOReactor(ioReactorConfig);
            // 忽略证书验证
            SSLIOSessionStrategy sslioSessionStrategy = new SSLIOSessionStrategy(
                    SSLContextBuilder.create().loadTrustMaterial((x509Certificates, s) -> true).build(),
                    (hostname, session) -> true);
            Registry<SchemeIOSessionStrategy> registry = RegistryBuilder.<SchemeIOSessionStrategy>create()
                    .register("http", NoopIOSessionStrategy.INSTANCE)
                    .register("https", sslioSessionStrategy)
                    .build();
            PoolingNHttpClientConnectionManager connManager = new PoolingNHttpClientConnectionManager(ioReactor, registry);
            connManager.setMaxTotal(maxConnTotal);
            connManager.setDefaultMaxPerRoute(maxPerRoute);
//            CredentialsProvider credsProvider = new BasicCredentialsProvider();
            httpAsyncClient = HttpAsyncClients.custom()
                    .setConnectionManager(connManager)
//                    .setDefaultRequestConfig(requestConfig)
//                    .setDefaultCredentialsProvider(credsProvider)
//                    .setSSLContext(SSLContextBuilder.create().loadTrustMaterial((x509Certificates, s) -> true).build())
//                    .setSSLHostnameVerifier((hostname, session) -> true)
                    .addInterceptorLast(new RequestTargetHost())
//                    .addInterceptorLast(new RequestAcceptEncoding(Arrays.asList("compress")))
                    .addInterceptorLast(new RequestConnControl())
                    .addInterceptorLast(new RequestContent(true))
                    .addInterceptorLast((HttpRequestInterceptor) (httpRequest, httpContext) -> {
                        httpRequest.setHeader("Accept-Encoding", "compress");
                        // 添加请求头伪装
                        WebRequest webRequest = ((HttpClientContext) httpContext).getAttribute(WebRequest.class.getName(), WebRequest.class);
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
                    .addInterceptorLast(new ResponseContentEncoding())
                    .addInterceptorLast((HttpResponseInterceptor) (httpResponse, httpContext) -> {
                        WebRequest webRequest = ((HttpClientContext) httpContext).getAttribute(WebRequest.class.getName(), WebRequest.class);
                        WebResponse webResponse = webRequest.getResponse();
                        if (null == webResponse) {
                            webResponse = new WebResponse();
                            webRequest.setResponse(webResponse);
                        }
                        int respCode = httpResponse.getStatusLine().getStatusCode();
                        // 允许跳转且处于跳转状态
                        if (respCode >= 300 && respCode < 400 && redirect) {
                            Header locationHeader = httpResponse.getFirstHeader("location");
                            if (null != locationHeader) {
                                String location = locationHeader.getValue();
                                location = location.replace(":80", "").replace(":443", "");
                                webResponse.setRedirect(true);
                                webResponse.setRedirectUrl(HttpOperator.recombineLink(location, webRequest.getUrl()));
                            }
                            return;
                        }
                        // 装填响应头信息
                        for (Header header : httpResponse.getAllHeaders()) {
                            if (webResponse.getHeaders().containsKey(header.getName())) {
                                String value = webResponse.getHeaders().get(header.getName());
                                value = value + ";" + header.getValue();
                                webResponse.getHeaders().put(header.getName(), value);
                            } else {
                                webResponse.getHeaders().put(header.getName(), header.getValue());
                            }
                        }
                    })
                    .build();
            httpAsyncClient.start();
            health = true;
        } catch (Exception e) {
            throw new WebException("异步请求器初始化失败", e);
        }
    }

    @Override
    public void execute(WebRequest webRequest) throws WebNetworkException {
        try {
            size.incrementAndGet();
            HttpRequestBase httpRequest;
            httpRequest = initMethod(webRequest);
            initHeaders(webRequest, httpRequest);
            if (webRequest.getBody() instanceof JSON) {
                httpRequest.setHeader("content-type", "application/json;charset=UTF-8");
            }
            initConfig(webRequest, httpRequest);
            HttpClientContext context = HttpClientContext.create();
            context.setAttribute(WebRequest.class.getName(), webRequest);
            httpAsyncClient.execute(
                    httpRequest,
                    context,
                    new AsyncFutureCallback(webRequest));
        } catch (Exception e) {
            size.decrementAndGet();
            throw new WebNetworkException("执行异步请求失败", e);
        }

    }

    /**
     * 装填响应主体
     *
     * @param httpResponse
     * @param webRequest
     * @throws IOException
     */
    private void fillResponseBody(HttpResponse httpResponse, WebRequest webRequest) throws IOException {
        WebResponse webResponse = webRequest.getResponse();
        // 装填响应状态
        webResponse.setResult(WebResponse.Result.valueOf(httpResponse.getStatusLine().getStatusCode()));
        // 装填响应码
        webResponse.setStatusCode(httpResponse.getStatusLine().getStatusCode());
        // 装填URL
        webResponse.setUrl(webRequest.getUrl());
        // 装填响应内容
        webResponse.setBytes(ByteUtils.readAllBytes(httpResponse.getEntity().getContent()));
        ContentType contentType;
        Charset charset;
        String encoding;
        if ((contentType = ContentType.get(httpResponse.getEntity())) != null && (charset = contentType.getCharset()) != null) {
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
    }

    /**
     * 初始化特定配置
     *
     * @param webRequest
     * @param httpRequest
     */
    private void initConfig(WebRequest webRequest, HttpRequestBase httpRequest) {
        RequestConfig.Builder builder = RequestConfig.custom();
        Proxy proxy = webRequest.getProxy();
        if (proxy != null && proxy != Proxy.NO_PROXY) {
            HttpHost proxyHost = new HttpHost(((InetSocketAddress) proxy.address()).getHostName(), ((InetSocketAddress) proxy.address()).getPort());
            builder.setProxy(proxyHost);
        }
        RequestConfig config = builder
                .setSocketTimeout(webRequest.getTimeout())
                .setConnectTimeout(webRequest.getTimeout())
                .setConnectionRequestTimeout(webRequest.getTimeout())
                .setRedirectsEnabled(redirect)
                .setMaxRedirects(maxRedirects)
                .setContentCompressionEnabled(false)
                .setExpectContinueEnabled(true)
                .build();
        httpRequest.setConfig(config);
    }

    /**
     * 根据请求方法初始化请求对象
     *
     * @param webRequest
     * @return
     */
    private HttpRequestBase initMethod(WebRequest webRequest) {
        HttpRequestBase httpRequest;
        switch (webRequest.getMethod()) {
            case GET:
                httpRequest = new HttpGet(webRequest.getUrl());
                break;
            case HEAD:
                httpRequest = new HttpHead(webRequest.getUrl());
                break;
            case POST:
                httpRequest = new HttpPost(webRequest.getUrl());
                initBody(webRequest, (HttpEntityEnclosingRequestBase) httpRequest);
                break;
            case PUT:
                httpRequest = new HttpPut(webRequest.getUrl());
                initBody(webRequest, (HttpEntityEnclosingRequestBase) httpRequest);
                break;
            case PATCH:
                httpRequest = new HttpPatch(webRequest.getUrl());
                initBody(webRequest, (HttpEntityEnclosingRequestBase) httpRequest);
                break;
            case DELETE:
                httpRequest = new HttpDelete(webRequest.getUrl());
                break;
            default:
                throw new IllegalArgumentException(String.format("不支持该请求方法[%s]", webRequest.getMethod().name()));
        }
        return httpRequest;
    }

    /**
     * 初始化请求承载数据
     *
     * @param webRequest
     * @param httpRequest
     */
    private void initBody(WebRequest webRequest, HttpEntityEnclosingRequestBase httpRequest) {
        if (webRequest.getBody() != null) {
            HttpEntityEnclosingRequestBase httpEntityEnclosingRequest = httpRequest;
            HttpEntity httpEntity;
            if (webRequest.getBody() instanceof String) {
                httpEntity = new StringEntity(webRequest.getBody().toString(), "utf-8");
            } else if (webRequest.getBody() instanceof JSON) {
                httpEntity = new StringEntity(webRequest.getBody().toString(), "utf-8");
            } else if (webRequest.getBody() instanceof byte[]) {
                BasicHttpEntity basicHttpEntity = new BasicHttpEntity();
                InputStream inputStream = new ByteArrayInputStream((byte[]) webRequest.getBody());
                basicHttpEntity.setContent(inputStream);
                basicHttpEntity.setContentLength(((byte[]) webRequest.getBody()).length);
                httpEntity = basicHttpEntity;
            } else {
                throw new IllegalArgumentException(String.format("请求的body类型不支持 ", webRequest.getBody().getClass()));
            }
            httpEntityEnclosingRequest.setEntity(httpEntity);
        }
    }

    /**
     * 初始化头信息
     *
     * @param webRequest
     * @param httpRequest
     */
    private void initHeaders(WebRequest webRequest, HttpRequestBase httpRequest) {
        if (MapUtils.isEmpty(webRequest.getHeaders())) {
            HeadersUtils.getHeaders().forEach((key, value) -> {
                if (httpRequest.containsHeader(key)) {
                    return;
                }
                httpRequest.setHeader(key, value);
            });
        } else {
            webRequest.getHeaders().forEach((key, value) -> {
                if (httpRequest.containsHeader(key)) {
                    return;
                }
                httpRequest.setHeader(key, value);
            });
        }
    }

    @Override
    public int size() {
        return size.get();
    }

    @Override
    public void close() {
        try {
            health = false;
            httpAsyncClient.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class AsyncFutureCallback implements FutureCallback<HttpResponse> {
        CrawlerContext crawlerContext;
        WebRequest webRequest;

        public AsyncFutureCallback(WebRequest webRequest) {
            this.webRequest = webRequest;
            crawlerContext = webRequest.context();
        }

        @Override
        public void completed(HttpResponse httpResponse) {
            size.decrementAndGet();
            try {
                WebResponse webResponse = webRequest.getResponse();
                if (null == webResponse) {
                    webRequest.succeed();
                    return;
                }
                fillResponseBody(httpResponse, webRequest);
                if (!ignoreHttpError) {
                    int respCode = webResponse.getStatusCode();
                    // 判断响应码是否正常
                    if (!(respCode >= 200 && respCode < 300)) {
                        throw new WebNetworkException(String.format("响应码不是2xx [%d]", httpResponse.getStatusLine().getStatusCode()));
                    }
                }
            } catch (Exception e) {
                WebNetworkExceptionEvent event = new WebNetworkExceptionEvent();
                event.setThrowable(e);
                event.setContext(crawlerContext);
                event.setRequest(webRequest);
                webRequest.getResponse().setErrorEvent(event);
            } finally {
                webRequest.succeed();
                HttpClientUtils.closeQuietly(httpResponse);
            }
        }

        @Override
        public void failed(Exception e) {
            size.decrementAndGet();
            try {
                Throwable throwable = e;
                if (e instanceof HttpException) {
                    throwable = e.getCause();
                }
                if (webRequest.getResponse() == null) {
                    webRequest.setResponse(new WebResponse());
                }
                WebNetworkExceptionEvent event = new WebNetworkExceptionEvent();
                event.setThrowable(throwable);
                event.setContext(crawlerContext);
                event.setRequest(webRequest);
                webRequest.getResponse().setErrorEvent(event);
            } finally {
                webRequest.succeed();
            }
        }

        @Override
        public void cancelled() {
            size.decrementAndGet();
            try {
                webRequest.setResponse(new WebResponse());
                WebNetworkExceptionEvent event = new WebNetworkExceptionEvent();
                event.setThrowable(new CancellationException("请求被强制取消"));
                event.setContext(crawlerContext);
                event.setRequest(webRequest);
                webRequest.getResponse().setErrorEvent(event);
            } finally {
                webRequest.succeed();
            }
        }
    }

}
