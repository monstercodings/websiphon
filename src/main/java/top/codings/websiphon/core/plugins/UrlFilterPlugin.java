package top.codings.websiphon.core.plugins;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import lombok.extern.slf4j.Slf4j;
import top.codings.websiphon.bean.MethodDesc;
import top.codings.websiphon.bean.ReturnPoint;
import top.codings.websiphon.bean.WebRequest;
import top.codings.websiphon.core.requester.WebRequester;
import top.codings.websiphon.core.schedule.RequestScheduler;
import top.codings.websiphon.exception.WebException;

import java.nio.charset.Charset;

@Slf4j
public class UrlFilterPlugin<T extends WebRequest> implements WebPlugin {
    private boolean memCache;
    private int maxCount;
    private double fpp;
    private BloomFilter<String> bloomFilter;
    private CustomUrlFilter<T> filter;
//    private ThreadLocal<Boolean> booleanThreadLocal = ThreadLocal.withInitial(() -> true);

    public UrlFilterPlugin() {
        this(true, null);
    }

    public UrlFilterPlugin(int maxCount, double fpp) {
        this(true, maxCount, fpp, null);
    }

    public UrlFilterPlugin(boolean memCache, CustomUrlFilter<T> filter) {
        this(memCache, 200000000, 0.001D, filter);
    }

    public UrlFilterPlugin(boolean memCache, int maxCount, double fpp, CustomUrlFilter<T> filter) {
        this.memCache = memCache;
        this.maxCount = maxCount;
        this.fpp = fpp;
        if (memCache) {
            bloomFilter = BloomFilter.create(Funnels.stringFunnel(Charset.forName("utf-8")), maxCount, fpp);
        }
        this.filter = filter;
    }

    public void clear() {
        if (memCache) {
            bloomFilter = BloomFilter.create(Funnels.stringFunnel(Charset.forName("utf-8")), maxCount, fpp);
        }
    }

    @Override
    public Object[] before(Object[] params, Class targetClass, MethodDesc methodDesc, ReturnPoint point) throws WebException {
        T request = (T) params[0];
        boolean setin = true;
        if (RequestScheduler.class.isAssignableFrom(targetClass)) {
            log.debug("过滤插件");
            if (bloomFilter == null) {
                setin = true;
            } else {
                setin = !bloomFilter.test(request.uri());
            }
            if (setin) {
                if (null != filter) {
                    setin = !filter.test(request);
                    if (!setin && bloomFilter != null) {
                        bloomFilter.put(request.uri());
                    }
                }
            }
        } else if (WebRequester.class.isAssignableFrom(targetClass)) {
            if (bloomFilter == null) {
                setin = true;
            } else {
                setin = bloomFilter.put(request.uri());
            }
            if (setin) {
                if (null != filter) {
                    setin = filter.put(request);
                }
            }
        }
        if (!setin) {
//                booleanThreadLocal.set(false);
            point.point = ReturnPoint.Point.BREAK;
        }
        return params;
    }

    @Override
    public Object after(Object proxy, Object[] params, Object result, Class targetClass, MethodDesc methodDesc, ReturnPoint point) throws WebException {
//        booleanThreadLocal.set(true);
        return result;
        /*try {
            if (!booleanThreadLocal.get()) {
                return PushResult.URL_REPEAT;
            }
            return result;
        } finally {
            booleanThreadLocal.set(true);
        }*/
    }

    @Override
    public Class[] getTargetInterface() {
        return new Class[]{RequestScheduler.class, WebRequester.class};
    }

    @Override
    public MethodDesc[] getMethods() {
        return new MethodDesc[]{
                new MethodDesc("handle", new Class[]{WebRequest.class}),
                new MethodDesc("execute", new Class[]{WebRequest.class})
        };
    }

    public interface CustomUrlFilter<T> {
        boolean test(T request);

        boolean put(T request);
    }
}
