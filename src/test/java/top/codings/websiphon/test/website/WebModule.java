package top.codings.websiphon.test.website;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
public class WebModule {
    protected String id;
    protected String domain;
    protected String homepage;
    protected boolean oversea;
    protected int level;
    protected short maxDepth;
    protected String websiteType;
    protected List<String> websiteModuleTemplatesId;
    protected List<WebTemplate> webTemplates;
    protected Set<String> sourceTags;
    protected int forceLevel;

    public WebModule(String id, String domain, String homepage, boolean oversea, int level) {
        this.id = id;
        this.domain = domain;
        this.homepage = homepage;
        this.oversea = oversea;
        this.level = level;
    }
}
