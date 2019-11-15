package top.codings.websiphon.core.plugins;

import top.codings.websiphon.bean.MethodDesc;
import top.codings.websiphon.bean.ReturnPoint;
import top.codings.websiphon.bean.WebRequest;
import top.codings.websiphon.core.context.BasicCrawlerContext;
import top.codings.websiphon.core.context.CrawlerContext;
import top.codings.websiphon.core.context.event.async.WebExceptionEvent;
import top.codings.websiphon.core.context.event.sync.WebLinkEvent;
import top.codings.websiphon.core.parser.WebParser;
import top.codings.websiphon.exception.WebException;
import top.codings.websiphon.util.HttpOperator;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.net.Proxy;
import java.util.Collection;
import java.util.HashSet;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class ExtractUrlPlugin implements WebPlugin {
    private String rawRegex = "(\\w+)://([^/:]+)(:\\d*)?([^# ]*)";
    private Pattern rawPattern = Pattern.compile(rawRegex);
    private String regex = rawRegex;
    private Pattern pattern;
    private boolean sameDomain;
    private boolean allowHomepage;
    private Predicate<String> filter;

    public ExtractUrlPlugin(boolean sameDomain, boolean allowHomepage) {
        this(sameDomain, allowHomepage, null, null);
    }

    public ExtractUrlPlugin(Predicate<String> filter) {
        this(false, true, null, filter);
    }

    public ExtractUrlPlugin(boolean sameDomain, boolean allowHomepage, String regex) {
        this(sameDomain, allowHomepage, regex, null);
    }

    public ExtractUrlPlugin(boolean sameDomain, boolean allowHomepage, Predicate<String> filter) {
        this(sameDomain, allowHomepage, null, filter);
    }

    public ExtractUrlPlugin(boolean sameDomain, boolean allowHomepage, String regex, Predicate<String> filter) {
        this.sameDomain = sameDomain;
        this.allowHomepage = allowHomepage;
        if (StringUtils.isNotBlank(regex)) {
            this.regex = regex;
        }
        pattern = Pattern.compile(this.regex);
        if (null != filter) {
            this.filter = filter;
        } else {
//            this.filter = BloomFilter.create(Funnels.stringFunnel(Charset.forName("utf-8")), 200000000, 0.001D)::put;
        }
    }

    @Override
    public Object[] before(Object[] params, ReturnPoint point) throws WebException {
        return params;
    }

    @Override
    public Object after(Object proxy, Object[] params, Object result, MethodDesc methodDesc, ReturnPoint point) throws WebException {
        if (point.point == ReturnPoint.Point.ERROR) {
            return result;
        }
        if (params.length == 2 && WebRequest.class.isAssignableFrom(params[0].getClass())) {
            WebRequest request = (WebRequest) params[0];
            // 判断
            if (request.getResponse().getContentType().startsWith("text")) {
                Document document = Jsoup.parse(request.getResponse().getHtml());
                Collection<String> urls = new HashSet<>();
                document.getElementsByTag("a").forEach(element -> {
                    if (!element.hasAttr("href")) {
                        return;
                    }
                    String href = element.attr("href");
                    if (StringUtils.isBlank(href) || href.startsWith("java") || href.startsWith("#")) {
                        return;
                    }
                    String url = HttpOperator.recombineLink(href, request.getUrl());
                    if (StringUtils.isBlank(url) || url.equals(request.getUrl())) {
                        return;
                    }
                    if (sameDomain) {
                        Matcher oldMatcher = rawPattern.matcher(request.getUrl());
                        Matcher newMatcher = rawPattern.matcher(url);
                        if (oldMatcher.find() && newMatcher.find()) {
                            String oldDomain = oldMatcher.group(2);
                            String newDomain = newMatcher.group(2);
                            if (!oldDomain.equals(newDomain)) {
                                return;
                            }
                        }
                    }
                    if (!allowHomepage) {
                        Matcher matcher = rawPattern.matcher(url);
                        if (!matcher.find()) {
                            return;
                        }
                        String path = matcher.group(4);
                        if (StringUtils.isBlank(path) || path.equals("/")) {
                            return;
                        }
                    }
                    Matcher matcher = pattern.matcher(url);
                    if (!matcher.find()) {
                        return;
                    }
                    if (filter == null) {
                        urls.add(url);
                    } else if (filter.test(url)) {
                        urls.add(url);
                    }
                });
                BasicCrawlerContext context = (BasicCrawlerContext) params[1];
//                log.debug("扩散总数 -> {}", urls.size());
                urls.forEach(url -> {
                    try {
                        WebRequest clone = request.getClass().getDeclaredConstructor().newInstance();
                        clone.setUrl(url);
                        clone.setMethod(request.getMethod());
                        clone.setProxy(request.getProxy() == Proxy.NO_PROXY ? request.getProxy() : null);
                        clone.setHeaders(request.getHeaders());
                        clone.setTimeout(request.getTimeout());
                        clone.setDepth((short) (request.getDepth() + 1));
                        clone.setMaxDepth(request.getMaxDepth());
                        WebLinkEvent event = new WebLinkEvent();
                        event.setContext(context);
                        event.setRequest(request);
                        event.setNewRequest(clone);
                        context.postSyncEvent(event);
                    } catch (Exception e) {
                        WebExceptionEvent event = new WebExceptionEvent();
                        event.setThrowable(e);
                        event.setContext(context);
                        event.setRequest(request);
                        context.postAsyncEvent(event);
                    }
                });
            }
        }
        return result;
    }

    @Override
    public Class[] getTargetInterface() {
        return new Class[]{WebParser.class};
    }

    @Override
    public MethodDesc[] getMethods() {
        return new MethodDesc[]{new MethodDesc("parse", new Class[]{WebRequest.class, CrawlerContext.class})};
    }
}
