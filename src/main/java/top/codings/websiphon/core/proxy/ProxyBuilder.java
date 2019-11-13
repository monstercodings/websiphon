package top.codings.websiphon.core.proxy;

import top.codings.websiphon.core.proxy.bean.ProxyExtension;
import top.codings.websiphon.core.proxy.manager.BasicProxyManager;
import top.codings.websiphon.core.proxy.manager.ProxyManager;
import top.codings.websiphon.core.proxy.strategy.ProxyStrategy;

import java.util.LinkedList;
import java.util.List;

public class ProxyBuilder {
    private String vaildUrl = "https://www.baidu.com/";
    private ProxyStrategy strategy = ProxyStrategy.RANDOM;
    private int checkMinute = 5;
    private List<ProxyExtension> list = new LinkedList<>();

    public final static ProxyBuilder create() {
        return new ProxyBuilder();
    }

    public ProxyBuilder setStrategy(ProxyStrategy strategy) {
        this.strategy = strategy;
        return this;
    }

    public ProxyBuilder setVaildUrl(String url) {
        vaildUrl = url;
        return this;
    }

    public ProxyBuilder setCheckMinute(int minute) {
        this.checkMinute = minute;
        return this;
    }

    public ProxyBuilder addProxy(ProxyExtension proxy) {
        list.add(proxy);
        return this;
    }

    public ProxyManager build() {
        ProxyManager manager = new BasicProxyManager();
        manager.setCheckMinute(checkMinute);
        list.forEach(proxyExtension -> manager.addProxy(proxyExtension));
        manager.setStrategy(strategy);
        manager.setVaildUrl(vaildUrl);
        return manager;
    }
}
