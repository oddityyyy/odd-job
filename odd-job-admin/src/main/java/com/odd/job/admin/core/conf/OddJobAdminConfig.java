package com.odd.job.admin.core.conf;

import com.odd.job.admin.core.alarm.JobAlarmer;
import com.odd.job.admin.core.scheduler.OddJobScheduler;
import com.odd.job.admin.dao.*;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;

import javax.annotation.Resource;
import javax.sql.DataSource;
import java.util.Arrays;

/**
 * odd-job config
 *
 * @author oddity
 * @create 2023-12-11 19:33
 */
public class OddJobAdminConfig implements InitializingBean, DisposableBean {

    private static OddJobAdminConfig adminConfig = null;
    public static OddJobAdminConfig getAdminConfig() {
        return adminConfig;
    }

    // ---------------------- OddJobScheduler ----------------------

    private OddJobScheduler oddJobScheduler;

    @Override
    public void afterPropertiesSet() throws Exception {
        adminConfig = this;

        oddJobScheduler = new OddJobScheduler();
        oddJobScheduler.init();
    }

    @Override
    public void destroy() throws Exception {
        oddJobScheduler.destroy();
    }

    // ---------------------- OddJobScheduler ----------------------

    // conf
    @Value("${odd.job.i18n}") //zh_CN
    private String i18n;

    @Value("${odd.job.accessToken}")
    private String accessToken;

    @Value("${spring.mail.from}")
    private String emailFrom;

    @Value("${odd.job.triggerpool.fast.max}") // 200
    private int triggerPoolFastMax;

    @Value("${odd.job.triggerpool.slow.max}") // 100
    private int triggerPoolSlowMax;

    @Value("${odd.job.logretentiondays}")
    private int logretentiondays;

    // dao, service

    @Resource
    private OddJobLogDao oddJobLogDao;
    @Resource
    private OddJobInfoDao oddJobInfoDao;
    @Resource
    private OddJobRegistryDao oddJobRegistryDao;
    @Resource
    private OddJobGroupDao oddJobGroupDao;
    @Resource
    private OddJobLogReportDao oddJobLogReportDao;
    @Resource
    private JavaMailSender mailSender;
    @Resource
    private DataSource dataSource;
    @Resource
    private JobAlarmer jobAlarmer;

    public String getI18n(){
        if (!Arrays.asList("zh_CN", "zh_TC", "en").contains(i18n)){
            return "zh_CN";
        }
        return i18n;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getEmailFrom() {
        return emailFrom;
    }

    // 快线程池最小数量200
    public int getTriggerPoolFastMax(){
        if (triggerPoolFastMax < 200){
            return 200;
        }
        return triggerPoolFastMax;
    }

    // 慢线程池最小数量100
    public int getTriggerPoolSlowMax() {
        if (triggerPoolSlowMax < 100) {
            return 100;
        }
        return triggerPoolSlowMax;
    }

    // 最少得保留7天
    public int getLogretentiondays() {
        if (logretentiondays < 7) {
            return -1;  // Limit greater than or equal to 7, otherwise close
        }
        return logretentiondays;
    }

    public OddJobLogDao getOddJobLogDao() {
        return oddJobLogDao;
    }

    public OddJobInfoDao getOddJobInfoDao() {
        return oddJobInfoDao;
    }

    public OddJobRegistryDao getOddJobRegistryDao() {
        return oddJobRegistryDao;
    }

    public OddJobGroupDao getOddJobGroupDao() {
        return oddJobGroupDao;
    }

    public OddJobLogReportDao getOddJobLogReportDao() {
        return oddJobLogReportDao;
    }

    public JavaMailSender getMailSender() {
        return mailSender;
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public JobAlarmer getJobAlarmer() {
        return jobAlarmer;
    }
}
