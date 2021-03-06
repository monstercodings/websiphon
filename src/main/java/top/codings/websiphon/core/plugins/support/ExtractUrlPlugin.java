package top.codings.websiphon.core.plugins.support;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import top.codings.websiphon.bean.BasicWebRequest;
import top.codings.websiphon.bean.PushResult;
import top.codings.websiphon.bean.WebRequest;
import top.codings.websiphon.core.context.CrawlerContext;
import top.codings.websiphon.core.context.event.async.WebExceptionEvent;
import top.codings.websiphon.core.context.event.sync.WebLinkEvent;
import top.codings.websiphon.core.parser.WebParser;
import top.codings.websiphon.core.plugins.AspectInfo;
import top.codings.websiphon.core.plugins.WebPlugin;
import top.codings.websiphon.exception.WebException;
import top.codings.websiphon.util.HttpOperator;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 扩散链接插件
 * 必须添加在任务列表监控插件 MissionOverAlertPlugin 之前
 */
@Slf4j
public class ExtractUrlPlugin implements WebPlugin {
    private String rawfilterRegex = "(\\w+)://([^/:]+)(:\\d*)?([^# ]*)";
    private Pattern rawFilterPattern = Pattern.compile(rawfilterRegex);
    private String filterRegex = rawfilterRegex;
    private Pattern filterPattern;
    private boolean sameDomain;
    private boolean allowHomepage;
    private Predicate<String> filter;

    public ExtractUrlPlugin(boolean sameDomain, boolean allowHomepage) {
        this(sameDomain, allowHomepage, null, null);
    }

    public ExtractUrlPlugin(Predicate<String> filter) {
        this(false, true, null, filter);
    }

    public ExtractUrlPlugin(boolean sameDomain, boolean allowHomepage, String filterRegex) {
        this(sameDomain, allowHomepage, filterRegex, null);
    }

    public ExtractUrlPlugin(boolean sameDomain, boolean allowHomepage, Predicate<String> filter) {
        this(sameDomain, allowHomepage, null, filter);
    }

    public ExtractUrlPlugin(boolean sameDomain, boolean allowHomepage, String filterRegex, Predicate<String> filter) {
        this.sameDomain = sameDomain;
        this.allowHomepage = allowHomepage;
        if (StringUtils.isNotBlank(filterRegex)) {
            this.filterRegex = filterRegex;
        }
        filterPattern = Pattern.compile(this.filterRegex);
        if (null != filter) {
            this.filter = filter;
        } else {
//            this.filter = BloomFilter.create(Funnels.stringFunnel(Charset.forName("utf-8")), 200000000, 0.001D)::put;
        }
    }

    /*@Override
    public Object[] before(Object[] params, Class targetClass, MethodDesc methodDesc, ReturnPoint point) throws WebException {
        return params;
    }

    @Override
    public Object after(Object proxy, Object[] params, Object result, Class targetClass, MethodDesc methodDesc, ReturnPoint point) throws WebException {
        if (point.point == ReturnPoint.Point.ERROR) {
            return result;
        }
        if (params.length == 1 && WebRequest.class.isAssignableFrom(params[0].getClass())) {
            WebRequest request = (WebRequest) params[0];
            *//*if (request.getMaxDepth() > 0 && request.getDepth() >= request.getMaxDepth()) {
                return result;
            }*//*
            if (request.response() == null ||
                    StringUtils.isBlank(request.response().getContentType()) ||
                    !request.response().getContentType().startsWith("text")
            ) {
                return result;
            }
            // 判断
            Document document;
            try {
                document = Jsoup.parse(request.response().getHtml());
            } catch (Exception e) {
                // TODO Jsoup解析失败
                return result;
            }
            Collection<String> urls = new HashSet<>();
            document.getElementsByTag("a").forEach(element -> {
                if (!element.hasAttr("href")) {
                    return;
                }
                String href = element.attr("href");
                if (StringUtils.isBlank(href) || href.startsWith("javascript") || href.startsWith("#")) {
                    return;
                }
                String url = HttpOperator.recombineLink(href, request.uri());
                if (StringUtils.isBlank(url) || url.equals(request.uri()) || !HttpOperator.urlLegalVerify(url)) {
                    return;
                }
                url = url.trim();
                if (sameDomain) {
                    Matcher oldMatcher = rawFilterPattern.matcher(request.uri());
                    Matcher newMatcher = rawFilterPattern.matcher(url);
                    if (oldMatcher.find() && newMatcher.find()) {
                        String oldDomain = oldMatcher.group(2);
                        String newDomain = newMatcher.group(2);
                        if (!oldDomain.equals(newDomain)) {
                            return;
                        }
                    }
                }
                if (!allowHomepage) {
                    Matcher matcher = rawFilterPattern.matcher(url);
                    if (!matcher.find()) {
                        return;
                    }
                    String path = matcher.group(4);
                    if (StringUtils.isBlank(path) || path.equals("/")) {
                        return;
                    }
                }
                Matcher matcher = filterPattern.matcher(url);
                if (!matcher.find()) {
                    return;
                }
                if (filter == null) {
                    urls.add(url);
                } else if (filter.test(url)) {
                    urls.add(url);
                }
            });
            CrawlerContext context = request.context();
            urls.forEach(url -> {
                try {
                    PushResult pushResult;
                    WebLinkEvent event = new WebLinkEvent();
                    event.setRequest(request);
                    event.setNewUrl(url);
                    if (context.postSyncEvent(event)) {
                        List<WebRequest> out = event.getOut();
                        for (WebRequest wr : out) {
                            if ((pushResult = context.getCrawler().push(wr)) != PushResult.SUCCESS) {
                                if (pushResult != PushResult.URL_REPEAT) {
                                    log.warn("推送扩散链接给爬虫失败 -> {}", pushResult.value);
                                }
                            }
                        }
                    } else {
                        BasicWebRequest basicWebRequest = new BasicWebRequest();
                        basicWebRequest.setUri(url);
                        if ((pushResult = context.getCrawler().push(basicWebRequest)) != PushResult.SUCCESS) {
                            if (pushResult != PushResult.URL_REPEAT) {
                                log.warn("推送扩散链接给爬虫失败 -> {}", pushResult.value);
                            }
                        }
                    }
                } catch (Exception e) {
                    WebExceptionEvent event = new WebExceptionEvent();
                    event.setThrowable(e);
                    event.setRequest(request);
                    context.postAsyncEvent(event);
                }
            });
        }
        return result;
    }

    @Override
    public Class[] getTargetInterface() {
        return new Class[]{WebParser.class};
    }

    @Override
    public MethodDesc[] getMethods() {
        return new MethodDesc[]{new MethodDesc("parse", new Class[]{WebRequest.class})};
    }*/

