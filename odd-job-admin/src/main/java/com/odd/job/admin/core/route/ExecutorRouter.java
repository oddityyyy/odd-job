package com.odd.job.admin.core.route;

import com.odd.job.core.biz.model.ReturnT;
import com.odd.job.core.biz.model.TriggerParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @author oddity
 * @create 2023-12-12 14:57
 */
public abstract class ExecutorRouter {

    protected static Logger logger = LoggerFactory.getLogger(ExecutorRouter.class);

    /**
     * route address
     *
     * @param addressList
     * @return  ReturnT.content=address
     */
    public abstract ReturnT<String> route(TriggerParam triggerParam, List<String> addressList);
}
