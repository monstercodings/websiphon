package top.codings.websiphon.util;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Optional;
import java.util.Properties;

@Slf4j
final class PropUtils {
    public static String ENV;
    private static Properties properties = new Properties();
    private static Properties propertiesOther = new Properties();
    private static volatile boolean init = false;

    protected static void init() {
        try {
            properties.load(PropUtils.class.getResourceAsStream("/application.properties"));
            if (null != ENV) {
                propertiesOther.load(PropUtils.class.getResourceAsStream("/application-" + ENV + ".properties"));
            }
            init = true;
        } catch (IOException e) {
//            throw new RuntimeException("读取resource下的配置文件失败", e);
            log.error("读取resource下的配置文件失败 -> {}", e.getLocalizedMessage());
        }
    }

    protected static String getProperty(String key) {
        if (!init) {
            synchronized (PropUtils.class) {
                if (!init) {
                    init();
                }
            }
        }
        return Optional.ofNullable(properties.getProperty(key)).orElseGet(() -> propertiesOther.getProperty(key));
    }
}
