package top.codings.websiphon.operation;/*
package top.codings.websiphon.operation;

import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class ClasspathJarScanner implements JarScaner, PackageScanner {
    private boolean autoStart = true;
    private LoadCrawler loadCrawler;
    private Map<String, JarCrawlerContext> jars = new ConcurrentHashMap<>();

    public ClasspathJarScanner() {
        this.loadCrawler = new LoadCrawler() {
            @Override
            public Object getDepend(Class clazz, String alias) {
                return null;
            }
        };
    }

    public ClasspathJarScanner(LoadCrawler loadCrawler) {
        this.loadCrawler = loadCrawler;
    }

    public ClasspathJarScanner(boolean autoStart, LoadCrawler loadCrawler) {
        this.autoStart = autoStart;
        this.loadCrawler = loadCrawler;
    }

    @Override
    public Map<String, JarCrawlerContext> getJarCrawlers() {
        return jars;
    }

    @Override
    public Map<String, JarCrawlerContext> scan(String[] packages) {
        Map<String, JarCrawlerContext> jarMap = loadCrawler.load(packages);
        Map<String, JarCrawlerContext> last = new HashMap<>();
        jarMap.forEach((id, jarCrawler) -> Optional.ofNullable(jars.get(id) == null ? "" : null).ifPresent(none -> last.put(id, jarCrawler)));
        if (autoStart) {
            last.forEach((id, jarCrawlerContext) -> {
                jarCrawlerContext.getCrawler().start();
                if (jarCrawlerContext.getCrawler().getContext().isRunning()) {
                    jars.put(jarCrawlerContext.getCrawler().getId(), jarCrawlerContext);
                } else {
                    log.warn("爬虫[{}]启动失败，请检查错误日志!!!", id);
                }
            });
        }
        return last;
    }
}
*/
