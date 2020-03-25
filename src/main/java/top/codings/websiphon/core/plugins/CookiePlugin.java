package top.codings.websiphon.core.plugins;

import io.netty.handler.codec.http.cookie.ClientCookieEncoder;
import io.netty.handler.codec.http.cookie.Cookie;
import lombok.extern.slf4j.Slf4j;
import top.codings.websiphon.bean.MethodDesc;
import top.codings.websiphon.bean.ReturnPoint;
import top.codings.websiphon.bean.WebRequest;
import top.codings.websiphon.bean.WebResponse;
import top.codings.websiphon.core.context.CrawlerContext;
import top.codings.websiphon.core.requester.WebRequester;
import top.codings.websiphon.exception.WebException;
import top.codings.websiphon.util.HttpOperator;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class CookiePlugin implements WebPlugin {
    private Map<String, Map<String, Cookie>> hostCookies = new ConcurrentHashMap<>();

    @Override
    public Object[] before(Object[] params, Class targetClass, MethodDesc methodDesc, ReturnPoint point) throws WebException {
        if (WebRequester.class.isAssignableFrom(targetClass)) {
            WebRequest request = (WebRequest) params[0];
            HttpOperator.HttpProtocol protocol = HttpOperator.resolve(request.uri());
            Map<String, Cookie> cookies = hostCookies.get(protocol.getHost());
            if (null != cookies) {
                request.headers().put("cookie", ClientCookieEncoder.LAX.encode(cookies.values()));
            }
        } else {
            if (WebRequest.class.isAssignableFrom(params[0].getClass())) {
                WebRequest request = (WebRequest) params[0];
                HttpOperator.HttpProtocol protocol = HttpOperator.resolve(request.uri());
                Map<String, Cookie> cookies = hostCookies.get(protocol.getHost());
                if (null == cookies) {
                    synchronized (this) {
                        cookies = hostCookies.get(protocol.getHost());
                        if (null == cookies) {
                            cookies = new ConcurrentHashMap<>();
                            hostCookies.put(protocol.getHost(), cookies);
                        }
                    }
                }
                WebResponse response = request.response();
                List<Cookie> respCookies = response.getCookies();
                for (Cookie respCookie : respCookies) {
                    cookies.put(respCookie.name(), respCookie);
                }
            }
        }
        return params;
    }

    @Override
    public Object after(Object proxy, Object[] params, Object result, Class targetClass, MethodDesc methodDesc, ReturnPoint point) throws WebException {
        return result;
    }

    @Override
    public Class[] getTargetInterface() {
        return new Class[]{WebRequester.class, CrawlerContext.class};
    }

    @Override
    public MethodDesc[] getMethods() {
        return new MethodDesc[]{
                new MethodDesc("execute", new Class[]{WebRequest.class}),
                new MethodDesc("doOnFinished", new Class[]{Object.class})};
    }
}
