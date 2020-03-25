package top.codings.websiphon.core.proxy.pool;

import top.codings.websiphon.core.proxy.bean.WebProxy;

public interface ProxyPool {
    default void init() {

    }

    default void shutdown() {

    }

    ProxyPool add(WebProxy proxy);

    WebProxy remove(WebProxy proxy);

    WebProxy remove(String proxyIp, int proxyPort);

    void clear();

    WebProxy select();
}
