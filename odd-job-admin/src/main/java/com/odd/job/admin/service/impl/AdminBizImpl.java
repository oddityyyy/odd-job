package com.odd.job.admin.service.impl;

import com.odd.job.admin.core.thread.JobCompleteHelper;
import com.odd.job.admin.core.thread.JobRegistryHelper;
import com.odd.job.core.biz.AdminBiz;
import com.odd.job.core.biz.model.HandleCallbackParam;
import com.odd.job.core.biz.model.RegistryParam;
import com.odd.job.core.biz.model.ReturnT;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 接收来自执行器
 *
 * @author oddity
 * @create 2023-12-13 1:00
 */

@Service
public class AdminBizImpl implements AdminBiz {

    @Override
    public ReturnT<String> callback(List<HandleCallbackParam> callbackParamList) {
        return JobCompleteHelper.getInstance().callback(callbackParamList);
    }

    @Override
    public ReturnT<String> registry(RegistryParam registryParam) {
        return JobRegistryHelper.getInstance().registry(registryParam);
    }

    @Override
    public ReturnT<String> registryRemove(RegistryParam registryParam) {
        return JobRegistryHelper.getInstance().registryRemove(registryParam);
    }
}
