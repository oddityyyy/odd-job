package com.odd.job.executor.sample.frameless;

import com.odd.job.executor.sample.frameless.config.FrameLessOddJobConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * @author oddity
 * @create 2023-12-17 15:00
 */
public class FramelessApplication {

    private static Logger logger = LoggerFactory.getLogger(FramelessApplication.class);

    public static void main(String[] args) {

        try {
            // start
            FrameLessOddJobConfig.getInstance().initOddJobExecutor();

            // Blocks until interrupted
            while (true) {
                try {
                    TimeUnit.HOURS.sleep(1);
                } catch (InterruptedException e) {
                    break;
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        } finally {
            // destroy
            FrameLessOddJobConfig.getInstance().destroyOddJobExecutor();
        }

    }
}
