package top.codings.websiphon.core.proxy.pool;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import top.codings.websiphon.core.proxy.bean.WebProxy;
import top.codings.websiphon.core.proxy.strategy.ProxyStrategy;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 代理IP池
 */
@Slf4j
@NoArgsConstructor
public class BasicProxyPool implements ProxyPool {
    private ProxyStrategy strategy = ProxyStrategy.WEIGHT;
    private Map<String, WebProxy> proxies = new ConcurrentHashMap();

    public BasicProxyPool(ProxyStrategy strategy) {
        this.strategy = strategy;
    }

    @Override
    public ProxyPool add(WebProxy proxy) {
        String proxyIp = proxy.getProxyIp();
        int proxyPort = proxy.getProxyPort();
        if (StringUtils.isBlank(proxyIp)) {
            throw new IllegalArgumentException("代理的ip地址不能为空");
        }
        proxies.put(new StringBuilder(proxyIp).append(":").append(proxyPort).toString(), proxy);
        return this;
    }

    @Override
    public WebProxy remove(WebProxy proxy) {
        return remove(proxy.getProxyIp(), proxy.getProxyPort());
    }

    @Override
    public WebProxy remove(String proxyIp, int proxyPort) {
        return proxies.remove(new StringBuilder(proxyIp).append(":").append(proxyPort).toString());
    }

    @Override
    public void clear() {
        proxies.clear();
    }

    @Override
    public void shutdown() {
        clear();
    }

    @Override
    public WebProxy select() {
        Collection<WebProxy> collection = proxies.values().stream().filter(proxy -> proxy.isEnabled() && proxy.isHealthy()).collect(Collectors.toList());
        return strategy.select(collection);
    }
}
