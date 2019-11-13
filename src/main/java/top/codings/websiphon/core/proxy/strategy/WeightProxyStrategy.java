package top.codings.websiphon.core.proxy.strategy;

import top.codings.websiphon.core.proxy.bean.ProxyExtension;
import org.apache.commons.collections.CollectionUtils;

import java.net.Proxy;
import java.util.List;

/**
 * 权重策略
 */
public class WeightProxyStrategy implements ProxyStrategy {
    @Override
    public void init() {

    }

    @Override
    public Proxy select(List<ProxyExtension> proxies) {
        if (CollectionUtils.isEmpty(proxies)) {
            return Proxy.NO_PROXY;
        }
        // TODO 策略
        return null;
    }
}
