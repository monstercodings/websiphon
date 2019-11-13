package top.codings.websiphon.core.pipeline;

import top.codings.websiphon.bean.WebRequest;
import lombok.Data;

@Data
public abstract class ReadWritePipelineAdapter<T extends WebRequest> implements ReadWritePipeline<T> {
    protected volatile boolean pause = false;
}
