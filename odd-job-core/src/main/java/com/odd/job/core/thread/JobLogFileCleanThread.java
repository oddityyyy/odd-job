package com.odd.job.core.thread;

import com.odd.job.core.log.OddJobFileAppender;
import com.odd.job.core.util.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * job file clean thread
 *
 * @author oddity
 * @create 2023-12-06 21:05
 */
public class JobLogFileCleanThread {

    private static Logger logger = LoggerFactory.getLogger(JobLogFileCleanThread.class);

    private static JobLogFileCleanThread instance = new JobLogFileCleanThread();
    public static JobLogFileCleanThread getInstance() {
        return instance;
    }

    private Thread localThread;
    private volatile boolean toStop = false;

    public void start(final long logRetentionDays){

        // limit min value (默认是30天)
        if (logRetentionDays < 3){
            return;
        }

        localThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!toStop){
                    try {
                        // clean log dir, over logRetentionDays
                        File[] childDirs = new File(OddJobFileAppender.getLogPath()).listFiles();
                        if (childDirs != null && childDirs.length > 0){

                            // today
                            Calendar todayCal = Calendar.getInstance();
                            todayCal.set(Calendar.HOUR_OF_DAY, 0);
                            todayCal.set(Calendar.MINUTE, 0);
                            todayCal.set(Calendar.SECOND, 0);
                            todayCal.set(Calendar.MILLISECOND, 0);

                            Date todayDate = todayCal.getTime();

                            for (File childFile : childDirs){

                                // valid
                                if (!childFile.isDirectory()){
                                    continue;
                                }
                                // 如果文件名中不包含连字符，则会跳过对该文件的处理
                                if (childFile.getName().indexOf("-") == -1){
                                    continue;
                                }

                                // file create date
                                Date logFileCreateDate = null;

                                try {
                                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
                                    //解析子目录的文件名，获取日志文件的创建日期
                                    logFileCreateDate = simpleDateFormat.parse(childFile.getName());
                                } catch (ParseException e) {
                                    logger.error(e.getMessage(), e);
                                }
                                if (logFileCreateDate == null){
                                    continue;
                                }

                                if ((todayDate.getTime() - logFileCreateDate.getTime()) >= logRetentionDays * (24 * 60 * 60 * 1000)){
                                    FileUtil.deleteRecursively(childFile);
                                }
                            }
                        }
                    } catch (Exception e) {
                        if (!toStop){
                            logger.error(e.getMessage(), e);
                        }
                    }

                    try {
                        TimeUnit.DAYS.sleep(1);
                    } catch (InterruptedException e) {
                        if (!toStop) {
                            logger.error(e.getMessage(), e);
                        }
                    }
                }
                logger.info(">>>>>>>>>>> odd-job, executor JobLogFileCleanThread thread destroy.");
            }
        });
        localThread.setDaemon(true);
        localThread.setName("odd-job, executor JobLogFileCleanThread");
        localThread.start();
    }

    public void toStop() {
        toStop = true;

        if (localThread == null) {
            return;
        }

        // interrupt and wait
        localThread.interrupt();
        try {
            // 等待线程结束
            localThread.join();
        } catch (InterruptedException e) {
            logger.error(e.getMessage(), e);
        }
    }
}
