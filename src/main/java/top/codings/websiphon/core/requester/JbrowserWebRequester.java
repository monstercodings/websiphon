package top.codings.websiphon.core.requester;/*
package top.codings.websiphon.core.requester;

import top.codings.websiphon.bean.WebRequest;
import top.codings.websiphon.bean.WebResponse;
import com.google.common.net.MediaType;
import com.machinepublishers.jbrowserdriver.JBrowserDriver;
import com.machinepublishers.jbrowserdriver.RequestHeaders;
import com.machinepublishers.jbrowserdriver.Settings;
import com.machinepublishers.jbrowserdriver.Timezone;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

@Slf4j
public class JbrowserWebRequester<W extends WebRequest> implements WebRequester<W> {
    private volatile boolean health = true;
    private static ConcurrentLinkedQueue<JBrowserDriver> queue = new ConcurrentLinkedQueue<>();

    @Override
    public WebResponse execute(W request) {
        JBrowserDriver driver = null;
        WebResponse webResponse = new WebResponse();
        try {
            driver = queue.poll();
            if (driver == null) {
                driver = new JBrowserDriver(
                        Settings.builder()
                                .requestHeaders(RequestHeaders.CHROME)
                                .timezone(Timezone.ASIA_SHANGHAI)
                                .headless(true)
                                .cache(false)
                                .loggerLevel(Level.SEVERE)
                                .quickRender(true)
                                .ajaxWait(5000)
                                .build()
                );
            }
            driver.get(request.getUrl());
            int status = driver.getStatusCode();
            webResponse.setStatusCode(status);
            WebResponse.Result result = WebResponse.Result.valueOf(status);
            webResponse.setResult(result);
            webResponse.setUrl(driver.getCurrentUrl());
            webResponse.setContentType(MediaType.HTML_UTF_8);
            if (result == WebResponse.Result.OK) {
                String content = null;
                while (content == null) {
                    try {
                        content = driver.getPageSource();
                    } catch (Exception e) {
                        try {
                            TimeUnit.SECONDS.sleep(1);
                        } catch (InterruptedException e1) {
                            content = driver.getPageSource();
                        }
                    }
                }
                webResponse.setDocument(Jsoup.parse(content, request.getUrl()));
            }
        } finally {
            if (null != driver) queue.offer(driver);
            if (!health) closing();
        }
        return webResponse;
    }

    @Override
    public boolean isHealth() {
        return health;
    }

    @Override
    public synchronized boolean close() {
        health = false;
        closing();
        return true;
    }

    private synchronized void closing() {
        JBrowserDriver driver = queue.poll();
        while (driver != null) {
            driver.quit();
            driver = queue.poll();
        }
    }
}
*/
