package top.codings.websiphon.core.context.event.sync;

import lombok.Getter;
import lombok.Setter;
import top.codings.websiphon.bean.WebRequest;
import top.codings.websiphon.core.context.event.WebSyncEvent;

import java.util.LinkedList;
import java.util.List;

@Getter
@Setter
public class WebLinkEvent<T extends WebRequest> extends WebSyncEvent<T> {
    protected String newUrl;
    protected List<T> out = new LinkedList<>();
}
