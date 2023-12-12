package com.odd.job.core.biz.client;

import com.odd.job.core.biz.ExecutorBiz;
import com.odd.job.core.biz.model.*;
import com.odd.job.core.util.OddJobRemotingUtil;

/**
 * admin api for OddJobScheduler
 *
 * @author oddity
 * @create 2023-12-12 16:42
 */
public class ExecutorBizClient implements ExecutorBiz {

    public ExecutorBizClient() {
    }

    public ExecutorBizClient(String addressUrl, String accessToken) {
        this.addressUrl = addressUrl;
        this.accessToken = accessToken;

        // valid
        if (!this.addressUrl.endsWith("/")){
            this.addressUrl = this.addressUrl + "/";
        }
    }

    private String addressUrl; //执行器地址
    private String accessToken;
    private int timeout = 3;

    @Override
    public ReturnT<String> beat() {
        return OddJobRemotingUtil.postBody(addressUrl+"beat", accessToken, timeout, "", String.class);
    }

    @Override
    public ReturnT<String> idleBeat(IdleBeatParam idleBeatParam) {
        return OddJobRemotingUtil.postBody(addressUrl+"idleBeat", accessToken, timeout, idleBeatParam, String.class);
    }

    @Override
    public ReturnT<String> run(TriggerParam triggerParam) {
        return OddJobRemotingUtil.postBody(addressUrl + "run", accessToken, timeout, triggerParam, String.class);
    }

    @Override
    public ReturnT<String> kill(KillParam killParam) {
        return OddJobRemotingUtil.postBody(addressUrl + "kill", accessToken, timeout, killParam, String.class);
    }

    @Override
    public ReturnT<LogResult> log(LogParam logParam) {
        return OddJobRemotingUtil.postBody(addressUrl + "log", accessToken, timeout, logParam, LogResult.class);
    }
}
