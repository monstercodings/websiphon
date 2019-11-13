package top.codings.websiphon.bean;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WebRequestDoc extends WebRequest {
    private ResultDoc resultDoc = new ResultDoc();
}
