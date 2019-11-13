package top.codings.websiphon.util;

import top.codings.websiphon.bean.RateResult;
import top.codings.websiphon.bean.StatisticResult;
import top.codings.websiphon.operation.JarCrawlerContext;
import top.codings.websiphon.operation.JarScaner;

import java.util.*;

public abstract class StatisticsUtils {
    public final static Map<String, Object> globalResult(Collection<JarCrawlerContext> crawlerContexts) {
        return globalResult(crawlerContexts.toArray(new JarCrawlerContext[0]));
    }

    public final static Map<String, Object> globalResult(JarCrawlerContext... jarCrawlerContexts) {
        if (jarCrawlerContexts.length == 0) {
            return Collections.EMPTY_MAP;
        }
        Map<String, Object> total = new LinkedHashMap<>();
        StatisticResult statisticResult = new StatisticResult();

        Map<String, Object> rateMap = new HashMap<>();
        rateMap.put("QPS", 0L);
        rateMap.put("每秒处理成功数", 0L);
        rateMap.put("成功完成速率", 0D);
        rateMap.put("请求完成总数", 0L);
        rateMap.put("消费消息总数", 0L);
        for (JarCrawlerContext jarCrawlerContext : jarCrawlerContexts) {
            everyCrawler(total, statisticResult, rateMap, jarCrawlerContext);
        }
        collect(total, statisticResult, rateMap, jarCrawlerContexts.length);
        return total;
    }

    public static final Map<String, Object> globalResult(JarScaner scaner) {
        Map<String, Object> total = new LinkedHashMap<>();
        StatisticResult statisticResult = new StatisticResult();

        Map<String, Object> rateMap = new HashMap<>();
        rateMap.put("QPS", 0L);
        rateMap.put("每秒处理成功数", 0L);
        rateMap.put("成功完成速率", 0D);
        rateMap.put("请求完成总数", 0L);
        rateMap.put("消费消息总数", 0L);

        Map<String, JarCrawlerContext> jarMap = scaner.getJarCrawlers();
        jarMap.forEach((id, jarCrawlerContext) -> everyCrawler(total, statisticResult, rateMap, jarCrawlerContext));

        collect(total, statisticResult, rateMap, jarMap.size());
        return total;
    }

