package top.codings.websiphon.core.proxy.strategy;

import lombok.AllArgsConstructor;
import org.apache.commons.collections.CollectionUtils;
import top.codings.websiphon.core.proxy.bean.WebProxy;

import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.util.Collection;
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
    public WebProxy select(Collection<WebProxy> proxies) {
        if (CollectionUtils.isEmpty(proxies)) {
            return null;
        }
        long count = INDEX.getAndIncrement();
        int index = (int) (count % proxies.size());
        // TODO 顺序代理策略
        return null;
    }
}
