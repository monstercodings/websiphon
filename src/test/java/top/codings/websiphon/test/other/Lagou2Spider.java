package top.codings.websiphon.test.other;

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
public class Lagou2Spider {
    @Test
    public void test() throws InterruptedException {
        Crawler crawler = CrawlerBuilder
                .create()
                .addLast(new WebProcessorAdapter() {
                    @Override
                    public void process(WebRequest request) throws WebParseException {
                        log.debug("html -> \n{}", request.response().getHtml());
                    }
                })
                .addLast(new CookiePlugin(
                        CookiePlugin.ReadFromFile.from("config/cookie.txt"),
                        CookiePlugin.WriteToFile.to("config/cookie.txt")
                ))
                .build();
        crawler.start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> crawler.close()));
        BasicWebRequest request = new BasicWebRequest();
        request.setUri("https://www.lagou.com/jobs/positionAjax.json?px=default&needAddtionalResult=false");
        request.setMethod(WebRequest.Method.POST);
        request.getFormData().put("first", "true");
        request.getFormData().put("pn", "1");
        request.getFormData().put("kd", "安保");
        request.headers().put("Referer", "https://www.lagou.com/jobs/list_%E5%AE%89%E4%BF%9D/p-city_0?px=default");
        request.headers().put("cookie", "_ga=GA1.2.1340118664.1560760181; user_trace_token=20190617162942-0e3bee02-90da-11e9-a399-5254005c3644; LGUID=20190617162942-0e3bf2fb-90da-11e9-a399-5254005c3644; LG_LOGIN_USER_ID=ce6c8d9f349415c6c78b230af2c760f16e5619da38e4fac2; LG_HAS_LOGIN=1; JSESSIONID=ABAAABAAAEEAAII13353F95C715F6567976639E9EC34180; X_MIDDLE_TOKEN=a77cfe47d9ed7ca0afa10367c47b2167; WEBTJ-ID=20200330003600-1712726659a62-0839fbeb23f245-396f7f07-1296000-1712726659bc4b; sensorsdata2015jssdkcross=%7B%22distinct_id%22%3A%22171272666a0c91-041ccec001fa8e-396f7f07-1296000-171272666a1d42%22%2C%22%24device_id%22%3A%22171272666a0c91-041ccec001fa8e-396f7f07-1296000-171272666a1d42%22%7D; sajssdk_2015_cross_new_user=1; _gid=GA1.2.1799546573.1585499760; LGSID=20200330003600-4c38bca9-90c9-4429-a727-6ba5240bfaad; Hm_lvt_4233e74dff0ae5bd0a3d81c6ccf756e6=1585499760; _gat=1; Hm_lpvt_4233e74dff0ae5bd0a3d81c6ccf756e6=1585501964; LGRID=20200330011244-59908b2c-dd9b-474f-9024-27c591861ef4; X_HTTP_TOKEN=830b9ed42eed1a4f9402055851deba8bbef37f43d5; SEARCH_ID=bb727358230c4db58c43e4effc69f6a7");
        crawler.push(request);
        Thread.currentThread().join();
    }
}
