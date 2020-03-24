package top.codings.websiphon.core.processor.support;

import top.codings.websiphon.bean.ResultDoc;
import top.codings.websiphon.bean.WebRequestDoc;
import top.codings.websiphon.core.context.CrawlerContext;
import top.codings.websiphon.core.processor.WebProcessorAdapter;
import top.codings.websiphon.exception.WebParseException;
import top.codings.websiphon.util.ContentExtractor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;

import java.text.ParseException;
import java.text.SimpleDateFormat;

@Slf4j
public class DmicNewsExtractorProcessor extends WebProcessorAdapter<WebRequestDoc> {
    @Override
    public void process(WebRequestDoc request, CrawlerContext context) throws WebParseException {
        ResultDoc resultDoc = request.getResultDoc();
        try {
            ContentExtractor.News news = ContentExtractor.getNewsByDoc(Jsoup.parse(request.response().getHtml()));
            resultDoc.setTitleStr(news.getTitle());
            resultDoc.setCreatedAtStr(news.getTime());
            try {
                resultDoc.setCreatedAt(news.getTime().contains(" ") ? new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(news.getTime()) : new SimpleDateFormat("yyyy-MM-dd").parse(news.getTime()));
            } catch (Exception e) {
                log.debug("解析日期失败 > {}", news.getTime());
            }
            resultDoc.setContentEle(news.getContentElement());
            resultDoc.setContentStr(news.getContent());
        } catch (Exception e) {
            throw new WebParseException(e);
        }
        fireProcess(request, context);
    }
}
