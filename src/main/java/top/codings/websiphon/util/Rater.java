package top.codings.websiphon.util;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.Map;

/**
 * 计算Element的分数
 */
public class Rater {
    /**
     * 评估叶子节点的分值
     * @param element
     * @return
     */
    public static int doRate(Element element) {
        Map<String, Integer> map;
        int s;
        Elements br_elements = element.getElementsByTag("br");
        int br_size = br_elements.size();
        Elements p_elements = element.getElementsByTag("p");
        int p_size = p_elements.size();
        String htmlString = element.html();
        /**
         * 获取各种字符的个数
         */
        map = CharsCounter.getNum(htmlString);
        int chCharacter = map.get("chCharacter");
        int chPunctuationCharacter = map.get("chPunctuationCharacter");
        int otherCharacter = map.get("otherCharacter");
        //计算得分
        s = br_size * Weight.BRSecore
                + p_size * Weight.PSecore
                + chCharacter * Weight.CNCHARSCORE
                + chPunctuationCharacter * Weight.CNPNCHARSCORE
                + otherCharacter / 5;
		/*System.out.println("中文个数有--" + chCharacter);
        System.out.println("中文标点个数有--" + chPunctuationCharacter);
		System.out.println("其他字符个数有--" + otherCharacter);*/
        element.attr("score", String.valueOf(s));
        return s;
    }

    /**
     * 仅评估自身分值 不包括子孙节点分值
     * @param element
     * @return
     */
    public static int doOwnTextRate(Element element) {
        Map<String, Integer> map;
        int s;
        String textString = element.ownText();
        /**
         * 获取各种字符的个数
         */
        map = CharsCounter.getNum(textString);
        int chCharacter = map.get("chCharacter");
        int chPunctuationCharacter = map.get("chPunctuationCharacter");
        int otherCharacter = map.get("otherCharacter");
        //计算得分
        s = chCharacter * Weight.CNCHARSCORE
                + chPunctuationCharacter * Weight.CNPNCHARSCORE
                + otherCharacter / 5;

        /*System.out.println("中文个数有--" + chCharacter);
        System.out.println("中文标点个数有--" + chPunctuationCharacter);
        System.out.println("其他字符个数有--" + otherCharacter);
        System.out.println("-------------------------------------------------------");*/
        element.attr("score", String.valueOf(s));
        return s;
    }

    public static Element getMaxScoreElement(Element element) {
        if (element.childNodeSize() == 0) {
            return element;
        }
        Elements children = element.children();
        if (children == null || children.size() == 0) {
            return element;
        }
        Element maxScoreElement = children.first();
        int score = 0;
        for (Element e : children) {
            String strScore = e.attr("score");
            if (strScore == null || strScore.equals("")) {
                continue;
            }
            if (Integer.valueOf(strScore) > score) {
                maxScoreElement = e;
                score = Integer.valueOf(strScore);
            }
        }
        return getMaxScoreElement(maxScoreElement);
    }
}
