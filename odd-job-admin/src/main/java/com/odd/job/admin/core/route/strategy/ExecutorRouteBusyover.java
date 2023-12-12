package com.odd.job.admin.core.route.strategy;

import com.odd.job.admin.core.route.ExecutorRouter;
import com.odd.job.admin.core.scheduler.OddJobScheduler;
import com.odd.job.admin.core.util.I18nUtil;
import com.odd.job.core.biz.ExecutorBiz;
import com.odd.job.core.biz.model.IdleBeatParam;
import com.odd.job.core.biz.model.ReturnT;
import com.odd.job.core.biz.model.TriggerParam;

import java.util.List;

/**
 * 忙碌转移
 * 按照顺序依次进行空闲检测，第一个空闲检测成功的机器选定为目标执行器并发起调度；
 *
 * @author oddity
 * @create 2023-12-12 22:55
 */
public class ExecutorRouteBusyover extends ExecutorRouter {

    @Override
    public ReturnT<String> route(TriggerParam triggerParam, List<String> addressList) {
        StringBuffer idleBeatResultSB = new StringBuffer();
        for (String address : addressList) {
            // beat
            ReturnT<String> idleBeatResult = null;
            try {
                ExecutorBiz executorBiz = OddJobScheduler.getExecutorBiz(address);
                idleBeatResult = executorBiz.idleBeat(new IdleBeatParam(triggerParam.getJobId()));
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                idleBeatResult = new ReturnT<String>(ReturnT.FAIL_CODE, "" + e);
            }
            idleBeatResultSB.append( (idleBeatResultSB.length()>0)?"<br><br>":"")
                    .append(I18nUtil.getString("jobconf_idleBeat") + "：")
                    .append("<br>address：").append(address)
                    .append("<br>code：").append(idleBeatResult.getCode())
                    .append("<br>msg：").append(idleBeatResult.getMsg());

            // beat success
            if (idleBeatResult.getCode() == ReturnT.SUCCESS_CODE) {
                idleBeatResult.setMsg(idleBeatResultSB.toString());
                idleBeatResult.setContent(address);
                return idleBeatResult;
            }
        }

        return new ReturnT<String>(ReturnT.FAIL_CODE, idleBeatResultSB.toString());
    }
}
