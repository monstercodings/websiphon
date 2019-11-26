package top.codings.websiphon.test;

import org.junit.jupiter.api.Test;
import top.codings.websiphon.bean.WebRequest;
import top.codings.websiphon.core.requester.BasicAsyncWebRequester;
import top.codings.websiphon.core.requester.SeimiAgentWebRequest;
import top.codings.websiphon.core.requester.WebRequester;

import java.net.InetSocketAddress;
import java.net.Proxy;

public class DefaultAsyncTest {
    @Test
    public void test() throws Exception {
        WebRequester requester = new SeimiAgentWebRequest("http://121.201.107.77:51000");
        requester.init();
        WebRequest webRequest = new WebRequest();
        webRequest.setTimeout(60000);
        webRequest.setUrl("http://monitor.szkedun.cn/#/index");
        requester.execute(webRequest);
        Thread.currentThread().join();
    }
}
