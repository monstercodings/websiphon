package top.codings.websiphon.core.context.event.sync;

import top.codings.websiphon.bean.WebRequest;
import top.codings.websiphon.core.context.event.WebSyncEvent;
import top.codings.websiphon.exception.StopWebRequestException;

public class WebBeforeRequestEvent<T extends WebRequest> extends WebSyncEvent<T> {
    public void stop() {
        throw new StopWebRequestException();
    }
}
