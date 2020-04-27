package top.codings.websiphon.core.plugins;

import lombok.extern.slf4j.Slf4j;
import net.sf.cglib.proxy.Enhancer;
import top.codings.websiphon.exception.WebPluginException;

@Slf4j
public class PluginFactory {
    public final static <T> T create0(WebPlugin webPlugin, T target) {
        if (target == null) {
            throw new WebPluginException("插件的目标对象不能为空");
        }
        return create0(webPlugin, target, (Class<T>) target.getClass());
    }

    public final static <T> T create0(WebPlugin webPlugin, Class<T> clazz) {
        if (clazz == null) {
            throw new WebPluginException("插件的Class不能为空");
        }
        return create0(webPlugin, null, clazz);
    }

    public final static <T> T create0(WebPlugin webPlugin, T target, Class<T> clazz) {
        if (clazz.getName().contains("$$")) {
            try {
                clazz = (Class<T>) Class.forName(clazz.getName().substring(0, clazz.getName().indexOf("$$")));
//                clazz = (Class<T>) clazz.getClassLoader().loadClass(clazz.getName().substring(0, clazz.getName().indexOf("$$")));
            } catch (ClassNotFoundException e) {
                log.error("无法找到增强类 -> {}", clazz.getName(), e);
                return target;
            }
        }
        Enhancer enhancer = new Enhancer();
        enhancer.setCallback(new WebInterceptor(clazz.isInterface(), target, webPlugin));
        enhancer.setSuperclass(clazz);
        return (T) enhancer.create();
    }

    /*public final static <T> T create(WebPlugin webPlugin, T target) {
        return create(webPlugin, target, null);
    }

    public final static <T> T create(WebPlugin webPlugin, Class<T> clazz) {
        return create(webPlugin, null, clazz);
    }

    public final static <T> T create(WebPlugin webPlugin, Object target, Class<T> clazz) {
        if (target == null && clazz == null) {
            throw new WebPluginException("插件的目标对象或Class需要至少一个不为空");
        }
        boolean real = true;
        if (target == null && clazz.isInterface()) {
            real = false;
        }
        if (clazz == null) {
            clazz = (Class<T>) target.getClass();
        }
        clazz = loop(clazz);
        Enhancer enhancer = new Enhancer();
        enhancer.setCallback(new WebInterceptor(real, target, webPlugin));
        enhancer.setSuperclass(clazz);
        return (T) enhancer.create();
    }

    private static Class loop(Class clazz) {
        if (clazz.getSimpleName().contains("Enhancer")) {
            return loop(clazz.getSuperclass());
        }
        return clazz;
    }*/
}
