package top.codings.websiphon.operation;

import java.util.List;
import java.util.Map;

public interface PackageScanner {
    Map<String, JarCrawlerContext> scan(String[] packages);
}
