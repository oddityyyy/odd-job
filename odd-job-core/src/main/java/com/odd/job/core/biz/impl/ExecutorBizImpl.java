package com.odd.job.core.biz.impl;

import com.odd.job.core.biz.ExecutorBiz;
import com.odd.job.core.biz.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ExecutorBizImpl for EmbedServer
 *
 * @author oddity
 * @create 2023-12-08 18:21
 */
public class ExecutorBizImpl implements ExecutorBiz {

    private static Logger logger = LoggerFactory.getLogger(ExecutorBizImpl.class);

    @Override
    public ReturnT<String> beat() {
        return ReturnT.SUCCESS;
    }

    @Override
    public ReturnT<String> idleBeat(IdleBeatParam idleBeatParam) {

        // isRunningOrHasQueue
        boolean isRunningOrHasQueue = false;
        OddJob

    }

    @Override
    public ReturnT<String> run(TriggerParam triggerParam) {
        return null;
    }

    @Override
    public ReturnT<String> kill(KillParam killParam) {
        return null;
    }

    @Override
    public ReturnT<LogResult> log(LogParam logParam) {
        return null;
    }
}
