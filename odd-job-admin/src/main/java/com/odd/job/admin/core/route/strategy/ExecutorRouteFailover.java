package com.odd.job.admin.core.route.strategy;

import com.odd.job.admin.core.route.ExecutorRouter;
import com.odd.job.admin.core.scheduler.OddJobScheduler;
import com.odd.job.admin.core.util.I18nUtil;
import com.odd.job.core.biz.ExecutorBiz;
import com.odd.job.core.biz.model.ReturnT;
import com.odd.job.core.biz.model.TriggerParam;

import java.util.List;

/**
 * 故障转移
 * 如果某一台执行器发生故障，该策略支持自动进行Failover切换到一台正常的执行器机器并且完成调度请求流程。
 * 按照顺序依次进行心跳检测，第一个心跳检测成功的机器选定为目标执行器并发起调度；
 *
 * @author oddity
 * @create 2023-12-12 16:23
 */
public class ExecutorRouteFailover extends ExecutorRouter {

    @Override
    public ReturnT<String> route(TriggerParam triggerParam, List<String> addressList) {

        StringBuffer beatResultSB = new StringBuffer();
        for (String address : addressList) {
            // beat
            ReturnT<String> beatResult = null;
            try {
                ExecutorBiz executorBiz = OddJobScheduler.getExecutorBiz(address);
                beatResult = executorBiz.beat();
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                beatResult = new ReturnT<String>(ReturnT.FAIL_CODE, "" + e);
            }
            beatResultSB.append( (beatResultSB.length()>0)?"<br><br>":"")
                    .append(I18nUtil.getString("jobconf_beat") + "：")
                    .append("<br>address：").append(address)
                    .append("<br>code：").append(beatResult.getCode())
                    .append("<br>msg：").append(beatResult.getMsg());

            // beat success
            if (beatResult.getCode() == ReturnT.SUCCESS_CODE) {

                beatResult.setMsg(beatResultSB.toString());
                beatResult.setContent(address);
                return beatResult;
            }
        }
        return new ReturnT<String>(ReturnT.FAIL_CODE, beatResultSB.toString());
    }
}
