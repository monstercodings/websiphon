package top.codings.websiphon.core.plugins.support;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Sets;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import top.codings.websiphon.bean.BasicWebRequest;
import top.codings.websiphon.bean.WebRequest;
import top.codings.websiphon.core.parser.WebParser;
import top.codings.websiphon.core.plugins.AspectInfo;
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
import java.util.concurrent.TimeoutException;

/**
 * 爬虫无任务时通知插件
 * 必须在URL过滤插件 UrlFilterPlugin 之前添加
 *
 * @param <T>
 */
@Slf4j
public class MissionOverAlertPlugin<T extends WebRequest> implements WebPlugin {
    private long expired;
    private MissionOverHandler handler;
    @Getter
    private Set<T> requestHolder;
    private boolean monitor;
    private ExecutorService exe;
    private Map<String, AspectInfo> aspectInfoMap = new HashMap<>();

    public MissionOverAlertPlugin(MissionOverHandler<T> handler) {
        this(handler, false, 300 * 1000);
    }

    public MissionOverAlertPlugin(MissionOverHandler handler, boolean monitor, long expired) {
        this.handler = handler;
        this.monitor = monitor;
        this.expired = expired;
        try {
            aspectInfoMap.put("handle", new AspectInfo(RequestScheduler.class, RequestScheduler.class.getMethod("handle", WebRequest.class)));
            aspectInfoMap.put("release", new AspectInfo(RequestScheduler.class, RequestScheduler.class.getMethod("release", WebRequest.class)));
            aspectInfoMap.put("parse", new AspectInfo(WebParser.class, WebParser.class.getMethod("parse", WebRequest.class)));
        } catch (NoSuchMethodException e) {

        }
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
    public void onBefore(AspectInfo aspectInfo, Object[] args) throws WebException {
        if (aspectInfoMap.get("handle") == aspectInfo) {
            requestHolder.add((T) args[0]);
        } else if (aspectInfoMap.get("release") == aspectInfo) {
            WebRequest request = (WebRequest) args[0];
            if (request.status() != WebRequest.Status.SUCCEED) {
                requestHolder.remove(args[0]);
                checkLast((T) args[0]);
            }
        }
    }

    @Override
    public Object onAfterReturning(AspectInfo aspectInfo, Object[] args, Object returnValue) {
        WebRequest request = (WebRequest) args[0];
        return returnValue;
    }

    @Override
    public void onAfterThrowing(AspectInfo aspectInfo, Object[] args, Throwable throwable) {

    }

    @Override
    public void onFinal(AspectInfo aspectInfo, Object[] args, Throwable throwable) {
        if (aspectInfoMap.get("parse") == aspectInfo) {
            requestHolder.remove(args[0]);
            checkLast((T) args[0]);
        }
    }

    @Override
    public AspectInfo[] aspectInfos() {
        return aspectInfoMap.values().toArray(new AspectInfo[0]);
    }

    @Override
    public int index() {
        return 8000;
    }

    @Override
    public void init() {
        requestHolder = Sets.newConcurrentHashSet();
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
                            if (webRequest.status() == WebRequest.Status.DOING) {
                                iterator.remove();
                                webRequest.failed(new TimeoutException("超时未完成请求"));
                            }
//                            checkLast(webRequest);
                        }
                    }
                    log.debug("监控结果\n{}", JSON.toJSONString(result, true));
                    result.clear();
                    TimeUnit.MILLISECONDS.sleep(expired);
                } catch (InterruptedException e) {
                    return;
                } catch (Exception e) {
                    log.error("监督队列情况出现异常", e);
                }
            }
        });
    }
}
