package top.codings.websiphon.core.processor.support;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.select.Elements;
import top.codings.websiphon.bean.ResultDoc;
import top.codings.websiphon.bean.WebImage;
import top.codings.websiphon.bean.WebRequestDoc;
import top.codings.websiphon.core.context.CrawlerContext;
import top.codings.websiphon.core.processor.WebProcessorAdapter;
import top.codings.websiphon.exception.WebParseException;
import top.codings.websiphon.util.HttpOperator;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BasicDocImageProcessor extends WebProcessorAdapter<WebRequestDoc> {
    private String className = "basic-default-image";

    @Override
    public void process(WebRequestDoc request, CrawlerContext context) throws WebParseException {
        ResultDoc result = request.getResultDoc();
        if (result.getContentEle() == null) {
            fireProcess(request, context);
            return;
        }
        Elements imgElements = result.getContentEle().getElementsByTag("img");
        if (imgElements.isEmpty()) {
            fireProcess(request, context);
            return;
        }
        imgElements.forEach(img -> {
            if (StringUtils.isBlank(img.attr("src"))) {
                return;
            }
            WebImage webImage = new WebImage();
            webImage.setClassName(className);
            webImage.setSrc(HttpOperator.recombineLink(img.attr("src"), request.getResponse().getUrl()));
            webImage.setFullTag(String.format("<img class='%s' src='%s' />", className, webImage.getSrc()));
            result.getImages().add(webImage);
        });
        fireProcess(request, context);
    }
}
