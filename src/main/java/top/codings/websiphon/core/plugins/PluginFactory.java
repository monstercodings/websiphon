package top.codings.websiphon.core.plugins;

import top.codings.websiphon.exception.WebPluginException;
import lombok.extern.slf4j.Slf4j;
import net.sf.cglib.proxy.Enhancer;

@Slf4j
public class PluginFactory {
    public final static <T> T create(WebPlugin webPlugin, T target) {
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
    }
}
