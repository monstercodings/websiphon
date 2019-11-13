package top.codings.websiphon.core.requester;/*
package top.codings.websiphon.core.requester;

import top.codings.websiphon.bean.WebRequest;
import top.codings.websiphon.bean.WebResponse;
import com.google.common.net.MediaType;
import io.webfolder.cdp.Launcher;
import io.webfolder.cdp.command.Network;
import io.webfolder.cdp.exception.LoadTimeoutException;
import io.webfolder.cdp.session.Session;
import io.webfolder.cdp.session.SessionFactory;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

@Slf4j
public class ChromeWebRequester<W extends WebRequest> implements WebRequester<W> {
    private static ConcurrentLinkedQueue<Session> queue = new ConcurrentLinkedQueue<>();

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
        Map<String, Object> objHeaders = new HashMap<>();
        objHeaders.putAll(headers);
        String content;
        Session session = Inner.getSession();
        try {
            long start = System.currentTimeMillis();
            session.navigate(url);
            Network network = session.getCommand().getNetwork();
            network.enable();
            network.setExtraHTTPHeaders(objHeaders);
            session.waitDocumentReady(timeout);
            content = session.getContent();
            url = session.getLocation();
            log.debug("加载页面耗时：{}ms", (System.currentTimeMillis() - start));
        } catch (NullPointerException e) {
            log.error("session打开URL[{}]出错 >>>>> {}", url, e.getMessage());
            webResponse.setResult(WebResponse.Result.FAIL);
            return webResponse;
        } catch (LoadTimeoutException e) {
            webResponse.setResult(WebResponse.Result.TIME_OUT);
            return webResponse;
        } catch (Exception e) {
            log.error("动态爬取发生未知异常，URL >>>>> {}", url, e);
//            siteCrawlFailUrlClient.addFailUrl(CrawlFailUtils.instance(websiteEntry, false, CrawlFailEnum.CDP4J_ERROR_SESSION, e));
            webResponse.setResult(WebResponse.Result.FAIL);
            return webResponse;
        } finally {
            webResponse.setUrl(url);
            queue.offer(session);
            if (!isHealth()) close();
        }
        webResponse.setResult(WebResponse.Result.OK);
        webResponse.setDocument(Jsoup.parse(content, url));
        webResponse.setContentType(MediaType.HTML_UTF_8);
        return webResponse;
    }

    @Override
    public boolean isHealth() {
        return Inner.health;
    }

    @Override
    public synchronized boolean close() {
        Inner.health = false;
        Inner.close();
        return true;
    }

    private static class Inner {
        private static Launcher launcher;
        private static SessionFactory factory;
        private volatile static boolean health = true;

        static {
            try {
                launcher = new Launcher();
                factory = launcher.launch(Collections.singletonList("--headless"));
            } catch (Exception e) {
                log.error("初始化Chrome内核失败");
                health = false;
            }
        }

        public static Session getSession() {
            Session session = queue.poll();
            if (session == null) {
                session = factory.create();
            }
            return session;
        }

        public static void close() {
            while (true) {
                Session session = queue.poll();
                if (null == session) {
                    break;
                }
                factory.close(session);
            }
            factory.close();
            launcher.kill();
        }
    }
}
*/
