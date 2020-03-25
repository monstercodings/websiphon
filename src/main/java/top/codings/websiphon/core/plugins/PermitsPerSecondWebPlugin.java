package top.codings.websiphon.core.plugins;

import com.google.common.util.concurrent.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import top.codings.websiphon.bean.MethodDesc;
import top.codings.websiphon.bean.ReturnPoint;
import top.codings.websiphon.bean.WebRequest;
import top.codings.websiphon.core.requester.WebRequester;
import top.codings.websiphon.exception.WebException;

/**
 * 令牌桶插件
 */
@Slf4j
public class PermitsPerSecondWebPlugin implements WebPlugin {
    private RateLimiter rateLimiter;

    public PermitsPerSecondWebPlugin(double permitsPerSecond) {
        rateLimiter = RateLimiter.create(permitsPerSecond);
    }

    @Override
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
    }
}
