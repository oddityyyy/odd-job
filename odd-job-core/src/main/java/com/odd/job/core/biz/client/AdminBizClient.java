package com.odd.job.core.biz.client;

import com.odd.job.core.biz.AdminBiz;
import com.odd.job.core.biz.model.HandleCallbackParam;
import com.odd.job.core.biz.model.RegistryParam;
import com.odd.job.core.biz.model.ReturnT;
import com.odd.job.core.util.OddJobRemotingUtil;

import java.awt.*;
import java.util.List;

/**
 * Admin Client For JobExecutor
 *
 * @author oddity
 * @create 2023-12-06 16:30
 */
public class AdminBizClient implements AdminBiz {

    public AdminBizClient() {
    }

    public AdminBizClient(String addressUrl, String accessToken) {
        this.addressUrl = addressUrl;
        this.accessToken = accessToken;

        // valid
        if (!this.addressUrl.endsWith("/")){
            this.addressUrl = this.addressUrl + "/";
        }
    }

    private String addressUrl;
    private String accessToken;
    private int timeout = 3;

    @Override
    public ReturnT<String> callback(List<HandleCallbackParam> callbackParamList) {
        return OddJobRemotingUtil.postBody(addressUrl + "api/callback", accessToken, timeout, callbackParamList, String.class);
    }

    @Override
    public ReturnT<String> registry(RegistryParam registryParam) {
        return OddJobRemotingUtil.postBody(addressUrl + "api/registry", accessToken, timeout, registryParam, String.class);
    }

    @Override
    public ReturnT<String> registryRemove(RegistryParam registryParam) {
        return OddJobRemotingUtil.postBody(addressUrl + "api/registryRemove", accessToken, timeout, registryParam, String.class);
    }
}
