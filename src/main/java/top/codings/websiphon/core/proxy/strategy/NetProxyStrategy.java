package top.codings.websiphon.core.proxy.strategy;

import top.codings.websiphon.core.proxy.bean.ProxyExtension;
import lombok.AllArgsConstructor;
import org.apache.commons.collections.CollectionUtils;

import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 网络请求API接口随机获取代理IP策略
 */
@AllArgsConstructor
public class NetProxyStrategy implements ProxyStrategy {
    private String user;
    private String password;
    private static final AtomicLong INDEX = new AtomicLong(0);

    @Override
    public void init() {
        Authenticator.setDefault(new Authenticator() {
            public PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(user, password.toCharArray());
            }
        });
    }

    @Override
    public Proxy select(List<ProxyExtension> proxies) {
        if (CollectionUtils.isEmpty(proxies)) {
            return null;
        }
        long count = INDEX.getAndIncrement();
        int index = (int) (count % proxies.size());
        ProxyExtension proxyExtension = proxies.get(index);
        proxyExtension.invoke(true);
        return proxyExtension.getProxy();
    }
}
