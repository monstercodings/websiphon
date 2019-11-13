package top.codings.websiphon.core.proxy.strategy;

import top.codings.websiphon.core.proxy.bean.ProxyExtension;

import java.net.Proxy;
import java.util.List;

public interface ProxyStrategy {
    void init();
    Proxy select(List<ProxyExtension> proxies);

    ProxyStrategy RANDOM=new RandomProxyStrategy();
    ProxyStrategy WEIGHT=new WeightProxyStrategy();
}
