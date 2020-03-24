package top.codings.websiphon.core.processor.support;

import lombok.Getter;
import lombok.Setter;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import top.codings.websiphon.bean.WebRequestDoc;
import top.codings.websiphon.core.context.CrawlerContext;
import top.codings.websiphon.core.processor.WebProcessorAdapter;
import top.codings.websiphon.exception.WebParseException;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
public class BasicDocDenoiseProcessor extends WebProcessorAdapter<WebRequestDoc> {
    private Set<String> excludeTags = new HashSet<>();
    private Set<String> excludePaths = new HashSet<>();

    {
        excludeTags.add("script");
        excludeTags.add("style");
        excludeTags.add("select");
        excludeTags.add("link");
        excludeTags.add("input");
        excludeTags.add("object");
        excludeTags.add("textarea");
        excludeTags.add("meta");
    }

    @Override
    public void process(WebRequestDoc request, CrawlerContext context) throws WebParseException {
        Document document = Jsoup.parse(request.response().getHtml());
//        document.getElementsByTag("a").attr("href", "javascript:void(0)").remove();
        document.getElementsByAttributeValue("display", "none").remove();
        document.getElementsByAttributeValueContaining("style", "display:none").remove();
        document.getElementsByAttributeValueContaining("style", "overflow: hidden").remove();
        excludeTags.forEach(s -> document.getElementsByTag(s).remove());
        excludePaths.forEach(s -> document.select(s).remove());
        fireProcess(request, context);
    }
}
