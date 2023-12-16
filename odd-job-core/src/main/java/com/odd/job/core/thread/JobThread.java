package com.odd.job.core.thread;

import com.odd.job.core.biz.model.HandleCallbackParam;
import com.odd.job.core.biz.model.ReturnT;
import com.odd.job.core.biz.model.TriggerParam;
import com.odd.job.core.context.OddJobContext;
import com.odd.job.core.context.OddJobHelper;
import com.odd.job.core.executor.OddJobExecutor;
import com.odd.job.core.handler.IJobHandler;
import com.odd.job.core.log.OddJobFileAppender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.*;

/**
 * handler thread
 *
 * @author oddity
 * @create 2023-12-09 14:43
 */
public class JobThread extends Thread{

    private static Logger logger = LoggerFactory.getLogger(JobThread.class);

    private int jobId;
    private IJobHandler handler;
    private LinkedBlockingQueue<TriggerParam> triggerQueue; // 放置单个执行器堆积的调度请求
    private Set<Long> triggerLogIdSet; // avoid repeat trigger for the same TRIGGER_LOG_ID

    private volatile boolean toStop = false;
    private String stopReason;

    private boolean running = false; // if running job
    private int idleTimes = 0;       // idle times

    public JobThread(int jobId, IJobHandler handler) {
        this.jobId = jobId;
        this.handler = handler;
        this.triggerQueue = new LinkedBlockingQueue<TriggerParam>();
        this.triggerLogIdSet = Collections.synchronizedSet(new HashSet<Long>());

        // assign job thread name
        this.setName("odd-job, JobThread-" + jobId + "-" + System.currentTimeMillis());
    }

    public IJobHandler getHandler() {
        return handler;
    }

    /**
     * new trigger to queue
     *
     * @param triggerParam
     * @return
     */
    public ReturnT<String> pushTriggerQueue(TriggerParam triggerParam){
        // avoid repeat
        if (triggerLogIdSet.contains(triggerParam.getLogId())){
            logger.info(">>>>>>>>>>> repeate trigger job, logId:{}", triggerParam.getLogId());
            return new ReturnT<String>(ReturnT.FAIL_CODE, "repeate trigger job, logId:" + triggerParam.getLogId());
        }

        triggerLogIdSet.add(triggerParam.getLogId());
        triggerQueue.add(triggerParam);
        return ReturnT.SUCCESS;
    }

    /**
     * kill job thread
     *
     * @param stopReason
     */
    public void toStop(String stopReason){
        /**
         * Thread.interrupt只支持终止线程的阻塞状态(wait、join、sleep)，
         * 在阻塞处抛出InterruptedException异常,但是并不会终止运行的线程本身；
         * 所以需要注意，此处彻底销毁本线程，需要通过共享变量方式；
         */
        this.toStop = true;
        this.stopReason = stopReason;
    }

    /**
     * is running job
     * @return
     */
    public boolean isRunningOrHasQueue(){
        return running || triggerQueue.size() > 0;
    }

