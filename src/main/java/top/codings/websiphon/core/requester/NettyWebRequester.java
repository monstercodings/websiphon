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
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.AttributeKey;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.ssl.SSLContextBuilder;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.nio.charset.Charset;
import java.security.cert.X509Certificate;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class NettyWebRequester<W extends WebRequest> implements WebRequester<W> {
    @Getter
    private boolean health = true;
    private EventLoopGroup workerGroup = new NioEventLoopGroup();
    private boolean redirect;
//    private String lineSeparator = System.getProperty("line.separator", "\n");
    private AtomicInteger size = new AtomicInteger(0);
//    private SslContext context;

    private SSLContext sslContext;

    public NettyWebRequester() {
        this(false);
    }

    public NettyWebRequester(boolean redirect) {
        this.redirect = redirect;
    }

    private SSLEngine sslEngine(String host, int port) {
        SSLEngine sslEngine = sslContext.createSSLEngine(host, port);
        sslEngine.setUseClientMode(true);
        return sslEngine;
    }

    @Override
    public void init() throws Exception {
        try {
            /*KeyManager[] keyManagers = new KeyManager[]{new KeyManager() {
            }};
            TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
                private X509Certificate[] chain;
                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType) {
                }

                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType) {
                    log.debug("检查服务器证书");
                    this.chain = chain;
                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    log.debug("返回信任的证书");
                    return chain;
                }
            }};
            sslContext = SSLContext.getInstance("TLS");
            sslContext.init(keyManagers, trustAllCerts, new SecureRandom());*/
//            sslContext=SSLContext.getDefault();
            sslContext = SSLContextBuilder.create().loadTrustMaterial((x509Certificates, s) -> true).build();
        } catch (Exception e) {
            log.error("初始化SSL引擎失败", e);
        }
    }

    @Override
    public void execute(W request) {
        if (MapUtils.isEmpty(request.getHeaders())) {
            request.setHeaders(HeadersUtils.getHeaders());
        }
        CrawlerContext crawlerContext = request.context();
        try {
            Proxy proxy = request.getProxy();
            String proxyHost = null;
            int proxyPort = 0;
            if (null != proxy && proxy.type() == Proxy.Type.HTTP) {
                proxyHost = ((InetSocketAddress) proxy.address()).getHostName();
                proxyPort = ((InetSocketAddress) proxy.address()).getPort();
            }
            boolean isProxy = StringUtils.isNotBlank(proxyHost);
            HttpOperator.HttpProtocol httpProtocol = HttpOperator.resolve(request.getUrl());
            Bootstrap b = new Bootstrap();
            b.group(workerGroup)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.SO_KEEPALIVE, false)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, request.getTimeout())
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) throws Exception {
                            if (!isProxy && httpProtocol.getScheme().equals("https")) {
//                                ch.pipeline().addFirst(new SslHandler(context.newEngine(ch.alloc())));
//                                ch.pipeline().addLast(new SslHandler(prepareEngine(httpProtocol.getHost(), httpProtocol.getPort())));
                                ch.pipeline().addLast(new SslHandler(sslEngine(httpProtocol.getHost(), httpProtocol.getPort())));
//                                ch.pipeline().addLast(new OptionalSslHandler(SslContextBuilder.forClient().build()));
                            }
                            ch.pipeline()
                                    .addLast("LiveIdleStateHandler", new LiveIdleStateHandler(request, crawlerContext, request.getTimeout()))
                                    .addLast("HttpClientCodec", new HttpClientCodec())
//                                    .addLast("HttpRequestEncoder", new HttpRequestEncoder())
                                    .addLast("HttpResponseDecoder", new HttpResponseDecoder())
                            ;
                            if (!isProxy) {
                                ch.pipeline()
                                        .addLast("HttpContentDecompressor", new HttpContentDecompressor())
                                        .addLast("HttpObjectAggregator", new HttpObjectAggregator(64 * 1024 * 1024));
                            }
                            if (isProxy && !httpProtocol.getScheme().equals("https")) {
                                ch.pipeline()
                                        .addLast("HttpContentDecompressor", new HttpContentDecompressor())
                                        .addLast("HttpObjectAggregator", new HttpObjectAggregator(64 * 1024 * 1024));
                            }
                            ch.pipeline()
                                    .addLast("ChunkedWriteHandler", new ChunkedWriteHandler())
                                    .addLast(new SimpleChannelInboundHandler<HttpResponse>() {
                                        @Override
                                        protected void channelRead0(ChannelHandlerContext channelHandlerContext, HttpResponse httpResponse) throws Exception {
                                            channelHandlerContext.channel().attr(AttributeKey.valueOf("success")).set(true);
                                            // 针对代理的处理 直接转发数据
                                            if (httpResponse.headers().size() == 0 && isProxy) {
                                                try {
                                                    channelHandlerContext.pipeline()
                                                            .addAfter("HttpResponseDecoder", "HttpContentDecompressor", new HttpContentDecompressor())
                                                            .addAfter("HttpContentDecompressor", "HttpObjectAggregator", new HttpObjectAggregator(64 * 1024 * 1024))
                                                    ;
                                                    if (channelHandlerContext.channel().pipeline().get(SslHandler.class) == null) {
                                                        if (httpProtocol.getScheme().equals("https")) {
                                                            //                                                        channelHandlerContext.channel().pipeline().addAfter("LiveIdleStateHandler", "SslHandler", new SslHandler(context.newEngine(ch.alloc())));
                                                            //                                                        channelHandlerContext.channel().pipeline().addAfter("LiveIdleStateHandler", "SslHandler", new SslHandler(prepareEngine(httpProtocol.getHost(), httpProtocol.getPort())));
                                                            channelHandlerContext.channel().pipeline().addAfter("LiveIdleStateHandler", "SslHandler", new SslHandler(sslEngine(httpProtocol.getHost(), httpProtocol.getPort())));
                                                            //                                                        channelHandlerContext.channel().pipeline().addAfter("LiveIdleStateHandler", "SslHandler", new OptionalSslHandler(SslContextBuilder.forClient().build()));
                                                        }
                                                    }
                                                    Object body = request.getBody();
                                                    ByteBuf byteBuf = null;
                                                    if (body != null) {
                                                        if (body instanceof String) {
                                                            byteBuf = Unpooled.wrappedBuffer(((String) body).getBytes("utf-8"));
                                                        }
                                                    }
                                                    FullHttpRequest httpRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.valueOf(request.getMethod().name()), httpProtocol.getPath(), byteBuf == null ? Unpooled.EMPTY_BUFFER : byteBuf);
                                                    request.getHeaders().forEach((key, value) -> httpRequest.headers().set(key, value));
                                                    httpRequest.headers()
                                                            .set("Host", httpProtocol.getHost())
                                                            .set("Referer", httpProtocol.getHost())
                                                            .set("Accept-Encoding", "gzip, deflate, compress");
                                                    channelHandlerContext.writeAndFlush(httpRequest).addListener((ChannelFutureListener) future -> {
                                                        if (sendSuccess(request, crawlerContext, future)) return;
                                                        channelHandlerContext.channel().close();
                                                    });
                                                    channelHandlerContext.channel().attr(AttributeKey.valueOf("success")).set(null);
                                                    return;
                                                } catch (Exception e) {
                                                    initNetworkException(e, crawlerContext, request);
//                                                    crawlerContext.finishRequest(request);
                                                    channelHandlerContext.channel().close();
                                                    return;
                                                }
                                            }
                                            try {
                                                finish((FullHttpResponse) httpResponse, request, crawlerContext);
                                            } catch (Exception e) {
                                                initNetworkException(e, crawlerContext, request);
                                            }
//                                            crawlerContext.finishRequest(request);
                                            channelHandlerContext.channel().close();
                                        }

                                        @Override
                                        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
                                            try {
                                                /*if (cause.getCause() != null && cause.getCause() instanceof SSLException) {
                                                    return;
                                                }*/
                                                log.trace("netty网络请求发生异常 -> {}", cause.getLocalizedMessage());
                                                WebNetworkExceptionEvent event = new WebNetworkExceptionEvent();
                                                event.setThrowable(cause);
                                                event.setContext(crawlerContext);
                                                event.setRequest(request);
                                                WebResponse response = request.getResponse();
                                                if (null == response) {
                                                    response = new WebResponse();
                                                    request.setResponse(response);
                                                }
                                                response.setErrorEvent(event);
//                                                crawlerContext.finishRequest(request);
                                                ctx.channel().close();
                                            } catch (Exception e) {
                                                log.error("捕获netty异常时发生其他异常", e);
                                            }
                                        }
                                    })
                            ;
                        }
                    });
            ChannelFuture future;
            if (isProxy) {
                future = b.connect(proxyHost, proxyPort);
            } else {
                future = b.connect(httpProtocol.getHost(), httpProtocol.getPort());
            }
            future.addListener((ChannelFutureListener) channelFuture -> {
                if (!channelFuture.isSuccess()) {
                    initNetworkException(new WebNetworkException(String.format("无法连接目标服务器[%s:%d]", httpProtocol.getHost(), httpProtocol.getPort())), crawlerContext, request);
                    crawlerContext.finishRequest(request);
                    return;
                }
                size.getAndIncrement();
                Object body = request.getBody();
                ByteBuf byteBuf = null;
                if (body != null) {
                    if (body instanceof String) {
                        byteBuf = Unpooled.wrappedBuffer(((String) body).getBytes("utf-8"));
                    }
                }
                HttpRequest httpRequest;
                if (isProxy && httpProtocol.getScheme().equals("https")) {
//                    channelFuture.channel().attr(AttributeKey.valueOf("proxy")).set(new Object());
                    httpRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.CONNECT, httpProtocol.getHost() + ":" + httpProtocol.getPort());
                    httpRequest.headers()
                            .set("Host", httpProtocol.getHost() + ":" + httpProtocol.getPort())
                            .set("Referer", httpProtocol.getHost())
                            .set("Accept-Encoding", "gzip, deflate, compress");
                    request.getHeaders().forEach((key, value) -> httpRequest.headers().set(key, value));
                } else {
                    httpRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.valueOf(request.getMethod().name()), httpProtocol.getPath(), byteBuf == null ? Unpooled.EMPTY_BUFFER : byteBuf);
                    httpRequest.headers()
                            .set("Host", httpProtocol.getHost() + ":" + httpProtocol.getPort())
                            .set("Referer", httpProtocol.getHost())
                            .set("Accept-Encoding", "gzip, deflate, compress");
                    request.getHeaders().forEach((key, value) -> httpRequest.headers().set(key, value));
                }
                channelFuture.channel().writeAndFlush(httpRequest).addListener((ChannelFutureListener) channelFuture1 -> {
                    if (sendSuccess(request, crawlerContext, channelFuture1)) return;
                    channelFuture1.channel().close();
                });
            });
        } catch (Exception e) {
            initNetworkException(e, crawlerContext, request);
            crawlerContext.finishRequest(request);
        }
    }

    /**
     * 构造网络异常相关数据
     *
     * @param e
     * @param crawlerContext
     * @param request
     */
    private void initNetworkException(Exception e, CrawlerContext crawlerContext, W request) {
        WebNetworkExceptionEvent event = new WebNetworkExceptionEvent();
        event.setThrowable(e);
        event.setContext(crawlerContext);
        event.setRequest(request);
        request.setResponse(Optional.ofNullable(request.getResponse()).orElse(new WebResponse()));
        request.getResponse().setErrorEvent(event);
    }

    @Override
    public int size() {
        return size.get();
    }

    private boolean sendSuccess(W request, CrawlerContext crawlerContext, ChannelFuture channelFuture) {
        if (channelFuture.isSuccess()) {
            return true;
        }
        initNetworkException(new WebNetworkException("发送请求失败"), crawlerContext, request);
//        crawlerContext.finishRequest(request);
        return false;
    }

    @Override
    public void close() {
        workerGroup.shutdownGracefully();
    }

    private SSLEngine prepareEngine(String host, int port) throws Exception {
        SSLContext ctx = SSLContext.getInstance("TLS");
        ctx.init(null, new TrustManager[]{new X509TrustManager() {
            public X509Certificate[] getAcceptedIssuers() {
//                return new java.security.cert.X509Certificate[]{};
                return null;
            }

            public void checkClientTrusted(X509Certificate[] chain, String authType) {
            }

            public void checkServerTrusted(X509Certificate[] chain, String authType) {
            }
        }
        }, null/*new java.security.SecureRandom()*/);
        SSLEngine sslEngine = ctx.createSSLEngine();
        sslEngine.setUseClientMode(true);

        return sslEngine;
    }

    private boolean finish(FullHttpResponse fullHttpResponse, W request, CrawlerContext crawlerContext) throws WebNetworkException {
        // 判断是否需要跳转
        if (fullHttpResponse.status().code() >= 300 && fullHttpResponse.status().code() < 400 && redirect) {
            String location = fullHttpResponse.headers().get("location");
            if (StringUtils.isNotBlank(location)) {
                W redirectRequest;
                try {
                    redirectRequest = (W) BeanUtils.cloneBean(request);
                } catch (Exception e) {
                    log.error("跳转时拷贝对象发生异常", e);
                    return false;
                }
                location = location.replace(":80", "").replace(":443", "");
                redirectRequest.setUrl(HttpOperator.recombineLink(location, request.getUrl()));
                WebResponse response = new WebResponse();
                response.setRedirect(true);
                redirectRequest.setResponse(response);
                PushResult pushResult = crawlerContext.getCrawler().push(redirectRequest);
                if (pushResult != PushResult.SUCCESS && pushResult != PushResult.URL_REPEAT) {
                    log.debug("跳转失败 [{}] | 原链接:{} | 跳转链接:{}", pushResult.value, request.getUrl(), redirectRequest.getUrl());
                }
            }
            return false;
        }
        request.setResponse(Optional.ofNullable(request.getResponse()).orElse(new WebResponse()));
        WebResponse response = request.getResponse();
        response.setUrl(request.getUrl());
        response.setStatusCode(fullHttpResponse.status().code());
        response.setResult(WebResponse.Result.valueOf(fullHttpResponse.status().code()));
        if (!(response.getStatusCode() >= 200 && response.getStatusCode() < 300)) {
            throw new WebNetworkException(String.format("响应码不是2xx [%d]", response.getStatusCode()));
        }
        // 装填响应内容
        ByteBuf byteBuf = fullHttpResponse.content();
        response.setBytes(ByteBufUtil.getBytes(byteBuf));
        // 装填响应类型
        String contentType = fullHttpResponse.headers().get("Content-Type");
        if (StringUtils.isBlank(contentType)) {
            contentType = "";
        } else {
            contentType = contentType.trim().toLowerCase();
        }
        response.setContentType(contentType);
        // 查找编码
        String charset = Charset.defaultCharset().name();
        if (contentType.contains("charset")) {
            charset = contentType.substring(contentType.indexOf("charset") + "charset=".length());
        } else if (contentType.startsWith("text")) {
            charset = HttpDecodeUtils.findCharset(response.getBytes());
        }
        if (StringUtils.isNotBlank(contentType)) {
            if (contentType.startsWith("text")) {
                String content = byteBuf.toString(Charset.forName(charset));
                response.setHtml(content);
            } else if (contentType.contains("json")) {
                String content = byteBuf.toString(Charset.forName(charset));
                response.setJson((JSON) JSON.parse(content));
            } else {
                response.setBytes(ByteBufUtil.getBytes(byteBuf));
            }
        }
//        log.trace("{}", content);
        return true;
    }

    private class LiveIdleStateHandler extends IdleStateHandler {
        private W request;
        private CrawlerContext crawlerContext;

        public LiveIdleStateHandler(W request, CrawlerContext crawlerContext, int readerIdleTime) {
            super(readerIdleTime, readerIdleTime, readerIdleTime, TimeUnit.MILLISECONDS);
            this.request = request;
            this.crawlerContext = crawlerContext;
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            size.getAndDecrement();
            /*if (ctx.channel().attr(AttributeKey.valueOf("success")).get() != null) {
                return;
            }
            initNetworkException(new WebNetworkException("网络异常中断"), crawlerContext, request);*/
            crawlerContext.finishRequest(request);
        }

        @Override
        protected void channelIdle(ChannelHandlerContext ctx, IdleStateEvent evt) throws Exception {
//            log.warn("通道长时间未读取数据，有可能由于网络波动已断开连接 {} -> {}", ctx.channel().localAddress(), ctx.channel().remoteAddress());
            initNetworkException(new WebNetworkException(String.format("通道长时间未读取数据，有可能是由于网络波动 [%s]", request.getUrl())), crawlerContext, request);
//            crawlerContext.finishRequest(request);
            ctx.channel().attr(AttributeKey.valueOf("success")).set(true);
            ctx.channel().close();
        }
    }
}
