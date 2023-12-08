package com.odd.job.core.biz.model;

import java.io.Serializable;

/**
 * @author oddity
 * @create 2023-12-08 16:52
 */
public class LogParam implements Serializable {

    private static final long serialVersionUID = 42L;

    private long logDateTim;
    private long logId;
    private int fromLineNum;

    public LogParam() {
    }

    public LogParam(long logDateTim, long logId, int fromLineNum) {
        this.logDateTim = logDateTim;
        this.logId = logId;
        this.fromLineNum = fromLineNum;
    }

    public long getLogDateTim() {
        return logDateTim;
    }

    public void setLogDateTim(long logDateTim) {
        this.logDateTim = logDateTim;
    }

    public long getLogId() {
        return logId;
    }

    public void setLogId(long logId) {
        this.logId = logId;
    }

    public int getFromLineNum() {
        return fromLineNum;
    }

    public void setFromLineNum(int fromLineNum) {
        this.fromLineNum = fromLineNum;
    }
}
