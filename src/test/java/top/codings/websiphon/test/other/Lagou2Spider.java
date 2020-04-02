package top.codings.websiphon.test.other;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.util.StringUtils;
import top.codings.websiphon.bean.BasicWebRequest;
import top.codings.websiphon.bean.WebRequest;
import top.codings.websiphon.bean.WebResponse;
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
                        if (request.response().getResult() == WebResponse.Result.FOUND) {
                            String newUrl = request.response().getHeaders().get("Location");
                            log.debug("[{}] 需要跳转 -> {}", request.response().getResult().getKey(), newUrl);
                            if (StringUtils.isNotBlank(newUrl)) {
                                BasicWebRequest webRequest = new BasicWebRequest();
                                webRequest.setUri(newUrl);
                                request.context().getCrawler().push(webRequest);
                                return;
                            }
                        }
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
        request.setUri("https://www.lagou.com/jobs/list_%E5%AE%89%E4%BF%9D?&cl=false&fromSearch=true&labelWords=&suginput=");
//        request.setMethod(WebRequest.Method.POST);
//        request.getFormData().put("first", "true");
//        request.getFormData().put("pn", "1");
//        request.getFormData().put("kd", "安保");
//        request.headers().put("Referer", "https://www.lagou.com/jobs/list_%E5%AE%89%E4%BF%9D/p-city_0?px=default");
        request.headers().put("cookie", "sensorsdata2015session=%7B%7D; JSESSIONID=ABAAAECAAFDAAEH02827CFC1C74A4257BB4F9E9C891326B; WEBTJ-ID=20200312090431-170cc457d441d7-06b47d148b34ff-3f6b4e04-2073600-170cc457d45527; user_trace_token=20200312090433-53242e9e-41fd-4cc7-9a87-f04b9891e8dd; LGUID=20200312090433-176d6db9-30a3-4345-bf8f-db0efeb27bc2; _ga=GA1.2.1865287624.1583975072; Hm_lvt_4233e74dff0ae5bd0a3d81c6ccf756e6=1583975072; lagou_utm_source=C; _putrc=8AD7FABB1CEE9D49; login=true; unick=%E4%BD%95%E5%81%A5; index_location_city=%E6%B7%B1%E5%9C%B3; sensorsdata2015jssdkcross=%7B%22distinct_id%22%3A%228443452%22%2C%22%24device_id%22%3A%22170cc4349341cd-04ac362f2865ed-3f6b4e04-2073600-170cc43493527e%22%2C%22props%22%3A%7B%22%24latest_traffic_source_type%22%3A%22%E7%9B%B4%E6%8E%A5%E6%B5%81%E9%87%8F%22%2C%22%24latest_referrer%22%3A%22%22%2C%22%24latest_referrer_host%22%3A%22%22%2C%22%24latest_search_keyword%22%3A%22%E6%9C%AA%E5%8F%96%E5%88%B0%E5%80%BC_%E7%9B%B4%E6%8E%A5%E6%89%93%E5%BC%80%22%2C%22%24os%22%3A%22Windows%22%2C%22%24browser%22%3A%22Chrome%22%2C%22%24browser_version%22%3A%2280.0.3987.132%22%7D%2C%22first_id%22%3A%22170cc4349341cd-04ac362f2865ed-3f6b4e04-2073600-170cc43493527e%22%7D; PRE_UTM=; PRE_HOST=; PRE_LAND=https%3A%2F%2Fwww.lagou.com%2F; LGSID=20200330090657-b62af765-dd21-4ee2-9adf-e88252666539; PRE_SITE=https%3A%2F%2Fwww.lagou.com; _gid=GA1.2.1960890994.1585530417; TG-TRACK-CODE=index_search; SEARCH_ID=8b1f565b3c7f47dc9a15232d40d4411d; X_HTTP_TOKEN=4767f47efbe0a4a41081355851dc38c04129cbaae7; _gat=1; LGRID=20200330093002-70e9f67e-1806-4a3b-9fb5-dd0fddb8b780; Hm_lpvt_4233e74dff0ae5bd0a3d81c6ccf756e6=1585531801");
        crawler.push(request);
        Thread.currentThread().join();
    }
}
