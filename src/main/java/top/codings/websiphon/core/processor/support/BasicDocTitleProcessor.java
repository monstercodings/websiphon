package top.codings.websiphon.core.processor.support;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import top.codings.websiphon.bean.ResultDoc;
import top.codings.websiphon.bean.WebRequestDoc;
import top.codings.websiphon.core.context.CrawlerContext;
import top.codings.websiphon.core.processor.WebProcessorAdapter;
import top.codings.websiphon.exception.WebParseException;
import top.codings.websiphon.util.JsoupUtils;

@Getter
@Setter
public class BasicDocTitleProcessor extends WebProcessorAdapter<WebRequestDoc> {
    private int maxTier = 6;

    @Override
    public void process(WebRequestDoc request, CrawlerContext context) throws WebParseException {
        int tier = 0;
        ResultDoc result = request.getResultDoc();
        Element element = result.getContentEle();
        // 先往外找maxTier层
        while (tier < maxTier) {
            if (element == null) {
                break;
            }
            for (Element now : element.siblingElements()) {
                if (now.elementSiblingIndex() > element.elementSiblingIndex()) {
                    continue;
                }
                for (int i = 1; i < 7; i++) {
                    Elements hTitles = now.getElementsByTag("h" + i);
                    if (setTitle(result, hTitles)) {
                        fireProcess(request, context);
                        return;
                    }
                }
            }
            element = element.parent();
            tier++;
        }
        //找不到的话就在正文内找
        for (int i = 1; i < 7; i++) {
            Elements hTitles = result.getContentEle().getElementsByTag("h" + i);
            if (setTitle(result, hTitles)) {
                fireProcess(request, context);
                return;
            }
        }
        Element titleEle = Jsoup.parse(request.getResponse().getHtml()).select("title").first();
        if (titleEle != null) {
            String title = titleEle.text();
            if (StringUtils.isNotBlank(title)) {
                result.setTitleEle(titleEle);
                result.setTitleCss(JsoupUtils.getPath(titleEle));
                result.setTitleStr(title);
                fireProcess(request, context);
                return;
            }
        }
        Element contentEle = result.getContentEle();
        result.setTitleStr(contentEle.text().substring(0, contentEle.text().length() > 20 ? 20 : contentEle.text().length()));
        fireProcess(request, context);
    }

    private boolean setTitle(ResultDoc result, Elements hTitles) {
        if (!hTitles.isEmpty()) {
            for (Element hTitle : hTitles) {
                if (StringUtils.isNotBlank(hTitle.text())) {
                    result.setTitleEle(hTitle);
                    result.setTitleCss(JsoupUtils.getPath(hTitle));
                    result.setTitleStr(hTitle.text());
                    return true;
                }
            }
        }
        return false;
    }
}
