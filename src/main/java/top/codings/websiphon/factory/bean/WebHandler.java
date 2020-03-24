package top.codings.websiphon.factory.bean;

import top.codings.websiphon.bean.PushResult;
import top.codings.websiphon.bean.WebRequest;
import top.codings.websiphon.core.context.CrawlerContext;
import top.codings.websiphon.core.context.event.WebAsyncEvent;
import top.codings.websiphon.core.context.event.WebSyncEvent;
import top.codings.websiphon.core.context.event.async.WebErrorAsyncEvent;
import top.codings.websiphon.core.proxy.manager.ProxyManager;

public interface WebHandler<IN extends WebRequest> {
    void request(CrawlerContext context) throws InterruptedException;

    /**
     * 将请求对象推入待处理请求池
     *
     * @param request
     * @return
     */
    PushResult write(IN request);

    /**
     * 完成时处理请求数据
     *
     * @param data
     */
    void doOnFinished(Object data);

    /**
     * 启用代理
     *
     * @param manager
     */
    void enableProxy(ProxyManager manager);

    /**
     * 初始化持有器
     *
     * @param networkCount 最大网络并发请求数
     * @param parseCount   最大解析处理数
     */
    void init(int networkCount, int parseCount);

    /**
     * 推送异步事件
     *
     * @param event
     */
    boolean postAsyncEvent(WebAsyncEvent event);

    /**
     * 推送同步事件
     *
     * @param event
     * @throws Exception
     */
    boolean postSyncEvent(WebSyncEvent event) throws Exception;

    /**
     * 关闭爬虫
     * 释放资源
     */
    void close() throws Exception;
}
