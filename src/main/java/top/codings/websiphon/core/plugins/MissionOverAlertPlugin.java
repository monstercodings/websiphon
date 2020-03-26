package top.codings.websiphon.core.plugins;

import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import top.codings.websiphon.bean.MethodDesc;
import top.codings.websiphon.bean.ReturnPoint;
import top.codings.websiphon.bean.WebRequest;
import top.codings.websiphon.core.context.CrawlerContext;
import top.codings.websiphon.core.context.event.WebAsyncEvent;
import top.codings.websiphon.core.parser.WebParser;
import top.codings.websiphon.core.schedule.RequestScheduler;
import top.codings.websiphon.exception.WebException;

import java.util.Set;

@Slf4j
public class MissionOverAlertPlugin<T> implements WebPlugin {
    private MissionOverHandler handler;
    private Set<WebRequest> requestHolder = Sets.newConcurrentHashSet();

    public MissionOverAlertPlugin(MissionOverHandler<T> handler) {
        this.handler = handler;
    }

    @Override
    public Object[] before(Object[] params, Class targetClass, MethodDesc methodDesc, ReturnPoint point) throws WebException {
        if (RequestScheduler.class.isAssignableFrom(targetClass)) {
            if (methodDesc.getName().equals("handle")) {
                requestHolder.add((WebRequest) params[0]);
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

}
