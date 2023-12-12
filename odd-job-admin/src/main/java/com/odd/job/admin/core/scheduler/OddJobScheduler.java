package com.odd.job.admin.core.scheduler;

import com.odd.job.admin.core.conf.OddJobAdminConfig;
import com.odd.job.admin.core.util.I18nUtil;
import com.odd.job.core.biz.ExecutorBiz;
import com.odd.job.core.biz.client.ExecutorBizClient;
import com.odd.job.core.enums.ExecutorBlockStrategyEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 调度器
 *
 * @author oddity
 * @create 2023-12-11 19:38
 */

//TODO
public class OddJobScheduler {

    private static final Logger logger = LoggerFactory.getLogger(OddJobScheduler.class);

    public void init() throws Exception {
        // init i18n
        initI18n();
    }

    // ---------------------- I18n ----------------------

    private void initI18n(){
        for (ExecutorBlockStrategyEnum item :ExecutorBlockStrategyEnum.values()){
            item.setTitle(I18nUtil.getString("jobconf_block_".concat(item.name())));
        }
    }

    // ---------------------- executor-client ----------------------

    // 一个机器地址对应一个ExecutorBiz
    private static ConcurrentMap<String, ExecutorBiz> executorBizRepository = new ConcurrentHashMap<String, ExecutorBiz>();

    public static ExecutorBiz getExecutorBiz(String address) throws Exception {
        // valid
        if (address == null || address.trim().length() == 0){
            return null;
        }

        // load-cache
        address = address.trim();
        ExecutorBiz executorBiz = executorBizRepository.get(address);
        if (executorBiz != null){
            return executorBiz;
        }

        // set-cache
        executorBiz = new ExecutorBizClient(address, OddJobAdminConfig.getAdminConfig().getAccessToken());

        executorBizRepository.put(address, executorBiz);
        return executorBiz;
    }
}
