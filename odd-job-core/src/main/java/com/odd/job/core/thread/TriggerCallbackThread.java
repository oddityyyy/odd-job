package com.odd.job.core.thread;

import com.odd.job.core.biz.AdminBiz;
import com.odd.job.core.biz.model.HandleCallbackParam;
import com.odd.job.core.biz.model.ReturnT;
import com.odd.job.core.context.OddJobContext;
import com.odd.job.core.context.OddJobHelper;
import com.odd.job.core.enums.RegistryConfig;
import com.odd.job.core.log.executor.OddJobExecutor;
import com.odd.job.core.log.OddJobFileAppender;
import com.odd.job.core.util.FileUtil;
import com.odd.job.core.util.JdkSerializeTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * @author oddity
 * @create 2023-12-07 17:05
 */
public class TriggerCallbackThread {

    private static Logger logger = LoggerFactory.getLogger(TriggerCallbackThread.class);

    private static TriggerCallbackThread instance = new TriggerCallbackThread();
    public static TriggerCallbackThread getInstance(){
        return instance;
    }

    /**
     * job results callback queue
     */
    private LinkedBlockingQueue<HandleCallbackParam> callBackQueue = new LinkedBlockingQueue<HandleCallbackParam>();
    public static void pushCallBack(HandleCallbackParam callback){
        getInstance().callBackQueue.add(callback);
        logger.debug(">>>>>>>>>>> odd-job, push callback request, logId:{}", callback.getLogId());
    }

    /**
     * callback thread
     */
    private Thread triggerCallbackThread;
    private Thread triggerRetryCallbackThread;
    private volatile boolean toStop = false;

