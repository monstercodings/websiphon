package top.codings.websiphon.core.parser;

import top.codings.websiphon.bean.WebProcessorDefinition;
import top.codings.websiphon.bean.WebRequest;
import top.codings.websiphon.core.context.CrawlerContext;
import top.codings.websiphon.core.context.WebType;
import top.codings.websiphon.core.processor.WebProcessor;
import top.codings.websiphon.exception.WebParseException;

import java.util.List;

/**
 * 解析器接口
 */
public interface WebParser<IN extends WebRequest> extends WebType {
    /**
     * 获取解析结果
     *
     * @return
     */
    void parse(IN param, CrawlerContext context) throws WebParseException;

    /**
     * 设置处理类
     *
     * @param definition
     * @return
     */
    WebParser addProcessor(WebProcessorDefinition definition);

    /**
     * 获取处理链
     *
     * @return
     */
    List<WebProcessor> getProcessors();
}
