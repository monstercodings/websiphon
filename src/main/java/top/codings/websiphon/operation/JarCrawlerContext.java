package top.codings.websiphon.operation;

import top.codings.websiphon.bean.StatisticResult;
import top.codings.websiphon.core.Crawler;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
public class JarCrawlerContext {
    private String id;
    private String jarPath;
    private Crawler crawler;
    private StatisticResult statisticResult;

    public boolean close() {
        /*CloseLock closeLock = crawler.shutdown();
        boolean result = closeLock.awaitTermination(2, TimeUnit.MINUTES);
        if (result) {
            log.info("已成功关闭Jar包爬虫[{}]", crawler.getId());
        } else {
            log.error("关闭爬虫[{}]失败", crawler.getId());
        }
        return result;*/
        return true;
    }
}
