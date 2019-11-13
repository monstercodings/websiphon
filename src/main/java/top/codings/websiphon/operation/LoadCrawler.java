package top.codings.websiphon.operation;/*
package top.codings.websiphon.operation;

import top.codings.websiphon.annotation.*;
import top.codings.websiphon.core.Crawler;
import top.codings.websiphon.core.context.CrawlerContext;
import top.codings.websiphon.core.context.WebConfiguration;
import top.codings.websiphon.core.context.WebType;
import top.codings.websiphon.core.context.event.WebSyncEventListener;
import top.codings.websiphon.core.support.CrawlerBuilder;
import com.google.common.collect.Sets;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.reflections.Reflections;
import org.reflections.scanners.FieldAnnotationsScanner;
import org.reflections.scanners.MethodAnnotationsScanner;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

@Slf4j
public abstract class LoadCrawler {
    public Map<String, JarCrawlerContext> load(String[] packages) {
        Map<String, JarCrawlerContext> jarMap = new HashMap<>(packages.length);
        for (String aPackage : packages) {
            Reflections reflections = new Reflections(new ConfigurationBuilder()
                    .setUrls(ClasspathHelper.forPackage(aPackage))
                    .setScanners(new TypeAnnotationsScanner(), new SubTypesScanner(), new FieldAnnotationsScanner(), new MethodAnnotationsScanner()));
            reflections.getTypesAnnotatedWith(Configurable.class).forEach(aClass -> {
                try {
                    Map<Class, List<Object>> current = new HashMap<>();
                    // 初始化配置类
                    WebConfiguration webConfiguration = (WebConfiguration) aClass.newInstance();
                    injectOnControl(aClass, webConfiguration);
                    current.put(aClass, Arrays.asList(webConfiguration));
                    // 初始化爬取事件
                    Map<Class<?>, WebSyncEventListener> crawlEventMap = getWebEventListener(webConfiguration, reflections.getTypesAnnotatedWith(CrawlEvent.class));
                    crawlEventMap.forEach((clazz, listener) -> {
                        List<Object> list = current.get(clazz);
                        if (list == null) {
                            list = new LinkedList<>();
                            current.put(clazz, list);
                        }
                        list.add(listener);
                    });
                    // 初始化处理器、插件
                    List<WebType> webTypes = Arrays.asList(webConfiguration.getWebType());
                    webTypes.forEach(webType -> {
                        List<Object> list = current.get(webType.getClass());
                        if (list == null) {
                            list = new LinkedList<>();
                            current.put(webType.getClass(), list);
                        }
                        list.add(webType);
                    });
                    // 爬虫依赖的组件初始化
                    reflections.getTypesAnnotatedWith(CrawlComponent.class).forEach(componentClass -> {
                        try {
                            List<Object> list = current.get(componentClass);
                            if (list == null) {
                                list = new LinkedList<>();
                                current.put(componentClass, list);
                            }
                            list.add(componentClass.newInstance());
                        } catch (InstantiationException e) {
                            e.printStackTrace();
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        }
                    });
                    // 注入各组件所需依赖
                    injectOnControl(current, reflections);
                    Set<Method> methodSet = reflections.getMethodsAnnotatedWith(ExecuteAfterInject.class);
                    // 注入结束后待执行的方法列表
                    List<ExecuteBean> executeBeans = new LinkedList<>();
                    methodSet.forEach(method -> Optional.ofNullable(current.get(method.getDeclaringClass())).ifPresent(targets -> targets.forEach(target -> executeBeans.add(new ExecuteBean(target, method, method.getAnnotation(ExecuteAfterInject.class).value())))));
                    executeBeans.sort(Comparator.naturalOrder());
                    executeBeans.forEach(executeBean -> {
                        try {
                            if (!Modifier.isPublic(executeBean.method.getModifiers())) {
                                executeBean.method.setAccessible(true);
                            }
                            executeBean.method.invoke(executeBean.object);
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        } catch (InvocationTargetException e) {
                            e.printStackTrace();
                        }
                    });
                    Crawler crawler = CrawlerBuilder.create().setThreadSize(webConfiguration.getThreadSize()).addLast(webTypes).build();
                    CrawlerContext context = crawler.getContext();
                    crawlEventMap.forEach((clazz, listener) -> {
                        if (clazz.getAnnotation(CrawlEvent.class).async()) {
                            context.registerAsyncEvent(listener);
                        } else {
                            context.registerSyncEvent(listener);
                        }
                    });
                    context.setId(webConfiguration.getId());
                    JarCrawlerContext jarCrawlerContext = new JarCrawlerContext();
                    jarCrawlerContext.setCrawler(crawler);
                    jarCrawlerContext.setStatisticResult(webConfiguration.getStatisticResult());
                    jarCrawlerContext.setId(webConfiguration.getId());
                    jarMap.put(crawler.getId(), jarCrawlerContext);
                } catch (InstantiationException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            });
        }
        return jarMap;

    }

    private Map<Class<?>, WebSyncEventListener> getWebEventListener(WebConfiguration webConfiguration, Set<Class<?>> classListeners) {
        Map<Class<?>, WebSyncEventListener> map = new HashMap<>();
        classListeners.forEach(classListener -> {
            CrawlEvent crawlEvent = classListener.getAnnotation(CrawlEvent.class);
            Class[] classes = crawlEvent.value();
            boolean goNext = false;
            if (classes.length == 0) {
                goNext = true;
            } else {
                for (Class aClass : classes) {
                    if (aClass == webConfiguration.getClass()) {
                        goNext = true;
                        break;
                    }
                }
            }
            if (!goNext) {
                return;
            }
            try {
                WebSyncEventListener webEventListener = (WebSyncEventListener) classListener.newInstance();
                map.put(classListener, webEventListener);
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        });
        return map;
    }


    @Data
    @AllArgsConstructor
    private class ExecuteBean implements Comparable<ExecuteBean> {
        Object object;
        Method method;
        int order;

        @Override
        public int compareTo(ExecuteBean o) {
            return order >= o.order ? 1 : -1;
        }
    }

    */
