package top.codings.websiphon.core.proxy.strategy;

import org.apache.commons.collections.CollectionUtils;
import top.codings.websiphon.core.proxy.bean.WebProxy;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * 权重策略
 */
public class WeightProxyStrategy implements ProxyStrategy {
    @Override
    public WebProxy select(Collection<WebProxy> proxies) {
        if (CollectionUtils.isEmpty(proxies)) {
            return null;
        }
        WebProxy[] weight = proxies.toArray(new WebProxy[0]);
        int[] count = new int[weight.length];
        for (int i = 0; i < weight.length; i++) {
            count[i] = weight[i].invokedCount();
        }
        //当前权重
        Double[] current = new Double[weight.length];
        for (int w = 0; w < weight.length; w++) {
            current[w] = Double.valueOf(weight[w].getWeight()) / (count[w] == 0 ? 1 : count[w]);
        }
        int index = 0;
        Double currentMax = current[0];
        for (int d = 1; d < current.length; d++) {
            //考虑全等的情况
            Boolean isTrue = true;
            while (isTrue) {
                Set set = new HashSet();
                for (Double c : current) {
                    set.add(c);
                }
                if (set.size() == 1) {//代表全等
                    for (int e = 0; e < current.length; e++) {
                        current[e] = current[e] * Math.random();
                    }
                } else {
                    isTrue = false;
                }
            }
            //比较所有的数,寻找出下标最大的哪一位
            if (currentMax < current[d]) {
                currentMax = current[d];
                index = d;
            }
        }
        weight[index].invoked();
        return weight[index];
    }
}
