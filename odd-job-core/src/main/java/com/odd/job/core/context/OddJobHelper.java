package com.odd.job.core.context;

import com.odd.job.core.log.OddJobFileAppender;
import com.odd.job.core.util.DateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;

/**
 * helper for odd-job
 *
 * @author oddity
 * @create 2023-12-07 20:08
 */
public class OddJobHelper {

    // ---------------------- base info ----------------------

    /**
     * current JobId
     *
     * @return
     */
    public static long getJobId() {
        OddJobContext oddJobContext = OddJobContext.getOddJobContext();
        if (oddJobContext == null){
            return -1;
        }

        return oddJobContext.getJobId();
    }

    /**
     * current JobParam
     *
     * @return
     */
    public static String getJobParam(){
        OddJobContext oddJobContext = OddJobContext.getOddJobContext();
        if (oddJobContext == null){
            return null;
        }

        return oddJobContext.getJobParam();
    }

    // ---------------------- for log ----------------------

    /**
     * current JobLogFileName
     *
     * @return
     */
    public static String getJobLogFileName(){
        OddJobContext oddJobContext = OddJobContext.getOddJobContext();
        if (oddJobContext != null){
            return null;
        }

        return oddJobContext.getJobLogFileName();
    }

    // ---------------------- for shard ----------------------

    /**
     * current ShardIndex
     *
     * @return
     */
    public static int getShardIndex() {
        OddJobContext oddJobContext = OddJobContext.getOddJobContext();
        if (oddJobContext == null){
            return -1;
        }

        return oddJobContext.getShardIndex();
    }

    /**
     * current ShardTotal
     *
     * @return
     */
    public static int getShardTotal() {
        OddJobContext oddJobContext = OddJobContext.getOddJobContext();
        if (oddJobContext == null){
            return -1;
        }

        return oddJobContext.getShardTotal();
    }

    // ---------------------- tool for log ----------------------

    private static Logger logger = LoggerFactory.getLogger("odd-job logger");

    /**
     * append log with pattern
     *
     * @param appendLogPattern  like "aaa {} bbb {} ccc"
     * @param appendLogArguments    like "111, true"
     */
    public static boolean log(String appendLogPattern, Object ... appendLogArguments){

        FormattingTuple ft = MessageFormatter.arrayFormat(appendLogPattern, appendLogArguments);
        String appendLog = ft.getMessage();

        StackTraceElement callInfo = new Throwable().getStackTrace()[1];
        return logDetail(callInfo, appendLog);
    }

    /**
     * append exception stack
     *
     * @param e
     */
    public static boolean log(Throwable e) {

        StringWriter stringWriter = new StringWriter();
        e.printStackTrace(new PrintWriter(stringWriter));
        String appendLog = stringWriter.toString();

        StackTraceElement callInfo = new Throwable().getStackTrace()[1];
        return logDetail(callInfo, appendLog);
    }

    /**
     * append log
     *
     * @param callInfo
     * @param appendLog
     */
    private static boolean logDetail(StackTraceElement callInfo, String appendLog){
        OddJobContext oddJobContext = OddJobContext.getOddJobContext();
        if (oddJobContext == null){
            return false;
        }

        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append(DateUtil.formatDateTime(new Date())).append(" ")
                .append("["+ callInfo.getClassName() + "#" + callInfo.getMethodName() +"]").append("-")
                .append("["+ callInfo.getLineNumber() +"]").append("-")
                .append("["+ Thread.currentThread().getName() +"]").append(" ")
                .append(appendLog!=null?appendLog:"");
        String formatAppendLog = stringBuffer.toString();

        // appendlog
        String logFileName = oddJobContext.getJobLogFileName();

        if (logFileName != null && logFileName.trim().length() > 0){
            OddJobFileAppender.appendLog(logFileName, formatAppendLog);
            return true;
        } else {
            logger.info(">>>>>>>>>>> {}", formatAppendLog);
            return false;
        }
    }

    // ---------------------- tool for handleResult ----------------------

    /**
     * handle success
     *
     * @return
     */
    public static boolean handleSuccess() {
        return handleResult(OddJobContext.HANDLE_CODE_SUCCESS, null);
    }

    /**
     * handle success with log msg
     *
     * @param handleMsg
     * @return
     */
    public static boolean handleSuccess(String handleMsg){
        return handleResult(OddJobContext.HANDLE_CODE_SUCCESS, handleMsg);
    }

    /**
     * handle fail
     *
     * @return
     */
    public static boolean handleFail() {
        return handleResult(OddJobContext.HANDLE_CODE_FAIL, null);
    }

    /**
     * handle fail with log msg
     *
     * @param handleMsg
     * @return
     */
    public static boolean handleFail(String handleMsg) {
        return handleResult(OddJobContext.HANDLE_CODE_FAIL, handleMsg);
    }

    /**
     * handle timeout
     *
     * @return
     */
    public static boolean handleTimeout(){
        return handleResult(OddJobContext.HANDLE_CODE_TIMEOUT, null);
    }

    /**
     * handle timeout with log msg
     *
     * @param handleMsg
     * @return
     */
    public static boolean handleTimeout(String handleMsg){
        return handleResult(OddJobContext.HANDLE_CODE_TIMEOUT, handleMsg);
    }

    /**
     * @param handleCode
     *
     *      200 : success
     *      500 : fail
     *      502 : timeout
     *
     * @param handleMsg
     * @return
     */
    public static boolean handleResult(int handleCode, String handleMsg){
        OddJobContext oddJobContext = OddJobContext.getOddJobContext();
        if (oddJobContext == null){
            return false;
        }

        oddJobContext.setHandleCode(handleCode);
        if (handleMsg != null) {
            oddJobContext.setHandleMsg(handleMsg);
        }
        return true;
    }
}
