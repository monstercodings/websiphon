package top.codings.websiphon.bean;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum PushResult {
    SUCCESS("成功"),
    FULL_QUEUE("队列已满"),
    URL_REPEAT("URL重复"),
    CRAWLER_STOP("爬虫尚未运行"),
    ;
    public String value;
}
