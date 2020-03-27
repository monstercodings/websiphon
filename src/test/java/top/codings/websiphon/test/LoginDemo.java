package top.codings.websiphon.test;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import top.codings.websiphon.bean.BasicWebRequest;
import top.codings.websiphon.bean.WebRequest;
import top.codings.websiphon.core.Crawler;
import top.codings.websiphon.core.plugins.support.CookiePlugin;
import top.codings.websiphon.core.processor.WebProcessorAdapter;
import top.codings.websiphon.core.support.CrawlerBuilder;
import top.codings.websiphon.exception.WebParseException;

@Slf4j
public class LoginDemo {
    @Test
    public void test() throws InterruptedException {
        Crawler crawler = CrawlerBuilder
                .create()
                .addLast(new WebProcessorAdapter() {
                    @Override
                    public void process(WebRequest request) throws WebParseException {
                        log.debug("响应[{}] -> \n{}", request.response().getResult().getKey(), request.response().getHtml());
                    }
                })
                .addLast(new CookiePlugin(
                        CookiePlugin.ReadFromFile.from("cookie.txt"),
                        CookiePlugin.WriteToFile.to("cookie.txt")))
                .build();
        crawler.getContext().setId("gitlab");
        crawler.start();
        BasicWebRequest request = new BasicWebRequest();
        request.setUri("http://gitlab.szkedun.cn/kd-business/monitor");
        crawler.push(request);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> crawler.close()));
        Thread.currentThread().join(3000);
    }
}
