package top.codings.websiphon.test.netty;

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
import org.apache.http.entity.ContentType;
import org.apache.http.ssl.SSLContextBuilder;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.util.StringUtils;
import top.codings.websiphon.bean.WebRequest;
import top.codings.websiphon.core.requester.SuperWebRequester;
import top.codings.websiphon.util.HttpOperator;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

@Slf4j
public class ProxyTest {
    private String url = "https://login.51job.com/login.php?lang=c";
    private boolean proxy = false;
    private String proxyIp = "127.0.0.1";
    private int proxyPort = 1080;

    @Test
    public void test() throws Exception {
        WebRequest webRequest = new WebRequest();
        webRequest.setUrl(url);
        webRequest.setCharset("gbk");
        webRequest.setMethod(WebRequest.Method.POST);
        webRequest.setBody(String.format("loginname=%s&password=%s&isread=on&from_domain=i&action=save&lang=c",
                URLEncoder.encode("13826531204", "utf-8"),
                URLEncoder.encode("hejian518", "utf-8")
        ));
        SuperWebRequester requester = new SuperWebRequester();
        requester.init();
        requester.execute(webRequest);
        Thread.currentThread().join();
    }

}
