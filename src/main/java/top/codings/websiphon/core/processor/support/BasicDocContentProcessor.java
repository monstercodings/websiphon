package top.codings.websiphon.core.processor.support;

import top.codings.websiphon.bean.ResultDoc;
import top.codings.websiphon.bean.WebRequestDoc;
import top.codings.websiphon.core.context.CrawlerContext;
import top.codings.websiphon.core.processor.WebProcessorAdapter;
import top.codings.websiphon.exception.WebParseException;
import top.codings.websiphon.util.JsoupUtils;
import top.codings.websiphon.util.Rater;
import lombok.Getter;
import lombok.Setter;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

@Getter
@Setter
public class BasicDocContentProcessor extends WebProcessorAdapter<WebRequestDoc> {
    private int extractMax = 3;
    private double percent = 0.5d;

    @Override
    public void process(WebRequestDoc request, CrawlerContext context) throws WebParseException {
        ResultDoc result = request.getResultDoc();
        result.setContentEle(getContent(Rater.getMaxScoreElement(Jsoup.parse(request.getResponse().getHtml()).body())));
        if (result.getContentEle() == null) {
            fireProcess(request, context);
            return;
        }
        result.setContentCss(JsoupUtils.getPath(result.getContentEle()));
        result.setContentStr(result.getContentEle().text());
        fireProcess(request, context);
    }

    private Element getContent(Element max) {
        int extract = 0;
        for (int i = 0; i < max.parents().size() && extract < extractMax; i++, extract++) {
            Element parent = max.parents().get(i);
            if (parent.attr("score") == null ||
                    parent.attr("score").equals("") ||
                    parent.attr("score").equals(0) ||
                    Float.parseFloat(parent.attr("score")) - Float.parseFloat(max.attr("score")) <= 0f) {
                extract--;
                continue;
            }
            float contentScore = Float.parseFloat(max.attr("score"));
            float parentScore = Float.parseFloat(parent.attr("score"));
            if (((parentScore - contentScore) / contentScore) > percent) {
                return getContent(parent);
            }
        }
        return max;
    }
}
