package top.codings.websiphon.bean;

import lombok.ToString;

import java.util.concurrent.atomic.AtomicLong;

@ToString
public class StatisticResult {
    /************************************************************/
    /**                        链接情况                        **/
    /************************************************************/
    public final AtomicLong TOTAL_URL_REQUEST = new AtomicLong(0);
    public final AtomicLong TOTAL_URL_REQUEST_TODAY = new AtomicLong(0);
    public final AtomicLong TOTAL_URL_REQUEST_SUCCESS = new AtomicLong(0);
    public final AtomicLong TOTAL_URL_REQUEST_SUCCESS_TODAY = new AtomicLong(0);
    public final AtomicLong TOTAL_URL_REQUEST_FAIL = new AtomicLong(0);
    public final AtomicLong TOTAL_URL_REQUEST_FAIL_TODAY = new AtomicLong(0);
    public final AtomicLong TOTAL_URL_REQUEST_404 = new AtomicLong(0);
    public final AtomicLong TOTAL_URL_REQUEST_404_TODAY = new AtomicLong(0);
    public final AtomicLong TOTAL_URL_REQUEST_TIMEOUT = new AtomicLong(0);
    public final AtomicLong TOTAL_URL_REQUEST_TIMEOUT_TODAY = new AtomicLong(0);
    /************************************************************/
    /**                        文章情况                        **/
    /************************************************************/
    public final AtomicLong TOTAL_ARTICLE_TOMQ_SUCCESS = new AtomicLong(0);
    public final AtomicLong TOTAL_ARTICLE_TOMQ_SUCCESS_TODAY = new AtomicLong(0);
    public final AtomicLong TOTAL_ARTICLE_TOMQ_FAIL = new AtomicLong(0);
    public final AtomicLong TOTAL_ARTICLE_TOMQ_FAIL_TODAY = new AtomicLong(0);
    public final AtomicLong TOTAL_ARTICLE_PARSE_SUCCESS = new AtomicLong(0);
    public final AtomicLong TOTAL_ARTICLE_PARSE_SUCCESS_TODAY = new AtomicLong(0);
    public final AtomicLong TOTAL_ARTICLE_PARSE_FAIL = new AtomicLong(0);
    public final AtomicLong TOTAL_ARTICLE_PARSE_FAIL_TODAY = new AtomicLong(0);
    public final AtomicLong TOTAL_ARTICLE_TIMEOUT = new AtomicLong(0);
    public final AtomicLong TOTAL_ARTICLE_TIMEOUT_TODAY = new AtomicLong(0);
    public final AtomicLong TOTAL_ARTICLE_REPEAT = new AtomicLong(0);
    public final AtomicLong TOTAL_ARTICLE_REPEAT_TODAY = new AtomicLong(0);
}
