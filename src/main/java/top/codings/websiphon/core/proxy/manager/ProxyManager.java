package top.codings.websiphon.core.proxy.manager;

import top.codings.websiphon.core.proxy.bean.ProxyExtension;
import top.codings.websiphon.core.proxy.strategy.ProxyStrategy;

import java.net.Proxy;
import java.util.List;

public interface ProxyManager {
    ProxyManager addProxy(ProxyExtension proxy);

    ProxyManager deleteProxy(String host, int port);

    ProxyManager clear();

    Proxy select();

    void setCheckMinute(int checkMinute);

    void setStrategy(ProxyStrategy strategy);

    void setVaildUrl(String vaildUrl);

    List<ProxyExtension> getProxies();

    List<ProxyExtension> getIllnessProxies();

    int getCheckMinute();

    String getVaildUrl();

    ProxyStrategy getStrategy();
}
