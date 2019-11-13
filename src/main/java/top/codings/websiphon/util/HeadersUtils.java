package top.codings.websiphon.util;

import java.util.HashMap;
import java.util.Map;

public class HeadersUtils {
    private final static Map<String, String> HEADERS = new HashMap<>();

    static {
        HEADERS.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3");
//        HEADERS.put("Accept-Encoding", "gzip, deflate, br");
        HEADERS.put("Accept-Language", "zh-CN,zh;q=0.9");
        HEADERS.put("Cache-Control", "no-cache");
        HEADERS.put("Connection", "keep-alive");
        HEADERS.put("DNT", "1");
        HEADERS.put("Upgrade-Insecure-Requests", "1");
        HEADERS.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/73.0.3683.86 Safari/537.36");
    }

    public final static Map<String, String> getHeaders() {
        return HEADERS;
    }
}
