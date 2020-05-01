package top.codings.websiphon.core.proxy.bean;

import lombok.Getter;
import lombok.Setter;

import java.util.concurrent.atomic.AtomicInteger;

public class WebProxy {
    public final static WebProxy NO_PROXY = new WebProxy(null, 0);

    @Getter
    @Setter
    protected String proxyIp;
    @Getter
    @Setter
    protected int proxyPort;
    @Getter
    @Setter
    protected volatile boolean enabled = true;
    @Getter
    @Setter
    protected volatile boolean healthy = true;
    /**
     * 权重
     */
    @Setter
    @Getter
    private int weight = 1;
    /**
     * 调用次数
     */
    protected AtomicInteger invoked = new AtomicInteger(0);

    public WebProxy(String proxyIp, int proxyPort) {
        this.proxyIp = proxyIp;
        this.proxyPort = proxyPort;
    }

    public WebProxy(String proxyIp, int proxyPort, int weight) {
        this.proxyIp = proxyIp;
        this.proxyPort = proxyPort;
        this.weight = weight;
    }

    public void invoked() {
        invoked.incrementAndGet();
    }

    public int invokedCount() {
        return invoked.get();
    }
}
