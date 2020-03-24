package top.codings.websiphon.core.context;

public interface WebType {
    default String getId() {
        return this.getClass().getSimpleName();
    }
}
