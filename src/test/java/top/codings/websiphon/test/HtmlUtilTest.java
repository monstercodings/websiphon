package top.codings.websiphon.test;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class HtmlUtilTest {
    @Test
    public void test() throws IOException {
        try (final WebClient webClient = new WebClient()) {
            final HtmlPage page = webClient.getPage("http://monitor.szkedun.cn/");
            final String pageAsXml = page.asXml();
            System.out.println(pageAsXml);
            System.out.println("-------------------------------------------------------------");
            final String pageAsText = page.asText();
            System.out.println(pageAsText);
        }
    }
}
