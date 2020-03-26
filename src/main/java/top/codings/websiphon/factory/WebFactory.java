package top.codings.websiphon.factory;

import top.codings.websiphon.bean.WebRequest;
import top.codings.websiphon.core.Crawler;
import top.codings.websiphon.core.context.WebType;

public interface WebFactory<T extends WebRequest> {
    WebFactory<T> setNetworkThread(int max);

    WebFactory<T> setParseThread(int max);

    WebFactory<T> setPermitForHost(int permitForHost);

    WebFactory<T> addLast(WebType type);

    Crawler build();

}
