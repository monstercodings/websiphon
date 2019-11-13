package top.codings.websiphon.core.processor.support;

import top.codings.websiphon.bean.ResultDoc;
import top.codings.websiphon.bean.WebRequestDoc;
import top.codings.websiphon.core.context.CrawlerContext;
import top.codings.websiphon.core.processor.WebProcessorAdapter;
import top.codings.websiphon.exception.WebParseException;
import top.codings.websiphon.util.DateProcessUtils;
import top.codings.websiphon.util.JsoupUtils;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.nodes.Element;

import java.util.Date;

public class BasicDocPubdateProcessor extends WebProcessorAdapter<WebRequestDoc> {
    @Override
    public void process(WebRequestDoc request, CrawlerContext context) throws WebParseException {
        int tier = 0;
        Element element = request.getResultDoc().getContentEle();
        if (element == null) {
            fireProcess(request, context);
            return;
        }
        //往外找6层
        while (tier < 6) {
            for (Element now : element.siblingElements()) {
                String string = now.text();
                if (StringUtils.isBlank(string)) {
                    continue;
                }
                Date date = DateProcessUtils.process(string);
                if (setDate(request.getResultDoc(), now, string, date)) {
                    fireProcess(request, context);
                    return;
                }

            }
            element = element.parent();
            if (element == null) {
                break;
            }
            tier++;
        }
        //找不到就在正文内容里找
        for (Element now : request.getResultDoc().getContentEle().getAllElements()) {
            String string = now.ownText();
            if (StringUtils.isBlank(string)) {
                continue;
            }
            Date date = DateProcessUtils.process(string);
            if (setDate(request.getResultDoc(), now, string, date)) {
                fireProcess(request, context);
                return;
            }
        }
        fireProcess(request, context);
    }

    private boolean setDate(ResultDoc result, Element now, String string, Date date) {
        if (date != null) {
            result.setCreatedAt(date);
            result.setCreatedAtEle(now);
            result.setCreatedAtStr(string);
            result.setCreatedAtCss(JsoupUtils.getPath(now));
            return true;
        }
        return false;
    }
}
