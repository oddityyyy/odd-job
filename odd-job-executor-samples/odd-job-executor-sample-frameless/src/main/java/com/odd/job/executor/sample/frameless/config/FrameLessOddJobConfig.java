package com.odd.job.executor.sample.frameless.config;

import com.odd.job.core.executor.impl.OddJobSimpleExecutor;
import com.odd.job.executor.sample.frameless.jobhandler.SampleOddJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Properties;

/**
 * FrameLess和SpringBoot的区别就是FrameLess需要自己去手动添加（扫描）所有SampleOddJob(执行器需要执行的方法所在的类)
 * SpringBoot会根据SpringBootContext,扫描 Spring 容器中带有 @OddJob 注解的方法, 自动扫描，省去了手动添加的步骤
 *
 * @author oddity
 * @create 2023-12-17 15:02
 */
public class FrameLessOddJobConfig {

    private static Logger logger = LoggerFactory.getLogger(FrameLessOddJobConfig.class);


    private static FrameLessOddJobConfig instance = new FrameLessOddJobConfig();
    public static FrameLessOddJobConfig getInstance() {
        return instance;
    }


    private OddJobSimpleExecutor oddJobExecutor = null;

    /**
     * init
     */
    public void initOddJobExecutor() {

        // load executor prop
        Properties oddJobProp = loadProperties("odd-job-executor.properties");

        // init executor
        oddJobExecutor = new OddJobSimpleExecutor();
        oddJobExecutor.setAdminAddresses(oddJobProp.getProperty("odd.job.admin.addresses"));
        oddJobExecutor.setAccessToken(oddJobProp.getProperty("odd.job.accessToken"));
        oddJobExecutor.setAppname(oddJobProp.getProperty("odd.job.executor.appname"));
        oddJobExecutor.setAddress(oddJobProp.getProperty("odd.job.executor.address"));
        oddJobExecutor.setIp(oddJobProp.getProperty("odd.job.executor.ip"));
        oddJobExecutor.setPort(Integer.valueOf(oddJobProp.getProperty("odd.job.executor.port")));
        oddJobExecutor.setLogPath(oddJobProp.getProperty("odd.job.executor.logpath"));
        oddJobExecutor.setLogRetentionDays(Integer.valueOf(oddJobProp.getProperty("odd.job.executor.logretentiondays")));

        // registry job bean 手动扫描
        oddJobExecutor.setOddJobBeanList(Arrays.asList(new SampleOddJob()));

        // start executor
        try {
            oddJobExecutor.start();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    /**
     * destroy
     */
    public void destroyOddJobExecutor() {
        if (oddJobExecutor != null) {
            oddJobExecutor.destroy();
        }
    }


    public static Properties loadProperties(String propertyFileName) {
        InputStreamReader in = null;
        try {
            ClassLoader loder = Thread.currentThread().getContextClassLoader();

            in = new InputStreamReader(loder.getResourceAsStream(propertyFileName), "UTF-8");;
            if (in != null) {
                Properties prop = new Properties();
                prop.load(in);
                return prop;
            }
        } catch (IOException e) {
            logger.error("load {} error!", propertyFileName);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    logger.error("close {} error!", propertyFileName);
                }
            }
        }
        return null;
    }
}
