package top.codings.websiphon.core.processor.support;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import top.codings.websiphon.bean.WebRequestDoc;
import top.codings.websiphon.core.processor.WebProcessorAdapter;
import top.codings.websiphon.exception.WebParseException;
import top.codings.websiphon.util.Rater;

public class BasicDocMarkScoreProcessor extends WebProcessorAdapter<WebRequestDoc> {
    @Override
    public void process(WebRequestDoc request) throws WebParseException {
        Element body = Jsoup.parse(request.response().getHtml()).body();
        doScoreToElement(body);
        fireProcess(request);
    }

    private int doScoreToElement(Element element) {
        Elements children = element.children();
        if (children.size() == 0) {//不含有子节点
            return Rater.doRate(element);
        } else {//含有子节点
            int accum = Rater.doOwnTextRate(element);
            for (Element child : children) {
                accum += doScoreToElement(child);
            }
            element.attr("score", String.valueOf(accum));
            return accum;
        }
    }
}
