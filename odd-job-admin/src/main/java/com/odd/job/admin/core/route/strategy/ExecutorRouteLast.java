package com.odd.job.admin.core.route.strategy;

import com.odd.job.admin.core.route.ExecutorRouter;
import com.odd.job.core.biz.model.ReturnT;
import com.odd.job.core.biz.model.TriggerParam;

import java.util.List;

/**
 * @author oddity
 * @create 2023-12-12 15:03
 */
public class ExecutorRouteLast extends ExecutorRouter {
    @Override
    public ReturnT<String> route(TriggerParam triggerParam, List<String> addressList) {
        return new ReturnT<String>(addressList.get(addressList.size()-1));
    }
}
