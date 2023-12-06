package com.odd.job.core.biz;

import com.odd.job.core.biz.model.HandleCallbackParam;
import com.odd.job.core.biz.model.RegistryParam;
import com.odd.job.core.biz.model.ReturnT;

import java.util.List;

/**
 * @author oddity
 * @create 2023-12-06 15:33
 */
public interface AdminBiz {

    // ---------------------- callback ----------------------

    /**
     * callback
     *
     * @param callbackParamList
     * @return
     */
    public ReturnT<String> callback(List<HandleCallbackParam> callbackParamList);


    // ---------------------- registry ----------------------

    /**
     * registry
     *
     * @param registryParam
     * @return
     */
    public ReturnT<String> registry(RegistryParam registryParam);

    /**
     * registry remove
     *
     * @param registryParam
     * @return
     */
    public ReturnT<String> registryRemove(RegistryParam registryParam);
}
