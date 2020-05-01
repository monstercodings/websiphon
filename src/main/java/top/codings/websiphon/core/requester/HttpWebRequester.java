package top.codings.websiphon.core.requester;

import com.alibaba.fastjson.JSON;
import lombok.AllArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.http.*;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.DeflateInputStreamFactory;
import org.apache.http.client.entity.GZIPInputStreamFactory;
import org.apache.http.client.entity.InputStreamFactory;
import org.apache.http.client.methods.*;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.protocol.RequestAcceptEncoding;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.NoConnectionReuseStrategy;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.nio.conn.ssl.SSLIOSessionStrategy;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;
import top.codings.websiphon.bean.BasicWebRequest;
import top.codings.websiphon.bean.WebRequest;
import top.codings.websiphon.bean.WebResponse;
import top.codings.websiphon.core.context.CrawlerContext;
import top.codings.websiphon.core.context.event.sync.WebDownloadEvent;
import top.codings.websiphon.core.proxy.bean.WebProxy;
import top.codings.websiphon.exception.WebNetworkException;
import top.codings.websiphon.util.HttpDecodeUtils;
import top.codings.websiphon.util.HttpOperator;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Slf4j
public class HttpWebRequester implements WebRequester<BasicWebRequest> {
    private boolean redirect;
    private CloseableHttpAsyncClient client;
    private RequestConfig config;
    private Registry<InputStreamFactory> decoderRegistry;
    @Setter
    private CrawlerContext context;

    public HttpWebRequester() {
        this(false);
    }

    public HttpWebRequester(boolean redirect) {
        this.redirect = redirect;
    }

    @Override
    public void init() throws Exception {
        decoderRegistry = RegistryBuilder.<InputStreamFactory>create()
                .register("gzip", GZIPInputStreamFactory.getInstance())
                .register("x-gzip", GZIPInputStreamFactory.getInstance())
                .register("deflate", DeflateInputStreamFactory.getInstance())
                .build();
        config = RequestConfig
                .custom()
                .setContentCompressionEnabled(true)
                .setRedirectsEnabled(redirect)
                .setRelativeRedirectsAllowed(redirect)
                .setCircularRedirectsAllowed(false)
                .build();
        client = HttpAsyncClients
                .custom()
                .setSSLStrategy(new SSLIOSessionStrategy(
                        SSLContextBuilder.create().loadTrustMaterial((x509Certificates, s) -> true).build(),
                        (hostname, session) -> true)
                )
                .setConnectionReuseStrategy(NoConnectionReuseStrategy.INSTANCE)
                .addInterceptorLast(new RequestAcceptEncoding())
                .addInterceptorLast((HttpRequestInterceptor) (request, context) -> {
                    WebRequest webRequest = (WebRequest) context.getAttribute("webRequest");
                    webRequest.headers().forEach(request::addHeader);
                    if (!request.containsHeader("referer")) {
                        request.addHeader("referer", webRequest.uri());
                    }
                })
                .addInterceptorLast((HttpResponseInterceptor) (response, context) -> {
                    WebRequest webRequest = (WebRequest) context.getAttribute("webRequest");
                    int code = response.getStatusLine().getStatusCode();
                    if (code >= 300 && code < 400) {
                        webRequest.response().setRedirect(true);
                        Optional.ofNullable(response.getFirstHeader("Location")).ifPresent(header -> webRequest.response().setRedirectUrl(HttpOperator.recombineLink(header.getValue(), webRequest.uri())));
                    }
                })
//                .addInterceptorLast(new ResponseContentEncoding())
                .disableCookieManagement()
                .setDefaultRequestConfig(config)
                .setDefaultHeaders(Arrays.asList(
                        new BasicHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3"),
                        new BasicHeader("Accept-Language", "zh-CN,zh;q=0.9"),
                        new BasicHeader("Cache-Control", "no-cache"),
                        new BasicHeader("Connection", "keep-alive"),
                        new BasicHeader("Pragma", "no-cache"),
                        new BasicHeader("DNT", "1"),
                        new BasicHeader("Upgrade-Insecure-Requests", "1")
                ))
                .setUserAgent("Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/73.0.3683.86 Safari/537.36")
                .setMaxConnPerRoute(10)
                .setMaxConnTotal(100)
                .build();
        client.start();
    }

    @Override
    public void execute(BasicWebRequest basicWebRequest) throws WebNetworkException {
        HttpUriRequest request = initMethod(basicWebRequest);
        HttpClientContext context = HttpClientContext.create();
        context.setAttribute("webRequest", basicWebRequest);
        RequestConfig.Builder builder = RequestConfig
                .copy(config)
                .setSocketTimeout(basicWebRequest.timeout())
                .setConnectTimeout(basicWebRequest.timeout())
                .setConnectionRequestTimeout(basicWebRequest.timeout());
        WebProxy proxy = basicWebRequest.getProxy();
        if (proxy != null && proxy != WebProxy.NO_PROXY) {
            builder.setProxy(new HttpHost(proxy.getProxyIp(), proxy.getProxyPort()));
        }
        context.setRequestConfig(builder.build());
        client.execute(request, context, new CustomFutureCallback(basicWebRequest));
    }

    @Override
    public void close() {
        if (null != client) {
            try {
                client.close();
            } catch (IOException e) {
                log.error("关闭HTTP请求器异常", e);
            }
        }
    }

