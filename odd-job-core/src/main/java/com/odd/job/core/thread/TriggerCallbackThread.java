package com.odd.job.core.thread;

import com.odd.job.core.biz.AdminBiz;
import com.odd.job.core.biz.model.HandleCallbackParam;
import com.odd.job.core.biz.model.ReturnT;
import com.odd.job.core.context.OddJobContext;
import com.odd.job.core.context.OddJobHelper;
import com.odd.job.core.executor.OddJobExecutor;
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
                }
            }
        })
    }

    //TODO 此处若调度中心集群，这样的回调方式会不会有问题
    // （细看admin的代码，有可能是adminBiz.callback(callbackParamList)远程发送请求的时候，amdin端做了校验，
    // 不是本调度中心调度的任务不予返回成功结果）
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
}