    /**
     * 此线程不断运行，消费调度队列中的请求，异步返回执行结果
     */
    @Override
    public void run() {

        // init
        try {
            handler.init();
        } catch (Throwable e) {
            logger.error(e.getMessage(), e);
        }

        // execute
        while(!toStop){
            running = false;
            idleTimes++;

            TriggerParam triggerParam = null;

            try {
                // to check toStop signal, we need cycle, so we cannot use queue.take(), instead of poll(timeout)
                triggerParam = triggerQueue.poll(3L, TimeUnit.SECONDS);
                if (triggerParam != null){
                    running = true;
                    idleTimes = 0;
                    triggerLogIdSet.remove(triggerParam.getLogId());

                    // log filename, like "logPath/yyyy-MM-dd/9999.log"
                    String logFileName = OddJobFileAppender.makeLogFileName(new Date(triggerParam.getLogDateTime()), triggerParam.getLogId());
                    OddJobContext oddJobContext = new OddJobContext(
                            triggerParam.getJobId(),
                            triggerParam.getExecutorParams(),
                            logFileName,
                            triggerParam.getBroadcastIndex(),
                            triggerParam.getBroadcastTotal());

                    // init job context
                    OddJobContext.setOddJobContext(oddJobContext);

                    // execute
                    OddJobHelper.log("<br>----------- odd-job job execute start -----------<br>----------- Param:" + oddJobContext.getJobParam());

                    if (triggerParam.getExecutorTimeout() > 0){
                        // limit timeout
                        Thread futureThread = null;
                        try {
                            FutureTask<Boolean> futureTask = new FutureTask<Boolean>(new Callable<Boolean>() {
                                @Override
                                public Boolean call() throws Exception {

                                    // init job context
                                    OddJobContext.setOddJobContext(oddJobContext);

                                    handler.execute();
                                    return true;
                                }
                            });
                            futureThread = new Thread(futureTask);
                            futureThread.start();

                            Boolean tempResult = futureTask.get(triggerParam.getExecutorTimeout(), TimeUnit.SECONDS);
                        } catch (TimeoutException e) {
                            OddJobHelper.log("<br>----------- odd-job job execute timeout");
                            OddJobHelper.log(e);

                            // handle result
                            OddJobHelper.handleTimeout("job execute timeout ");
                        } finally {
                            futureThread.interrupt();
                        }
                    } else {
                        // just execute 此处让JobHandler通过反射执行任务
                        handler.execute();
                    }

                    // valid execute handle data
                    if (OddJobContext.getOddJobContext().getHandleCode() <= 0){
                        OddJobHelper.handleFail("job handle result lost.");
                    } else {
                        String tempHandleMsg = OddJobContext.getOddJobContext().getHandleMsg();
                        tempHandleMsg = (tempHandleMsg != null && tempHandleMsg.length() > 50000)
                                ? tempHandleMsg.substring(0, 50000).concat("...")
                                : tempHandleMsg;
                        OddJobContext.getOddJobContext().setHandleMsg(tempHandleMsg);
                    }
                    OddJobHelper.log("<br>----------- odd-job job execute end(finish) -----------<br>----------- Result: handleCode="
                            + OddJobContext.getOddJobContext().getHandleCode()
                            + ", handleMsg = "
                            + OddJobContext.getOddJobContext().getHandleMsg()
                    );
                } else {
                    if (idleTimes > 30) {
                        // 移除空闲线程
                        if (triggerQueue.size() == 0){ // avoid concurrent trigger causes jobId-lost
                            OddJobExecutor.removeJobThread(jobId, "executor idle times over limit.");
                        }
                    }
                }
            } catch (Throwable e) {
                if (toStop){
                    OddJobHelper.log("<br>----------- JobThread toStop, stopReason:" + stopReason);
                }

                // handle result
                StringWriter stringWriter = new StringWriter();
                e.printStackTrace(new PrintWriter(stringWriter));
                String errorMsg = stringWriter.toString();

                OddJobHelper.handleFail(errorMsg);

                OddJobHelper.log("<br>----------- JobThread Exception:" + errorMsg + "<br>----------- odd-job job execute end(error) -----------");

            } finally {
                if (triggerParam != null){
                    // callback handler info
                    if (!toStop){
                        // common
                        TriggerCallbackThread.pushCallBack(new HandleCallbackParam(
                                triggerParam.getLogId(),
                                triggerParam.getLogDateTime(),
                                OddJobContext.getOddJobContext().getHandleCode(),
                                OddJobContext.getOddJobContext().getHandleMsg()
                        ));
                    }else {
                        // is killed 执行完没有回调成功也算失败（执行完突然stop）
                        TriggerCallbackThread.pushCallBack(new HandleCallbackParam(
                                triggerParam.getLogId(),
                                triggerParam.getLogDateTime(),
                                OddJobContext.HANDLE_CODE_FAIL,
                                stopReason + " [job running, killed]"
                        ));
                    }
                }
            }
        }

        // callback trigger request in queue 返回仍在调度队列中没来得及执行的任务为失败结果（thread killed）
        while (triggerQueue != null && triggerQueue.size() > 0){
            TriggerParam triggerParam = triggerQueue.poll();
            if (triggerParam != null){
                // is killed
                TriggerCallbackThread.pushCallBack(new HandleCallbackParam(
                        triggerParam.getLogId(),
                        triggerParam.getLogDateTime(),
                        OddJobContext.HANDLE_CODE_FAIL,
                        stopReason + " [job not executed, in the job queue, killed.]"
                ));
            }
        }

        // destroy
        try {
            handler.destroy();
        } catch (Throwable e) {
            logger.error(e.getMessage(), e);
        }

        logger.info(">>>>>>>>>>> odd-job JobThread stoped, hashCode:{}", Thread.currentThread());
    }
}
