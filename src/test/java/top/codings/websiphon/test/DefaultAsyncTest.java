package top.codings.websiphon.test;

import org.junit.jupiter.api.Test;
import top.codings.websiphon.bean.WebRequest;
import top.codings.websiphon.core.requester.BasicAsyncWebRequester;

import java.net.InetSocketAddress;
import java.net.Proxy;

public class DefaultAsyncTest {
    @Test
    public void test() throws Exception {
        BasicAsyncWebRequester requester = new BasicAsyncWebRequester();
        requester.init();
        WebRequest webRequest = new WebRequest();
        webRequest.setUrl("http://2000019.ip138.com/");
        String body = ("{\n" +
                "  \"accountNonExpired\": true,\n" +
                "  \"accountNonLocked\": true,\n" +
                "  \"authorities\": [\n" +
                "    {\n" +
                "      \"authority\": \"string\",\n" +
                "      \"createdAt\": 0,\n" +
                "      \"enabled\": true,\n" +
                "      \"id\": 0,\n" +
                "      \"method\": \"string\",\n" +
                "      \"name\": \"string\",\n" +
                "      \"updatedAt\": 0,\n" +
                "      \"url\": \"string\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"avatarUrl\": \"string\",\n" +
                "  \"createdAt\": 0,\n" +
                "  \"credentialsNonExpired\": true,\n" +
                "  \"enabled\": true,\n" +
                "  \"id\": 0,\n" +
                "  \"nickname\": \"string\",\n" +
                "  \"password\": \"admin\",\n" +
                "  \"pid\": 0,\n" +
                "  \"rolesId\": [\n" +
                "    0\n" +
                "  ],\n" +
                "  \"rolesName\": [\n" +
                "    \"string\"\n" +
                "  ],\n" +
                "  \"updatedAt\": 0,\n" +
                "  \"username\": \"admin\"\n" +
                "}");
//        webRequest.setBody(JSON.parse(body));
//        webRequest.setMethod(WebRequest.Method.POST);
        webRequest.setProxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress("127.0.0.1", 1080)));
        requester.execute(webRequest);
        Thread.currentThread().join();
    }
}
