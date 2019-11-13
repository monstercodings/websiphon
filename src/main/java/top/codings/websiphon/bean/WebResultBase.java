package top.codings.websiphon.bean;

import com.alibaba.fastjson.JSONObject;
import com.google.common.net.MediaType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WebResultBase {
    protected String url;
    protected Document document;
    protected JSONObject json;
    protected MediaType mediaType;
}
