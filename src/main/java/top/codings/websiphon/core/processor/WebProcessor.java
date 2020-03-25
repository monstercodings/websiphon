package top.codings.websiphon.core.processor;

import top.codings.websiphon.bean.WebProcessorDefinition;
import top.codings.websiphon.bean.WebRequest;
import top.codings.websiphon.core.context.WebType;
import top.codings.websiphon.exception.WebParseException;

import java.util.Iterator;

public interface WebProcessor<IN extends WebRequest> extends WebType {
    ThreadLocal<Boolean> BOOLEAN_THREAD_LOCAL = ThreadLocal.withInitial(() -> false);

    void process(IN request) throws WebParseException;

    void fireProcess(IN request) throws WebParseException;
}
