package top.codings.websiphon.core.proxy.strategy;

import top.codings.websiphon.core.proxy.bean.ProxyExtension;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.RandomUtils;

import java.net.Proxy;
import java.util.List;

/**
 * 随机代理策略
 */
public class RandomProxyStrategy implements ProxyStrategy {
    @Override
    public void init() {

    }

    @Override
    public Proxy select(List<ProxyExtension> proxies) {
        if (CollectionUtils.isEmpty(proxies)) {
            return null;
        }
        ProxyExtension proxyExtension = proxies.get(RandomUtils.nextInt(0, proxies.size()));
        proxyExtension.invoke(true);
        return proxyExtension.getProxy();
    }
}
