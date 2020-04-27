package top.codings.websiphon.core.plugins.support;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import lombok.extern.slf4j.Slf4j;
import top.codings.websiphon.bean.WebRequest;
import top.codings.websiphon.core.plugins.AspectInfo;
import top.codings.websiphon.core.plugins.WebPlugin;
import top.codings.websiphon.core.requester.WebRequester;
import top.codings.websiphon.core.schedule.RequestScheduler;
import top.codings.websiphon.exception.WebException;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class UrlFilterPlugin<T extends WebRequest> implements WebPlugin {
    private boolean memCache;
    private int maxCount;
    private double fpp;
    private BloomFilter<String> bloomFilter;
    private CustomUrlFilter<T> filter;
    private Map<Class, AspectInfo> aspectInfoMap = new HashMap<>();
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
        try {
            aspectInfoMap.put(RequestScheduler.class, new AspectInfo(RequestScheduler.class, RequestScheduler.class.getMethod("handle", WebRequest.class)));
            aspectInfoMap.put(WebRequester.class, new AspectInfo(WebRequester.class, WebRequester.class.getMethod("execute", WebRequest.class)));
        } catch (NoSuchMethodException e) {

        }

    }

    public void clear() {
        if (memCache) {
            bloomFilter = BloomFilter.create(Funnels.stringFunnel(Charset.forName("utf-8")), maxCount, fpp);
        }
    }

    /*@Override
    public Object[] before(Object[] params, Class targetClass, MethodDesc methodDesc, ReturnPoint point) throws WebException {
        T request = (T) params[0];
        boolean setin = true;
        if (RequestScheduler.class.isAssignableFrom(targetClass)) {
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
            if (!setin) {
                request.stop();
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
    }

    @Override
    public Class[] getTargetInterface() {
        return new Class[]{
                RequestScheduler.class,
                WebRequester.class};
    }

    @Override
    public MethodDesc[] getMethods() {
        return new MethodDesc[]{
                new MethodDesc("handle", new Class[]{WebRequest.class}),
                new MethodDesc("execute", new Class[]{WebRequest.class})
        };
    }*/

    @Override
    public void onBefore(AspectInfo aspectInfo, Object[] args) throws WebException {
        T request = (T) args[0];
        boolean setin = true;
        if (aspectInfoMap.get(RequestScheduler.class) == aspectInfo) {
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
        } else if (aspectInfoMap.get(WebRequester.class) == aspectInfo) {
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
            if (!setin) {
                request.stop();
            }
        }
        if (!setin) {
//                booleanThreadLocal.set(false);
            request.stop();
        }
    }

    @Override
    public Object onAfterReturning(AspectInfo aspectInfo, Object[] args, Object returnValue) {
        return returnValue;
    }

    @Override
    public void onAfterThrowing(AspectInfo aspectInfo, Object[] args, Throwable throwable) {

    }

    @Override
    public void onFinal(AspectInfo aspectInfo, Object[] args, Throwable throwable) {

    }

    @Override
    public AspectInfo[] aspectInfos() {
        return aspectInfoMap.values().toArray(new AspectInfo[0]);
    }

    @Override
    public int index() {
        return 6000;
    }

    public interface CustomUrlFilter<T> {
        boolean test(T request);

        boolean put(T request);
    }
}
