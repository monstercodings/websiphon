package top.codings.websiphon.test.website;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WebTemplate {
    protected String id;
    protected String reviseTitlePath;
    protected String reviseAuthorPath;
    protected String reviseCreatedAtPath;
    protected String reviseContentPath;
    protected String status;
    protected Set<String> sourceTagsSet;
    protected boolean oversea;
    protected String websiteType;
}
