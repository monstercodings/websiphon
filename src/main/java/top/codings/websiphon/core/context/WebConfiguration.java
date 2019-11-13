package top.codings.websiphon.core.context;

import top.codings.websiphon.bean.StatisticResult;

public interface WebConfiguration {
    int getThreadSize();

    WebType[] getWebType();

    String getId();

    default StatisticResult getStatisticResult() {
        return new StatisticResult();
    }
}
