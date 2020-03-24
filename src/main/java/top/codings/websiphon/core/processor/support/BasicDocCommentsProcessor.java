package top.codings.websiphon.core.processor.support;

import top.codings.websiphon.bean.WebComment;
import top.codings.websiphon.bean.WebRequest;
import top.codings.websiphon.bean.WebRequestDoc;
import top.codings.websiphon.core.context.CrawlerContext;
import top.codings.websiphon.core.processor.WebProcessorAdapter;
import top.codings.websiphon.exception.WebParseException;
import top.codings.websiphon.util.JsoupUtils;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class BasicDocCommentsProcessor extends WebProcessorAdapter<WebRequestDoc> {
    @Override
    public void process(WebRequestDoc request, CrawlerContext context) throws WebParseException {
        Document document = Jsoup.parse(request.response().getHtml());
        Map<String, Integer> map = new HashMap<>();
        String path = null;
        int max = 0;
        for (Element element : document.getElementsByAttribute("class")) {
            String key = JsoupUtils.getPathWithoutId(element);
            key = key.substring(0, key.lastIndexOf(":nth"));
            Integer count = map.get(key);
            if (count == null) {
                count = 1;
                map.put(key, count);
                if (count > max) {
                    max = count;
                    path = key;
                }
            } else {
                count++;
                map.replace(key, count);
                if (count > max) {
                    max = count;
                    path = key;
                }
            }
        }
        if (StringUtils.isNotBlank(path)) {
            List<WebComment> comments = new LinkedList<>();
            Elements commentsEle = document.select(path);
            commentsEle.forEach(element -> {
                WebComment comment = new WebComment();
                comment.setRawContent(element.text());
                comments.add(comment);
            });
            request.getResultDoc().setComments(comments);
        }
        fireProcess(request, context);
    }
}
