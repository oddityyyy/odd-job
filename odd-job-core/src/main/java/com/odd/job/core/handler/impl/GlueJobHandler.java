package com.odd.job.core.handler.impl;

import com.odd.job.core.context.OddJobHelper;
import com.odd.job.core.handler.IJobHandler;

/**
 * 对IJobHandler的封装
 *
 * @author oddity
 * @create 2023-12-05 22:00
 */
public class GlueJobHandler extends IJobHandler {

    private long glueUpdatetime;
    private IJobHandler jobHandler;

    public GlueJobHandler(IJobHandler jobHandler, long glueUpdatetime) {
        this.glueUpdatetime = glueUpdatetime;
        this.jobHandler = jobHandler;
    }

    public long getGlueUpdatetime() {
        return glueUpdatetime;
    }

    @Override
    public void execute() throws Exception {
        OddJobHelper.log("----------- glue.version:"+ glueUpdatetime +" -----------");
        jobHandler.execute();
    }

    @Override
    public void init() throws Exception {
        this.jobHandler.init();
    }

    @Override
    public void destroy() throws Exception {
        this.jobHandler.destroy();
    }
}
