package top.codings.websiphon.core.context.event.listener;

import top.codings.websiphon.core.context.WebType;
import top.codings.websiphon.core.context.event.WebSyncEvent;
import top.codings.websiphon.exception.WebException;

public abstract class WebSyncEventListener<E extends WebSyncEvent> implements WebType {
    public abstract void listen(E event) throws WebException;
}
