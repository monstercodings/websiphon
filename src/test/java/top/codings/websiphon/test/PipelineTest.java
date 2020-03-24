package top.codings.websiphon.test;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import top.codings.websiphon.bean.WebRequest;
import top.codings.websiphon.core.Crawler;
import top.codings.websiphon.core.context.CrawlerContext;
import top.codings.websiphon.core.pipeline.FilePipeline;
import top.codings.websiphon.core.plugins.ExtractUrlPlugin;
import top.codings.websiphon.core.plugins.UrlFilterPlugin;
import top.codings.websiphon.core.processor.WebProcessorAdapter;
import top.codings.websiphon.core.requester.SuperWebRequester;
import top.codings.websiphon.core.support.CrawlerBuilder;
import top.codings.websiphon.exception.WebParseException;

@Slf4j
public class PipelineTest {
    @Test
    public void test() throws InterruptedException {
        Crawler crawler = CrawlerBuilder
                .create()
                .addLast(new SuperWebRequester())
                .addLast(new FilePipeline("list.txt", "utf-8"))
                .addLast(new WebProcessorAdapter() {
                    @Override
                    public void process(WebRequest request, CrawlerContext context) throws WebParseException {
                        log.debug("请求完成 -> {}", request.uri());
                    }
                })
//                .addLast(new ExtractUrlPlugin(true, false))
                .addLast(new UrlFilterPlugin())
                .queueMonitor((ctx, requestHolder, force) -> log.debug("完结"))
                .build();
        crawler.getContext().setId("test");
        crawler.start();
        Thread.currentThread().join();
        /*ReadWritePipeline pipeline = new FilePipeline("list.txt", "utf-8");
        pipeline.init();
        while (true) {
            WebRequest request = pipeline.read();
            if (null == request) {
                continue;
            }
            log.debug("请求对象 -> {}", request.uri());
            TimeUnit.SECONDS.sleep(3);
        }*/
    }
}
