package top.codings.websiphon.test;

import com.alibaba.fastjson.JSONObject;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.ssl.SSLContextBuilder;
import org.junit.jupiter.api.Test;

import javax.net.ssl.SSLContext;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

@Slf4j
public class NewHttpTest {
    @SneakyThrows
    @Test
    public void test() {
        HttpClient client = HttpClient.newBuilder()
                .executor(Executors.newSingleThreadExecutor())
                .connectTimeout(Duration.ofSeconds(6))
                .version(HttpClient.Version.HTTP_2)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .sslContext(SSLContextBuilder.create().loadTrustMaterial((x509Certificates, s) -> true).build())
//                .proxy(ProxySelector.of(new InetSocketAddress("127.0.0.1", 1080)))
//                .authenticator(Authenticator.getDefault())
                .build();
        JSONObject json = new JSONObject();
        json.put("phone", "13826531204");
        json.put("passwd", "hejian");
        HttpRequest request = HttpRequest.newBuilder()
                /*.uri(URI.create("https://auth.glli.top:30001/auth/vpn/pc/login"))
                .POST(HttpRequest.BodyPublishers.ofString(json.toJSONString(), Charset.forName("utf-8")))
                .header("Content-Type", "application/json")*/
                .uri(URI.create("https://codings.top"))
                .timeout(Duration.ofSeconds(30))
                .build();
        int total = 1;
        CompletableFuture[] futures = new CompletableFuture[total];
        for (int i = 0; i < total; i++) {
            futures[i] = client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .whenCompleteAsync((stringHttpResponse, throwable) -> {
                        if (throwable != null) {
                            log.error("发生异常 -> {}", throwable.getMessage());
                            return;
                        }
                        log.debug(stringHttpResponse.body());
                    })
            ;
        }
        try {
            CompletableFuture.allOf(futures).join();
        } catch (Exception e) {

        }
    }
}
