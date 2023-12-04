package com.odd.job.core.log;

import com.odd.job.core.biz.model.LogResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.LineNumberReader;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * store trigger log in each log-file
 *
 * @author oddity
 * @create 2023-12-05 15:57
 */
public class OddJobFileAppender {

    private static Logger logger = LoggerFactory.getLogger(OddJobFileAppender.class);

    /**
     * log base path
     *
     * strut like:
     * 	---/
     * 	---/gluesource/
     * 	---/gluesource/10_1514171108000.js
     * 	---/gluesource/10_1514171108000.js
     * 	---/2017-12-25/
     * 	---/2017-12-25/639.log
     * 	---/2017-12-25/821.log
     *
     */
    private static String logBasePath = "/data/applogs/odd-job/jobhandler";
    private static String glueSrcPath = logBasePath.concat("/gluesource");

    public static void initLogPath(String logPath){
        // init
        if (logPath != null && logPath.trim().length() > 0){
            logBasePath = logPath;
        }
        // mk base dir
        File logPathDir = new File(logBasePath);
        if (!logPathDir.exists()){
            logPathDir.mkdir();
        }
        logBasePath = logPathDir.getPath();

        // mk glue dir
        File glueBaseDir = new File(logPathDir, "gluesource");
        if (!glueBaseDir.exists()){
            glueBaseDir.mkdirs();
        }
        glueSrcPath = glueBaseDir.getPath();
    }

    public static String getLogPath() {
        return logBasePath;
    }
    public static String getGlueSrcPath() {
        return glueSrcPath;
    }

    /**
     * log filename, like "logPath/yyyy-MM-dd/9999.log"
     *
     * @param triggerDate
     * @param logId
     * @return
     */
    public static String makeLogFileName(Date triggerDate, long logId){
        // filePath/yyyy-MM-dd
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");	// avoid concurrent problem, can not be static
        File logFilePath = new File(getLogPath(), sdf.format(triggerDate));
        if (!logFilePath.exists()) {
            logFilePath.mkdir();
        }

        // filePath/yyyy-MM-dd/9999.log
        String logFileName = logFilePath.getPath()
                .concat(File.separator)
                .concat(String.valueOf(logId))
                .concat(".log");
        return logFileName;
    }

    /**
     * append log
     *
     * @param logFileName
     * @param appendLog
     */
    public static void appendLog(String logFileName, String appendLog){

        // log file
        if (logFileName==null || logFileName.trim().length()==0) {
            return;
        }
        File logFile = new File(logFileName);

        if (!logFile.exists()) {
            try {
                logFile.createNewFile();
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
                return;
            }
        }

        // log
        if (appendLog == null) {
            appendLog = "";
        }
        appendLog += "\r\n";

        // append file content
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(logFile, true);
            fos.write(appendLog.getBytes("utf-8"));
            fos.flush();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }
    }

    /**
     * support read log-file
     *
     * @param logFileName
     * @return log content
     */
    public static LogResult readLog(String logFileName, int fromLineNum){

        // valid log file
        if (logFileName == null || logFileName.trim().length()==0) {
            return new LogResult(fromLineNum, 0, "readLog fail, logFile not found", true);
        }
        File logFile = new File(logFileName);

        if (!logFile.exists()) {
            return new LogResult(fromLineNum, 0, "readLog fail, logFile not exists", true);
        }

        // read file
        StringBuffer logContentBuffer = new StringBuffer();
        int toLineNum = 0;
        LineNumberReader reader = null;
    }
}
