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

    /**
     * 同一个执行器集群的appname必须一致，执行器回调地址（odd.job.admin.addresses）需要保持一致
     * @return
     */
    @Bean
    public OddJobSpringExecutor oddJobExecutor() {
        logger.info(">>>>>>>>>>> odd-job config init.");
        OddJobSpringExecutor oddJobSpringExecutor = new OddJobSpringExecutor();
        oddJobSpringExecutor.setAdminAddresses(adminAddresses);
        oddJobSpringExecutor.setAppname(appname);
        oddJobSpringExecutor.setAddress(address);
        oddJobSpringExecutor.setIp(ip);
        oddJobSpringExecutor.setPort(port);
        oddJobSpringExecutor.setAccessToken(accessToken);
        oddJobSpringExecutor.setLogPath(logPath);
        oddJobSpringExecutor.setLogRetentionDays(logRetentionDays);

        return oddJobSpringExecutor;
    }

    /**
     * 针对多网卡、容器内部署等情况，可借助 "spring-cloud-commons" 提供的 "InetUtils" 组件灵活定制注册IP；
     *
     *      1、引入依赖：
     *          <dependency>
     *             <groupId>org.springframework.cloud</groupId>
     *             <artifactId>spring-cloud-commons</artifactId>
     *             <version>${version}</version>
     *         </dependency>
     *
     *      2、配置文件，或者容器启动变量
     *          spring.cloud.inetutils.preferred-networks: 'xxx.xxx.xxx.'
     *
     *      3、获取IP
     *          String ip_ = inetUtils.findFirstNonLoopbackHostInfo().getIpAddress();
     */
}
