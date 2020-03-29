package top.codings.websiphon.test.other;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.Test;
import top.codings.websiphon.bean.BasicWebRequest;
import top.codings.websiphon.bean.WebRequest;
import top.codings.websiphon.core.Crawler;
import top.codings.websiphon.core.plugins.support.CookiePlugin;
import top.codings.websiphon.core.plugins.support.UrlFilterPlugin;
import top.codings.websiphon.core.processor.WebProcessorAdapter;
import top.codings.websiphon.core.support.CrawlerBuilder;
import top.codings.websiphon.exception.WebParseException;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

@Slf4j
public class Job51Spider {
    @Test
    public void test() throws InterruptedException, UnsupportedEncodingException {
        String keyword = "安保";
        String uri = "https://search.51job.com/list/040000,000000,0000,00,0,99,%s,2,%d.html";
        Crawler crawler = CrawlerBuilder
                .create()
                .addLast(new WebProcessorAdapter() {
                    @Override
                    public void process(WebRequest request) throws WebParseException {
                        StringBuilder sb = new StringBuilder();
                        Document document;
                        try {
                            document = Jsoup.parse(new String(request.response().getBytes(), "gbk"));
                        } catch (UnsupportedEncodingException e) {
                            return;
                        }
                        int hidTotalPage = Integer.parseInt(document.select("#hidTotalPage").attr("value"));
                        BasicWebRequest pageRequest;
                        for (int i = 2; i <= hidTotalPage; i++) {
                            pageRequest = new BasicWebRequest();
                            pageRequest.setUri(getUrl(uri, keyword, i));
                            request.context().getCrawler().push(pageRequest);
                        }
                        log.debug("总页数 -> {}", hidTotalPage);
                        Elements resultList = document.select("#resultList .el");
                        log.debug(request.uri());
                        for (Element element : resultList) {
                            if (!element.className().equals("el")) {
                                continue;
                            }
                            String position_name = element.select(".t1 a").attr("title");
                            String position_url = element.select(".t1 a").attr("href");
                            String company_name = element.select(".t2 a").attr("title");
                            String company_url = element.select(".t2 a").attr("href");
                            String position_place = element.select(".t3").text();
                            String position_salary = element.select(".t4").text();
                            String release_time = element.select(".t5").text();
                            sb.append("-------------------------------------------------------\n")
                                    .append(String.format("职位:%s", position_name)).append("\n")
                                    .append(String.format("职位链接:%s", position_url)).append("\n")
                                    .append(String.format("公司:%s", company_name)).append("\n")
                                    .append(String.format("公司主页:%s", company_url)).append("\n")
                                    .append(String.format("工作地点:%s", position_place)).append("\n")
                                    .append(String.format("薪资:%s", position_salary)).append("\n")
                                    .append(String.format("发布时间:%s", release_time)).append("\n");
                        }
                        log.debug(sb.toString());
                    }
                })
                /*.addLast(new CookiePlugin(
                        CookiePlugin.ReadFromFile.from("config/cookie.txt"),
                        CookiePlugin.WriteToFile.to("config/cookie.txt")
                ))*/
                .addLast(new UrlFilterPlugin())
                .setNetworkThread(1)
                .build();
        crawler.start();
        BasicWebRequest request = new BasicWebRequest();
        request.setUri(getUrl(uri, keyword, 1));
        crawler.push(request);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> crawler.close()));
        Thread.currentThread().join();
    }

    private String getUrl(String uri, String key, int page) {
        try {
            key = URLEncoder.encode(key, "utf-8");
            return String.format(uri, key, page);
        } catch (UnsupportedEncodingException e) {
            return null;
        }
    }

    public static class Job51First extends BasicWebRequest {
    }

    public static class Job51Ohter extends BasicWebRequest {
    }
}
