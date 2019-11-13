package top.codings.websiphon.bean;

public class ReturnPoint {
    public Point point = Point.BEFORE;

    public enum Point {
        BREAK(),
        BEFORE(),
        INVOKE(),
        ERROR(),
        ;
    }
}
