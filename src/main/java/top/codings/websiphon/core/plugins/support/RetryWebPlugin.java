package top.codings.websiphon.core.plugins.support;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import top.codings.websiphon.bean.MethodDesc;
import top.codings.websiphon.bean.ReturnPoint;
import top.codings.websiphon.bean.WebRequest;
import top.codings.websiphon.bean.WebResponse;
import top.codings.websiphon.core.plugins.WebPlugin;
import top.codings.websiphon.core.requester.WebRequester;

import java.util.Map;

@Data
@Slf4j
@Deprecated
public class RetryWebPlugin implements WebPlugin {
    private final static ThreadLocal<Integer> COUNT = ThreadLocal.withInitial(() -> 0);
    private int retry;
    private long sleep = 1500;

    public RetryWebPlugin(int retry) {
        this.retry = retry;
    }

    @Override
    public Object[] before(Object[] params, Class targetClass, MethodDesc methodDesc, ReturnPoint point) {
        COUNT.set(0);
        return params;
    }

    @Override
    public Object after(Object proxy, Object[] params, Object result, Class targetClass, MethodDesc methodDesc, ReturnPoint point) {
        if (null == result) {
            return result;
        }
        WebResponse response = (WebResponse) result;
        if ((response.getResult() == WebResponse.Result.FAIL || response.getResult() == WebResponse.Result.TIME_OUT) && COUNT.get() < retry) {
            COUNT.set(COUNT.get() + 1);
            try {
                Thread.sleep(sleep);
                log.warn("请求URL[{}]失败 | 重试[{}]", response.getUrl(), COUNT.get());
            } catch (InterruptedException e) {
                log.error("重试URL[{}]期间线程休眠中断 >>>>> {}", response.getUrl(), e.getLocalizedMessage());
                point.point = ReturnPoint.Point.BREAK;
                return result;
            }
            point.point = ReturnPoint.Point.INVOKE;
            return result;
        }
        point.point = ReturnPoint.Point.BREAK;
        return result;
    }

    @Override
    public Class[] getTargetInterface() {
        return new Class[]{WebRequester.class};
    }

    @Override
    public MethodDesc[] getMethods() {
        return new MethodDesc[]{
                new MethodDesc("execute", new Class[]{String.class, Map.class, int.class}),
                new MethodDesc("execute", new Class[]{WebRequest.class})
        };
    }
}
