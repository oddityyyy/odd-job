package com.odd.job.core.executor;

import com.odd.job.core.biz.AdminBiz;
import com.odd.job.core.biz.client.AdminBizClient;
import com.odd.job.core.handler.IJobHandler;
import com.odd.job.core.handler.annotation.OddJob;
import com.odd.job.core.handler.impl.MethodJobHandler;
import com.odd.job.core.log.OddJobFileAppender;
import com.odd.job.core.server.EmbedServer;
import com.odd.job.core.thread.JobLogFileCleanThread;
import com.odd.job.core.thread.TriggerCallbackThread;
import com.odd.job.core.util.IpUtil;
import com.odd.job.core.util.NetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
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

        // init invoker, admin-client
        initAdminBizList(adminAddresses, accessToken);

        // init JobLogFileCleanThread
        JobLogFileCleanThread.getInstance().start(logRetentionDays);

        // init TriggerCallbackThread
        TriggerCallbackThread.getInstance().start();

        // init executor-server
        initEmbedServer(address, ip, port, appname, accessToken);
    }

    // ---------------------- admin-client (rpc invoker) ----------------------
    private static List<AdminBiz> adminBizList;
    private void initAdminBizList(String adminAddresses, String accessToken) throws Exception {
        if (adminAddresses != null && adminAddresses.trim().length() > 0){
            for (String address : adminAddresses.trim().split(",")){
                if (address != null && address.trim().length() > 0) {

                    AdminBiz adminBiz = new AdminBizClient(address.trim(), accessToken);

                    if (adminBizList == null) {
                        adminBizList = new ArrayList<AdminBiz>();
                    }
                    adminBizList.add(adminBiz);
                }
            }
        }
    }

    public static List<AdminBiz> getAdminBizList() {
        return adminBizList;
    }

    // ---------------------- executor-server (rpc provider) ----------------------
    private EmbedServer embedServer = null;

    private void initEmbedServer(String address, String ip, int port, String appname, String accessToken) throws Exception {

        // fill ip port
        port = port > 0 ? port : NetUtil.findAvailablePort(9999);
        ip = (ip != null && ip.trim().length() > 0) ? ip : IpUtil.getIp();

        // generate address
        if (address == null || address.trim().length() == 0){
            String ip_port_address = IpUtil.getIpPort(ip, port);// registry-address：default use address to registry , otherwise use ip:port if address is null
            address = "http://{ip_port}/".replace("{ip_port}", ip_port_address);
        }

        // accessToken
        if (accessToken == null || accessToken.trim().length() == 0){
            logger.warn(">>>>>>>>>>> odd-job accessToken is empty. To ensure system security, please set the accessToken.");
        }

        // start
        embedServer = new EmbedServer();
        embedServer.start(address, port, appname, accessToken);
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
