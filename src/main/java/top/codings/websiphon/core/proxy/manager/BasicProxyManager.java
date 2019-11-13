package top.codings.websiphon.core.proxy.manager;

import top.codings.websiphon.core.proxy.bean.ProxyExtension;
import top.codings.websiphon.core.proxy.strategy.ProxyStrategy;
import lombok.Data;

import java.net.Proxy;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

@Data
public class BasicProxyManager implements ProxyManager {
    private String vaildUrl = "https://www.baidu.com/";
    private ProxyStrategy strategy = ProxyStrategy.RANDOM;
    private int checkMinute = 5;
    private List<ProxyExtension> proxies = new CopyOnWriteArrayList<>();
    private List<ProxyExtension> illnessProxies = new CopyOnWriteArrayList<>();

    @Override
    public ProxyManager addProxy(ProxyExtension proxy) {
        synchronized (this) {
            illnessProxies.forEach(illProxy -> {
                if (illProxy.getProxy().address().equals(proxy)) {
                    illnessProxies.remove(proxy);
                }
            });
            proxies.add(proxy);
            this.notifyAll();
        }
        return this;
    }

    @Override
    public ProxyManager deleteProxy(String host, int port) {
        synchronized (this) {
            illnessProxies.forEach(illProxy->{
                if (illProxy.getHost().equals(host) && illProxy.getPort() == port) {
                    illnessProxies.remove(illProxy);
                }
            });
            proxies.forEach(illProxy->{
                if (illProxy.getHost().equals(host) && illProxy.getPort() == port) {
                    proxies.remove(illProxy);
                }
            });
        }
        return this;
    }

    @Override
    public ProxyManager clear() {
        synchronized (this) {
            illnessProxies.clear();
            proxies.clear();
        }
        return this;
    }

    @Override
    public Proxy select() {
        return Optional.ofNullable(strategy.select(proxies)).orElse(Proxy.NO_PROXY);
    }
}
