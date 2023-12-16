package com.odd.job.core.biz.impl;

import com.odd.job.core.biz.ExecutorBiz;
import com.odd.job.core.biz.model.*;
import com.odd.job.core.enums.ExecutorBlockStrategyEnum;
import com.odd.job.core.executor.OddJobExecutor;
import com.odd.job.core.glue.GlueFactory;
import com.odd.job.core.glue.GlueTypeEnum;
import com.odd.job.core.handler.IJobHandler;
import com.odd.job.core.handler.impl.GlueJobHandler;
import com.odd.job.core.handler.impl.ScriptJobHandler;
import com.odd.job.core.log.OddJobFileAppender;
import com.odd.job.core.thread.JobThread;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

/**
 * ExecutorBizImpl for EmbedServer (provider)
 *
 * @author oddity
 * @create 2023-12-08 18:21
 */
public class ExecutorBizImpl implements ExecutorBiz {

    private static Logger logger = LoggerFactory.getLogger(ExecutorBizImpl.class);

    @Override
    public ReturnT<String> beat() {
        return ReturnT.SUCCESS;
    }

    @Override
    public ReturnT<String> idleBeat(IdleBeatParam idleBeatParam) {

        // isRunningOrHasQueue
        boolean isRunningOrHasQueue = false;
        JobThread jobThread = OddJobExecutor.loadJobThread(idleBeatParam.getJobId());
        if (jobThread != null && jobThread.isRunningOrHasQueue()){
            isRunningOrHasQueue = true;
        }

        if (isRunningOrHasQueue){
            return new ReturnT<String>(ReturnT.FAIL_CODE, "job thread is running or has trigger queue.");
        }
        return ReturnT.SUCCESS;
    }

