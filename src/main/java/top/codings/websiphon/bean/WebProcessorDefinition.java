package top.codings.websiphon.bean;

import top.codings.websiphon.core.processor.WebProcessor;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Iterator;

@Data
public class WebProcessorDefinition {
    private WebProcessor processor;
    private Class type;
    private Iterator<WebProcessor> iterator;
}
