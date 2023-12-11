package com.odd.job.admin.core.alarm;

import com.odd.job.admin.core.model.OddJobInfo;
import com.odd.job.admin.core.model.OddJobLog;

/**
 * @author oddity
 * @create 2023-12-11 20:31
 */
public interface JobAlarm {

    /**
     * job alarm
     *
     * @param info
     * @param jobLog
     * @return
     */
    public boolean doAlarm(OddJobInfo info, OddJobLog jobLog);
}
