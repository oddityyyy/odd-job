package com.odd.job.admin.core.route.strategy;

import com.odd.job.admin.core.route.ExecutorRouter;
import com.odd.job.core.biz.model.ReturnT;
import com.odd.job.core.biz.model.TriggerParam;

import java.util.List;
import java.util.Random;

/**
 * @author oddity
 * @create 2023-12-12 15:14
 */
public class ExecutorRouteRandom extends ExecutorRouter {

    private static Random localRandom = new Random();

    @Override
    public ReturnT<String> route(TriggerParam triggerParam, List<String> addressList) {
        String address = addressList.get(localRandom.nextInt(addressList.size()));
        return new ReturnT<String>(address);
    }
}
