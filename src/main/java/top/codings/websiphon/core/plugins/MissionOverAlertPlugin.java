package top.codings.websiphon.core.plugins;

import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import top.codings.websiphon.bean.MethodDesc;
import top.codings.websiphon.bean.ReturnPoint;
import top.codings.websiphon.bean.WebRequest;
import top.codings.websiphon.core.schedule.RequestScheduler;
import top.codings.websiphon.exception.WebException;

import java.util.Set;

@Slf4j
public class MissionOverAlertPlugin implements WebPlugin {
    private Set<WebRequest> requestHolder = Sets.newConcurrentHashSet();

    @Override
    public Object[] before(Object[] params, Class targetClass, MethodDesc methodDesc, ReturnPoint point) throws WebException {
        log.debug("任务完成通知插件");
        requestHolder.add((WebRequest) params[0]);
        return params;
    }

    @Override
    public Object after(Object proxy, Object[] params, Object result, Class targetClass, MethodDesc methodDesc, ReturnPoint point) throws WebException {
        return result;
    }

    @Override
    public Class[] getTargetInterface() {
        return new Class[]{
                RequestScheduler.class
        };
    }

    @Override
    public MethodDesc[] getMethods() {
        return new MethodDesc[]{
                new MethodDesc("handle", new Class[]{WebRequest.class})
        };
    }
}
