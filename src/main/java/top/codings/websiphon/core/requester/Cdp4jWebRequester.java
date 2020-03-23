package top.codings.websiphon.core.requester;

import top.codings.websiphon.bean.WebRequest;
import top.codings.websiphon.bean.WebResponse;
import top.codings.websiphon.core.context.CrawlerContext;
import top.codings.websiphon.core.context.event.async.WebNetworkExceptionEvent;
import top.codings.websiphon.util.HttpDecodeUtils;
import com.alibaba.fastjson.JSON;
import io.webfolder.cdp.Launcher;
import io.webfolder.cdp.session.Session;
import io.webfolder.cdp.session.SessionFactory;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class Cdp4jWebRequester<W extends WebRequest> implements WebRequester<W> {
    @Getter
    private boolean health = false;
    private boolean headless;
    private String chromePath;
    private Launcher launcher;
    private SessionFactory sessionFactory;
    private LinkedTransferQueue<Session> queue = new LinkedTransferQueue<>();
    private ExecutorService executorService = Executors.newCachedThreadPool();
    private AtomicInteger size = new AtomicInteger(0);

    public Cdp4jWebRequester() {
        this(null, true);
    }

    public Cdp4jWebRequester(boolean headless) {
        this(null, headless);
    }

    public Cdp4jWebRequester(String chromePath, boolean headless) {
        this.chromePath = chromePath;
        this.headless = headless;
    }

    @Override
    public void init() throws Exception {
        try {
            String path = "./chrome_data";
//            DelFileUtils.delFolder(path);
            launcher = new Launcher();
            List<String> params = Arrays.asList(
//                    "--incognito",
//                    "–-in-process-plugins",
                    "-–disable-images",
                    "--ignore-certificate-errors",
                    "--disable-gpu",
                    "--user-data-dir=" + path,
                    "-–single-process"
            );
            if (headless) {
                List<String> temp = new ArrayList<>(params.size() + 1);
                temp.addAll(params);
                temp.add("--headless");
                params = temp;
            }
            sessionFactory = launcher.launch(StringUtils.isBlank(chromePath) ? launcher.findChrome() : chromePath, params);
        } catch (Exception e) {
            log.error("初始化Chrome内核失败", e);
            return;
        }
        health = true;
        log.info("初始化Chrome内核完成");
    }

    @Override
    public void execute(W request) {
        CrawlerContext crawlerContext = request.context();
        executorService.submit(() -> {
            Session session = queue.poll();
            if (session == null) {
                synchronized (this) {
                    log.debug("打开新标签页");
                    session = sessionFactory.create();
                }
            }
            size.getAndIncrement();
            Throwable throwable = null;
            String content;
            try {
                session.navigate(request.getUrl()).waitDocumentReady(request.getTimeout());
            } catch (Exception e) {
                throwable = e;
                session.stop();
            } finally {
                try {
                    synchronized (this) {
                        session.activate();
                        content = session.getContent();
                    }
                } finally {
                    queue.offer(session);
                    size.getAndDecrement();
                }
            }

            request.setResponse(Optional.ofNullable(request.getResponse()).orElse(new WebResponse()));
            WebResponse response = request.getResponse();
            response.setUrl(request.getUrl());
            response.setResult(WebResponse.Result.OK);
            response.setStatusCode(200);
            if (StringUtils.isBlank(content) && throwable != null) {
                WebNetworkExceptionEvent event = new WebNetworkExceptionEvent();
                event.setThrowable(throwable);
//                event.setContext(crawlerContext);
                event.setRequest(request);
                response.setErrorEvent(event);
                crawlerContext.doOnFinished(request);
                return;
            }
            String encoding = HttpDecodeUtils.findCharset(content);
            response.setHtml(content);
            try {
                response.setBytes(content.getBytes(encoding));
            } catch (UnsupportedEncodingException e) {
                response.setBytes(content.getBytes());
            }
            if ((content.startsWith("{") && content.endsWith("}")) || (content.startsWith("[") && content.endsWith("]"))) {
                response.setContentType("application/json");
                response.setJson((JSON) JSON.parse(content));
            } else {
                response.setContentType("text/html");
            }
            crawlerContext.doOnFinished(request);
        });
    }

    @Override
    public int size() {
        return size.get();
    }

    @Override
    public void close() {
        if (sessionFactory != null) {
            sessionFactory.close();
        }
        if (launcher != null) {
            launcher.kill();
        }
        executorService.shutdownNow();
    }
}
