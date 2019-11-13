package top.codings.websiphon.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jsoup.nodes.Element;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResultDoc {
    private Element titleEle;
    private Element createdAtEle;
    private Element authorEle;
    private Element contentEle;

    private String titleStr;
    private String createdAtStr;
    private String authorStr;
    private String contentStr;

    private String titleCss;
    private String createdAtCss;
    private String authorCss;
    private String contentCss;

    private Date createdAt;

    private List<WebImage> images = new LinkedList<>();

    private List<WebComment> comments;

    private String nowDomain;
}
