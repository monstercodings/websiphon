package top.codings.websiphon.core.plugins.support;

import lombok.extern.slf4j.Slf4j;
import top.codings.websiphon.bean.BasicWebRequest;
import top.codings.websiphon.bean.WebRequest;
import top.codings.websiphon.core.plugins.AspectInfo;
import top.codings.websiphon.core.plugins.WebPluginPro;
import top.codings.websiphon.core.proxy.bean.WebProxy;
import top.codings.websiphon.core.proxy.pool.ProxyPool;
import top.codings.websiphon.core.requester.WebRequester;
import top.codings.websiphon.exception.WebException;

/**
 * 启动代理能力的插件
 */
@Slf4j
public class ProxyPlugin implements WebPluginPro {
    private ProxyPool pool;

    public ProxyPlugin(ProxyPool pool) {
        this.pool = pool;
    }

    @Override
    public void onBefore(AspectInfo aspectInfo, Object[] args) throws WebException {
        BasicWebRequest request = (BasicWebRequest) args[0];
        if (request.getProxy() == null) {
            WebProxy webProxy = pool.select();
            if (webProxy != null) {
                request.setProxy(webProxy);
            }
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
        return 0;
    }

    @Override
    public void init() {
        pool.init();
    }

    @Override
    public void close() {
        pool.shutdown();
    }

    /*@Override
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
    }*/
}
