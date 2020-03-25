package top.codings.websiphon.core.plugins;

import top.codings.websiphon.bean.MethodDesc;
import top.codings.websiphon.bean.ReturnPoint;
import top.codings.websiphon.core.context.WebType;
import top.codings.websiphon.exception.WebException;
import top.codings.websiphon.factory.WebFactory;

public interface WebPlugin extends WebType {
    Object[] before(Object[] params, Class targetClass, MethodDesc methodDesc, ReturnPoint point) throws WebException;

    Object after(Object proxy, Object[] params, Object result, Class targetClass, MethodDesc methodDesc, ReturnPoint point) throws WebException;

    Class[] getTargetInterface();

    MethodDesc[] getMethods();

    default void setWebFactory(WebFactory webFactory) {
    }

    default void init() {

    }

    default void close() {

    }

}
