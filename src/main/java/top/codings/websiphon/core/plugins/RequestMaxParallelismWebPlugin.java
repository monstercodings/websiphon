package top.codings.websiphon.core.plugins;

import top.codings.websiphon.bean.MethodDesc;
import top.codings.websiphon.bean.ReturnPoint;
import top.codings.websiphon.bean.WebRequest;
import top.codings.websiphon.core.context.CrawlerContext;
import top.codings.websiphon.core.requester.WebRequester;
import top.codings.websiphon.exception.WebException;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Semaphore;

/**
 * 功能重复
 * 已废弃
 */
@Deprecated
@Slf4j
public class RequestMaxParallelismWebPlugin {
    //    private final static ThreadLocal<Boolean> TOKEN = ThreadLocal.withInitial(() -> false);
    private Semaphore semaphore;
    private WebPlugin requireToken = new RequireToken();
    private WebPlugin releaseToken = new ReleaseToken();

    public RequestMaxParallelismWebPlugin(int flow) {
        semaphore = new Semaphore(flow, true);
    }

    public WebPlugin requireTokenPlugin() {
        return requireToken;
    }

    public WebPlugin releaseTokenPlugin() {
        return releaseToken;
    }

    private class RequireToken implements WebPlugin {
        @Override
        public Object[] before(Object[] params, ReturnPoint point) throws WebException {
            try {
                log.trace("准备获取令牌，剩余令牌数量：{}", semaphore.availablePermits());
                semaphore.acquire();
                log.trace("成功获取令牌");
            } catch (InterruptedException e) {
                throw new WebException("等待获取请求令牌时被中断", e);
            }
            return params;
        }

        @Override
        public Object after(Object proxy, Object[] params, Object result, MethodDesc methodDesc, ReturnPoint point) throws WebException {
            return result;
        }

        @Override
        public Class[] getTargetInterface() {
            return new Class[]{
                    WebRequester.class
            };
        }

        @Override
        public MethodDesc[] getMethods() {
            return new MethodDesc[]{
                    new MethodDesc("execute", new Class[]{WebRequest.class, CrawlerContext.class})
            };
        }
    }

    private class ReleaseToken implements WebPlugin {
        @Override
        public Object[] before(Object[] params, ReturnPoint point) throws WebException {
            log.trace("准备释放令牌，剩余令牌数量：{}", semaphore.availablePermits());
            semaphore.release();
            return params;
        }

        @Override
        public Object after(Object proxy, Object[] params, Object result, MethodDesc methodDesc, ReturnPoint point) throws WebException {
            return result;
        }

        @Override
        public Class[] getTargetInterface() {
            return new Class[]{CrawlerContext.class};
        }

        @Override
        public MethodDesc[] getMethods() {
            return new MethodDesc[]{new MethodDesc("finishRequest", new Class[]{WebRequest.class})};
        }
    }

}
