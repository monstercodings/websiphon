package top.codings.websiphon.core.requester;/*
package top.codings.websiphon.core.requester;

import top.codings.websiphon.bean.WebRequest;
import top.codings.websiphon.bean.WebResponse;
import com.alibaba.fastjson.JSON;
import com.google.common.net.MediaType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

@Slf4j
@Data
@NoArgsConstructor
public class PhantomWebRequester<W extends WebRequest> implements WebRequester<W> {
    private String phantomPath;
    private String jsPath;
    private volatile boolean health = true;

    public PhantomWebRequester(String phantomPath, String jsPath) {
        this.phantomPath = phantomPath;
        this.jsPath = jsPath;
    }

    @Override
    public WebResponse execute(W request) {
        StringBuilder stringBuilder = new StringBuilder();
        WebResponse response = new WebResponse();
        response.setUrl(request.getUrl());
        if (!health) {
            response.setResult(WebResponse.Result.ILL_HEALTH);
            return response;
        }
        boolean result = false;
        try {
            result = process(request.getUrl(), stringBuilder);
        } catch (IOException e) {
            log.error("使用Phantomjs请求[{}]时发生IO异常", request.getUrl(), e);
            response.setThrowable(e);
        }
        String content = stringBuilder.toString();
        if (!result || StringUtils.isBlank(content)) {
            response.setResult(WebResponse.Result.FAIL);
            return response;
        }
        content = content.trim();
        response.setResult(WebResponse.Result.OK);
        if (content.startsWith("<")) {
            response.setContentType(MediaType.HTML_UTF_8);
            response.setDocument(Jsoup.parse(content, request.getUrl()));
        } else if (content.startsWith("{")) {
            response.setContentType(MediaType.JSON_UTF_8);
            response.setJson(JSON.parseObject(content));
        }
        return response;
    }

    @Override
    public boolean isHealth() {
        return health;
    }

    @Override
    public boolean close() {
        health = false;
        return true;
    }

    private boolean process(String url, StringBuilder content) throws IOException {
        String BLANK = " ";
        long start = System.currentTimeMillis();
        Process process = null;
        BufferedReader reader = null;
        try {
            process = Runtime.getRuntime().exec(
                    new StringBuilder()
                            .append(phantomPath)
                            .append(BLANK)
                            .append(jsPath)
                            .append(BLANK)
                            .append(url)
                            .toString());
            reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = reader.readLine();
            if (line == null) {
                return false;
            }
            boolean isBegin = false;
            while ((line = reader.readLine()) != null) {
                if (!isBegin) {
                    if (line.startsWith("SUCCESS|")) {
                        log.debug("Phantomjs请求网址[{}]耗时：{}ms", url, line.split(":")[1]);
                        isBegin = true;
                    } else if (line.startsWith("FAIL|")) {
//                        log.error("请求[{}]失败原因 >>>>>> {}", url, line.substring(line.indexOf("|") + 1));
                        return false;
                    } else if (line.startsWith("LOG|")) {
                        log.debug("Phantomjs日志：{}", line.substring(line.indexOf("|") + 1));
                    } else if (line.startsWith("ReferenceError")) {
                        log.debug("Phantomjs异常：{}", line);
                    }
                } else {
                    content.append(line);
                }
            }
            return true;
        } finally {
            if (reader != null) {
                reader.close();
            }
            if (process != null) {
                process.destroy();
            }
            log.debug("调用Phantomjs进程耗时：{}ms", (System.currentTimeMillis() - start));
        }
    }
}
*/
