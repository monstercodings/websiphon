package top.codings.websiphon.core.requester;

import top.codings.websiphon.bean.WebRequest;
import top.codings.websiphon.bean.WebResponse;
import top.codings.websiphon.core.context.CrawlerContext;
import top.codings.websiphon.core.context.WebType;
import top.codings.websiphon.exception.WebNetworkException;

import java.io.IOException;

public interface WebRequester<IN extends WebRequest> extends WebType {
    /**
     * 请将所有初始化请求器的操作都放在此处
     * 禁止在构造器里进行初始化操作
     */
    void init() throws Exception;

    void execute(IN in) throws WebNetworkException;

    /**
     * 当前正在请求的连接数
     *
     * @return
     */
    int size();

    boolean isHealth();

    void close();
}