    private static void collect(Map<String, Object> total, StatisticResult statisticResult, Map<String, Object> rateMap, int crawlerSize) {
        Map<String, Object> result = new LinkedHashMap<>();
        Map<String, Object> resultToday = new LinkedHashMap<>();
        result.put("总爬取", statisticResult.TOTAL_URL_REQUEST.get());
        resultToday.put("今日爬取", statisticResult.TOTAL_URL_REQUEST_TODAY.get());
        result.put("总请求成功", statisticResult.TOTAL_URL_REQUEST_SUCCESS.get());
        resultToday.put("今日请求成功", statisticResult.TOTAL_URL_REQUEST_SUCCESS_TODAY.get());
        result.put("总请求失败", statisticResult.TOTAL_URL_REQUEST_FAIL.get());
        resultToday.put("今日请求失败", statisticResult.TOTAL_URL_REQUEST_FAIL_TODAY.get());
        result.put("总请求超时", statisticResult.TOTAL_URL_REQUEST_TIMEOUT.get());
        resultToday.put("今日请求超时", statisticResult.TOTAL_URL_REQUEST_TIMEOUT_TODAY.get());
        result.put("总请求404", statisticResult.TOTAL_URL_REQUEST_404.get());
        resultToday.put("今日请求404", statisticResult.TOTAL_URL_REQUEST_404_TODAY.get());
        result.put("总请求成功率", String.format("%.2f%s", (Double.parseDouble(String.valueOf(statisticResult.TOTAL_URL_REQUEST_SUCCESS.get())) / statisticResult.TOTAL_URL_REQUEST.get()) * 100, "%"));
        resultToday.put("今日请求成功率", String.format("%.2f%s", (Double.parseDouble(String.valueOf(statisticResult.TOTAL_URL_REQUEST_SUCCESS_TODAY.get())) / statisticResult.TOTAL_URL_REQUEST_TODAY.get()) * 100, "%"));
        result.put("总成功入库文章", statisticResult.TOTAL_ARTICLE_TOMQ_SUCCESS.get());
        resultToday.put("今日成功入库文章", statisticResult.TOTAL_ARTICLE_TOMQ_SUCCESS_TODAY.get());
        result.put("总失败入库文章", statisticResult.TOTAL_ARTICLE_TOMQ_FAIL.get());
        resultToday.put("今日失败入库文章", statisticResult.TOTAL_ARTICLE_TOMQ_FAIL_TODAY.get());
        result.put("总解析成功", statisticResult.TOTAL_ARTICLE_PARSE_SUCCESS.get());
        resultToday.put("今日解析成功", statisticResult.TOTAL_ARTICLE_PARSE_SUCCESS_TODAY.get());
        result.put("总文章时间超出范围", statisticResult.TOTAL_ARTICLE_TIMEOUT.get());
        resultToday.put("今日文章时间超出范围", statisticResult.TOTAL_ARTICLE_TIMEOUT_TODAY.get());
        result.put("总文章重复", statisticResult.TOTAL_ARTICLE_REPEAT.get());
        resultToday.put("今日文章重复", statisticResult.TOTAL_ARTICLE_REPEAT_TODAY.get());
        result.put("总解析失败数", statisticResult.TOTAL_ARTICLE_PARSE_FAIL.get());
        resultToday.put("今日解析失败数", statisticResult.TOTAL_ARTICLE_PARSE_FAIL_TODAY.get());
        result.put("总入库成功率", String.format("%.2f%s", (Double.parseDouble(String.valueOf(statisticResult.TOTAL_ARTICLE_TOMQ_SUCCESS.get())) / statisticResult.TOTAL_URL_REQUEST.get()) * 100, "%"));
        resultToday.put("今日入库成功率", String.format("%.2f%s", (Double.parseDouble(String.valueOf(statisticResult.TOTAL_ARTICLE_TOMQ_SUCCESS_TODAY.get())) / statisticResult.TOTAL_URL_REQUEST_TODAY.get()) * 100, "%"));
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("所有结果", result);
//            map.put("今日结果", resultToday);

        Map<String, Object> rate = new HashMap<>();
        rate.put("QPS", rateMap.get("QPS") + "/s");
        rate.put("每秒处理成功数", rateMap.get("每秒处理成功数") + "/s");
        rate.put("成功完成速率", String.format("%.2fs", ((Double) rateMap.get("成功完成速率") / crawlerSize)));
        rate.put("请求完成总数", rateMap.get("请求完成总数"));
        rate.put("消费消息总数", rateMap.get("消费消息总数"));
        map.put("速率统计", rate);

        total.put("总计", map);
    }

