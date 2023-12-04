package com.odd.job.executor.core.config;

import com.odd.job.core.executor.impl.OddJobSpringExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * odd-job config
 *
 * @author oddity
 * @create 2023-12-05 14:32
 */

@Configuration
public class OddJobConfig {

    private Logger logger = LoggerFactory.getLogger(OddJobConfig.class);

    @Value("${odd.job.admin.addresses}")
    private String adminAddresses;

    @Value("${odd.job.accessToken}")
    private String accessToken;

    @Value("${odd.job.executor.appname}")
    private String appname;

    @Value("${odd.job.executor.address}")
    private String address;

    @Value("${odd.job.executor.ip}")
    private String ip;

    @Value("${odd.job.executor.port}")
    private int port;

    @Value("${odd.job.executor.logpath}")
    private String logPath;

    @Value("${odd.job.executor.logretentiondays}")
    private int logRetentionDays;

    @Bean
    public OddJobSpringExecutor oddJobExecutor() {

    }
}
