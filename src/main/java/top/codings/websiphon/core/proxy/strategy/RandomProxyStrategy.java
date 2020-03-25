package top.codings.websiphon.core.proxy.strategy;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.RandomUtils;
import top.codings.websiphon.core.proxy.bean.WebProxy;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 随机代理策略
 */
public class RandomProxyStrategy implements ProxyStrategy {
    @Override
    public void init() {

    }

    @Override
    public WebProxy select(Collection<WebProxy> proxies) {
        if (CollectionUtils.isEmpty(proxies)) {
            return null;
        }
        List<WebProxy> list = proxies.stream().filter(webProxy -> webProxy.isEnabled()).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(list)) {
            return null;
        }
        int index = RandomUtils.nextInt(0, list.size());
        WebProxy proxy = list.get(index);
        proxy.invoked();
        return proxy;
    }
}
