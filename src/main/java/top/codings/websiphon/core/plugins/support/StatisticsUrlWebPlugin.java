package top.codings.websiphon.core.plugins.support;

import lombok.AllArgsConstructor;
import lombok.Data;
import top.codings.websiphon.bean.*;
import top.codings.websiphon.core.plugins.WebPlugin;
import top.codings.websiphon.core.requester.WebRequester;
import top.codings.websiphon.exception.WebException;

@Data
@AllArgsConstructor
public class StatisticsUrlWebPlugin implements WebPlugin {
    private StatisticResult statisticResult;

    @Override
    public Object[] before(Object[] params, Class targetClass, MethodDesc methodDesc, ReturnPoint point) throws WebException {
        statisticResult.TOTAL_URL_REQUEST.incrementAndGet();
        statisticResult.TOTAL_URL_REQUEST_TODAY.incrementAndGet();
        return params;
    }

    @Override
    public Object after(Object proxy, Object[] params, Object result, Class targetClass, MethodDesc methodDesc, ReturnPoint point) throws WebException {
        if (null == result || !(result instanceof WebResponse)) {
            return result;
        }
        WebResponse response = (WebResponse) result;
        switch (response.getResult()) {
            case OK:
                statisticResult.TOTAL_URL_REQUEST_SUCCESS.incrementAndGet();
                statisticResult.TOTAL_URL_REQUEST_SUCCESS_TODAY.incrementAndGet();
                break;
            case NOT_FOUND:
                statisticResult.TOTAL_URL_REQUEST_404.incrementAndGet();
                statisticResult.TOTAL_URL_REQUEST_404_TODAY.incrementAndGet();
                break;
            case TIME_OUT:
                statisticResult.TOTAL_URL_REQUEST_TIMEOUT.incrementAndGet();
                statisticResult.TOTAL_URL_REQUEST_TIMEOUT_TODAY.incrementAndGet();
                break;
        }
        if (response.getResult() != WebResponse.Result.OK) {
            statisticResult.TOTAL_URL_REQUEST_FAIL.incrementAndGet();
            statisticResult.TOTAL_URL_REQUEST_FAIL_TODAY.incrementAndGet();
        }
        return result;
    }

    @Override
    public Class[] getTargetInterface() {
        return new Class[]{
                WebRequester.class
        };
    }

    @Override
    public MethodDesc[] getMethods() {
        return new MethodDesc[]{
                new MethodDesc("execute", new Class[]{WebRequest.class})
        };
    }
}
