package top.codings.websiphon.core.plugins.support;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import top.codings.websiphon.bean.MethodDesc;
import top.codings.websiphon.bean.ReturnPoint;
import top.codings.websiphon.bean.WebRequest;
import top.codings.websiphon.core.context.CrawlerContext;
import top.codings.websiphon.core.context.event.async.WebErrorAsyncEvent;
import top.codings.websiphon.core.plugins.WebPlugin;
import top.codings.websiphon.exception.WebException;
import top.codings.websiphon.util.HttpOperator;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Slf4j
public class WebsiphonStatsPlugin implements WebPlugin {
    private Map<Integer, AtomicLong> statusCodeCount = new ConcurrentHashMap<>();
    private Map<String, Map<Integer, AtomicLong>> hostStatusCodeCount = new ConcurrentHashMap<>();
    private AtomicLong failed = new AtomicLong(0);
    private Map<String, AtomicLong> hostFailed = new ConcurrentHashMap<>();

    @Override
    public Object[] before(Object[] params, Class targetClass, MethodDesc methodDesc, ReturnPoint point) throws WebException {
        if (params[0] instanceof WebRequest) {
            WebRequest request = (WebRequest) params[0];
            statsTotal(request);
        } else if (params[0] instanceof WebErrorAsyncEvent) {
            WebErrorAsyncEvent event = (WebErrorAsyncEvent) params[0];
            WebRequest request = event.getRequest();
            statsTotal(request);
        }
        return params;
    }

    private void statsTotal(WebRequest request) {
        HttpOperator.HttpProtocol protocol = HttpOperator.resolve(request.uri());
        if (request.response().getResult() == null) {
            AtomicLong atomicLong = hostFailed.get(protocol.getHost());
            if (null == atomicLong) {
                synchronized (hostFailed) {
                    atomicLong = hostFailed.get(protocol.getHost());
                    if (null == atomicLong) {
                        atomicLong = new AtomicLong(0);
                        hostFailed.put(protocol.getHost(), atomicLong);
                    }
                }
            }
            failed.incrementAndGet();
            atomicLong.incrementAndGet();
            return;
        }
        int statusCode = request.response().getResult().getKey();
        AtomicLong atomicLong = statusCodeCount.get(statusCode);
        if (null == atomicLong) {
            synchronized (statusCodeCount) {
                atomicLong = statusCodeCount.get(statusCode);
                if (null == atomicLong) {
                    atomicLong = new AtomicLong(0);
                    statusCodeCount.put(statusCode, atomicLong);
                }
            }
        }
        atomicLong.incrementAndGet();
        statsHost(request, protocol.getHost());
    }

    private void statsHost(WebRequest request, String host) {
        Map<Integer, AtomicLong> myStatusCodeCount = hostStatusCodeCount.get(host);
        if (null == myStatusCodeCount) {
            synchronized (hostStatusCodeCount) {
                myStatusCodeCount = hostStatusCodeCount.get(host);
                if (null == myStatusCodeCount) {
                    myStatusCodeCount = new ConcurrentHashMap<>();
                    hostStatusCodeCount.put(host, myStatusCodeCount);
                }
            }
        }
        int statusCode = request.response().getResult().getKey();
        AtomicLong atomicLong = myStatusCodeCount.get(statusCode);
        if (null == atomicLong) {
            synchronized (myStatusCodeCount) {
                atomicLong = myStatusCodeCount.get(statusCode);
                if (null == atomicLong) {
                    atomicLong = new AtomicLong(0);
                    myStatusCodeCount.put(statusCode, atomicLong);
                }
            }
        }
        atomicLong.incrementAndGet();
    }

    @Override
    public Object after(Object proxy, Object[] params, Object result, Class targetClass, MethodDesc methodDesc, ReturnPoint point) throws WebException {
        return result;
    }

    @Override
    public Class[] getTargetInterface() {
        return new Class[]{CrawlerContext.class};
    }

    @Override
    public MethodDesc[] getMethods() {
        return new MethodDesc[]{
                new MethodDesc("doOnFinished", new Class[]{Object.class})
        };
    }

    public WebsiphonStats stats() {
        WebsiphonStats stats = new WebsiphonStats();
        for (Map.Entry<Integer, AtomicLong> entry : statusCodeCount.entrySet()) {
            int key = entry.getKey();
            long value = entry.getValue().get();
            stats.getTotalStatusCode().put(key, value);
        }
        stats.setTotalFaild(failed.get());
        for (Map.Entry<String, Map<Integer, AtomicLong>> entry : hostStatusCodeCount.entrySet()) {
            String host = entry.getKey();
            Map<Integer, AtomicLong> myStatusCode = entry.getValue();
            Map<Integer, Long> map = new HashMap<>();
            stats.getHostStatusCode().put(host, map);
            for (Map.Entry<Integer, AtomicLong> inEntry : myStatusCode.entrySet()) {
                int code = inEntry.getKey();
                long count = inEntry.getValue().get();
                map.put(code, count);
            }
        }
        for (Map.Entry<String, AtomicLong> entry : hostFailed.entrySet()) {
            String host = entry.getKey();
            long count = entry.getValue().get();
            stats.getHostFaild().put(host, count);
        }
        return stats;
    }

    public void clear() {
        statusCodeCount.clear();
        failed.set(0);
        hostStatusCodeCount.clear();
        hostFailed.clear();
    }

    @Getter
    public static class WebsiphonStats {
        protected Map<Integer, Long> totalStatusCode = new HashMap<>();
        @Setter
        protected long totalFaild;
        protected Map<String, Map<Integer, Long>> hostStatusCode = new HashMap<>();
        protected Map<String, Long> hostFaild = new HashMap<>();
    }

}
