package top.codings.websiphon.bean;

import lombok.Data;

import java.util.Date;

@Data
public class WebComment {
    private String author;
    private Date createdAt;
    private String rawContent;
    private String content;
}
