package com.odd.job.core.thread;

import com.odd.job.core.biz.AdminBiz;
import com.odd.job.core.biz.model.RegistryParam;
import com.odd.job.core.biz.model.ReturnT;
import com.odd.job.core.enums.RegistryConfig;
import com.odd.job.core.executor.OddJobExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * @author oddity
 * @create 2023-12-10 15:51
 */
public class ExecutorRegistryThread {

    private static Logger logger = LoggerFactory.getLogger(ExecutorRegistryThread.class);

    private static ExecutorRegistryThread instance = new ExecutorRegistryThread();
    public static ExecutorRegistryThread getInstance(){
        return instance;
    }

    private Thread registryThread;
    private volatile boolean toStop = false;

    public void start(final String appname, final String address){

        // valid
        if (appname == null || appname.trim().length() == 0){
            logger.warn(">>>>>>>>>>> odd-job, executor registry config fail, appname is null.");
            return;
        }

        if (OddJobExecutor.getAdminBizList() == null){
            logger.warn(">>>>>>>>>>> odd-job, executor registry config fail, adminAddresses is null.");
            return;
        }

        registryThread = new Thread(new Runnable() {
            @Override
            public void run() {

                // registry
                while (!toStop){
                    try {
                        RegistryParam registryParam = new RegistryParam(RegistryConfig.RegistType.EXECUTOR.name(), appname, address);
                        // 给每个调度中心都要注册
                        for (AdminBiz adminBiz : OddJobExecutor.getAdminBizList()){
                            try {
                                ReturnT<String> registryResult = adminBiz.registry(registryParam);
                                if (registryResult != null && ReturnT.SUCCESS_CODE == registryResult.getCode()){
                                    registryResult = ReturnT.SUCCESS;
                                    logger.debug(">>>>>>>>>>> odd-job registry success, registryParam:{}, registryResult:{}", new Object[]{registryParam, registryResult});
                                    break;
                                } else {
                                    logger.info(">>>>>>>>>>> odd-job registry fail, registryParam:{}, registryResult:{}", new Object[]{registryParam, registryResult});
                                }
                            } catch (Exception e) {
                                logger.info(">>>>>>>>>>> odd-job registry error, registryParam:{}", registryParam, e);
                            }
                        }
                    } catch (Exception e) {
                        if (!toStop){
                            logger.error(e.getMessage(), e);
                        }
                    }

                    // 不断循环注册，保持一个心跳连接
                    try {
                        if (!toStop){
                            TimeUnit.SECONDS.sleep(RegistryConfig.BEAT_TIMEOUT); //30s
                        }
                    } catch (InterruptedException e) {
                        if (!toStop){
                            logger.warn(">>>>>>>>>>> odd-job, executor registry thread interrupted, error msg:{}", e.getMessage());
                        }
                    }
                }

                // registry remove 执行器注册线程stop后，已经向调度中心注册了的执行器需要取消注册
                try {
                    RegistryParam registryParam = new RegistryParam(RegistryConfig.RegistType.EXECUTOR.name(), appname, address);
                    for (AdminBiz adminBiz : OddJobExecutor.getAdminBizList()){
                        try {
                            ReturnT<String> registryResult = adminBiz.registryRemove(registryParam);
                            if (registryResult != null && ReturnT.SUCCESS_CODE == registryResult.getCode()){
                                registryResult = ReturnT.SUCCESS;
                                logger.info(">>>>>>>>>>> odd-job registry-remove success, registryParam:{}, registryResult:{}", new Object[]{registryParam, registryResult});
                                break;
                            } else {
                                logger.info(">>>>>>>>>>> odd-job registry-remove fail, registryParam:{}, registryResult:{}", new Object[]{registryParam, registryResult});
                            }
                        } catch (Exception e) {
                            if (!toStop) {
                                logger.info(">>>>>>>>>>> odd-job registry-remove error, registryParam:{}", registryParam, e);
                            }
                        }
                    }
                } catch (Exception e) {
                    if (!toStop) {
                        logger.error(e.getMessage(), e);
                    }
                }
                logger.info(">>>>>>>>>>> odd-job, executor registry thread destroy.");
            }
        });

        registryThread.setDaemon(true);
        registryThread.setName("odd-job, executor ExecutorRegistryThread");
        registryThread.start();
    }

    public void toStop(){
        toStop = true;

        // interrupt and wait
        if (registryThread != null){
            registryThread.interrupt();
            try {
                registryThread.join();
            } catch (InterruptedException e){
                logger.error(e.getMessage(), e);
            }
        }
    }
}