    private static void everyCrawler(Map<String, Object> total, StatisticResult statisticResult, Map<String, Object> rateMap, JarCrawlerContext jarCrawlerContext) {
        RateResult rateResult = jarCrawlerContext.getCrawler().getContext().getRateResult();
        StatisticResult temp = jarCrawlerContext.getStatisticResult();
        Map<String, Object> result = new LinkedHashMap<>();
        Map<String, Object> resultToday = new LinkedHashMap<>();
        result.put("总爬取", temp.TOTAL_URL_REQUEST.get());
        resultToday.put("今日爬取", temp.TOTAL_URL_REQUEST_TODAY.get());
        statisticResult.TOTAL_URL_REQUEST.addAndGet(temp.TOTAL_URL_REQUEST.get());
        statisticResult.TOTAL_URL_REQUEST_TODAY.addAndGet(temp.TOTAL_URL_REQUEST_TODAY.get());
        result.put("总请求成功", temp.TOTAL_URL_REQUEST_SUCCESS.get());
        resultToday.put("今日请求成功", temp.TOTAL_URL_REQUEST_SUCCESS_TODAY.get());
        statisticResult.TOTAL_URL_REQUEST_SUCCESS.addAndGet(temp.TOTAL_URL_REQUEST_SUCCESS.get());
        statisticResult.TOTAL_URL_REQUEST_SUCCESS_TODAY.addAndGet(temp.TOTAL_URL_REQUEST_SUCCESS_TODAY.get());
        result.put("总请求失败", temp.TOTAL_URL_REQUEST_FAIL.get());
        resultToday.put("今日请求失败", temp.TOTAL_URL_REQUEST_FAIL_TODAY.get());
        statisticResult.TOTAL_URL_REQUEST_FAIL.addAndGet(temp.TOTAL_URL_REQUEST_FAIL.get());
        statisticResult.TOTAL_URL_REQUEST_FAIL_TODAY.addAndGet(temp.TOTAL_URL_REQUEST_FAIL_TODAY.get());
        result.put("总请求超时", temp.TOTAL_URL_REQUEST_TIMEOUT.get());
        resultToday.put("今日请求超时", temp.TOTAL_URL_REQUEST_TIMEOUT_TODAY.get());
        statisticResult.TOTAL_URL_REQUEST_TIMEOUT.addAndGet(temp.TOTAL_URL_REQUEST_TIMEOUT.get());
        statisticResult.TOTAL_URL_REQUEST_TIMEOUT_TODAY.addAndGet(temp.TOTAL_URL_REQUEST_TIMEOUT_TODAY.get());
        result.put("总请求404", temp.TOTAL_URL_REQUEST_404.get());
        resultToday.put("今日请求404", temp.TOTAL_URL_REQUEST_404_TODAY.get());
        statisticResult.TOTAL_URL_REQUEST_404.addAndGet(temp.TOTAL_URL_REQUEST_404.get());
        statisticResult.TOTAL_URL_REQUEST_404_TODAY.addAndGet(temp.TOTAL_URL_REQUEST_404_TODAY.get());
        result.put("总请求成功率", String.format("%.2f%s", (Double.parseDouble(String.valueOf(temp.TOTAL_URL_REQUEST_SUCCESS.get())) / temp.TOTAL_URL_REQUEST.get()) * 100, "%"));
        resultToday.put("今日请求成功率", String.format("%.2f%s", (Double.parseDouble(String.valueOf(temp.TOTAL_URL_REQUEST_SUCCESS_TODAY.get())) / temp.TOTAL_URL_REQUEST_TODAY.get()) * 100, "%"));
        result.put("总成功入库文章", temp.TOTAL_ARTICLE_TOMQ_SUCCESS.get());
        resultToday.put("今日成功入库文章", temp.TOTAL_ARTICLE_TOMQ_SUCCESS_TODAY.get());
        statisticResult.TOTAL_ARTICLE_TOMQ_SUCCESS.addAndGet(temp.TOTAL_ARTICLE_TOMQ_SUCCESS.get());
        statisticResult.TOTAL_ARTICLE_TOMQ_SUCCESS_TODAY.addAndGet(temp.TOTAL_ARTICLE_TOMQ_SUCCESS_TODAY.get());
        result.put("总失败入库文章", statisticResult.TOTAL_ARTICLE_TOMQ_FAIL.get());
        resultToday.put("今日失败入库文章", statisticResult.TOTAL_ARTICLE_TOMQ_FAIL_TODAY.get());
        statisticResult.TOTAL_ARTICLE_TOMQ_FAIL.addAndGet(temp.TOTAL_ARTICLE_TOMQ_FAIL.get());
        statisticResult.TOTAL_ARTICLE_TOMQ_FAIL_TODAY.addAndGet(temp.TOTAL_ARTICLE_TOMQ_FAIL_TODAY.get());
        result.put("总解析成功", temp.TOTAL_ARTICLE_PARSE_SUCCESS.get());
        resultToday.put("今日解析成功", temp.TOTAL_ARTICLE_PARSE_SUCCESS_TODAY.get());
        statisticResult.TOTAL_ARTICLE_PARSE_SUCCESS.addAndGet(temp.TOTAL_ARTICLE_PARSE_SUCCESS.get());
        statisticResult.TOTAL_ARTICLE_PARSE_SUCCESS_TODAY.addAndGet(temp.TOTAL_ARTICLE_PARSE_SUCCESS_TODAY.get());
        result.put("总文章时间超出范围", temp.TOTAL_ARTICLE_TIMEOUT.get());
        resultToday.put("今日文章时间超出范围", temp.TOTAL_ARTICLE_TIMEOUT_TODAY.get());
        statisticResult.TOTAL_ARTICLE_TIMEOUT.addAndGet(temp.TOTAL_ARTICLE_TIMEOUT.get());
        statisticResult.TOTAL_ARTICLE_TIMEOUT_TODAY.addAndGet(temp.TOTAL_ARTICLE_TIMEOUT_TODAY.get());
        result.put("总文章重复", temp.TOTAL_ARTICLE_REPEAT.get());
        resultToday.put("今日文章重复", temp.TOTAL_ARTICLE_REPEAT_TODAY.get());
        statisticResult.TOTAL_ARTICLE_REPEAT.addAndGet(temp.TOTAL_ARTICLE_REPEAT.get());
        statisticResult.TOTAL_ARTICLE_REPEAT_TODAY.addAndGet(temp.TOTAL_ARTICLE_REPEAT_TODAY.get());
        result.put("总解析失败数", temp.TOTAL_ARTICLE_PARSE_FAIL.get());
        resultToday.put("今日解析失败数", temp.TOTAL_ARTICLE_PARSE_FAIL_TODAY.get());
        statisticResult.TOTAL_ARTICLE_PARSE_FAIL.addAndGet(temp.TOTAL_ARTICLE_PARSE_FAIL.get());
        statisticResult.TOTAL_ARTICLE_PARSE_FAIL_TODAY.addAndGet(temp.TOTAL_ARTICLE_PARSE_FAIL_TODAY.get());
        result.put("总入库成功率", String.format("%.2f%s", (Double.parseDouble(String.valueOf(temp.TOTAL_ARTICLE_TOMQ_SUCCESS.get())) / temp.TOTAL_URL_REQUEST.get()) * 100, "%"));
        resultToday.put("今日入库成功率", String.format("%.2f%s", (Double.parseDouble(String.valueOf(temp.TOTAL_ARTICLE_TOMQ_SUCCESS_TODAY.get())) / temp.TOTAL_URL_REQUEST_TODAY.get()) * 100, "%"));
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("所有结果", result);
//            map.put("今日结果", resultToday);
        Map<String, Object> rate = new HashMap<>();
        rate.put("QPS", rateResult.getEverySecondMessage() + "/s");
        rateMap.put("QPS", (Long) rateMap.get("QPS") + rateResult.getEverySecondMessage());
        rate.put("每秒处理成功数", rateResult.getEverySecondCount() + "/s");
        rateMap.put("每秒处理成功数", (Long) rateMap.get("每秒处理成功数") + rateResult.getEverySecondCount());
        rate.put("成功完成速率", String.format("%.2fs", rateResult.getTimeConsuming()));
        rateMap.put("成功完成速率", (Double) rateMap.get("成功完成速率") + rateResult.getTimeConsuming());
        rate.put("请求完成总数", rateResult.getSuccessCount().get());
        rateMap.put("请求完成总数", ((Long) rateMap.get("请求完成总数") + rateResult.getSuccessCount().get()));
        rate.put("消费消息总数", rateResult.getTotalMessage().get());
        rateMap.put("消费消息总数", ((Long) rateMap.get("消费消息总数") + rateResult.getTotalMessage().get()));
        map.put("速率统计", rate);
        total.put(jarCrawlerContext.getCrawler().getId(), map);
    }
}