    /**
     * 返回的是向调度队列push调度参数成功与否
     * @param triggerParam
     * @return
     */
    @Override
    public ReturnT<String> run(TriggerParam triggerParam) {
        // load old: jobHandler + jobThread
        JobThread jobThread = OddJobExecutor.loadJobThread(triggerParam.getJobId());
        IJobHandler jobHandler = jobThread != null ? jobThread.getHandler() : null;
        String removeOldReason = null;

        // valid: jobHandler + jobThread
        GlueTypeEnum glueTypeEnum = GlueTypeEnum.match(triggerParam.getGlueType());
        if (GlueTypeEnum.BEAN == glueTypeEnum){

            // new jobhandler
            IJobHandler newJobHandler = OddJobExecutor.loadJobHandler(triggerParam.getExecutorHandler());

            // valid old jobThread
            if (jobThread != null && jobHandler != newJobHandler) { // 中途换任务了，在前端编辑了jobHandler选项
                // change handler, need kill old thread
                removeOldReason = "change jobhandler or glue type, and terminate the old job thread.";

                jobThread = null;
                jobHandler = null;
            }

            // valid
            if (jobHandler == null){
                jobHandler = newJobHandler;
                if (jobHandler == null){ // jobHandler是在初始化bean后就扫描注解并注册到OddJobExecutor中的jobHandlerRepository了的，所以此处正常情况下不应该为空
                    // jobHandler在前端输入的有误
                    return new ReturnT<String>(ReturnT.FAIL_CODE, "job handler [" + triggerParam.getExecutorHandler() + "] not found.");
                }
            }
        } else if (GlueTypeEnum.GLUE_GROOVY == glueTypeEnum) {

            // valid old jobThread
            if (jobThread != null &&
                    !(jobThread.getHandler() instanceof GlueJobHandler
                            && ((GlueJobHandler)jobThread.getHandler()).getGlueUpdatetime() == triggerParam.getGlueUpdatetime())){
                // change handler or glueSource updated, need kill old thread
                removeOldReason = "change job source or glue type, and terminate the old job thread.";

                jobThread = null;
                jobHandler = null;
            }

            // valid handler
            if (jobHandler == null){
                try {
                    IJobHandler originJobHandler = GlueFactory.getInstance().loadNewInstance(triggerParam.getGlueSource());
                    jobHandler = new GlueJobHandler(originJobHandler, triggerParam.getGlueUpdatetime());
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                    return new ReturnT<String>(ReturnT.FAIL_CODE, e.getMessage());
                }
            }
        } else if (glueTypeEnum != null && glueTypeEnum.isScript()){

            // valid old jobThread
            if (jobThread != null &&
                    !((jobThread.getHandler() instanceof ScriptJobHandler)
                            && ((ScriptJobHandler) jobThread.getHandler()).getGlueUpdatetime() == triggerParam.getGlueUpdatetime())){
                // change script or gluesource updated, need kill old thread
                removeOldReason = "change job source or glue type, and terminate the old job thread.";

                jobThread = null;
                jobHandler = null;
            }

            // valid handler
            if (jobHandler == null){
                jobHandler = new ScriptJobHandler(triggerParam.getJobId(), triggerParam.getGlueUpdatetime(), triggerParam.getGlueSource(), GlueTypeEnum.match(triggerParam.getGlueType()));
            }
        } else {
            return new ReturnT<String>(ReturnT.FAIL_CODE, "glueType[" + triggerParam.getGlueType() + "] is not valid.");
        }

        // executor block strategy
        if (jobThread != null){
            ExecutorBlockStrategyEnum blockStrategy = ExecutorBlockStrategyEnum.match(triggerParam.getExecutorBlockStrategy(), null);
            if (ExecutorBlockStrategyEnum.DISCARD_LATER == blockStrategy) {
                // discard when running
                if (jobThread.isRunningOrHasQueue()){
                    return new ReturnT<>(ReturnT.FAIL_CODE, "block strategy effect："+ExecutorBlockStrategyEnum.DISCARD_LATER.getTitle());
                }
            } else if (ExecutorBlockStrategyEnum.COVER_EARLY == blockStrategy) {
                // kill running jobThread
                if (jobThread.isRunningOrHasQueue()){
                    removeOldReason = "block strategy effect：" + ExecutorBlockStrategyEnum.COVER_EARLY.getTitle();
                    jobThread = null;
                }
            } else {
                // just queue trigger
            }
        }

        // replace thread (new or exists invalid)
        if (jobThread == null){
            jobThread = OddJobExecutor.registJobThread(triggerParam.getJobId(), jobHandler, removeOldReason);
        }

        // push data to queue 此处是根据一个任务id初始化一个jobThread, jobThread里封装了triggerQueue来放置单个执行器堆积的调度请求，jobThread可以根据jobId进行复用，
        // 中途如果在前端编辑了jobHandler选项换任务，则需要重新注册JobThread，然后根据Corn表达式不断往队列中push triggerParam
        ReturnT<String> pushResult = jobThread.pushTriggerQueue(triggerParam);
        // 此处仅仅只能表示向triggerQueue中成功push了请求，具体是否执行成功还不得知，需要等待异步调用结果
        return pushResult;
    }

    @Override
    public ReturnT<String> kill(KillParam killParam) {
        // kill handlerThread, and create new one
        JobThread jobThread = OddJobExecutor.loadJobThread(killParam.getJobId());
        if (jobThread != null){
            OddJobExecutor.removeJobThread(killParam.getJobId(), "scheduling center kill job.");
            return ReturnT.SUCCESS;
        }

        return new ReturnT<String>(ReturnT.SUCCESS_CODE, "job thread already killed.");
    }

    /**
     * 读日志并返回日志结果
     * @param logParam
     * @return
     */
    @Override
    public ReturnT<LogResult> log(LogParam logParam) {
        // log filename: logPath/yyyy-MM-dd/9999.log
        String logFileName = OddJobFileAppender.makeLogFileName(new Date(logParam.getLogDateTim()), logParam.getLogId());

        LogResult logResult = OddJobFileAppender.readLog(logFileName, logParam.getFromLineNum());
        return new ReturnT<LogResult>(logResult);
    }
}
