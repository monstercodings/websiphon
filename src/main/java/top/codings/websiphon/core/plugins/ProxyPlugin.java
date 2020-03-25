package top.codings.websiphon.core.plugins;

import lombok.extern.slf4j.Slf4j;
import top.codings.websiphon.bean.BasicWebRequest;
import top.codings.websiphon.bean.MethodDesc;
import top.codings.websiphon.bean.ReturnPoint;
import top.codings.websiphon.bean.WebRequest;
import top.codings.websiphon.core.proxy.bean.WebProxy;
import top.codings.websiphon.core.proxy.pool.ProxyPool;
import top.codings.websiphon.core.requester.WebRequester;
import top.codings.websiphon.exception.WebException;

/**
 * 启动代理能力的插件
 */
@Slf4j
public class ProxyPlugin implements WebPlugin {
    private ProxyPool pool;

    public ProxyPlugin(ProxyPool pool) {
        this.pool = pool;
    }

    @Override
    public void init() {
        pool.init();
    }

    @Override
    public void close() {
        pool.shutdown();
    }

    @Override
    public Object[] before(Object[] params, Class targetClass, MethodDesc methodDesc, ReturnPoint point) throws WebException {
        BasicWebRequest request = (BasicWebRequest) params[0];
        if (request.getProxy() == null) {
            WebProxy webProxy = pool.select();
            if (webProxy != null) {
                request.setProxy(webProxy);
            }
        }
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
        return new MethodDesc[]{new MethodDesc("execute", new Class[]{WebRequest.class})};
    }
}
