package top.codings.websiphon.core.requester;

import com.alibaba.fastjson.JSON;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.cookie.ClientCookieDecoder;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.entity.ContentType;
import org.apache.http.ssl.SSLContextBuilder;
import top.codings.websiphon.bean.BasicWebRequest;
import top.codings.websiphon.bean.WebRequest;
import top.codings.websiphon.bean.WebResponse;
import top.codings.websiphon.core.proxy.bean.WebProxy;
import top.codings.websiphon.exception.WebNetworkException;
import top.codings.websiphon.util.HttpOperator;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import java.io.UnsupportedEncodingException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class SuperWebRequester implements WebRequester {
    private EventLoopGroup workerGroup;

    @Override
    public void init() throws Exception {
        workerGroup = new NioEventLoopGroup();
    }

    @Override
    public void execute(WebRequest webRequest) throws WebNetworkException {
        boolean proxy = true;
        WebProxy webProxy = null;
        if (webRequest instanceof BasicWebRequest) {
            webProxy = ((BasicWebRequest) webRequest).getProxy();
        }
        if (webProxy == null) {
            proxy = false;
        }
        HttpOperator.HttpProtocol httpProtocol = HttpOperator.resolve(webRequest.uri());
        Bootstrap bootstrap = new Bootstrap();
        bootstrap
                .group(workerGroup)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.SO_KEEPALIVE, false)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 30000)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel channel) throws Exception {
                        channel
                                .pipeline()
                                .addLast(new HttpClientCodec())
                                .addLast(new HttpResponseDecoder())
                                .addLast(new HttpContentDecompressor())
                                .addLast(new HttpObjectAggregator(512 * 1024))
                                .addLast(new SimpleChannelInboundHandler<FullHttpResponse>() {
                                    @Override
                                    protected void channelRead0(ChannelHandlerContext channelHandlerContext, FullHttpResponse fullHttpResponse) throws Exception {
                                        if (fullHttpResponse.decoderResult().isFailure()) {
                                            // TODO 失败处理
                                            channelHandlerContext.close();
                                            webRequest.failed(fullHttpResponse.decoderResult().cause());
                                            return;
                                        }
                                        boolean proxyResp = (boolean) channelHandlerContext.channel().attr(AttributeKey.valueOf("proxy")).get();
                                        if (proxyResp) {
                                            initSsl(channelHandlerContext.channel(), httpProtocol);
                                            FullHttpRequest request = initFullHttpRequest(webRequest, httpProtocol);
                                            channelHandlerContext.channel().attr(AttributeKey.valueOf("proxy")).set(false);
                                            channelHandlerContext.writeAndFlush(request).addListener((ChannelFutureListener) channelFuture -> {
                                                if (channelFuture.isSuccess()) {
                                                    return;
                                                }
                                                // TODO 失败处理
                                                channelFuture.channel().close();
                                                webRequest.failed(channelFuture.cause());
                                            });
                                            return;
                                        }
                                        WebResponse response = webRequest.response();
                                        Map<String, String> headers = new HashMap<>();
                                        ContentType contentType = null;
                                        String contentTypeStr = null;
                                        String charset = null;
//                                        StringBuilder sb = new StringBuilder();
                                        for (Map.Entry<String, String> header : fullHttpResponse.headers()) {
                                            if (header.getKey().equalsIgnoreCase("Set-Cookie")) {
                                                Cookie cookie = ClientCookieDecoder.LAX.decode(header.getValue());
                                                response.getCookies().add(cookie);
                                            }
                                            headers.put(header.getKey(), header.getValue());
                                            if (header.getKey().equalsIgnoreCase("Content-Type")) {
                                                contentTypeStr = header.getValue();
                                                contentType = ContentType.parse(contentTypeStr);
                                                if (contentType != null && contentType.getCharset() != null) {
                                                    charset = contentType.getCharset().name();
                                                } else if (webRequest instanceof BasicWebRequest) {
                                                    charset = ((BasicWebRequest) webRequest).getCharset();
                                                }
                                            }
//                                            sb.append(header.getKey()).append(":").append(header.getValue()).append("\n");
                                        }
                                        response.setHeaders(headers);
                                        if (StringUtils.isBlank(charset)) {
                                            charset = "utf-8";
                                        }
                                        byte[] bytes = ByteBufUtil.getBytes(fullHttpResponse.content());
                                        if (contentType != null) {
                                            if (contentType.getMimeType().contains("text") || contentType.getMimeType().contains("application")) {
                                                response.setHtml(new String(bytes, charset));
                                            }
                                            if (contentType.getMimeType().contains("json")) {
                                                response.setJson((JSON) JSON.parse(response.getHtml()));
                                            }
                                        }
                                        // TODO 处理3xx跳转问题
                                        response.setResult(WebResponse.Result.valueOf(fullHttpResponse.status().code()));
                                        response.setContentType(contentTypeStr);
                                        response.setBytes(bytes);
                                        response.setUrl(webRequest.uri());
                                        channelHandlerContext.close();
                                        webRequest.succeed();
                                    }

                                    @Override
                                    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
                                        // TODO 失败处理
                                        ctx.close();
                                        webRequest.failed(cause);
                                    }
                                })
                        ;
                    }
                });
        ChannelFuture future;
        if (proxy) {
            future = bootstrap.connect(webProxy.getProxyIp(), webProxy.getProxyPort());
        } else {
            future = bootstrap.connect(httpProtocol.getHost(), httpProtocol.getPort());
        }
        boolean finalProxy = proxy;
        WebProxy finalWebProxy = webProxy;
        future.addListener((ChannelFutureListener) channelFuture -> {
            if (!channelFuture.isSuccess()) {
                if (finalProxy) {
                    finalWebProxy.setHealthy(false);
                }
                // TODO 失败处理
                channelFuture.channel().close();
                webRequest.failed(channelFuture.cause());
                return;
            }
            channelFuture.channel().attr(AttributeKey.valueOf("proxy")).set(finalProxy);
            FullHttpRequest request;
            if (finalProxy) {
                request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.CONNECT, httpProtocol.getHost() + ":" + httpProtocol.getPort());
                request.headers().set("Host", httpProtocol.getHost() + ":" + httpProtocol.getPort());
            } else {
                initSsl(channelFuture.channel(), httpProtocol);
                request = initFullHttpRequest(webRequest, httpProtocol);
            }
            channelFuture.channel().writeAndFlush(request).addListener((ChannelFutureListener) channelFuture1 -> {
                if (channelFuture1.isSuccess()) {
                    return;
                }
                // TODO 失败处理
                channelFuture1.channel().close();
                webRequest.failed(channelFuture1.cause());
            });
        })
        ;
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public boolean isHealth() {
        return true;
    }

    @Override
    public void close() {
        workerGroup.shutdownGracefully();
    }

    private FullHttpRequest initFullHttpRequest(WebRequest webRequest, HttpOperator.HttpProtocol httpProtocol) throws UnsupportedEncodingException {
        HttpMethod httpMethod = null;
        WebRequest.Method method = webRequest.method();
        switch (method) {
            case GET:
                httpMethod = HttpMethod.GET;
                break;
            case POST:
                httpMethod = HttpMethod.POST;
                break;
        }
        byte[] body = new byte[0];
        if (webRequest.body() != null) {
            if (webRequest.body() instanceof JSON) {
                body = webRequest.body().toString().getBytes("utf-8");
            } else {
                body = webRequest.body().toString().getBytes("ISO-8859-1");
            }
        }
        FullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, httpMethod, httpProtocol.getPath(), Unpooled.wrappedBuffer(body));
        request
                .headers()
                .set("Host", httpProtocol.getHost() + ":" + httpProtocol.getPort())
                .set("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3")
                .set("Accept-Encoding", "gzip, deflate, compress")
                .set("Accept-Language", "zh-CN,zh;q=0.9")
                .set("Cache-Control", "no-cache")
                .set("Connection", "keep-alive")
                .set("DNT", "1")
                .set("Origin", httpProtocol.getScheme() + "://" + httpProtocol.getHost() + ":" + httpProtocol.getPort())
                .set("Referer", webRequest.uri())
                .set("Upgrade-Insecure-Requests", "1")
                .set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/73.0.3683.86 Safari/537.36");
        for (Map.Entry<String, String> entry : webRequest.headers().entrySet()) {
            request.headers().set(entry.getKey(), entry.getValue());
        }
        if (webRequest.body() != null) {
            if (webRequest.body() instanceof JSON) {
                request.headers().set("Content-Type", ContentType.APPLICATION_JSON);
            } else {
                request.headers().set("Content-Type", ContentType.APPLICATION_FORM_URLENCODED);
            }
        }
        return request;
    }

    private void initSsl(Channel channel, HttpOperator.HttpProtocol httpProtocol) throws NoSuchAlgorithmException, KeyManagementException, KeyStoreException {
        if (httpProtocol.getScheme().equals("https")) {
            SSLContext sslContext = SSLContextBuilder.create().loadTrustMaterial((x509Certificates, s) -> true).build();
            SSLEngine sslEngine = sslContext.createSSLEngine(httpProtocol.getHost(), httpProtocol.getPort());
            sslEngine.setUseClientMode(true);
            channel.pipeline().addFirst(new SslHandler(sslEngine));
        }
    }
}