package top.codings.websiphon.util;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HttpOperator {
    private static Pattern pattern = Pattern.compile("(\\w+)://([^/:]+)(:\\d*)?([^# ]*)");

    /**
     * 重组链接
     *
     * @param href
     * @param realUrl
     * @return
     */
    public static String recombineLink(String href, String realUrl) {
        if (StringUtils.isBlank(href)) {
            return "";
        }
        href = href.trim();
        String nowDomain = realUrl;
        if (nowDomain.startsWith("http")) {
            nowDomain = nowDomain.substring(nowDomain.indexOf("//") + 2);
        }
        if (nowDomain.contains("/")) {
            nowDomain = nowDomain.substring(0, nowDomain.indexOf("/"));
        }
        if (StringUtils.isBlank(href) || href.startsWith("javascript")) {
            return null;
        }
        if (href.startsWith("//")) {
            href = realUrl.substring(0, realUrl.indexOf("//")) + href;
        } else if (href.startsWith("/")) {
            href = realUrl.substring(0, realUrl.indexOf("//")) + "//" + nowDomain + href;
        } else if (href.startsWith("http")) {
        } else if (href.startsWith("../")) {
            int loc = href.indexOf("../");
            while (loc >= 0) {
                href = href.substring(3);
                loc = href.indexOf("../");
                if (realUrl.endsWith("/")) {
                    realUrl = realUrl.substring(0, realUrl.length() - 1);
                }
                realUrl = realUrl.substring(0, realUrl.lastIndexOf("/") < 0 ? realUrl.length() : realUrl.lastIndexOf("/"));
            }
            href = realUrl + "/" + href;
        } else if (href.startsWith("./")) {
            if (realUrl.endsWith("/")) {
                realUrl = realUrl.substring(0, realUrl.length() - 1);
                href = realUrl + href.substring(1);
            } else {
                href = realUrl + href.substring(1);
            }
//            href = realUrl.substring(0, realUrl.indexOf("//")) + "//" + nowDomain + href.substring(1);
        } else if (href.startsWith(nowDomain)) {
            href = realUrl.substring(0, realUrl.indexOf(":") + 1) + "//" + href;
        } else {
            href = realUrl.substring(0, realUrl.lastIndexOf("/") + 1) + href;
        }
        return href;
    }

    public static String extractDomain(String url) {
        if (url.startsWith("http")) {
            url = url.substring(url.indexOf("//") + 2);
        }
        if (url.contains("/")) {
            url = url.substring(0, url.indexOf("/"));
        }
        return url;
    }

    public static HttpProtocol resolve(String url) throws IllegalArgumentException {
        if (StringUtils.isBlank(url)) {
            throw new IllegalArgumentException("URL不能为空");
        }
        Matcher matcher = pattern.matcher(url);
        if (!matcher.find()) {
            throw new IllegalArgumentException(String.format("URL不正确 %s", url));
        }
        String scheme = matcher.group(1);
        String domain = matcher.group(2);
        int port = -1;
        if (StringUtils.isNotBlank(matcher.group(3)) && matcher.group(3).length() > 1) {
            port = Integer.parseInt(matcher.group(3).substring(1));
        }
        if (port == -1) {
            switch (scheme) {
                case "http":
                    port = 80;
                    break;
                default:
                    port = 443;
            }
        }
        String path = matcher.group(4);
        if (StringUtils.isBlank(path)) {
            path = "/";
        }
        return new HttpProtocol(scheme, path, domain, port);
    }

    /**
     * URL合法性校验
     *
     * @param url
     * @return
     */
    public static boolean urlLegalVerify(String url) {
        return pattern.matcher(url).find();
    }

    @Data
    @AllArgsConstructor
    public static class HttpProtocol {
        String scheme;
        String path;
        String host;
        int port;
    }
}
