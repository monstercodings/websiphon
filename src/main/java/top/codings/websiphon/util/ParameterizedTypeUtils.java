package top.codings.websiphon.util;

import top.codings.websiphon.bean.WebRequest;
import top.codings.websiphon.core.context.event.listener.WebAsyncEventListener;
import top.codings.websiphon.core.context.event.listener.WebSyncEventListener;
import top.codings.websiphon.core.processor.WebProcessor;
import top.codings.websiphon.core.processor.WebProcessorAdapter;
import top.codings.websiphon.core.requester.WebRequester;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public class ParameterizedTypeUtils {
    /*public static Class[] getType(Object object) {
        List<Class> list = new LinkedList<>();
        Type[] types = object.getClass().getGenericInterfaces();
        if (types.length == 1 && types[0] == Factory.class) {
            types = ((Class) object.getClass().getGenericSuperclass()).getGenericInterfaces();
            if (types.length == 0) {
                types = ((ParameterizedType) ((Class) object.getClass().getGenericSuperclass()).getGenericSuperclass()).getActualTypeArguments();
                for (Type type : types) {
                    list.add((Class) type);
                }
            }
        } else if (types.length > 0) {
            for (Type type : types) {
                if (type instanceof ParameterizedType) {
                    for (Type actualType : ((ParameterizedType) type).getActualTypeArguments()) {
                        Class param;
                        if (actualType instanceof TypeVariable) {
                            param = (Class) ((TypeVariable) actualType).getBounds()[0];
                        } else if (actualType instanceof Class) {
                            param = (Class) actualType;
                        } else {
                            continue;
                        }
                        list.add(param);
                    }
                }
            }
        } else {
            if (object.getClass().getGenericSuperclass() instanceof ParameterizedType) {
                for (Type actualType : ((ParameterizedType) object.getClass().getGenericSuperclass()).getActualTypeArguments()) {
                    if (actualType instanceof Class) {
                        list.add((Class) actualType);
                    } else if (actualType instanceof ParameterizedType) {
                        list.add((Class) ((ParameterizedType) actualType).getRawType());
                    }
                }
            }
        }
        return list.toArray(new Class[0]);
    }*/

    public static Class getType(Object object) {
        if (object instanceof WebRequester) {
            Type[] types = object.getClass().getGenericInterfaces();
            if (types.length > 0) {
                for (Type type : types) {
                    if (ParameterizedType.class.isAssignableFrom(type.getClass())) {
                        ParameterizedType parameterizedType = (ParameterizedType) type;
                        if (parameterizedType.getRawType() == WebRequester.class) {
                            return (Class) parameterizedType.getActualTypeArguments()[0];
                        }
                    } else if (WebRequester.class == type) {
                        return WebRequest.class;
                    }
                }
            }/* else {
                Type type = object.getClass().getGenericSuperclass();
                if (ParameterizedType.class.isAssignableFrom(type.getClass())) {
                    ParameterizedType parameterizedType = (ParameterizedType) type;
                    return (Class) parameterizedType.getActualTypeArguments()[0];
                }
            }*/
        } else if (object instanceof WebProcessorAdapter) {
            String className;
            Type type = null;
            do {
                if (type == null) {
                    type = object.getClass();
                } else {
                    type = ((Class) type).getGenericSuperclass();
                }
                if (type instanceof Class) {
                    className = ((Class) type).getSimpleName();
                } else {
                    break;
                }
            } while (className.contains("$$"));
//            Type type = object.getClass().getGenericSuperclass();
            if (ParameterizedType.class.isAssignableFrom(type.getClass())) {
                ParameterizedType parameterizedType = (ParameterizedType) type;
                return (Class) parameterizedType.getActualTypeArguments()[0];
            } else {
                return WebRequest.class;
            }
        } else if (object instanceof WebProcessor) {
            Type[] types = object.getClass().getGenericInterfaces();
            if (types.length > 0) {
                for (Type type : types) {
                    if (ParameterizedType.class.isAssignableFrom(type.getClass())) {
                        ParameterizedType parameterizedType = (ParameterizedType) type;
                        if (parameterizedType.getRawType() == WebProcessor.class) {
                            return (Class) parameterizedType.getActualTypeArguments()[0];
                        }
                    } else if (WebProcessor.class == type) {
                        return WebRequest.class;
                    }
                }
            }
        } else if ((object instanceof WebSyncEventListener || object.getClass().getGenericSuperclass() == WebSyncEventListener.class) ||
                (object instanceof WebAsyncEventListener || object.getClass().getGenericSuperclass() == WebAsyncEventListener.class)) {
            Type type = object.getClass().getGenericSuperclass();
            if (ParameterizedType.class.isAssignableFrom(type.getClass())) {
                ParameterizedType parameterizedType = (ParameterizedType) type;
//                if (parameterizedType.getRawType() == WebSyncEventListener.class) {
                type = parameterizedType.getActualTypeArguments()[0];
                if (type instanceof Class) {
                    return (Class) type;
                }
                parameterizedType = (ParameterizedType) type;
                return (Class) parameterizedType.getRawType();
//                }
            }
        }
        throw new RuntimeException("无法获取泛型");
    }
}
