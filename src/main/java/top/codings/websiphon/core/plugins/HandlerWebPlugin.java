package top.codings.websiphon.core.plugins;/*
package top.codings.websiphon.core.plugins;

import top.codings.websiphon.bean.MethodDesc;
import top.codings.websiphon.bean.ReturnPoint;
import top.codings.websiphon.bean.WebRequest;
import top.codings.websiphon.bean.WebResponse;
import top.codings.websiphon.core.context.CrawlerContext;
import top.codings.websiphon.core.context.event.sync.WebBeforeParseEvent;
import top.codings.websiphon.core.context.event.sync.WebBeforeRequestEvent;
import top.codings.websiphon.core.requester.WebRequester;
import top.codings.websiphon.exception.WebException;
import top.codings.websiphon.exception.WebNetworkException;
import top.codings.websiphon.factory.WebFactory;
import top.codings.websiphon.factory.bean.WebHandler;
import top.codings.websiphon.factory.support.BasicWebFactory;
import top.codings.websiphon.util.Catcher;
import top.codings.websiphon.util.HeadersUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class HandlerWebPlugin<IN extends WebRequest> implements WebPlugin {
    private final int defaultTimeout = 30 * 1000;
    private final Map<String, String> defaultHeaders = HeadersUtils.getHeaders();
    private BasicWebFactory<IN> webFactory;

    @Override
    public Object[] before(Object[] params, ReturnPoint point) throws WebException {
        return params;
    }

    @Override
    public Object after(Object proxy, Object[] params, Object result, MethodDesc methodDesc, ReturnPoint point) throws WebException {
        IN param = (IN) params[0];
        CrawlerContext context = (CrawlerContext) params[1];
        Catcher.postSyncEvent(new WebBeforeRequestEvent<>(param).setContext(context));
        WebRequester requester = null;
        for (Map.Entry<Class, WebRequester> entry : webFactory.getRequesterMap().entrySet()) {
            if (entry.getKey().isAssignableFrom(param.getClass())) {
                requester = entry.getValue();
                break;
            }
        }
        if (null == requester) {
            requester = webFactory.getRequesterMap().get(WebRequest.class);
            if (null == requester) {
                return null;
            }
        }
        WebResponse response = requester.execute(param);
        if (response.getResult() != WebResponse.Result.OK) {
            throw new WebNetworkException(String.format("请求URL[%s]失败 >>>>>> %s", param.getUrl(), response.getResult().getValue()), response.getResult());
        }
        param.setResponse(response);
        Catcher.postSyncEvent(new WebBeforeParseEvent(param).setContext(context));
        webFactory.getWebParser().parse(param, context);
        return null;
    }

    @Override
    public Class[] getTargetInterface() {
        return new Class[]{WebHandler.class};
    }

    @Override
    public MethodDesc[] getMethods() {
        return new MethodDesc[]{
                new MethodDesc("get", new Class[]{WebRequest.class, CrawlerContext.class})
        };
    }

    @Override
    public void setWebFactory(WebFactory webFactory) {
        this.webFactory = (BasicWebFactory<IN>) webFactory;
    }
}
*/
