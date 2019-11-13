package top.codings.websiphon.core.support;/*
package top.codings.websiphon.core.support;

import top.codings.websiphon.bean.WebRequest;
import top.codings.websiphon.core.context.CrawlerContext;
import top.codings.websiphon.core.context.event.all.WebAfterParseEvent;
import top.codings.websiphon.core.context.event.async.*;
import top.codings.websiphon.exception.WebException;
import top.codings.websiphon.exception.WebNetworkException;
import top.codings.websiphon.exception.WebParseException;
import top.codings.websiphon.util.Catcher;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Semaphore;

@Slf4j
@AllArgsConstructor
public class WebExecutor<IN extends WebRequest> implements Runnable {
    private IN request;
    private CrawlerContext context;
    private Semaphore token;

    @Override
    public void run() {
        long timeMillis = System.currentTimeMillis();
        Exception exception = null;
        if (!context.isRunning()) {
            return;
        }
        try {
            context.getWebHandler().get(request, context);
            Catcher.postBothEvent(new WebAfterParseEvent(request).setContext(context));
            timeMillis = System.currentTimeMillis() - timeMillis;
            context.getRateResult().addSuccess(timeMillis);
        } catch (WebNetworkException e) {
            if (!Catcher.postAsyncEvent(new WebNetworkExceptionEvent(request, context, e))) exception = e;
        } catch (WebParseException e) {
            if (!Catcher.postAsyncEvent(new WebParseExceptionEvent(request, context, e))) exception = e;
        } catch (WebException e) {
            if (!Catcher.postAsyncEvent(new WebExceptionEvent(request, context, e))) exception = e;
        } catch (Exception e) {
            if (!Catcher.postAsyncEvent(new ExceptionEvent(request, context, e))) exception = e;
        } finally {
            if (exception != null) {
                if (!Catcher.postAsyncEvent(new AllExceptionEvent(request, context, exception)) && Catcher.isLOG_ERROR()) {
                    log.error("未订阅的异常", exception);
                }
            }
            context.getRateResult().addTotal();
            token.release();
        }
    }
}
*/
