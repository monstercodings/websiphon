package top.codings.websiphon.core.plugins;

import lombok.extern.slf4j.Slf4j;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;
import top.codings.websiphon.exception.WebException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@Slf4j
public class WebInterceptorPro implements MethodInterceptor {
    private boolean interfaceClass;
    private Object target;
    private WebPluginPro plugin;
    private AspectInfo[] aspectInfos;
    //    private List<WebPluginPro> plugins = new LinkedList<>();

    public WebInterceptorPro(boolean interfaceClass, Object target, WebPluginPro plugin) {
        this.interfaceClass = interfaceClass;
        this.target = target;
        this.plugin = plugin;
    }


    /*public WebInterceptorPro addPlugin(WebPluginPro plugin) {
        plugins.add(plugin);
        plugins.sort(Comparator.comparingInt(WebPluginPro::index));
        return this;
    }*/

    @Override
    public Object intercept(Object proxy, Method currentMethod, Object[] args, MethodProxy methodProxy) throws Throwable {
        if (null == aspectInfos) {
            aspectInfos = plugin.aspectInfos();
            if (null == aspectInfos) {
                aspectInfos = new AspectInfo[0];
            }
        }
        for (AspectInfo aspectInfo : aspectInfos) {
            if (aspectInfo.getClazz().isAssignableFrom(proxy.getClass()) &&
                    currentMethod.getName().equals(aspectInfo.getMethod().getName()) &&
                    equalParamTypes(aspectInfo.getMethod().getParameterTypes(), currentMethod.getParameterTypes())) {
                return doAspect(aspectInfo, plugin, proxy, args, methodProxy);
//                plugin.onBefore(aspectInfo, args);
            }
        }
        return target == null ? methodProxy.invokeSuper(proxy, args) : methodProxy.invoke(target, args);
    }

    private boolean equalParamTypes(Class<?>[] params1, Class<?>[] params2) {
        if (params1.length == params2.length) {
            for (int i = 0; i < params1.length; i++) {
                if (params1[i] != params2[i])
                    return false;
            }
            return true;
        }
        return false;
    }

    private Object doAspect(AspectInfo aspectInfo, WebPluginPro plugin, Object proxy, Object[] args, MethodProxy methodProxy) throws Throwable {
        Throwable t = null;
        Object result = null;
        try {
            /*try {
                plugin.onBefore(aspectInfo, args);
            } catch (WebException e) {
                return null;
            }*/
            plugin.onBefore(aspectInfo, args);
            if (target == null) {
                if (!interfaceClass) {
                    result = methodProxy.invokeSuper(proxy, args);
                }
            } else {
                result = methodProxy.invoke(target, args);
            }
            try {
                result = plugin.onAfterReturning(aspectInfo, args, result);
            } catch (Throwable e) {
                log.error("插件处理正常后置失败", e);
                return result;
            }
            return result;
        } catch (WebException e) {
            t = e;
            throw e;
        } catch (InvocationTargetException e) {
            do {
                if (t == null) t = e.getTargetException();
                if (t instanceof InvocationTargetException) {
                    Throwable temp = t.getCause();
                    if (null == temp) {
                        break;
                    }
                    t = temp;
                    continue;
                }
                break;
            } while (true);
            try {
                plugin.onAfterThrowing(aspectInfo, args, t);
            } catch (Throwable ex) {
                log.error("插件处理异常后置失败", ex);
            }
            throw t;
        } catch (Throwable throwable) {
            t = throwable;
            try {
                plugin.onAfterThrowing(aspectInfo, args, t);
            } catch (Throwable ex) {
                log.error("插件处理异常后置失败", ex);
            }
            throw t;
        } finally {
            try {
                plugin.onFinal(aspectInfo, args, t);
            } catch (Throwable e) {
                log.error("插件处理最终后置失败", e);
            }
        }
    }
}
