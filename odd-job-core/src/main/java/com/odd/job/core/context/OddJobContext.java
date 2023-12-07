package com.odd.job.core.context;

/**
 * odd-job context
 *
 * @author oddity
 * @create 2023-12-07 19:22
 */
public class OddJobContext {

    public static final int HANDLE_CODE_SUCCESS = 200;
    public static final int HANDLE_CODE_FAIL = 500;
    public static final int HANDLE_CODE_TIMEOUT = 502;

    // ---------------------- base info ----------------------

    /**
     * job id
     */
    private final long jobId;

    /**
     * job param
     */
    private final String jobParam;

    // ---------------------- for log ----------------------

    /**
     * job log filename
     */
    private final String jobLogFileName;

    // ---------------------- for shard ----------------------

    /**
     * shard index
     */
    private final int shardIndex;

    /**
     * shard total
     */
    private final int shardTotal;

    // ---------------------- for handle ----------------------

    /**
     * handleCode：The result status of job execution
     *
     *      200 : success
     *      500 : fail
     *      502 : timeout
     *
     */
    private int handleCode;

    /**
     * handleMsg：The simple log msg of job execution
     */
    private String handleMsg;

    public OddJobContext(long jobId, String jobParam, String jobLogFileName, int shardIndex, int shardTotal) {
        this.jobId = jobId;
        this.jobParam = jobParam;
        this.jobLogFileName = jobLogFileName;
        this.shardIndex = shardIndex;
        this.shardTotal = shardTotal;

        this.handleCode = HANDLE_CODE_SUCCESS; // default success
    }

    public long getJobId() {
        return jobId;
    }

    public String getJobParam() {
        return jobParam;
    }

    public String getJobLogFileName() {
        return jobLogFileName;
    }

    public int getShardIndex() {
        return shardIndex;
    }

    public int getShardTotal() {
        return shardTotal;
    }

    public int getHandleCode() {
        return handleCode;
    }

    public void setHandleCode(int handleCode) {
        this.handleCode = handleCode;
    }

    public String getHandleMsg() {
        return handleMsg;
    }

    public void setHandleMsg(String handleMsg) {
        this.handleMsg = handleMsg;
    }

    // ---------------------- tool ----------------------
    // 在任务调度中，需要将任务的一些上下文信息传递给子线程进行处理。

    // 主要区别于 ThreadLocal 的地方在于，它会在创建子线程时，把父线程的本地变量复制一份给子线程，
    // 使子线程可以访问父线程的本地变量。
    private static InheritableThreadLocal<OddJobContext> contextHolder = new InheritableThreadLocal<OddJobContext>(); // support for child thread of job handler

    public static void setOddJobContext(OddJobContext oddJobContext){
        contextHolder.set(oddJobContext);
    }

    public static OddJobContext getOddJobContext() {
        return contextHolder.get();
    }
}
