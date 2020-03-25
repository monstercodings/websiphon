package top.codings.websiphon.core.proxy.strategy;

import top.codings.websiphon.core.proxy.bean.WebProxy;

import java.util.Collection;

public interface ProxyStrategy {
    default void init() {

    }

    WebProxy select(Collection<WebProxy> proxies);

    ProxyStrategy RANDOM = new RandomProxyStrategy();
    ProxyStrategy WEIGHT = new WeightProxyStrategy();
}
