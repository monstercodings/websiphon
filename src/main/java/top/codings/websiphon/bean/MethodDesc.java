package top.codings.websiphon.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MethodDesc {
    private String name;
    private Class[] parameterTypes;
}
