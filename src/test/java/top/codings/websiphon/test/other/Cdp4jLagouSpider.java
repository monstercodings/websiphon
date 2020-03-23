package top.codings.websiphon.test.other;

import io.webfolder.cdp.Launcher;
import io.webfolder.cdp.session.Session;
import io.webfolder.cdp.session.SessionFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.platform.commons.util.StringUtils;
import top.codings.websiphon.util.DelFileUtils;
import top.codings.websiphon.util.HttpOperator;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
public class Cdp4jLagouSpider {
    private Launcher launcher;
    private SessionFactory sessionFactory;
    private Set<String> set = new HashSet<>();

    private boolean init() {
        try {
            String path = "./chrome_data";
            DelFileUtils.delFolder(path);
            launcher = new Launcher();
            List<String> params = Arrays.asList(
//                    "--incognito",
//                    "–-in-process-plugins",
                    "-–disable-images",
                    "--ignore-certificate-errors",
                    "--disable-gpu",
                    "--user-data-dir=" + path,
                    "-–single-process"
            );
            sessionFactory = launcher.launch(launcher.findChrome(), params);
            return true;
        } catch (Exception e) {
            log.error("初始化Chrome内核失败", e);
            return false;
        }
    }

    public static void main(String[] args) {
        String prevUrl = "https://www.lagou.com/zhaopin/";
        Cdp4jLagouSpider spider = new Cdp4jLagouSpider();
        spider.init();
        Session session = spider.sessionFactory.create();
        try {
            session.navigate(prevUrl).waitDocumentReady(60000);
        } catch (Exception e) {
            session.stop();
            return;
        }
        while (true) {
            String content = session.getContent();
            String url = spider.parse(content, prevUrl);
            if (StringUtils.isBlank(url)) {
                log.debug("结束");
//                session.close();
//                spider.sessionFactory.close();
//                spider.launcher.kill();
                return;
            }
            spider.page(session);
            prevUrl = url;
        }
    }

    private String parse(String content, String req) {
        Document document = Jsoup.parse(content);
        Elements lis = document.select("ul.item_con_list>li");
        for (Element li : lis) {
            String positionid = li.attr("data-positionid");
            if (!set.add(positionid)) {
                continue;
            }
            String positionname = li.attr("data-positionname");
            String company = li.attr("data-company");
            String salary = li.attr("data-salary");
            log.debug("公司：{} | 职位：{} | 薪资：{} | 职位id：{}", company, positionname, salary, positionid);
        }
        Element page = document.selectFirst(".pager_container");
        if (null != page) {
            String href = page.select("a").last().attr("href");
            if (href.startsWith("java")) {
                return null;
            }
            String url = HttpOperator.recombineLink(href, req);
            if (!req.startsWith(url))
                return url;
        }
        return null;
    }

    private void page(Session session) {
        session.evaluate("window.scrollBy(0,20000)");
        try {
            TimeUnit.SECONDS.sleep(RandomUtils.nextInt(1, 3));
        } catch (InterruptedException e) {
            return;
        }
        session.click(".page_no:last-child");
        try {
            TimeUnit.SECONDS.sleep(3);
        } catch (InterruptedException e) {
            return;
        }
    }

}
