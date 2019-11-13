package top.codings.websiphon.core.processor;

import top.codings.websiphon.bean.WebImage;
import top.codings.websiphon.bean.WebResultBase;

import java.util.List;

public interface ImageProcessor {
    /**
     * 提取正文范围内的图片
     *
     * @param result
     * @return
     */
    List<WebImage> extractImages(WebResultBase result);
}
