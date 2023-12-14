package com.odd.job.admin.core.thread;

import com.odd.job.admin.core.complete.OddJobCompleter;
import com.odd.job.admin.core.conf.OddJobAdminConfig;
import com.odd.job.admin.core.model.OddJobLog;
import com.odd.job.admin.core.util.I18nUtil;
import com.odd.job.core.biz.model.HandleCallbackParam;
import com.odd.job.core.biz.model.ReturnT;
import com.odd.job.core.util.DateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.List;
import java.util.concurrent.*;

/**
 * job lose-monitor instance
 *
 * @author oddity
 * @create 2023-12-13 1:05
 */
public class JobCompleteHelper {

    private static Logger logger = LoggerFactory.getLogger(JobCompleteHelper.class);

    private static JobCompleteHelper instance = new JobCompleteHelper();
    public static JobCompleteHelper getInstance(){
        return instance;
    }

    // ---------------------- monitor ----------------------
    // 监测丢失任务，并处理回调

    private ThreadPoolExecutor callbackThreadPool = null;
    private Thread monitorThread;
    private volatile boolean toStop = false;

    public void start(){

        // for callback 处理回调结果的线程池
        callbackThreadPool = new ThreadPoolExecutor(
                2,
                20,
                30L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(3000),
                new ThreadFactory() {
                    @Override
                    public Thread newThread(Runnable r) {
                        return new Thread(r, "odd-job, admin JobLosedMonitorHelper-callbackThreadPool-" + r.hashCode());
                    }
                },
                new RejectedExecutionHandler() {
                    @Override
                    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                        r.run();
                        logger.warn(">>>>>>>>>>> odd-job, callback too fast, match threadpool rejected handler(run now).");
                    }
                });


        // for monitor
        monitorThread = new Thread(new Runnable() {

            @Override
            public void run() {

                // wait for JobTriggerPoolHelper-init
                try {
                    TimeUnit.MILLISECONDS.sleep(50);
                } catch (InterruptedException e) {
                    if (!toStop) {
                        logger.error(e.getMessage(), e);
                    }
                }

                // monitor
                while (!toStop) {
                    try {
                        // 任务结果丢失处理：调度记录停留在 "运行中" 状态超过10min，且对应执行器心跳注册失败不在线，则将本地调度主动标记失败；
                        Date losedTime = DateUtil.addMinutes(new Date(), -10);
                        List<Long> losedJobIds  = OddJobAdminConfig.getAdminConfig().getOddJobLogDao().findLostJobIds(losedTime);

                        if (losedJobIds!=null && losedJobIds.size()>0) {
                            for (Long logId: losedJobIds) {

                                OddJobLog jobLog = new OddJobLog();
                                jobLog.setId(logId);

                                jobLog.setHandleTime(new Date());
                                jobLog.setHandleCode(ReturnT.FAIL_CODE);
                                jobLog.setHandleMsg( I18nUtil.getString("joblog_lost_fail") );

                                OddJobCompleter.updateHandleInfoAndFinish(jobLog);
                            }

                        }
                    } catch (Exception e) {
                        if (!toStop) {
                            logger.error(">>>>>>>>>>> odd-job, job fail monitor thread error:{}", e);
                        }
                    }

                    try {
                        TimeUnit.SECONDS.sleep(60);
                    } catch (Exception e) {
                        if (!toStop) {
                            logger.error(e.getMessage(), e);
                        }
                    }

                }

                logger.info(">>>>>>>>>>> odd-job, JobLosedMonitorHelper stop");

            }
        });
        monitorThread.setDaemon(true);
        monitorThread.setName("odd-job, admin JobLosedMonitorHelper");
        monitorThread.start();
    }

    public void toStop(){
        toStop = true;

        // stop registryOrRemoveThreadPool
        callbackThreadPool.shutdownNow();

        // stop monitorThread (interrupt and wait)
        monitorThread.interrupt();
        try {
            monitorThread.join();
        } catch (InterruptedException e) {
            logger.error(e.getMessage(), e);
        }
    }

    // ---------------------- helper ----------------------

    public ReturnT<String> callback(List<HandleCallbackParam> callbackParamList) {

        callbackThreadPool.execute(new Runnable() { //一个任务一个jobThread相应的一个任务一个callbackThread，下面队列中堆积着待执行的任务队列/待回调的任务队列按时间排序
            @Override
            public void run() {
                for (HandleCallbackParam handleCallbackParam : callbackParamList) {
                    ReturnT<String> callbackResult = callback(handleCallbackParam);
                    logger.debug(">>>>>>>>> JobApiController.callback {}, handleCallbackParam={}, callbackResult={}",
                            (callbackResult.getCode()== ReturnT.SUCCESS_CODE?"success":"fail"), handleCallbackParam, callbackResult);
                }
            }
        });

        return ReturnT.SUCCESS;
    }

    private ReturnT<String> callback(HandleCallbackParam handleCallbackParam) {
        // valid log item
        OddJobLog log = OddJobAdminConfig.getAdminConfig().getOddJobLogDao().load(handleCallbackParam.getLogId());
        if (log == null) {
            return new ReturnT<String>(ReturnT.FAIL_CODE, "log item not found.");
        }
        if (log.getHandleCode() > 0) { // 已经处理过
            return new ReturnT<String>(ReturnT.FAIL_CODE, "log repeate callback.");     // avoid repeat callback, trigger child job etc
        }

        //TODO 此处加入时间校验
        /**
        if (log.getTriggerTime().getTime() != handleCallbackParam.getLogDateTim()) {
            return new ReturnT<String>(ReturnT.FAIL_CODE, "log item is incompatible with current Admin.");
        }
         */

        // handle msg
        StringBuffer handleMsg = new StringBuffer();
        if (log.getHandleMsg() != null) {
            handleMsg.append(log.getHandleMsg()).append("<br>");
        }
        if (handleCallbackParam.getHandleMsg() != null) {
            handleMsg.append(handleCallbackParam.getHandleMsg());
        }

        // success, save log
        log.setHandleTime(new Date());
        log.setHandleCode(handleCallbackParam.getHandleCode());
        log.setHandleMsg(handleMsg.toString());
        OddJobCompleter.updateHandleInfoAndFinish(log);

        return ReturnT.SUCCESS;
    }
}
