package top.codings.websiphon.core.context.event.listener;

import top.codings.websiphon.core.context.WebType;
import top.codings.websiphon.core.context.event.WebAsyncEvent;

public abstract class WebAsyncEventListener<E extends WebAsyncEvent> implements WebType {
    public abstract void listen(E event);
}
