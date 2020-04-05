package top.codings.websiphon.core.plugins.support;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Sets;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import top.codings.websiphon.bean.BasicWebRequest;
import top.codings.websiphon.bean.MethodDesc;
import top.codings.websiphon.bean.ReturnPoint;
import top.codings.websiphon.bean.WebRequest;
import top.codings.websiphon.core.parser.WebParser;
import top.codings.websiphon.core.plugins.WebPlugin;
import top.codings.websiphon.core.schedule.RequestScheduler;
import top.codings.websiphon.exception.WebException;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 爬虫无任务时通知插件
 * 必须在URL过滤插件之前添加
 *
 * @param <T>
 */
@Slf4j
public class MissionOverAlertPlugin<T extends WebRequest> implements WebPlugin {
    private long expired = 300 * 1000;
    private MissionOverHandler handler;
    @Getter
    private Set<T> requestHolder = Sets.newConcurrentHashSet();
    private boolean monitor = false;
    private ExecutorService exe;

    public MissionOverAlertPlugin(MissionOverHandler<T> handler) {
        this.handler = handler;
    }

    public MissionOverAlertPlugin(MissionOverHandler handler, boolean monitor) {
        this.handler = handler;
        this.monitor = monitor;
    }

    @Override
    public Object[] before(Object[] params, Class targetClass, MethodDesc methodDesc, ReturnPoint point) throws WebException {
        if (RequestScheduler.class.isAssignableFrom(targetClass)) {
            if (methodDesc.getName().equals("handle")) {
                requestHolder.add((T) params[0]);
            } else if (methodDesc.getName().equals("release")) {
                WebRequest request = (WebRequest) params[0];
                if (request.response().getResult() == null) {
                    requestHolder.remove(params[0]);
                    checkLast((T) params[0]);
                }
            }
        }
        return params;
    }

    @Override
    public Object after(Object proxy, Object[] params, Object result, Class targetClass, MethodDesc methodDesc, ReturnPoint point) throws WebException {
        if (WebParser.class.isAssignableFrom(targetClass)) {
            requestHolder.remove(params[0]);
            checkLast((T) params[0]);
        }
        return result;
    }

    @Override
    public Class[] getTargetInterface() {
        return new Class[]{
                RequestScheduler.class,
                WebParser.class,
        };
    }

    @Override
    public MethodDesc[] getMethods() {
        return new MethodDesc[]{
                new MethodDesc("handle", new Class[]{WebRequest.class}),
                new MethodDesc("release", new Class[]{WebRequest.class}),
                new MethodDesc("parse", new Class[]{WebRequest.class}),
        };
    }

    private void checkLast(T request) {
        if (requestHolder.isEmpty()) {
            handler.handle(request);
        }
    }

    public interface MissionOverHandler<T> {
        void handle(T request);
    }

    @Override
    public void init() {
        if (!monitor) {
            return;
        }
        exe = Executors.newSingleThreadExecutor();
        exe.submit(() -> {
            Map<String, Integer> result = new HashMap<>();
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Iterator<T> iterator = requestHolder.iterator();
                    while (iterator.hasNext()) {
                        T webRequest = iterator.next();
                        if ((System.currentTimeMillis() - ((BasicWebRequest) webRequest).getBeginAt()) > expired) {
//                            log.warn("[{}]请求对象超时未完成 -> {}", webRequest.status(), webRequest);
                            Integer count = result.get(webRequest.status().name());
                            if (null == count) {
                                result.put(webRequest.status().name(), 1);
                            } else {
                                result.put(webRequest.status().name(), count + 1);
                            }
//                            iterator.remove();
//                            checkLast(webRequest);
                        }
                    }
                    log.info("监控结果\n{}", JSON.toJSONString(result, true));
                    result.clear();
                    TimeUnit.MINUTES.sleep(5);
                } catch (InterruptedException e) {
                    return;
                } catch (Exception e) {
                    log.error("监督队列情况出现异常", e);
                }
            }
        });
    }
}
