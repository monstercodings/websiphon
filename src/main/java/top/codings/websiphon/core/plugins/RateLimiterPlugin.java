package top.codings.websiphon.core.plugins;

import top.codings.websiphon.bean.MethodDesc;
import top.codings.websiphon.bean.ReturnPoint;
import top.codings.websiphon.bean.WebRequest;
import top.codings.websiphon.core.context.CrawlerContext;
import top.codings.websiphon.core.requester.WebRequester;
import top.codings.websiphon.exception.WebException;
import top.codings.websiphon.factory.bean.WebHandler;
import com.google.common.util.concurrent.RateLimiter;
import lombok.extern.slf4j.Slf4j;

/**
 * 令牌桶插件
 */
@Slf4j
public class RateLimiterPlugin implements WebPlugin {
    private RateLimiter rateLimiter;

    public RateLimiterPlugin(double permitsPerSecond) {
        rateLimiter = RateLimiter.create(permitsPerSecond);
    }

    @Override
    public Object[] before(Object[] params, ReturnPoint point) throws WebException {
        rateLimiter.acquire();
        return params;
    }

    @Override
    public Object after(Object proxy, Object[] params, Object result, MethodDesc methodDesc, ReturnPoint point) throws WebException {
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
                new MethodDesc("request", new Class[0])
        };
    }
}
