package top.codings.websiphon.core.requester;/*
package top.codings.websiphon.core.requester;

import top.codings.websiphon.bean.WebRequest;
import top.codings.websiphon.bean.WebResponse;
import top.codings.websiphon.exception.WebRuntimeException;
import top.codings.websiphon.util.HttpDecodeUtils;
import com.alibaba.fastjson.JSON;
import com.google.common.net.MediaType;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Connection;
import org.jsoup.Jsoup;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.Map;

@Data
@Slf4j
public class JsoupWebRequester<W extends WebRequest> implements WebRequester<W> {
    protected volatile boolean health = true;

    @Override
    public WebResponse execute(W request) {
        String url = request.getUrl();
        Map<String, String> headers = request.getHeaders();
        int timeout = request.getTimeout();
        WebResponse webResponse = new WebResponse();
        Connection.Response response;
        try {
            response = Jsoup.connect(url).headers(headers).maxBodySize(0).ignoreHttpErrors(true).ignoreContentType(true).timeout(timeout).validateTLSCertificates(false).proxy(request.getProxy()).execute();
        } catch (SocketTimeoutException e) {
            webResponse.setUrl(url);
            webResponse.setDocument(null);
            webResponse.setResult(WebResponse.Result.TIME_OUT);
            return webResponse;
        } catch (Exception e) {
            webResponse.setUrl(url);
            webResponse.setDocument(null);
            webResponse.setResult(WebResponse.Result.FAIL);
            webResponse.setThrowable(e);
            return webResponse;
        }
        webResponse.setUrl(response.url().toString());
        webResponse.setStatusCode(response.statusCode());
        WebResponse.Result result = WebResponse.Result.valueOf(response.statusCode());
        webResponse.setResult(result);
        if (result != WebResponse.Result.OK) {
            return webResponse;
        }
        if (StringUtils.isBlank(response.charset())) {
            response.charset(HttpDecodeUtils.findEncoding(Jsoup.parse(response.body())));
        }
        MediaType contentType = getContentType(response);
        if (null == contentType) {
            webResponse.setResult(WebResponse.Result.CONTENT_TYPE_ERROR);
        } else if (contentType == MediaType.HTML_UTF_8) {
            try {
                webResponse.setDocument(response.parse());
            } catch (IOException e) {
                throw new WebRuntimeException(e);
            }
        } else if (contentType == MediaType.JSON_UTF_8) {
            webResponse.setJson(JSON.parseObject(response.body()));
        } else if (contentType == MediaType.JPEG) {
            webResponse.setBytes(response.bodyAsBytes());
        } else {
            webResponse.setResult(WebResponse.Result.CONTENT_TYPE_ERROR);
        }
        webResponse.setContentType(contentType);
        return webResponse;
    }

    private MediaType getContentType(Connection.Response response) {
        MediaType contentType;
        String contentType = response.contentType();
        if (StringUtils.isBlank(contentType)) {
            return MediaType.HTML_UTF_8;
        }
        if (!contentType.contains("/")) {
            contentType = contentType + "/*";
        }
        if (contentType.indexOf(";") > 0) {
            contentType = contentType.substring(0, contentType.indexOf(";"));
        }
        if (contentType.contains("text") || contentType.contains("application")) {
            contentType = contentType + "; charset=utf-8";
        }
        try {
            contentType = MediaType.parse(contentType);
        } catch (Exception e) {
            log.error("无法解析的MediaType：{}", response.contentType(), e);
            contentType = null;
        }
        return contentType;
    }

    @Override
    public void close() {
        health = false;
    }
}
*/
