package top.codings.websiphon.bean;

import io.netty.handler.codec.http.cookie.Cookie;
import top.codings.websiphon.core.context.event.WebAsyncEvent;
import com.alibaba.fastjson.JSON;
import lombok.Getter;
import lombok.Setter;

import java.util.*;

@Getter
@Setter
public class WebResponse {
    protected String url;
    protected JSON json;
    protected String html;
    protected byte[] bytes;
    protected String contentType;
    protected int statusCode;
    protected Map<String, String> headers = new HashMap<>();
    protected List<Cookie> cookies = new LinkedList<>();
    protected boolean redirect = false;
    protected String redirectUrl;
}
