package top.codings.websiphon.core.proxy.bean;

import lombok.Getter;
import lombok.Setter;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class ProxyExtension {
    @Getter
    @Setter
    private String host;
    @Getter
    @Setter
    private int port;
    @Getter
    private Proxy proxy;
    /**
     * 调用次数
     */
    private AtomicLong invoke = new AtomicLong(0);
    /**
     * 是否健康
     */
    private AtomicBoolean fitness = new AtomicBoolean(true);
    /**
     * 权重
     */
    @Setter
    @Getter
    private int weight = 1;

    /**
     * 检测失败次数
     */
    @Getter
    @Setter
    private int fail = 0;
    /**
     * 最大允许失败次数
     */
    @Getter
    @Setter
    private int maxFail=10;

    public ProxyExtension(String host, int port) {
        this.host=host;
        this.port = port;
        proxy = new Proxy(Proxy.Type.HTTP, InetSocketAddress.createUnresolved(host, port));
    }

    public long invoke(boolean isInvoke) {
        if (isInvoke) {
            return invoke.incrementAndGet();
        }
        return invoke.get();
    }

    public void fitness(boolean isFitness) {
        fitness.set(isFitness);
    }

    public boolean isFitness() {
        return fitness.get();
    }
}
