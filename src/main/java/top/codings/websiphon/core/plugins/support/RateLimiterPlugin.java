package top.codings.websiphon.core.plugins.support;

import com.google.common.util.concurrent.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import top.codings.websiphon.bean.WebRequest;
import top.codings.websiphon.core.plugins.AspectInfo;
import top.codings.websiphon.core.plugins.WebPlugin;
import top.codings.websiphon.core.requester.WebRequester;
import top.codings.websiphon.exception.WebException;

/**
 * 令牌桶插件
 * 与URL过滤插件一起使用时，需要放在UrlFilterPlugin的前面
 */
@Slf4j
public class RateLimiterPlugin implements WebPlugin {
    private RateLimiter rateLimiter;

    public RateLimiterPlugin(double permitsPerSecond) {
        rateLimiter = RateLimiter.create(permitsPerSecond);
    }

    /*@Override
    public Object[] before(Object[] params, Class targetClass, MethodDesc methodDesc, ReturnPoint point) throws WebException {
        rateLimiter.acquire();
        return params;
    }

    @Override
    public Object after(Object proxy, Object[] params, Object result, Class targetClass, MethodDesc methodDesc, ReturnPoint point) throws WebException {
        return result;
    }

    @Override
    public Class[] getTargetInterface() {
        return new Class[]{WebRequester.class};
    }

    @Override
    public MethodDesc[] getMethods() {
        return new MethodDesc[]{
                new MethodDesc("execute", new Class[]{WebRequest.class})
        };
    }*/

    @Override
    public void onBefore(AspectInfo aspectInfo, Object[] args) throws WebException {
        rateLimiter.acquire();
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
        try {
            return new AspectInfo[]{
                    new AspectInfo(WebRequester.class, WebRequester.class.getMethod("execute", WebRequest.class))
            };
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    @Override
    public int index() {
        return 7000;
    }
}
