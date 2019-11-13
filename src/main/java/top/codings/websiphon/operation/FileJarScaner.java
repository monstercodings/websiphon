package top.codings.websiphon.operation;/*
package top.codings.websiphon.operation;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Data
@Slf4j
public class FileJarScaner implements JarScaner, LoopScanner {
    private String path;
    private boolean running;
    private List<String> paths = new LinkedList<>();
    private LoadCrawler loadCrawler;
    private Map<String, JarCrawlerContext> jars = new ConcurrentHashMap<>();
    private int sleep = 10;

    public FileJarScaner(String path, LoadCrawler loadCrawler) {
        this.path = path;
        this.loadCrawler = loadCrawler;
    }

    public FileJarScaner(String path, LoadCrawler loadCrawler, int sleep) {
        this.path = path;
        this.loadCrawler = loadCrawler;
        this.sleep = sleep;
    }

    @Override
    public void start() {
        if (StringUtils.isBlank(path)) {
            throw new RuntimeException("扫描路径不能为空");
        }
        File file = new File(path);
        if (!file.isDirectory()) {
            throw new RuntimeException("输入路径应该是目录而不是文件");
        }
        running = true;

        try {
            while (running) {
                List<String> list = new LinkedList<>();
                loop(file, list);
                if (paths.size() != list.size() || !paths.containsAll(list)) {
                    paths = list;
                    Iterator<Map.Entry<String, JarCrawlerContext>> iterator = jars.entrySet().iterator();
                    iterator.forEachRemaining(entry -> {
                        boolean exist = false;
                        JarCrawlerContext jarCrawlerContext = entry.getValue();
                        for (String path : paths) {
                            if (path.equals(jarCrawlerContext.getJarPath())) {
                                exist = true;
                                break;
                            }
                        }
                        if (!exist) {
                            if (jarCrawlerContext.getCrawler().getCrawlerContext().isRunning()) {
                                jarCrawlerContext.close();
                            }
                            iterator.remove();
                        }
                    });
                    paths.forEach(path -> {
                        jars.entrySet().iterator().forEachRemaining(entry -> {
                            JarCrawlerContext context = entry.getValue();
                            if (context.getJarPath().equals(path)) {
                                return;
                            }
                            log.info("即将加载爬虫Jar包 -> {}", path);
                            Map<String, JarCrawlerContext> tempJars = loadCrawler.load(new File(path));
                            tempJars.entrySet().iterator().forEachRemaining(inEntry -> {
                                JarCrawlerContext tempJar = inEntry.getValue();
                                log.info("{} - 加载完毕，开始启动爬虫[{}]", path, tempJar.getCrawler().getId());
                                if (tempJar.getCrawler().start().isRunning()) {
                                    jars.put(inEntry.getKey(), tempJar);
                                } else {
                                    log.error("爬虫[{}]启动失败，请检查错误日志!!!", inEntry.getKey());
                                }
                            });
                        });
                    });

                }
                TimeUnit.SECONDS.sleep(sleep);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("扫描目录时发生中断异常", e);
        } catch (Exception e) {
            log.error("扫描目录时发生未知异常", e);
        } finally {
            running = false;
        }
    }

    private void loop(File file, List<String> list) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            for (File f : files) {
                loop(f, list);
            }
        } else {
            if (file.getAbsolutePath().endsWith(".jar") && !list.contains(file.getAbsolutePath())) {
                list.add(file.getAbsolutePath());
            }
        }
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public void stop() {
    }

    @Override
    public Map<String, JarCrawlerContext> getJarCrawlers() {
        return jars;
    }
}
*/
