package top.codings.websiphon.core.context.event.sync;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.http.entity.ContentType;
import top.codings.websiphon.bean.WebRequest;
import top.codings.websiphon.core.context.event.WebSyncEvent;

/**
 * 数据下载事件
 * @param <T>
 */
@Getter
@AllArgsConstructor
public class WebDownloadEvent<T extends WebRequest> extends WebSyncEvent<T> {
    private ContentType contentType;
    private long contentLength;
}
