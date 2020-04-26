package top.codings.websiphon.bean;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum PushResult {
    SUCCESS("成功"),
    FULL_QUEUE("队列已满"),
    URL_REPEAT("URL重复"),
    URL_ERROR("URL格式错误"),
    CRAWLER_STOP("爬虫尚未运行"),
    BLOCK_FAILED("阻塞被打断"),
    TASK_EMPTY("爬取任务为空"),
    ;
    public String value;
}
