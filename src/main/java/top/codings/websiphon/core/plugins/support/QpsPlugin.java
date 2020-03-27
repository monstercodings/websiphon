package top.codings.websiphon.core.plugins.support;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import top.codings.websiphon.bean.MethodDesc;
import top.codings.websiphon.bean.ReturnPoint;
import top.codings.websiphon.bean.WebRequest;
import top.codings.websiphon.core.plugins.WebPlugin;
import top.codings.websiphon.exception.WebException;
import top.codings.websiphon.factory.bean.WebHandler;
import top.codings.websiphon.util.HttpOperator;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

@Slf4j
@NoArgsConstructor
public class QpsPlugin implements WebPlugin {
    private ExecutorService executorService;
    private AtomicLong totalNow = new AtomicLong(0);
    private AtomicLong totalPrev = new AtomicLong(0);
    private int totalQps;
    private Map<String, HostQps> hostQpsMap = new ConcurrentHashMap<>();
    private Consumer<QpsStats> listener;

    public QpsPlugin(Consumer<QpsStats> listener) {
        this.listener = listener;
    }

    @Override
    public void init() {
        totalNow = new AtomicLong(0);
        totalPrev = new AtomicLong(0);
        executorService = Executors.newSingleThreadExecutor();
        executorService.submit(() -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    long totalNowCount = totalNow.get();
                    long totalPrevCount = totalPrev.get();
                    totalQps = Math.toIntExact(totalNowCount - totalPrevCount);
                    totalPrev.set(totalNowCount);
                    hostQpsMap.values().forEach(hostQps -> {
                        long nowCount = hostQps.now.get();
                        long prevCount = hostQps.prev.get();
                        hostQps.qps = Math.toIntExact(nowCount - prevCount);
                        hostQps.prev.set(nowCount);
                    });
                    if (listener != null) {
                        listener.accept(stats());
                    }
                    TimeUnit.SECONDS.sleep(1);
                }
            } catch (InterruptedException e) {
                return;
            }
        });
    }

    @Override
    public void close() {
        executorService.shutdownNow();
        hostQpsMap.clear();
    }

    @Override
    public Object[] before(Object[] params, Class targetClass, MethodDesc methodDesc, ReturnPoint point) throws WebException {
        if (params[0] instanceof WebRequest) {
            totalNow.incrementAndGet();
            WebRequest request = (WebRequest) params[0];
            HttpOperator.HttpProtocol protocol = HttpOperator.resolve(request.uri());
            HostQps hostQps = hostQpsMap.get(protocol.getHost());
            if (null == hostQps) {
                synchronized (hostQpsMap) {
                    hostQps = hostQpsMap.get(protocol.getHost());
                    if (null == hostQps) {
                        hostQps = new HostQps(protocol.getHost());
                        hostQpsMap.put(protocol.getHost(), hostQps);
                    }
                }
            }
            hostQps.now.incrementAndGet();
        }
        return params;
    }

    @Override
    public Object after(Object proxy, Object[] params, Object result, Class targetClass, MethodDesc methodDesc, ReturnPoint point) throws WebException {
        return result;
    }

    @Override
    public Class[] getTargetInterface() {
        return new Class[]{
                WebHandler.class
        };
    }

    @Override
    public MethodDesc[] getMethods() {
        return new MethodDesc[]{
                new MethodDesc("doOnFinished", new Class[]{Object.class})
        };
    }

    public QpsStats stats() {
        QpsStats qpsStats = new QpsStats(totalQps, Collections.unmodifiableMap(hostQpsMap));
        return qpsStats;
    }

    @Getter
    @AllArgsConstructor
    public static class QpsStats {
        protected int qps;
        protected Map<String, HostQps> hostQpsMap;
    }


    public static class HostQps {
        @Getter
        protected String host;
        protected AtomicLong now = new AtomicLong(0);
        protected AtomicLong prev = new AtomicLong(0);
        @Getter
        protected int qps;

        public HostQps(String host) {
            this.host = host;
        }
    }

}