    // 开启两个线程
    public void start() {

        // valid
        if (OddJobExecutor.getAdminBizList() == null){
            logger.warn(">>>>>>>>>>> odd-job, executor callback config fail, adminAddresses is null.");
            return;
        }

        // callback
        triggerCallbackThread = new Thread(new Runnable() {
            @Override
            public void run() {

                // normal callback
                while(!toStop){
                    try {
                        HandleCallbackParam callback = getInstance().callBackQueue.take();
                        if (callback != null){

                            // callback list param
                            List<HandleCallbackParam> callbackParamList = new ArrayList<HandleCallbackParam>();
                            int drainToNum = getInstance().callBackQueue.drainTo(callbackParamList);
                            callbackParamList.add(callback);

                            // callback, will retry if error
                            if (callbackParamList != null && callbackParamList.size() > 0){
                                doCallback(callbackParamList);
                            }
                        }
                    } catch (Exception e) {
                        if (!toStop){
                            logger.error(e.getMessage(), e);
                        }
                    }
                }

                // last callback stop的时候回调残余的
                try {
                    List<HandleCallbackParam> callbackParamList = new ArrayList<HandleCallbackParam>();
                    int drainToNum = getInstance().callBackQueue.drainTo(callbackParamList);
                    if (callbackParamList != null && callbackParamList.size() > 0){
                        doCallback(callbackParamList);
                    }
                } catch (Exception e) {
                    if (!toStop){
                        logger.error(e.getMessage(), e);
                    }
                }
                logger.info(">>>>>>>>>>> odd-job, executor callback thread destroy.");
            }
        });
        triggerCallbackThread.setDaemon(true);
        triggerCallbackThread.setName("odd-job, executor TriggerCallbackThread");
        triggerCallbackThread.start();

        // retry
        triggerRetryCallbackThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!toStop){
                    try {
                        retryFailCallbackFile();
                    } catch (Exception e) {
                        if (!toStop){
                            logger.error(e.getMessage(), e);
                        }
                    }
                    try {
                        TimeUnit.SECONDS.sleep(RegistryConfig.BEAT_TIMEOUT); //30s
                    } catch (InterruptedException e) {
                        if (!toStop){
                            logger.error(e.getMessage(), e);
                        }
                    }
                }
                logger.info(">>>>>>>>>>> odd-job, executor retry callback thread destroy.");
            }
        });
        triggerRetryCallbackThread.setDaemon(true);
        triggerRetryCallbackThread.start();
    }

    public void toStop(){
        toStop = true;
        // stop callback, interrupt and wait
        if (triggerCallbackThread != null){
            triggerCallbackThread.interrupt();
            try {
                triggerCallbackThread.join();
            } catch (InterruptedException e) {
                logger.error(e.getMessage(), e);
            }
        }

        // stop retry, interrupt and wait
        if (triggerRetryCallbackThread != null){
            triggerRetryCallbackThread.interrupt();
            try {
                triggerRetryCallbackThread.join();
            } catch (InterruptedException e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    //TODO 此处若调度中心集群，这样的回调方式会不会有问题
    // （细看admin的代码，有可能是adminBiz.callback(callbackParamList)远程发送请求的时候，amdin端做了校验，
    // 不是本调度中心调度的任务不予返回成功结果）
    //答：admin确实做了校验，它首先会根据
    //// valid log item
    // OddJobLog log = OddJobAdminConfig.getAdminConfig().getOddJobLogDao().load(handleCallbackParam.getLogId());
    // 检查log是否在本地数据库中存在，而数据库odd_job又是持久化在调度中心机器上的，不是本调度中心调度的任务不会在本调度中心的数据库中存放job_log
    // 最终会返回失败结果，会报"log item not found."错误
    //TODO 但是如果是不同调度中心在同一时刻同时调度同一个jobId的任务，仍然会有并发执行会造成回调数据返回错乱的风险，但是实际使用中这种使用场景很少见，也说的过去
    //TODO 反正我觉得这块的实现还是有瑕疵，任务数量少的话容易撞车，admin端还可以加入更为严格的校验逻辑（验证代表admin身份的参数<Trigger_time/LogDateTime >）
    /**
     * do callback, will retry if error
     *
     * 找到第一个可用的调度中心，执行批量任务回调
     *
     * @param callbackParamList
     */
    private void doCallback(List<HandleCallbackParam> callbackParamList){
        boolean callbackRet = false;
        // callback, will retry if error
        for (AdminBiz adminBiz : OddJobExecutor.getAdminBizList()){
            try {
                ReturnT<String> callbackResult = adminBiz.callback(callbackParamList);
                if (callbackResult != null && ReturnT.SUCCESS_CODE == callbackResult.getCode()){
                    callbackLog(callbackParamList, "<br>----------- odd-job job callback finish.");
                    callbackRet = true;
                    break;
                } else {
                    callbackLog(callbackParamList, "<br>----------- odd-job job callback fail, callbackResult:" + callbackResult);
                }
            } catch (Exception e) {
                callbackLog(callbackParamList, "<br>----------- odd-job job callback error, errorMsg:" + e.getMessage());
            }
        }
        if (!callbackRet){
            appendFailCallbackFile(callbackParamList);
        }
    }

    /**
     * callback log
     *
     * 批量写日志，List<HandleCallbackParam>中的每一个任务单独一个文件
     */
    private void callbackLog(List<HandleCallbackParam> callbackParamList, String logContent){
        for (HandleCallbackParam callbackParam : callbackParamList){
            String logFileName = OddJobFileAppender.makeLogFileName(new Date(callbackParam.getLogDateTim()), callbackParam.getLogId());
            OddJobContext.setOddJobContext(new OddJobContext(
                    -1,
                    null,
                    logFileName,
                    -1,
                    -1));
            OddJobHelper.log(logContent);
        }
    }


    // ---------------------- fail-callback file ----------------------

    private static String failCallbackFilePath = OddJobFileAppender.getLogPath().concat(File.separator).concat("callbacklog").concat(File.separator);
    private static String failCallbackFileName = failCallbackFilePath.concat("odd-job-callback-{x}").concat(".log");

    // 直接把callbackParamList写入到日志文件
    private void appendFailCallbackFile(List<HandleCallbackParam> callbackParamList){
        // valid
        if (callbackParamList == null || callbackParamList.size() == 0){
            return;
        }

        // append file
        byte[] callbackParamList_bytes = JdkSerializeTool.serialize(callbackParamList);

        File callbackLogFile = new File(failCallbackFileName.replace("{x}", String.valueOf(System.currentTimeMillis())));
        if (callbackLogFile.exists()){
            for (int i = 0; i < 100; i++){
                callbackLogFile = new File(failCallbackFileName.replace("{x}", String.valueOf(System.currentTimeMillis()).concat("-").concat(String.valueOf(i))));
                if (!callbackLogFile.exists()){
                    break;
                }
            }
        }
        FileUtil.writeFileContent(callbackLogFile, callbackParamList_bytes);
    }

    // 对上面方法中的错误callback文件中的callbackParamList进行重复尝试回调
    private void retryFailCallbackFile(){

        // valid
        File callbackLogPath = new File(failCallbackFilePath);
        if (!callbackLogPath.exists()){
            return;
        }
        if (callbackLogPath.isFile()){
            callbackLogPath.delete();
        }
        if (!(callbackLogPath.isDirectory() && callbackLogPath.list() != null && callbackLogPath.list().length > 0)){
            return;
        }

        // load and clear file, retry
        for (File callbackLogFile : callbackLogPath.listFiles()){
            byte[] callbackParamList_bytes = FileUtil.readFileContent(callbackLogFile);

            // avoid empty file
            if (callbackParamList_bytes == null || callbackParamList_bytes.length < 1){
                callbackLogFile.delete();
                continue;
            }

            List<HandleCallbackParam> callbackParamList = (List<HandleCallbackParam>) JdkSerializeTool.deserialize(callbackParamList_bytes, List.class);

            callbackLogFile.delete();
            // 再尝试回调，若又失败，继续写错误回调日志，如此循环往复，直到toStop
            doCallback(callbackParamList);
        }
    }
}
