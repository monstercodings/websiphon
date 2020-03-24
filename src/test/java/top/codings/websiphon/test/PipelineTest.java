package top.codings.websiphon.test;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import top.codings.websiphon.bean.RateResult;
import top.codings.websiphon.bean.WebRequest;
import top.codings.websiphon.bean.WebResponse;
import top.codings.websiphon.core.Crawler;
import top.codings.websiphon.core.context.CrawlerContext;
import top.codings.websiphon.core.pipeline.FilePipeline;
import top.codings.websiphon.core.plugins.ExtractUrlPlugin;
import top.codings.websiphon.core.plugins.UrlFilterPlugin;
import top.codings.websiphon.core.processor.WebProcessorAdapter;
import top.codings.websiphon.core.requester.SuperWebRequester;
import top.codings.websiphon.core.support.CrawlerBuilder;
import top.codings.websiphon.exception.WebParseException;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
public class PipelineTest {
    @Test
    public void test() throws InterruptedException {
        Crawler crawler = CrawlerBuilder
                .create()
                .setPermitForHost(4)
                .addLast(new SuperWebRequester())
                .addLast(new FilePipeline("list.txt", "utf-8"))
                .addLast(new WebProcessorAdapter() {
                    @Override
                    public void process(WebRequest request, CrawlerContext context) throws WebParseException {
                        log.debug("请求完成 -> {}", request.uri());
                    }
                })
                .addLast(new ExtractUrlPlugin(true, false))
                .addLast(new UrlFilterPlugin())
                .queueMonitor((ctx, requestHolder, force) -> log.debug("完结"))
                .build();
        crawler.getContext().setId("test");
        crawler.start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> crawler.close()));
        RateResult rateResult = crawler.getContext().getRateResult();
        StringBuilder stringBuilder = new StringBuilder();
        while (true) {
            TimeUnit.SECONDS.sleep(1);
            stringBuilder.append("\n");
            for (Map.Entry<WebResponse.Result, AtomicLong> entry : rateResult.getResultStat().entrySet()) {
                stringBuilder.append(entry.getKey().getKey()).append(":").append(entry.getValue().get()).append("\n");
            }
            log.debug("{}\n{}", rateResult.getEverySecondMessage(), stringBuilder.toString());
            stringBuilder.delete(0, stringBuilder.length());
        }
    }
}