    /**
     * 根据请求方法初始化请求对象
     *
     * @param webRequest
     * @return
     */
    private HttpRequestBase initMethod(WebRequest webRequest) {
        HttpRequestBase httpRequest;
        switch (webRequest.method()) {
            case GET:
                httpRequest = new HttpGet(webRequest.uri());
                break;
            case HEAD:
                httpRequest = new HttpHead(webRequest.uri());
                break;
            case POST:
                httpRequest = new HttpPost(webRequest.uri());
                initBody(webRequest, (HttpEntityEnclosingRequestBase) httpRequest);
                break;
            case PUT:
                httpRequest = new HttpPut(webRequest.uri());
                initBody(webRequest, (HttpEntityEnclosingRequestBase) httpRequest);
                break;
            case PATCH:
                httpRequest = new HttpPatch(webRequest.uri());
                initBody(webRequest, (HttpEntityEnclosingRequestBase) httpRequest);
                break;
            case DELETE:
                httpRequest = new HttpDelete(webRequest.uri());
                break;
            default:
                throw new IllegalArgumentException(String.format("不支持该请求方法[%s]", webRequest.method().name()));
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
        if (webRequest.body() != null) {
            HttpEntityEnclosingRequestBase httpEntityEnclosingRequest = httpRequest;
            HttpEntity httpEntity;
            if (webRequest.body() instanceof String) {
                httpEntity = new StringEntity(webRequest.body().toString(), "utf-8");
            } else if (webRequest.body() instanceof JSON) {
                httpEntity = new StringEntity(webRequest.body().toString(), "utf-8");
            } else if (webRequest.body() instanceof byte[]) {
                BasicHttpEntity basicHttpEntity = new BasicHttpEntity();
                InputStream inputStream = new ByteArrayInputStream((byte[]) webRequest.body());
                basicHttpEntity.setContent(inputStream);
                basicHttpEntity.setContentLength(((byte[]) webRequest.body()).length);
                httpEntity = basicHttpEntity;
            } else {
                throw new IllegalArgumentException(String.format("请求的body类型不支持 ", webRequest.body().getClass()));
            }
            httpEntityEnclosingRequest.setEntity(httpEntity);
        }
    }

    @AllArgsConstructor
    private class CustomFutureCallback implements FutureCallback<HttpResponse> {
        private WebRequest webRequest;

        @Override
        public void completed(HttpResponse result) {
            try {
                WebDownloadEvent event = new WebDownloadEvent(ContentType.getOrDefault(result.getEntity()), result.getEntity().getContentLength());
                event.setRequest(webRequest);
                context.postSyncEvent(event);
                byte[] bytes = EntityUtils.toByteArray(result.getEntity());
                final Header ceheader = result.getEntity().getContentEncoding();
                if (ceheader != null) {
                    final HeaderElement[] codecs = ceheader.getElements();
                    for (final HeaderElement codec : codecs) {
                        final String codecname = codec.getName().toLowerCase(Locale.ROOT);
                        final InputStreamFactory decoderFactory = decoderRegistry.lookup(codecname);
                        if (decoderFactory != null) {
                            try (InputStream is = decoderFactory.create(new ByteArrayInputStream(bytes))) {
                                bytes = IOUtils.toByteArray(is);
                            }
                        }
                    }
                }
                fillHeaders(result, webRequest.response().getHeaders());
                fillResponse(result, webRequest.response(), bytes);
                webRequest.succeed();
            } catch (Exception e) {
                webRequest.failed(e);
            } finally {
                HttpClientUtils.closeQuietly(result);
            }
        }

        @Override
        public void failed(Exception ex) {
            webRequest.failed(ex);
        }

        @Override
        public void cancelled() {
            webRequest.failed(new WebNetworkException("请求被强制取消"));
        }

        /**
         * 填充响应头
         *
         * @param headers
         */
        private void fillHeaders(HttpResponse httpResponse, Map<String, String> headers) {
            for (Header header : httpResponse.getAllHeaders()) {
                if (headers.containsKey(header.getName())) {
                    String value = headers.get(header.getName());
                    value = value + ";" + header.getValue();
                    headers.put(header.getName(), value);
                } else {
                    headers.put(header.getName(), header.getValue());
                }
            }
        }

        /**
         * 装填响应主体
         *
         * @param httpResponse
         * @param webResponse
         * @throws IOException
         */
        private void fillResponse(HttpResponse httpResponse, WebResponse webResponse, byte[] bytes) throws IOException {
            // 装填响应状态
            webResponse.setStatusCode(httpResponse.getStatusLine().getStatusCode());
            // 装填URL
            webResponse.setUrl(webRequest.uri());
            // 装填响应内容
            webResponse.setBytes(bytes);
            ContentType contentType = ContentType.getOrDefault(httpResponse.getEntity());
            String content = new String(bytes, Optional.ofNullable(contentType.getCharset()).orElse(Charset.forName(HttpDecodeUtils.findCharset(webResponse.getBytes()))));
            // 装填响应类型
            webResponse.setContentType(contentType.getMimeType());
            if (contentType.getMimeType().contains("json")) {
                webResponse.setJson((JSON) JSON.parse(content));
            } else if (contentType.getMimeType().startsWith("text")) {
                webResponse.setHtml(content);
            }
        }
    }

}
