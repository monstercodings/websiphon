package top.codings.websiphon.core.context;

import java.util.UUID;

public interface WebType {
    default String getId() {
        return UUID.randomUUID().toString();
    }
}
