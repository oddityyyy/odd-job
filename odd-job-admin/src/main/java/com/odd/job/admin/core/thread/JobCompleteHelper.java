package com.odd.job.admin.core.thread;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * job lose-monitor instance
 *
 * @author oddity
 * @create 2023-12-13 1:05
 */
public class JobCompleteHelper {

    private static Logger logger = LoggerFactory.getLogger(JobCompleteHelper.class);

    private static JobCompleteHelper instance = new JobCompleteHelper();
    public static JobCompleteHelper getInstance(){
        return instance;
    }


}
