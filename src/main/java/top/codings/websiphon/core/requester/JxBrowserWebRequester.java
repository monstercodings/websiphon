package top.codings.websiphon.core.requester;/*
package top.codings.websiphon.core.requester;

import top.codings.websiphon.bean.WebRequest;
import top.codings.websiphon.bean.WebResponse;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
public class JxBrowserWebCrawler implements WebRequester<WebRequest> {
    private volatile boolean health = true;

    @Override
    public WebResponse execute(WebRequest request) {
        String url = request.getUrl();
        Map<String, String> headers = request.getHeaders();
        int timeout = request.getTimeout();
        WebResponse webResponse = new WebResponse();
        if (!isHealth()) {
            webResponse.setResult(WebResponse.Result.ILL_HEALTH);
            return webResponse;
        }
        String content;
        Browser browser = getBrowser();
        try {
            long start = System.currentTimeMillis();
            Browser.invokeAndWaitFinishLoadingMainFrame(browser, value -> value.loadURL(url), timeout / 1000);
            log.debug("加载页面耗时：{}ms", (System.currentTimeMillis() - start));
            url = browser.getURL();
            content = browser.getHTML();
        } catch (Exception e) {
            if (!(e.getCause() instanceof TimeoutException)) {
                log.error("JxBrowser请求时发生未知异常", e);
                webResponse.setResult(WebResponse.Result.FAIL);
                return webResponse;
            }
            log.error("JxBrowser请求网址超时", e.getLocalizedMessage());
            webResponse.setResult(WebResponse.Result.TIME_OUT);
            return webResponse;
        } finally {
            browser.dispose();
        }
        webResponse.setResult(WebResponse.Result.OK);
        webResponse.setContentType(WebResponse.ContentType.HTML);
        webResponse.setUrl(url);
        webResponse.setDocument(Jsoup.parse(content));
        return webResponse;
    }

    private Browser getBrowser() {
        Browser browser = new Browser(Inner.BROWSER_CONTEXT);
        BrowserPreferences preferences = browser.getPreferences();
        preferences.setImagesEnabled(false);
        preferences.setLoadsImagesAutomatically(false);
        browser.setPreferences(preferences);
        return browser;
    }

    @Override
    public boolean isHealth() {
        return health;
    }

    @Override
    public boolean close() {
        if (isHealth()) {
            synchronized (this) {
                if (isHealth()) {
                    health = false;
                }
            }
        }
        return true;
    }

    private static class Inner {
        private final static BrowserContext BROWSER_CONTEXT = new BrowserContext(new BrowserContextParams("./jxbrower-data"));
    }
}
*/
