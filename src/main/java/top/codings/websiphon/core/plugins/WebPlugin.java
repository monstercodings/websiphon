package top.codings.websiphon.core.plugins;

import top.codings.websiphon.core.context.WebType;
import top.codings.websiphon.exception.WebException;

/**
 * AOP插件
 */
public interface WebPlugin extends WebType {
    /**
     * 插件前置拦截
     *
     * @param aspectInfo 真正的执行对象相关Class信息
     * @param args       方法需要传入的参数
     * @throws WebException 抛出异常代表不执行目标方法
     */
    void onBefore(AspectInfo aspectInfo, Object[] args) throws WebException;

    /**
     * 当目标方法正常执行返回后调用后置拦截
     *
     * @param aspectInfo  真正的执行对象相关Class信息
     * @param args        入参
     * @param returnValue 目标方法返回值
     * @return
     */
    Object onAfterReturning(AspectInfo aspectInfo, Object[] args, Object returnValue);

    /**
     * 当目标方法发生异常时调用异常拦截
     *
     * @param aspectInfo 真正的执行对象相关Class信息
     * @param args       入参
     * @param throwable  目标方法抛出的异常
     */
    void onAfterThrowing(AspectInfo aspectInfo, Object[] args, Throwable throwable);

    /**
     * 无论目标方法是否执行成功都调用最终拦截
     *
     * @param aspectInfo 真正的执行对象相关Class信息
     * @param args       入参
     * @param throwable  目标方法抛出的异常
     */
    void onFinal(AspectInfo aspectInfo, Object[] args, Throwable throwable);

    /**
     * 需要代理的对象和方法
     *
     * @return
     */
    AspectInfo[] aspectInfos();

    /**
     * 在过滤器链中的执行顺序
     * 数字越小越先执行
     *
     * @return
     */
    int index();

    default void init() {

    }

    default void close() {

    }
}
