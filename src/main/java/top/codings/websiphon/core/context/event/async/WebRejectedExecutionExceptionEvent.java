package top.codings.websiphon.core.context.event.async;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import top.codings.websiphon.bean.WebRequest;

import java.util.concurrent.RejectedExecutionException;

@Getter
@Setter
@AllArgsConstructor
public class WebRejectedExecutionExceptionEvent<T extends WebRequest> extends WebExceptionEvent<T> {
    private RejectedExecutionException exception;
}
