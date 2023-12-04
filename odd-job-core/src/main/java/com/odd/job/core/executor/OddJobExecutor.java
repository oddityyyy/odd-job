package com.odd.job.core.executor;

import com.odd.job.core.handler.IJobHandler;
import com.odd.job.core.handler.annotation.OddJob;
import com.odd.job.core.handler.impl.MethodJobHandler;
import com.odd.job.core.log.OddJobFileAppender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author oddity
 * @create 2023-12-05 15:04
 */
public class OddJobExecutor {

    private static final Logger logger = LoggerFactory.getLogger(OddJobExecutor.class);

    // ---------------------- param ----------------------
    private String adminAddresses;
    private String accessToken;
    private String appname;
    private String address;
    private String ip;
    private int port;
    private String logPath;
    private int logRetentionDays;

    public void setAdminAddresses(String adminAddresses) {
        this.adminAddresses = adminAddresses;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public void setAppname(String appname) {
        this.appname = appname;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setLogPath(String logPath) {
        this.logPath = logPath;
    }

    public void setLogRetentionDays(int logRetentionDays) {
        this.logRetentionDays = logRetentionDays;
    }

    // ---------------------- start + stop ----------------------
    public void start() throws Exception{

        // init logPath
        OddJobFileAppender.initLogPath(logPath);
    }

    // ---------------------- job handler repository ----------------------
    private static ConcurrentMap<String, IJobHandler> jobHandlerRepository = new ConcurrentHashMap<String, IJobHandler>();

    public static IJobHandler loadJobHandler(String name) {
        return jobHandlerRepository.get(name);
    }
    public static IJobHandler registJobHandler(String name, IJobHandler jobHandler){
        logger.info(">>>>>>>>>>> odd-job register jobhandler success, name:{}, jobHandler:{}", name, jobHandler);
        return jobHandlerRepository.put(name, jobHandler);
    }

    protected void registJobHandler(OddJob oddJob, Object bean, Method executeMethod){
        if (oddJob == null) {
            return;
        }

        String name = oddJob.value();
        //make and simplify the variables since they'll be called several times later
        Class<?> clazz = bean.getClass();
        String methodName = executeMethod.getName();
        if (name.trim().length() == 0){
            throw new RuntimeException("odd-job method-jobhandler name invalid, for[" + clazz + "#" + methodName + "] .");
        }
        // 已经存在相同名称(jobhandler name)的JobHandler
        if (loadJobHandler(name) != null){
            throw new RuntimeException("odd-job jobhandler[" + name + "] naming conflicts.");
        }

        executeMethod.setAccessible(true);

        //init and destroy
        Method initMethod = null;
        Method destroyMethod = null;

        if (oddJob.init().trim().length() > 0){
            try {
                initMethod = clazz.getDeclaredMethod(oddJob.init());
                initMethod.setAccessible(true);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException("odd-job method-jobhandler initMethod invalid, for[" + clazz + "#" + methodName + "] .");
            }
        }
        if (oddJob.destroy().trim().length() > 0){
            try {
                destroyMethod = clazz.getDeclaredMethod(oddJob.destroy());
                destroyMethod.setAccessible(true);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException("odd-job method-jobhandler destroyMethod invalid, for[" + clazz + "#" + methodName + "] .");
            }
        }

        // registry jobhandler
        registJobHandler(name, new MethodJobHandler(bean, executeMethod, initMethod, destroyMethod));
    }


}
