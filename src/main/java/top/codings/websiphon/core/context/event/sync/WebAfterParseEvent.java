package top.codings.websiphon.core.context.event.sync;

import lombok.Getter;
import lombok.Setter;
import top.codings.websiphon.bean.WebRequest;
import top.codings.websiphon.core.context.event.WebSyncEvent;

@Getter
@Setter
public class WebAfterParseEvent<T extends WebRequest> extends WebSyncEvent<T> {
}
