package top.codings.websiphon.test.feign;

import feign.Feign;
import feign.RequestLine;
import feign.Retryer;
import feign.codec.Decoder;
import feign.gson.GsonDecoder;
import feign.gson.GsonEncoder;
import lombok.Data;
import top.codings.websiphon.test.website.WebModule;

public interface CrawlManager {
    @RequestLine("GET /crawl/task")
    CrawlTask getTask();

    @Data
    class CrawlTask {
        private int code;
        private String message;
        private WebModule data;
    }

    static CrawlManager create() {
        CrawlManager manager= Feign.builder()
                .encoder(new GsonEncoder())
                .decoder(new GsonDecoder())
                .retryer(new Retryer.Default())
                .requestInterceptor(requestTemplate -> {
                    requestTemplate.header("X-Token", "kd-x");
                })
                .target(CrawlManager.class, "http://127.0.0.1:8080");
        return manager;
    }
}
