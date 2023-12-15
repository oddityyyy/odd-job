package com.odd.job.admin.core.scheduler;

import com.odd.job.admin.core.conf.OddJobAdminConfig;
import com.odd.job.admin.core.thread.*;
import com.odd.job.admin.core.util.I18nUtil;
import com.odd.job.core.biz.ExecutorBiz;
import com.odd.job.core.biz.client.ExecutorBizClient;
import com.odd.job.core.enums.ExecutorBlockStrategyEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 调度器 - 初始化后负责控制调度中心各个后台线程
 *
 * @author oddity
 * @create 2023-12-11 19:38
 */

public class OddJobScheduler {

    private static final Logger logger = LoggerFactory.getLogger(OddJobScheduler.class);

    public void init() throws Exception {
        // init i18n
        initI18n();

        // admin trigger poll start
        JobTriggerPoolHelper.toStart(); // 初始化快慢线程池

        // admin registry monitor run
        JobRegistryHelper.getInstance().start(); // 启动自动注册，启动接收注册请求

        // admin fail-monitor run
        JobFailMonitorHelper.getInstance().start(); // 扫描执行失败任务，并进行后续处理

        // admin lose-monitor run ( depend on JobTriggerPoolHelper )
        JobCompleteHelper.getInstance().start(); // 处理丢失任务（死任务，一直在运行中）、统一处理回调

        // admin log report start
        JobLogReportHelper.getInstance().start(); // 刷新日志报表，删除过期日志

        // start-schedule ( depend on JobTriggerPoolHelper )
        JobScheduleHelper.getInstance().start(); // 时间轮调度

        logger.info(">>>>>>>>> init odd-job admin success.");
    }

    public void destroy() throws Exception {

        // stop-schedule
        JobScheduleHelper.getInstance().toStop();

        // admin log report stop
        JobLogReportHelper.getInstance().toStop();

        // admin lose-monitor stop
        JobCompleteHelper.getInstance().toStop();

        // admin fail-monitor stop
        JobFailMonitorHelper.getInstance().toStop();

        // admin registry stop
        JobRegistryHelper.getInstance().toStop();

        // admin trigger pool stop
        JobTriggerPoolHelper.toStop();

    }

    // ---------------------- I18n ----------------------

    private void initI18n(){
        for (ExecutorBlockStrategyEnum item : ExecutorBlockStrategyEnum.values()){
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
