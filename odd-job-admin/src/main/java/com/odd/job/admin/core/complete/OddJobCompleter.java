package com.odd.job.admin.core.complete;

import com.odd.job.admin.core.conf.OddJobAdminConfig;
import com.odd.job.admin.core.model.OddJobInfo;
import com.odd.job.admin.core.model.OddJobLog;
import com.odd.job.admin.core.thread.JobTriggerPoolHelper;
import com.odd.job.admin.core.trigger.TriggerTypeEnum;
import com.odd.job.admin.core.util.I18nUtil;
import com.odd.job.core.biz.model.ReturnT;
import com.odd.job.core.context.OddJobContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;

/**
 * @author oddity
 * @create 2023-12-13 1:02
 */
public class OddJobCompleter {

    private static Logger logger = LoggerFactory.getLogger(OddJobCompleter.class);

    /**
     * common fresh handle entrance (limit only once)
     *
     * @param oddJobLog
     * @return
     */
    public static int updateHandleInfoAndFinish(OddJobLog oddJobLog) {

        // finish
        finishJob(oddJobLog);

        // text最大64kb 避免长度过长
        if (oddJobLog.getHandleMsg().length() > 15000) {
            oddJobLog.setHandleMsg( oddJobLog.getHandleMsg().substring(0, 15000) );
        }

        // fresh handle
        return OddJobAdminConfig.getAdminConfig().getOddJobLogDao().updateHandleInfo(oddJobLog);
    }

    /**
     * do somethind to finish job
     */
    private static void finishJob(OddJobLog oddJobLog){

        // 1、handle success, to trigger child job
        String triggerChildMsg = null;
        if (OddJobContext.HANDLE_CODE_SUCCESS == oddJobLog.getHandleCode()) {
            OddJobInfo oddJobInfo = OddJobAdminConfig.getAdminConfig().getOddJobInfoDao().loadById(oddJobLog.getJobId());
            if (oddJobInfo!=null && oddJobInfo.getChildJobId()!=null && oddJobInfo.getChildJobId().trim().length()>0) {
                triggerChildMsg = "<br><br><span style=\"color:#00c0ef;\" > >>>>>>>>>>>"+ I18nUtil.getString("jobconf_trigger_child_run") +"<<<<<<<<<<< </span><br>";

                String[] childJobIds = oddJobInfo.getChildJobId().split(",");
                for (int i = 0; i < childJobIds.length; i++) {
                    int childJobId = (childJobIds[i]!=null && childJobIds[i].trim().length()>0 && isNumeric(childJobIds[i]))?Integer.valueOf(childJobIds[i]):-1;
                    if (childJobId > 0) {

                        JobTriggerPoolHelper.trigger(childJobId, TriggerTypeEnum.PARENT, -1, null, null, null);
                        ReturnT<String> triggerChildResult = ReturnT.SUCCESS;

                        // add msg
                        triggerChildMsg += MessageFormat.format(I18nUtil.getString("jobconf_callback_child_msg1"),
                                (i+1),
                                childJobIds.length,
                                childJobIds[i],
                                (triggerChildResult.getCode()== ReturnT.SUCCESS_CODE?I18nUtil.getString("system_success"):I18nUtil.getString("system_fail")),
                                triggerChildResult.getMsg());
                    } else {
                        triggerChildMsg += MessageFormat.format(I18nUtil.getString("jobconf_callback_child_msg2"),
                                (i+1),
                                childJobIds.length,
                                childJobIds[i]);
                    }
                }

            }
        }

        if (triggerChildMsg != null) {
            oddJobLog.setHandleMsg( oddJobLog.getHandleMsg() + triggerChildMsg );
        }

        // 2、fix_delay trigger next
        // on the way

    }

    // 判断是否能将字符转化为整型
    private static boolean isNumeric(String str){
        try {
            int result = Integer.valueOf(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
