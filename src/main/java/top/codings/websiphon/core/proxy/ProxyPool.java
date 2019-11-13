package top.codings.websiphon.core.proxy;

import top.codings.websiphon.core.proxy.bean.ProxyExtension;
import top.codings.websiphon.core.proxy.manager.ProxyManager;
import top.codings.websiphon.util.HeadersUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Connection;
import org.jsoup.Jsoup;

import javax.net.ssl.SSLSocketFactory;
import java.net.Proxy;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 代理IP池
 */
@Slf4j
public class ProxyPool {
    @Getter
    private ExecutorService executorService = Executors.newSingleThreadExecutor(new ThreadFactory() {
        private final AtomicInteger threadNumber = new AtomicInteger(1);

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(Thread.currentThread().getThreadGroup(), r,
                    "proxy-health-" + threadNumber.getAndIncrement(),
                    0);
            if (t.isDaemon())
                t.setDaemon(false);
            if (t.getPriority() != Thread.NORM_PRIORITY)
                t.setPriority(Thread.NORM_PRIORITY);
            return t;
        }
    });

    @Setter
    @Getter
    private ProxyManager manager;

    public Proxy select() {
        return manager == null ? null : manager.select();
    }

    public void checkProxyFitness() {
        while (!Thread.currentThread().isInterrupted()) {
            List<ProxyExtension> proxies = manager.getProxies();
            if (proxies.size() == 0) {
                synchronized (manager) {
                    try {
                        manager.wait();
                    } catch (InterruptedException e) {
                        break;
                    }
                    continue;
                }
            }
            proxies.forEach(proxyExtension -> {
                Exception result = null;
                for (int i = 0; i < 3; i++) {
                    result = requestByProxy(proxyExtension.getProxy());
                    if (result == null) {
                        break;
                    }
                    try {
                        TimeUnit.SECONDS.sleep(3);
                    } catch (InterruptedException e) {
                        return;
                    }
                }
                if (result == null) {
                    proxyExtension.setFail(0);
                    return;
                }
                proxyExtension.setFail(proxyExtension.getFail() + 1);
                if (proxyExtension.getFail() > proxyExtension.getMaxFail()) {
                    log.warn("代理IP[{}]节点不健康，原因 -> {}", proxyExtension.getProxy().address(), result.getLocalizedMessage());
                    proxies.remove(proxyExtension);
                    proxyExtension.fitness(false);
                    manager.getIllnessProxies().add(proxyExtension);
                }
            });
            try {
                TimeUnit.MINUTES.sleep(manager.getCheckMinute());
            } catch (InterruptedException e) {
                break;
            }
        }
        log.info("已停止代理池检测");
    }

    private Exception requestByProxy(Proxy proxy) {
        try {
            Connection.Response response = Jsoup.connect(manager.getVaildUrl()).headers(HeadersUtils.getHeaders()).maxBodySize(0).ignoreHttpErrors(true).ignoreContentType(true).timeout(30000).sslSocketFactory((SSLSocketFactory) SSLSocketFactory.getDefault()).proxy(proxy).execute();
            if (response.statusCode() != 200) {
                throw new IllegalStateException(String.format("请求返回码不正确[%d]", response.statusCode()));
            }
            return null;
        } catch (Exception e) {
            return e;
        }
    }

    public void init() {
        if (null == manager) {
            return;
//            manager = ProxyBuilder.create().build();
        }
        if (StringUtils.isBlank(manager.getVaildUrl())) {
            throw new IllegalArgumentException("请先设置代理池的校验地址vaildUrl");
        }
        manager.getStrategy().init();
        log.info("启动代理IP池，总计共{}个代理对象", manager.getProxies().size());
        executorService.submit(() -> checkProxyFitness());
    }

    public void shutdownNow() {
        executorService.shutdownNow();
    }
}
