package top.codings.websiphon.util;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.nodes.Element;

public class JsoupUtils {
    public static String getPath(Element element) {
        StringBuilder path = new StringBuilder();
        String id = element.attr("id");
        String className = element.attr("class");
        path.append(element.tagName());
        if (StringUtils.isNotBlank(id)) {
            path.append("#").append(id);
        } else if (StringUtils.isNotBlank(className)) {
            path.append(".").append(className.trim().replace(" ", "."));
        }
        path.append(":nth-child(").append(element.elementSiblingIndex() + 1).append(")");
        checkPath(element, path);
        return path.toString().substring(0, path.length());
    }

    public static String getPathWithoutId(Element element) {
        StringBuilder path = new StringBuilder();
        String className = element.attr("class");
        path.append(element.tagName());
        if (StringUtils.isNotBlank(className)) {
            path.append(".").append(className.trim().replace(" ", "."));
        }
        path.append(":nth-child(").append(element.elementSiblingIndex() + 1).append(")");
        checkPath(element, path);
        return path.toString().substring(0, path.length());
    }

    private static void checkPath(Element element, StringBuilder accum) {
        if (element == null) {
            return;
        }
        if (element.parent() == null) {
            return;
        }

        if (element.parent() != null) {
            Element parentElement = element.parent();
            String tagStr = parentElement.tagName();
            if ("body".equals(tagStr)) {
                accum.insert(0, tagStr + ":nth-child(" + (parentElement.elementSiblingIndex() + 1) + ")" + ">");
                return;
            } else if (StringUtils.isNotBlank(parentElement.attr("id"))) {
                // 如果能够找到带有ID属性的父节点就停止查找
                accum.insert(0, tagStr + "#" + parentElement.attr("id") + ">");
            } else if (StringUtils.isNotBlank(parentElement.attr("class"))) {
                String classStr = parentElement.attr("class").trim().replace(" ", ".");
                accum.insert(0, tagStr + "." + classStr + ":nth-child(" + (parentElement.elementSiblingIndex() + 1) + ")" + ">");

                /**
                 * 判断class是否唯一
                 * 如果是P标签，则往上查找
                 */
                /*if ("p".equals(tagStr) || document.getElementsByClass(classStr).size() > 1) {
                    checkPath(element.parent(), accum, document);
                } else {
                    return;
                }*/

            } else {
                accum.insert(0, tagStr + ":nth-child(" + (parentElement.elementSiblingIndex() + 1) + ")" + ">");
            }
            checkPath(element.parent(), accum);
        }
    }
}
