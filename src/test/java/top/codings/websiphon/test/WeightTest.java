package top.codings.websiphon.test;

import lombok.extern.slf4j.Slf4j;
import top.codings.websiphon.core.proxy.bean.WebProxy;

import java.util.*;

@Slf4j
public class WeightTest {
    public static void main(String[] args) {
        int number = 1000;//假设1000个订单数
        List<WebProxy> list = Arrays.asList(
                new WebProxy(null, 0, 1),
                new WebProxy(null, 0, 1),
                new WebProxy(null, 0, 1),
                new WebProxy(null, 0, 1)
                );
        for (int i = 0; i < number; i++) {
            select(list);
        }
        for (WebProxy proxy : list) {
            log.debug("{}", proxy.invokedCount());
        }
    }

    public static WebProxy select(Collection<WebProxy> proxies) {
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