    @Override
    public void onBefore(AspectInfo aspectInfo, Object[] args) throws WebException {

    }

    @Override
    public Object onAfterReturning(AspectInfo aspectInfo, Object[] args, Object returnValue) {
        WebRequest request = (WebRequest) args[0];
        if (request.response() == null ||
                StringUtils.isBlank(request.response().getContentType()) ||
                !request.response().getContentType().startsWith("text")
        ) {
            return returnValue;
        }
        // 判断
        Document document;
        try {
            document = Jsoup.parse(request.response().getHtml());
        } catch (Exception e) {
            // TODO Jsoup解析失败
            return returnValue;
        }
        Collection<String> urls = new HashSet<>();
        document.getElementsByTag("a").forEach(element -> {
            if (!element.hasAttr("href")) {
                return;
            }
            String href = element.attr("href");
            if (StringUtils.isBlank(href) || href.startsWith("javascript") || href.startsWith("#")) {
                return;
            }
            String url = HttpOperator.recombineLink(href, request.uri());
            if (StringUtils.isBlank(url) || url.equals(request.uri()) || !HttpOperator.urlLegalVerify(url)) {
                return;
            }
            url = url.trim();
            if (sameDomain) {
                Matcher oldMatcher = rawFilterPattern.matcher(request.uri());
                Matcher newMatcher = rawFilterPattern.matcher(url);
                if (oldMatcher.find() && newMatcher.find()) {
                    String oldDomain = oldMatcher.group(2);
                    String newDomain = newMatcher.group(2);
                    if (!oldDomain.equals(newDomain)) {
                        return;
                    }
                }
            }
            if (!allowHomepage) {
                Matcher matcher = rawFilterPattern.matcher(url);
                if (!matcher.find()) {
                    return;
                }
                String path = matcher.group(4);
                if (StringUtils.isBlank(path) || path.equals("/")) {
                    return;
                }
            }
            Matcher matcher = filterPattern.matcher(url);
            if (!matcher.find()) {
                return;
            }
            if (filter == null) {
                urls.add(url);
            } else if (filter.test(url)) {
                urls.add(url);
            }
        });
        CrawlerContext context = request.context();
        urls.forEach(url -> {
            try {
                PushResult pushResult;
                WebLinkEvent event = new WebLinkEvent();
                event.setRequest(request);
                event.setNewUrl(url);
                if (context.postSyncEvent(event)) {
                    List<WebRequest> out = event.getOut();
                    for (WebRequest wr : out) {
                        if ((pushResult = context.getCrawler().push(wr)) != PushResult.SUCCESS) {
                            if (pushResult != PushResult.URL_REPEAT) {
                                log.warn("推送扩散链接给爬虫失败 -> {}", pushResult.value);
                            }
                        }
                    }
                } else {
                    BasicWebRequest basicWebRequest = new BasicWebRequest();
                    basicWebRequest.setUri(url);
                    if ((pushResult = context.getCrawler().push(basicWebRequest)) != PushResult.SUCCESS) {
                        if (pushResult != PushResult.URL_REPEAT) {
                            log.warn("推送扩散链接给爬虫失败 -> {}", pushResult.value);
                        }
                    }
                }
            } catch (Exception e) {
                WebExceptionEvent event = new WebExceptionEvent();
                event.setThrowable(e);
                event.setRequest(request);
                context.postAsyncEvent(event);
            }
        });
        return returnValue;
    }

    @Override
    public void onAfterThrowing(AspectInfo aspectInfo, Object[] args, Throwable throwable) {

    }

    @Override
    public void onFinal(AspectInfo aspectInfo, Object[] args, Throwable throwable) {

    }

    @Override
    public AspectInfo[] aspectInfos() {
        try {
            return new AspectInfo[]{
                    new AspectInfo(WebParser.class, WebParser.class.getMethod("parse", WebRequest.class))
            };
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    @Override
    public int index() {
        return 9000;
    }
}
