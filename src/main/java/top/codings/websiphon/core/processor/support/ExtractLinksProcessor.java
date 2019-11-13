package top.codings.websiphon.core.processor.support;/*
package top.codings.websiphon.core.processor.support;

import top.codings.websiphon.bean.WebRequest;
import top.codings.websiphon.bean.WebResponse;
import top.codings.websiphon.core.context.CrawlerContext;
import top.codings.websiphon.core.context.event.all.WebLinksEvent;
import top.codings.websiphon.core.processor.WebProcessorAdapter;
import top.codings.websiphon.exception.WebException;
import top.codings.websiphon.exception.WebParseException;
import top.codings.websiphon.util.HttpOperator;
import top.codings.websiphon.util.JsoupUtils;
import com.alibaba.fastjson.JSONObject;
import com.google.common.net.MediaType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.cglib.core.ReflectUtils;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

@Slf4j
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ExtractLinksProcessor extends WebProcessorAdapter<WebRequest> {
    private Looper looper;
    private int depth = 0;

    public ExtractLinksProcessor(Looper looper) {
        this.looper = looper;
    }

    public ExtractLinksProcessor(int depth) {
        this.depth = depth;
    }

    @Override
    public void process(WebRequest request, CrawlerContext context) throws WebParseException {
        if (depth > 0 && request.getDepth() >= depth) {
            fireProcess(request, context);
            return;
        }
        WebLinksEvent event = null;
        WebResponse response = request.getResponse();
        if (response.getContentType() == MediaType.HTML_UTF_8) {
            event = extractByHtml(request, response);
        } else if (response.getContentType() == MediaType.JSON_UTF_8) {
            event = extractByJson(request, response);
        }
        if (event != null) {
            event.setContentType(response.getContentType());
            event.setContext(context);
            try {
                if (!Catcher.postBothEvent(event) && null != looper) {
                    WebLinksEvent copyEvent = event;
                    copyEvent.getLinks().forEach(link -> {
                        List<WebRequest> toCrawl = new LinkedList<>();
                        try {
                            WebRequest next = (WebRequest) ReflectUtils.newInstance(request.getClass());
                            BeanUtils.copyProperties(next, request);
                            next.setUrl(link.toString());
                            looper.execute(next, toCrawl);
                        } catch (Exception e) {
                            log.error("复制对象出现异常", e);
                            return;
                        }
                        toCrawl.forEach(next -> {
                            if (StringUtils.isBlank(next.getUrl())) next.setUrl(link.toString());
                            if (next.getHeaders() == null) next.setHeaders(request.getHeaders());
                            if (next.getTimeout() == 0) next.setTimeout(request.getTimeout());
                            if (StringUtils.isBlank(next.getId())) next.setId(request.getId());
                            next.setDepth(request.getDepth() + 1);
                            context.getCrawler().push(next);
                        });
                    });
                }
            } catch (WebException e) {
                throw new WebParseException(e);
            }
        }
        fireProcess(request, context);
    }

    private WebLinksEvent extractByHtml(WebRequest param, WebResponse result) {
        Document document = result.getDocument();
        Elements elements = document.select("a");
        List<Element> linkEles = new LinkedList<>(elements);
        List<String> links = new LinkedList<>();
        Iterator<Element> iterator = linkEles.iterator();
        while (iterator.hasNext()) {
            Element element = iterator.next();
            String url = element.attr("href");
            if (StringUtils.isBlank((url = HttpOperator.recombineLink(url, result.getUrl())))) {
                iterator.remove();
                continue;
            }
            element.attr("href", url);
            element.attr("data-css", JsoupUtils.getPath(element));
            links.add(url);
        }
        WebLinksEvent event = new WebLinksEvent();
        event.setDocument(document);
        event.setLinkEles(linkEles);
        event.setLinks(links);
        event.setRequest(param);
        return event;
    }

    private WebLinksEvent extractByJson(WebRequest param, WebResponse result) {
        JSONObject json = result.getJson();
        WebLinksEvent event = new WebLinksEvent();
        return event;
    }

    public interface Looper {
        void execute(WebRequest next, List<WebRequest> toCrawl);
    }
}
*/
