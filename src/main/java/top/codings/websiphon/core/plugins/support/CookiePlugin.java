package top.codings.websiphon.core.plugins.support;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import io.netty.handler.codec.http.cookie.ClientCookieDecoder;
import io.netty.handler.codec.http.cookie.ClientCookieEncoder;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.io.FileUtils;
import top.codings.websiphon.bean.MethodDesc;
import top.codings.websiphon.bean.ReturnPoint;
import top.codings.websiphon.bean.WebRequest;
import top.codings.websiphon.bean.WebResponse;
import top.codings.websiphon.core.context.CrawlerContext;
import top.codings.websiphon.core.plugins.WebPlugin;
import top.codings.websiphon.core.requester.WebRequester;
import top.codings.websiphon.exception.WebException;
import top.codings.websiphon.util.HttpOperator;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Slf4j
@NoArgsConstructor
public class CookiePlugin implements WebPlugin {
    private Map<String, Map<String, Cookie>> hostCookies = new ConcurrentHashMap<>();
    private Consumer<Map<String, Map<String, Cookie>>> initializer;
    private Consumer<Map<String, Map<String, Cookie>>> close;

    public CookiePlugin(Consumer<Map<String, Map<String, Cookie>>> initializer, Consumer<Map<String, Map<String, Cookie>>> close) {
        this.initializer = initializer;
        this.close = close;
    }

    @Override
    public void init() {
        if (initializer != null) {
            initializer.accept(hostCookies);
        }
    }

    @Override
    public void close() {
        if (close != null) {
            close.accept(hostCookies);
        }
    }

    @Override
    public Object[] before(Object[] params, Class targetClass, MethodDesc methodDesc, ReturnPoint point) throws WebException {
        if (WebRequester.class.isAssignableFrom(targetClass)) {
            WebRequest request = (WebRequest) params[0];
            if (request.headers().containsKey("cookie")) {
                Map<String, Cookie> cookies = getCookies(request);
                String str = request.headers().get("cookie");
                String[] cookieStrs = str.split(";");
                for (String cookieStr : cookieStrs) {
                    cookieStr = cookieStr.trim();
                    Cookie cookie = ClientCookieDecoder.LAX.decode(cookieStr);
                    cookies.put(cookie.name(), cookie);
                }
            } else {
                HttpOperator.HttpProtocol protocol = HttpOperator.resolve(request.uri());
                Map<String, Cookie> cookies = hostCookies.get(protocol.getHost());
                if (MapUtils.isNotEmpty(cookies)) {
                    request.headers().put("cookie", ClientCookieEncoder.LAX.encode(cookies.values()));
                }
            }
        } else {
            if (WebRequest.class.isAssignableFrom(params[0].getClass())) {
                WebRequest request = (WebRequest) params[0];
                Map<String, Cookie> cookies = getCookies(request);
                WebResponse response = request.response();
                List<Cookie> respCookies = response.getCookies();
                for (Cookie respCookie : respCookies) {
                    cookies.put(respCookie.name(), respCookie);
                }
            }
        }
        return params;
    }

    private Map<String, Cookie> getCookies(WebRequest request) {
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
        return cookies;
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

    public static class ReadFromFile {
        public static Consumer<Map<String, Map<String, Cookie>>> from(String filePath) {
            File file = new File(filePath);
            if (!file.exists()) {
                return null;
            }
            return hostCookies -> {
                try {
                    Map<String, Map<String, Cookie>> map = new HashMap<>();
                    String content = FileUtils.readFileToString(file, "utf-8");
                    JSONObject json = JSON.parseObject(content);
                    for (Map.Entry<String, Object> outE : json.entrySet()) {
                        String domain = outE.getKey();
                        JSONObject inJ = (JSONObject) outE.getValue();
                        Map<String, Cookie> cookieMap = new HashMap<>();
                        for (Map.Entry<String, Object> inE : inJ.entrySet()) {
                            String name = inE.getKey();
                            JSONObject cookieJ = (JSONObject) inE.getValue();
                            Cookie cookie = new DefaultCookie(name, cookieJ.getString("value"));
                            cookie.setDomain(cookieJ.getString("domain"));
                            cookie.setPath(cookieJ.getString("path"));
                            cookie.setHttpOnly(cookieJ.getBooleanValue("isHttpOnly"));
                            cookie.setMaxAge(cookieJ.getLongValue("maxAge"));
                            cookie.setSecure(cookieJ.getBooleanValue("isSecure"));
                            cookie.setWrap(cookieJ.getBooleanValue("wrap"));
                            cookieMap.put(name, cookie);
                        }
                        map.put(domain, cookieMap);
                    }
                    hostCookies.putAll(map);
                } catch (Exception e) {
                    log.error("初始化cookie异常", e);
                }
            };
        }
    }

    public static class WriteToFile {
        public static Consumer<Map<String, Map<String, Cookie>>> to(String filePath) {
            File file = new File(filePath);
            if (!file.exists()) {
                return null;
            }
            return hostCookies -> {
                try {
                    JSONObject json = new JSONObject();
                    for (Map.Entry<String, Map<String, Cookie>> outE : hostCookies.entrySet()) {
                        JSONObject inJ = new JSONObject();
                        for (Map.Entry<String, Cookie> inE : outE.getValue().entrySet()) {
                            JSONObject inC = new JSONObject();
                            Cookie cookie = inE.getValue();
                            inC.put("name", cookie.name());
                            inC.put("value", cookie.value());
                            inC.put("domain", cookie.domain());
                            inC.put("isHttpOnly", cookie.isHttpOnly());
                            inC.put("isSecure", cookie.isSecure());
                            inC.put("path", cookie.path());
                            inC.put("maxAge", cookie.maxAge());
                            inC.put("wrap", cookie.wrap());
                            inJ.put(inE.getKey(), inC);
                        }
                        json.put(outE.getKey(), inJ);
                    }
                    FileUtils.writeStringToFile(file, json.toString(), "utf-8");
                } catch (Exception e) {
                    log.error("持久化cookie异常", e);
                }
            };
        }
    }

}
