package top.codings.websiphon.test;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.DeflateInputStreamFactory;
import org.apache.http.client.entity.GZIPInputStreamFactory;
import org.apache.http.client.entity.InputStreamFactory;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.protocol.RequestAcceptEncoding;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.NoConnectionReuseStrategy;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.nio.conn.ssl.SSLIOSessionStrategy;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;

@Slf4j
public class ApacheDemo {
    @Test
    public void test() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        initSystemProperties();
        RequestConfig config = RequestConfig
                .custom()
                .setContentCompressionEnabled(true)
                .setRedirectsEnabled(true)
                .setCircularRedirectsAllowed(false)
                .build();
        try (CloseableHttpAsyncClient httpAsyncClient = HttpAsyncClients
                .custom()
                .setSSLStrategy(new SSLIOSessionStrategy(
                        SSLContextBuilder.create().loadTrustMaterial((x509Certificates, s) -> true).build(),
                        (hostname, session) -> true)
                )
                .setConnectionReuseStrategy(NoConnectionReuseStrategy.INSTANCE)
                .addInterceptorLast(new RequestAcceptEncoding())
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
                .build()) {
            httpAsyncClient.start();
            HttpUriRequest request = new HttpGet("https://blog.csdn.net/qijiqiguai/article/details/76034767");
            HttpClientContext context = HttpClientContext.create();
            context.setRequestConfig(RequestConfig
                    .copy(config)
                    .setProxy(new HttpHost("127.0.0.1", 1080))
                    .build());
            httpAsyncClient.execute(request, context, new FutureCallback<HttpResponse>() {
                @Override
                public void completed(HttpResponse result) {
                    log.debug("[{}] -> 编码 {}", result.getStatusLine(), result.getEntity().getContentEncoding());
                    InputStream is = null;
                    try {
                        byte[] bytes = EntityUtils.toByteArray(result.getEntity());
                        Registry<InputStreamFactory> decoderRegistry = RegistryBuilder.<InputStreamFactory>create()
                                .register("gzip", GZIPInputStreamFactory.getInstance())
                                .register("x-gzip", GZIPInputStreamFactory.getInstance())
                                .register("deflate", DeflateInputStreamFactory.getInstance())
                                .build();

                        final Header ceheader = result.getEntity().getContentEncoding();
                        if (ceheader != null) {
                            final HeaderElement[] codecs = ceheader.getElements();
                            for (final HeaderElement codec : codecs) {
                                final String codecname = codec.getName().toLowerCase(Locale.ROOT);
                                final InputStreamFactory decoderFactory = decoderRegistry.lookup(codecname);
                                if (decoderFactory != null) {
                                    is = decoderFactory.create(new ByteArrayInputStream(bytes));
                                }
                            }
                        }
                        if (is != null) {
                            bytes = IOUtils.toByteArray(is);
                        }
                        String html = new String(bytes, ContentType.getOrDefault(result.getEntity()).getCharset());
                        log.debug("\n{}", html);
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        if (null != is) {
                            try {
                                is.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    latch.countDown();
                }

                @Override
                public void failed(Exception ex) {
                    log.error("请求失败", ex);
                    latch.countDown();
                }

                @Override
                public void cancelled() {
                    log.warn("请求取消");
                    latch.countDown();
                }
            });
            latch.await();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initSystemProperties() {
//        System.setProperty("https.protocols", "TLSv1.3,TLSv1.2,TLSv1.1,TLSv1.0,SSLv3");
        System.setProperty("http.keepAlive", "false");
        System.setProperty("http.maxConnections", "10");
        System.setProperty("http.agent", "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/73.0.3683.86 Safari/537.36");
    }
}
