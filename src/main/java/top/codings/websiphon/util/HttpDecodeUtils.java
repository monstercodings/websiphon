package top.codings.websiphon.util;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

public class HttpDecodeUtils {
    public static String convert(Connection.Response response, String charset) {
        String content = response.body();
        if (StringUtils.isBlank(charset)) {
            return content;
        }
        try {
            content = new String(response.bodyAsBytes(), charset);
        } catch (UnsupportedEncodingException e) {
        }
        return content;
    }

    public static String findEncoding(Document document) {
        Elements metas = document.select("meta");
        for (Element meta : metas) {
            String charset = meta.attr("charset");
            if (StringUtils.isNotBlank(charset)) {
                return charset.toLowerCase();
            }
            charset = meta.attr("content");
            if (StringUtils.isNotBlank(charset)) {
                if (charset.toLowerCase().contains("utf-8")) {
                    return "utf-8";
                } else if (charset.toLowerCase().contains("gb2312")) {
                    return "gb2312";
                } else if (charset.toLowerCase().contains("gbk")) {
                    return "gbk";
                }
            }
        }
        return Charset.defaultCharset().name();
    }

    public static String findCharset(byte[] bytes) {
        return findCharset(new String(bytes));
    }

    public static String findCharset(String text) {
        try {
            Document document = Jsoup.parse(text);
            Elements metas = document.select("meta");
            for (Element meta : metas) {
                String encoding = meta.attr("charset");
                if (StringUtils.isNotBlank(encoding)) {
                    return encoding;
                }
                String content = meta.attr("content");
                if (StringUtils.isNotBlank(content) && content.contains("charset")) {
                    return content.substring(content.indexOf("charset=") + "charset=".length());
                }
            }
        } catch (Exception e) {
            // TODO Jsoup解析失败
        }
        return Charset.defaultCharset().name();
    }
}