/*public void getSuperclass(Class clazz, Set<Type> classes) {
        if (clazz == null || Object.class == clazz) {
            return;
        }
        if (!classes.add(clazz)) {
            return;
        }
        Type[] types = clazz.getGenericInterfaces();
        for (Type type : types) {
            if (type instanceof ParameterizedType) {
                getInterface((Class) ((ParameterizedType) type).getRawType(), classes);
            } else {
                if (((Class) type).isInterface()) {
                    getInterface((Class) type, classes);
                }
                if (classes.add(type)) {
                    getInterface((Class) type, classes);
                }
            }
        }
        getSuperclass((Class) clazz.getGenericSuperclass(), classes);
    }

    private void getInterface(Class clazz, Set<Type> classes) {
        Type[] types = clazz.getGenericInterfaces();
        for (Type type : types) {
            Class typeClass = (Class) type;
            if (classes.add(typeClass)) {
                getInterface(typeClass, classes);
            }
        }
    }*//*


    private void injectOnControl(Map<Class, List<Object>> current, Reflections reflections) {
        reflections.getFieldsAnnotatedWith(AutowiredBySelf.class).forEach(field ->
                Optional.ofNullable(current.get(field.getDeclaringClass())).ifPresent(targets ->
                        Optional.ofNullable(current.get(field.getType())).ifPresent(values -> {
                            if (values.size() == 1) {
                                targets.forEach(target -> setField(field, target, values.get(0)));
                                return;
                            }
                            throw new RuntimeException("可注入的值有多个，无法确定需要注入哪个");
//                            String name = field.getAnnotation(AutowiredBySelf.class).value();
                        })));
        reflections.getFieldsAnnotatedWith(AutowiredByFrame.class).forEach(field ->
                Optional.ofNullable(current.get(field.getDeclaringClass())).ifPresent(targets -> {
                    try {
                        Optional.ofNullable(getDepend(field.getType(), field.getAnnotation(AutowiredByFrame.class).value())).ifPresent(value -> targets.forEach(target -> setField(field, target, value)));
                    } catch (Exception e) {
                        log.error("从平台获取依赖对象时发生异常", e);
                    }
                }));
    }

    private void setField(Field field, Object target, Object value) {
        if (!Modifier.isPublic(field.getModifiers())) {
            field.setAccessible(true);
        }
        try {
            field.set(target, value);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    private void injectOnControl(Class clazz, Object object) {
        if (clazz == null || object == null) {
            return;
        }
        Field[] fields = getFields(clazz, null);
        for (Field field : fields) {
            AutowiredByFrame autowired = field.getAnnotation(AutowiredByFrame.class);
            if (null == autowired) {
                continue;
            }
            Object depend;
            try {
                depend = getDepend(field.getType(), autowired.value());
            } catch (Exception e) {
                log.error("从平台获取依赖对象时发生异常", e);
                continue;
            }
            field.setAccessible(true);
            try {
                field.set(object, depend);
            } catch (IllegalAccessException e) {
                log.error("注入依赖对象发生异常", e);
            }
        }
    }

    private Field[] getFields(Class clazz, Set<Field> fields) {
        if (fields == null) {
            fields = new HashSet<>();
            fields.addAll(Arrays.asList(clazz.getDeclaredFields()));
        }
        fields.addAll(Sets.newHashSet(clazz.getFields()));
        Class parentClass = clazz.getSuperclass();
        if (null == parentClass) {
            return fields.toArray(new Field[0]);
        } else {
            return getFields(parentClass, fields);
        }
    }

    public abstract Object getDepend(Class clazz, String alias);
}
*/
