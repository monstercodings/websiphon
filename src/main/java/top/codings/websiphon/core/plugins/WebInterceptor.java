package top.codings.websiphon.core.plugins;

import lombok.Data;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;
import top.codings.websiphon.bean.MethodDesc;
import top.codings.websiphon.bean.ReturnPoint;
import top.codings.websiphon.exception.WebException;
import top.codings.websiphon.exception.WebNetworkException;
import top.codings.websiphon.exception.WebParseException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@Data
public class WebInterceptor implements MethodInterceptor {
    private boolean real;
    private Object target;
    private WebPlugin webPlugin;
    private MethodDesc[] descs;

    public WebInterceptor(WebPlugin webPlugin) {
        this(null, webPlugin);

    }

    public WebInterceptor(Object target, WebPlugin webPlugin) {
        this.target = target;
        this.webPlugin = webPlugin;
        descs = webPlugin.getMethods();
    }

    public WebInterceptor(boolean real, Object target, WebPlugin webPlugin) {
        this.real = real;
        this.target = target;
        this.webPlugin = webPlugin;
        descs = webPlugin.getMethods();
    }

    @Override
    public Object intercept(Object o, Method method, Object[] objects, MethodProxy methodProxy) throws Throwable {
        Object result = null;
        MethodDesc methodDesc = null;
        for (int i = 0; i < descs.length; i++) {
            if (!method.getName().equals(descs[i].getName())) {
                continue;
            }
            Class[] methodParameterTypes = method.getParameterTypes();
            Class[] descParameterTypes = descs[i].getParameterTypes();
            if (methodParameterTypes.length != descParameterTypes.length) {
                continue;
            }
            boolean tempResult = true;
            for (int j = 0; j < methodParameterTypes.length; j++) {
                if (methodParameterTypes[j] != descParameterTypes[j]) {
                    tempResult = false;
                    break;
                }
            }
            if (tempResult) {
                methodDesc = descs[i];
                break;
            }
        }
        if (methodDesc != null) {
            Class targetClass = target == null ? null : target.getClass();
            ReturnPoint point = new ReturnPoint();
            boolean first = true;
            do {
                try {
                    if (point.point == ReturnPoint.Point.BEFORE || first) {
                        first = false;
                        objects = webPlugin.before(objects, targetClass, methodDesc, point);
                        if (point.point == ReturnPoint.Point.BREAK) {
                            break;
                        }
                        point.point = ReturnPoint.Point.BREAK;
                    }
                    // 如果代理的是真正实现对象的话就执行
                    if (real) {
                        if (target == null) {
                            result = methodProxy.invokeSuper(o, objects);
                        } else {
                            result = method.invoke(target, objects);
                        }
                    }
                } catch (Throwable throwable) {
                    point.point = ReturnPoint.Point.ERROR;
                    if (throwable instanceof InvocationTargetException) {
                        if (((InvocationTargetException) throwable).getTargetException() instanceof WebParseException) {
                            WebParseException exception = (WebParseException) ((InvocationTargetException) throwable).getTargetException();
                            throw exception;
                        } else if (((InvocationTargetException) throwable).getTargetException() instanceof WebNetworkException) {
                            WebNetworkException exception = (WebNetworkException) ((InvocationTargetException) throwable).getTargetException();
                            throw exception;
                        } else if (((InvocationTargetException) throwable).getTargetException() instanceof WebException) {
                            WebException exception = (WebException) ((InvocationTargetException) throwable).getTargetException();
                            throw exception;
                        }
                    }
                    Throwable inner = throwable;
                    while (true) {
                        Throwable temp = throwable.getCause();
                        if (null == temp || temp == inner) break;
                        inner = temp;
                    }
                    if (inner != null && inner instanceof InterruptedException) {
                        throw inner;
                    }
                    throw throwable;
                } finally {
                    result = webPlugin.after(o, objects, result, targetClass, methodDesc, point);
                }
            } while (point.point != ReturnPoint.Point.BREAK);
        } else {
            try {
                // 如果代理的是真正实现对象的话就执行
                if (real) {
                    if (target == null) {
                        result = methodProxy.invokeSuper(o, objects);
                    } else {
                        result = method.invoke(target, objects);
                    }
                }
            } catch (Throwable throwable) {
                Throwable inner = throwable;
                while (true) {
                    Throwable temp = throwable.getCause();
                    if (null == temp || temp == inner) break;
                    inner = temp;
                }
                if (inner != null && inner instanceof InterruptedException) {
                    throw inner;
                }
                throw throwable;
            }
        }
        return result;
    }
}
