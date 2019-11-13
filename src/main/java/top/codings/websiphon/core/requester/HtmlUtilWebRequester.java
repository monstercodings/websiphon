package top.codings.websiphon.core.requester;/*
package top.codings.websiphon.core.requester;

import top.codings.websiphon.bean.WebRequest;
import top.codings.websiphon.bean.WebResponse;
import com.alibaba.fastjson.JSON;
import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.NicelyResynchronizingAjaxController;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.google.common.net.MediaType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;

import java.util.Map;

@Slf4j
public class HtmlUtilWebRequester<W extends WebRequest> implements WebRequester<W> {
    private volatile boolean health = true;

    private WebClient getWebClient(Map<String, String> headers) {
        WebClient webClient = new WebClient(BrowserVersion.CHROME);
        webClient.getOptions().setThrowExceptionOnScriptError(false);
        webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
        webClient.getOptions().setActiveXNative(false);
        webClient.getOptions().setCssEnabled(false);
        webClient.setAjaxController(new NicelyResynchronizingAjaxController());
        headers.forEach((key, value) -> webClient.addRequestHeader(key, value));
        return webClient;
    }

    @Override
    public WebResponse execute(W request) {
        String url = request.getUrl();
        Map<String, String> headers = request.getHeaders();
        int timeout = request.getTimeout();
        WebResponse webResponse = new WebResponse();
        if (!isHealth()) {
            webResponse.setResult(WebResponse.Result.ILL_HEALTH);
            return webResponse;
        }
        HtmlPage page;
        String content;
        WebClient webClient = getWebClient(headers);
        webClient.getOptions().setTimeout(timeout);
        try {
            long start = System.currentTimeMillis();
            page = webClient.getPage(url);
            log.debug("加载页面耗时：{}ms", (System.currentTimeMillis() - start));
            url = page.getUrl().toString();
            content = page.asXml();
        } catch (Exception e) {
            log.error("HtmlUtil请求时发生异常", e.getLocalizedMessage());
            webResponse.setResult(WebResponse.Result.FAIL);
            return webResponse;
        } finally {
            webClient.close();
        }
        MediaType contentType = getContentType(page);
        if (null == contentType) {
            webResponse.setResult(WebResponse.Result.CONTENT_TYPE_ERROR);
        } else if (contentType == MediaType.HTML_UTF_8) {
            webResponse.setDocument(Jsoup.parse(content, url));
        } else if (contentType == MediaType.JSON_UTF_8) {
            webResponse.setJson(JSON.parseObject(content));
        } else {
            webResponse.setResult(WebResponse.Result.CONTENT_TYPE_ERROR);
        }
        webResponse.setContentType(contentType);
        webResponse.setUrl(url);
        return webResponse;
    }

    private MediaType getContentType(HtmlPage page) {
        MediaType contentType;
        String contentType = page.getContentType();
        if (StringUtils.isBlank(contentType)) {
            return MediaType.HTML_UTF_8;
        }
        if (!contentType.contains("/")) {
            contentType = contentType + "/*";
        }
        if (contentType.indexOf(";") > 0) {
            contentType = contentType.substring(0, contentType.indexOf(";"));
        }
        if (contentType.contains("text") || contentType.contains("application")) {
            contentType = contentType + "; charset=utf-8";
        }
        try {
            contentType = MediaType.parse(contentType);
        } catch (Exception e) {
            log.error("无法解析的MediaType：{}", page.getContentType(), e);
            contentType = null;
        }
        return contentType;
    }

    @Override
    public boolean isHealth() {
        return health;
    }

    @Override
    public void close() throws Exception{
        if (isHealth()) {
            synchronized (this) {
                if (isHealth()) {
                    health = false;
                }
            }
        }
    }
}
*/
