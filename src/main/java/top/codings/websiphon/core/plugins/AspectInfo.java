package top.codings.websiphon.core.plugins;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.lang.reflect.Method;

@Getter
@Setter
@AllArgsConstructor
public class AspectInfo {
    private Class clazz;
    private Method method;
}
