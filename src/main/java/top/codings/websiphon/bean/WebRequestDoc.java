package top.codings.websiphon.bean;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WebRequestDoc extends BasicWebRequest {
    private ResultDoc resultDoc = new ResultDoc();
}
