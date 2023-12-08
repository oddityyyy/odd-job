package com.odd.job.core.biz.model;

import java.io.Serializable;

/**
 * @author oddity
 * @create 2023-12-08 16:26
 */
public class IdleBeatParam implements Serializable {

    private static final long serialVersionUID = 42L;

    private int jobId;

    public IdleBeatParam(){
    }

    public IdleBeatParam(int jobId) {
        this.jobId = jobId;
    }

    public int getJobId() {
        return jobId;
    }

    public void setJobId(int jobId) {
        this.jobId = jobId;
    }
}
