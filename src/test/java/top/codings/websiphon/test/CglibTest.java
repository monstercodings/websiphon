package top.codings.websiphon.test;

import org.junit.jupiter.api.Test;
import top.codings.websiphon.core.plugins.AspectInfo;
import top.codings.websiphon.core.plugins.PluginFactory;
import top.codings.websiphon.core.plugins.WebPluginPro;
import top.codings.websiphon.exception.WebException;

import java.lang.reflect.Method;

public class CglibTest {
    @Test
    public void test() {
//        Ztest ztest = new Atest();
        Ztest ztest = PluginFactory.create0(new WebPluginPro() {
            @Override
            public void onBefore(AspectInfo aspectInfo, Object[] args) throws WebException {
                System.out.println("[1] 前置 -> " + args[0]);
            }

            @Override
            public Object onAfterReturning(AspectInfo aspectInfo, Object[] args, Object returnValue) {
                System.out.println("[1] 后置 -> " + returnValue);
                return returnValue == null ? "这是接口" : returnValue;
            }

            @Override
            public void onAfterThrowing(AspectInfo aspectInfo, Object[] args, Throwable throwable) {
                System.out.println("[1] 执行报错 -> " + throwable.getLocalizedMessage());
            }

            @Override
            public void onFinal(AspectInfo aspectInfo, Object[] args, Throwable throwable) {
                System.out.println("[1] 最后执行 是否有异常 -> " + (throwable != null));
            }

            @Override
            public AspectInfo[] aspectInfos() {
                try {
                    Method atestHello = Ztest.class.getMethod("hello", String.class);
                    Method atestNumber = Ztest.class.getMethod("number", int.class);
                    return new AspectInfo[]{
                            new AspectInfo(Ztest.class, atestHello),
                            new AspectInfo(Ztest.class, atestNumber),
                    };
                } catch (NoSuchMethodException e) {
                    return null;
                }
            }

            @Override
            public int index() {
                return 2;
            }
        }, Btest.class);
        ztest = PluginFactory.create0(new WebPluginPro() {
            @Override
            public void onBefore(AspectInfo aspectInfo, Object[] args) throws WebException {
                System.out.println("[2] 前置: -> " + args[0]);
            }

            @Override
            public Object onAfterReturning(AspectInfo aspectInfo, Object[] args, Object returnValue) {
                System.out.println("[2] 后置 -> " + returnValue);
                return returnValue;
            }

            @Override
            public void onAfterThrowing(AspectInfo aspectInfo, Object[] args, Throwable throwable) {
                System.out.println("[2] 执行报错 -> " + throwable.getLocalizedMessage());
            }

            @Override
            public void onFinal(AspectInfo aspectInfo, Object[] args, Throwable throwable) {
                System.out.println("[2] 最后执行 是否有异常 -> " + (throwable != null));
            }

            @Override
            public AspectInfo[] aspectInfos() {
                try {
                    Method atestHello = Ztest.class.getMethod("hello", String.class);
                    Method atestNumber = Ztest.class.getMethod("number", int.class);
                    return new AspectInfo[]{
                            new AspectInfo(Ztest.class, atestHello),
                            new AspectInfo(Ztest.class, atestNumber),
                    };
                } catch (NoSuchMethodException e) {
                    return null;
                }
            }

            @Override
            public int index() {
                return 1;
            }
        }, ztest);
        try {
            System.out.println("结果 -> " + ztest.hello("jack"));
        } catch (NullPointerException e) {
            System.out.println("空指针 -> " + e.getLocalizedMessage());
        }
    }

    public interface Ztest {
        String hello(String name);

        int number(int num);
    }


    public static class Atest implements Ztest {
        @Override
        public String hello(String name) {
            System.out.println("途中 -> " + number(10));
            return "My name is " + name;
        }

        @Override
        public int number(int num) {
            return num * 10;
        }
    }

    public static class Btest extends Atest {
        @Override
        public String hello(String name) {
            throw new NullPointerException("没名字");
        }

        @Override
        public int number(int num) {
            return num + 1;
        }
    }


}
