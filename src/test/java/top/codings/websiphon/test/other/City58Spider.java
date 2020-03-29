package top.codings.websiphon.test.other;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
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
public class City58Spider {
    @Test
    public void test() throws InterruptedException {
        Crawler crawler = CrawlerBuilder
                .create()
                .addLast(new WebProcessorAdapter() {
                    @Override
                    public void process(WebRequest request) throws WebParseException {
                        String jsonStr = request.response().getHtml();
                        jsonStr = jsonStr.substring(jsonStr.indexOf("{"), jsonStr.length() - 1);
                        JSONObject json = JSON.parseObject(jsonStr);
                        JSONArray resumeList = json.getJSONObject("data").getJSONArray("resumeList");
                        StringBuilder sb = new StringBuilder();
                        for (Object o : resumeList) {
                            JSONObject jo = (JSONObject) o;
                            sb.append("\n").append("姓名:").append(jo.getString("trueName")).append("\n").append("电话:").append(jo.getString("mobile"));
                        }
                        log.debug(sb.toString());
                    }
                })
                .addLast(new CookiePlugin(
                        CookiePlugin.ReadFromFile.from("config/cookie.txt"),
                        CookiePlugin.WriteToFile.to("config/cookie.txt")
                ))
                .build();
        crawler.start();
        BasicWebRequest request = new BasicWebRequest();
        request.setUri("https://employer.58.com/resume/deliverlist?showtype=2&feedbacktype=-1&hrTel=-1&deliversource=-1&keyword=&fontKey=54cc1790e8c44180a40c074692f6b235&createTime=&callback=jQuery18004223962894926059_1585475877101&_=1585475877409");
        crawler.push(request);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> crawler.close()));
        Thread.currentThread().join();
    }
}
