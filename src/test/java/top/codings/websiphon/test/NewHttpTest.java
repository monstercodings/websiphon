package top.codings.websiphon.test;

import com.alibaba.fastjson.JSONObject;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.ssl.SSLContextBuilder;
import org.junit.jupiter.api.Test;

import javax.net.ssl.*;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

@Slf4j
public class NewHttpTest {
    @SneakyThrows
    @Test
    public void test() {
        SSLContext sslContext = SSLContextBuilder.create().loadTrustMaterial((x509Certificates, s) -> true).build();
        sslContext.init(null, MyTrustManager.get(), null);
        HttpClient client = HttpClient.newBuilder()
                .executor(Executors.newSingleThreadExecutor())
                .connectTimeout(Duration.ofSeconds(6))
                .version(HttpClient.Version.HTTP_2)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .sslContext(sslContext)
                .sslParameters(new SSLParameters())
//                .proxy(ProxySelector.of(new InetSocketAddress("127.0.0.1", 1080)))
//                .authenticator(Authenticator.getDefault())
                .build();
        JSONObject json = new JSONObject();
        json.put("phone", "1");
        json.put("passwd", "1");
        HttpRequest request = HttpRequest.newBuilder()
                /*.uri(URI.create("https://auth.glli.top:30001/auth/vpn/pc/login"))
                .POST(HttpRequest.BodyPublishers.ofString(json.toJSONString(), Charset.forName("utf-8")))
                .header("Content-Type", "application/json")*/
                .uri(URI.create("https://vdash.codings.top:7921"))
                .build();
        int total = 1;
        CompletableFuture[] futures = new CompletableFuture[total];
        for (int i = 0; i < total; i++) {
            futures[i] = client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .whenCompleteAsync((stringHttpResponse, throwable) -> {
                        if (throwable != null) {
                            log.error("发生异常 -> {}", throwable.getMessage(), throwable);
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

class MyTrustManager extends X509ExtendedTrustManager {
    static TrustManager[] get() {
        return new TrustManager[]{new MyTrustManager()};
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType, Socket socket) throws CertificateException {

    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType, Socket socket) throws CertificateException {

    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType, SSLEngine engine) throws CertificateException {

    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType, SSLEngine engine) throws CertificateException {

    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {

    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {

    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return new X509Certificate[0];
    }
}