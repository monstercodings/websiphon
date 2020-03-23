package top.codings.websiphon.core.requester;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import top.codings.websiphon.bean.WebRequest;
import top.codings.websiphon.bean.WebResponse;
import top.codings.websiphon.core.context.event.async.WebNetworkExceptionEvent;
import top.codings.websiphon.exception.WebNetworkException;
import top.codings.websiphon.util.HttpOperator;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class SeimiAgentWebRequest implements WebRequester<WebRequest> {
    private String connectUrl;
    private HttpOperator.HttpProtocol httpProtocol;
    @Getter
    private boolean health = false;
    private AtomicInteger size = new AtomicInteger(0);
    private EventLoopGroup workerGroup = new NioEventLoopGroup();

    public SeimiAgentWebRequest(String connectUrl) {
        this.connectUrl = connectUrl;
        httpProtocol = HttpOperator.resolve(connectUrl);
    }

    @Override
    public void init() throws Exception {
    }

    @Override
    public void execute(WebRequest webRequest) throws WebNetworkException {
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(workerGroup)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, webRequest.getTimeout())
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel channel) throws Exception {
                        channel.pipeline()
                                .addLast(new IdleStateHandler(webRequest.getTimeout(), webRequest.getTimeout(), webRequest.getTimeout(), TimeUnit.MILLISECONDS) {
                                    @Override
                                    protected void channelIdle(ChannelHandlerContext ctx, IdleStateEvent evt) throws Exception {
                                        webRequest.setResponse(new WebResponse());
                                        WebNetworkExceptionEvent event = new WebNetworkExceptionEvent();
                                        event.setThrowable(new WebNetworkException("目标超时未响应"));
                                        event.setRequest(webRequest);
                                        webRequest.getResponse().setErrorEvent(event);
                                        webRequest.context().doOnFinished(webRequest);
                                        ctx.close();
                                    }
                                })
                                .addLast(new HttpClientCodec())
                                .addLast(new HttpContentDecompressor())
                                .addLast(new HttpObjectAggregator(64 * 1024 * 1024))
                                .addLast(new SimpleChannelInboundHandler<FullHttpResponse>() {
                                    @Override
                                    protected void channelRead0(ChannelHandlerContext channelHandlerContext, FullHttpResponse fullHttpResponse) throws Exception {
                                        /*log.debug("收到服务端的响应 -> {}", fullHttpResponse.status().toString());
                                        if (fullHttpResponse.headers().get(HttpHeaderNames.CONTENT_TYPE).startsWith("image")) {
                                            FileUtils.writeByteArrayToFile(new File("./log/img.png"), ByteBufUtil.getBytes(fullHttpResponse.content()));
                                            return;
                                        }
                                        String content = fullHttpResponse.content().toString(Charset.defaultCharset());
                                        log.debug("响应内容 -> \n{}", content);*/
                                        try {
                                            fillResponseBody(webRequest, fullHttpResponse);
                                        } catch (Exception e) {
                                            log.error("发生系统级的异常", e);
                                        } finally {
                                            webRequest.succeed();
                                            channelHandlerContext.close();
                                        }
                                    }
                                })
                        ;
                    }
                });
        bootstrap.connect(httpProtocol.getHost(), httpProtocol.getPort()).addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                DefaultFullHttpRequest httpRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/doload");
                httpRequest.content().writeBytes(String.format("url=%s", URLEncoder.encode(webRequest.getUrl(), "utf-8")).getBytes("utf-8"));
                httpRequest.headers().set(HttpHeaderNames.CONTENT_LENGTH, httpRequest.content().readableBytes());
                httpRequest.headers().set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_X_WWW_FORM_URLENCODED);
                future.channel().writeAndFlush(httpRequest).addListener((ChannelFutureListener) future2 -> {
                    if (future2.isSuccess()) {
                        return;
                    }
                    // TODO 发送请求失败的处理代码

                });
                return;
            }
            webRequest.setResponse(new WebResponse());
            WebNetworkExceptionEvent event = new WebNetworkExceptionEvent();
            event.setThrowable(future.cause());
            event.setRequest(webRequest);
            webRequest.getResponse().setErrorEvent(event);
            webRequest.context().doOnFinished(webRequest);
        });
    }

    /**
     * 装填响应主体
     *
     * @param webRequest
     * @param fullHttpResponse
     * @throws UnsupportedEncodingException
     */
    private void fillResponseBody(WebRequest webRequest, FullHttpResponse fullHttpResponse) throws UnsupportedEncodingException {
        WebResponse webResponse = new WebResponse();
        webRequest.setResponse(webResponse);
        // 装填响应状态
        webResponse.setResult(WebResponse.Result.valueOf(fullHttpResponse.status().code()));
        // 装填响应码
        webResponse.setStatusCode(fullHttpResponse.status().code());
        // 装填URL
        webResponse.setUrl(webRequest.getUrl());
        // 装填响应内容
        webResponse.setBytes(ByteBufUtil.getBytes(fullHttpResponse.content()));
        webResponse.setContentType("text/html;charset=utf-8");
        webResponse.setHtml(new String(webResponse.getBytes(), "utf-8"));
    }

    @Override
    public int size() {
        return size.get();
    }

    @Override
    public boolean isHealth() {
        return false;
    }

    @Override
    public void close() {

    }
}
