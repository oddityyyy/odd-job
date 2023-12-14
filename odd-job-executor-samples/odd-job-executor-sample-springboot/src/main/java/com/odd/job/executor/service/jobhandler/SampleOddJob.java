package com.odd.job.executor.service.jobhandler;

import com.odd.job.core.context.OddJobHelper;
import com.odd.job.core.handler.annotation.OddJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * OddJob开发示例（Bean模式）
 *
 * 开发步骤：
 * 1、任务开发：在Spring Bean实例中，开发Job方法；
 * 2、注解配置：为Job方法添加注解 "@OddJob(value="自定义jobhandler名称", init = "JobHandler初始化方法", destroy = "JobHandler销毁方法")"，注解value值对应的是调度中心新建任务的JobHandler属性的值。
 * 3、执行日志：需要通过 "OddJobHelper.log" 打印执行日志；
 * 4、任务结果：默认任务结果为 "成功" 状态，不需要主动设置；如有诉求，比如设置任务结果为失败，可以通过 "OddJobHelper.handleFail/handleSuccess" 自主设置任务结果； OddJobContext.java Line-70
 *
 * @author oddity
 * @create 2023-12-10 22:17
 */

@Component
public class SampleOddJob {
    private static Logger logger = LoggerFactory.getLogger(SampleOddJob.class);

    //test
    @OddJob("demoTest1")
    public void testApp1() throws InterruptedException {
        logger.info("demoTest1，执行时间为" + new Date());
        TimeUnit.SECONDS.sleep(5);
        System.out.println("可以进行其他操作");
    }

    //test
    @OddJob("demoTest2")
    public void testApp2() throws InterruptedException {
        logger.info("demoTest2，执行时间为" + new Date());
        TimeUnit.SECONDS.sleep(5);
        System.out.println("可以进行其他操作");
    }

    @OddJob("hi")
    public void testApp3() throws InterruptedException {
        System.out.println("hi~");
    }

    @OddJob("hello")
    public void testApp4() throws InterruptedException {
        System.out.println("hello~");
    }

    /**
     * 1、简单任务示例（Bean模式）
     */
    @OddJob("demoJobHandler")
    public void demoJobHandler() throws Exception {
        OddJobHelper.log("ODD-JOB, Hello World.");

        for (int i = 0; i < 5; i++) {
            OddJobHelper.log("beat at:" + i);
            TimeUnit.SECONDS.sleep(2);
        }
        // default success
    }

    //TODO 分片参数是从调度中心TriggerParam传过来的，怎么传过来的
    //答：是从调度中心发来的TriggerParam中传过来的，调度中心在发起调度前需要先根据路由规则确定要调度的机器地址，不论是分片广播（在TriggerParam中设置了index和total）还是其他策略(默认index = 0, length = 1), 在调度请求发出来之前，分片参数都会在TriggerParam中设置好
    /**
     * 2、分片广播任务
     */
    @OddJob("shardingJobHandler")
    public void shardingJobHandler() throws Exception {

        // 分片参数
        int shardIndex = OddJobHelper.getShardIndex();
        int shardTotal = OddJobHelper.getShardTotal();

        OddJobHelper.log("分片参数：当前分片序号 = {}, 总分片数 = {}", shardIndex, shardTotal);

        // 业务逻辑
        for (int i = 0; i < shardTotal; i++) {
            if (i == shardIndex) {
                OddJobHelper.log("第 {} 片, 命中分片开始处理", i);
            } else {
                OddJobHelper.log("第 {} 片, 忽略", i);
            }
        }

    }

    /**
     * 3、命令行任务
     */
    @OddJob("commandJobHandler")
    public void commandJobHandler() throws Exception {
        String command = OddJobHelper.getJobParam();
        int exitValue = -1;

        BufferedReader bufferedReader = null;
        try {
            // command process
            ProcessBuilder processBuilder = new ProcessBuilder();
            processBuilder.command(command);
            processBuilder.redirectErrorStream(true);

            Process process = processBuilder.start();
            //Process process = Runtime.getRuntime().exec(command);

            BufferedInputStream bufferedInputStream = new BufferedInputStream(process.getInputStream());
            bufferedReader = new BufferedReader(new InputStreamReader(bufferedInputStream));

            // command log
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                OddJobHelper.log(line);
            }

            // command exit
            process.waitFor();
            exitValue = process.exitValue();
        } catch (Exception e) {
            OddJobHelper.log(e);
        } finally {
            if (bufferedReader != null) {
                bufferedReader.close();
            }
        }

        if (exitValue == 0) {
            // default success
        } else {
            OddJobHelper.handleFail("command exit value("+exitValue+") is failed");
        }

    }

    /**
     * 4、跨平台Http任务
     *  参数示例：
     *      "url: http://www.baidu.com\n" +
     *      "method: get\n" +
     *      "data: content\n";
     */
    @OddJob("httpJobHandler")
    public void httpJobHandler() throws Exception {

        // param parse
        String param = OddJobHelper.getJobParam();
        if (param == null || param.trim().length() == 0) {
            OddJobHelper.log("param["+ param +"] invalid.");

            OddJobHelper.handleFail();
            return;
        }

        String[] httpParams = param.split("\n");
        String url = null;
        String method = null;
        String data = null;
        for (String httpParam: httpParams) {
            if (httpParam.startsWith("url:")) {
                url = httpParam.substring(httpParam.indexOf("url:") + 4).trim();
            }
            if (httpParam.startsWith("method:")) {
                method = httpParam.substring(httpParam.indexOf("method:") + 7).trim().toUpperCase();
            }
            if (httpParam.startsWith("data:")) {
                data = httpParam.substring(httpParam.indexOf("data:") + 5).trim();
            }
        }

        // param valid
        if (url==null || url.trim().length()==0) {
            OddJobHelper.log("url["+ url +"] invalid.");

            OddJobHelper.handleFail();
            return;
        }
        if (method==null || !Arrays.asList("GET", "POST").contains(method)) {
            OddJobHelper.log("method["+ method +"] invalid.");

            OddJobHelper.handleFail();
            return;
        }
        boolean isPostMethod = method.equals("POST");

        // request
        HttpURLConnection connection = null;
        BufferedReader bufferedReader = null;
        try {
            // connection
            URL realUrl = new URL(url);
            connection = (HttpURLConnection) realUrl.openConnection();

            // connection setting
            connection.setRequestMethod(method);
            connection.setDoOutput(isPostMethod);
            connection.setDoInput(true);
            connection.setUseCaches(false);
            connection.setReadTimeout(5 * 1000);
            connection.setConnectTimeout(3 * 1000);
            connection.setRequestProperty("connection", "Keep-Alive");
            connection.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
            connection.setRequestProperty("Accept-Charset", "application/json;charset=UTF-8");

            // do connection
            connection.connect();

            // data
            if (isPostMethod && data!=null && data.trim().length()>0) {
                DataOutputStream dataOutputStream = new DataOutputStream(connection.getOutputStream());
                dataOutputStream.write(data.getBytes("UTF-8"));
                dataOutputStream.flush();
                dataOutputStream.close();
            }

            // valid StatusCode
            int statusCode = connection.getResponseCode();
            if (statusCode != 200) {
                throw new RuntimeException("Http Request StatusCode(" + statusCode + ") Invalid.");
            }

            // result
            bufferedReader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
            StringBuilder result = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                result.append(line);
            }
            String responseMsg = result.toString();

            OddJobHelper.log(responseMsg);

            return;
        } catch (Exception e) {
            OddJobHelper.log(e);

            OddJobHelper.handleFail();
            return;
        } finally {
            try {
                if (bufferedReader != null) {
                    bufferedReader.close();
                }
                if (connection != null) {
                    connection.disconnect();
                }
            } catch (Exception e2) {
                OddJobHelper.log(e2);
            }
        }

    }

    /**
     * 5、生命周期任务示例：任务初始化与销毁时，支持自定义相关逻辑；
     */
    @OddJob(value = "demoJobHandler2", init = "init", destroy = "destroy")
    public void demoJobHandler2() throws Exception {
        OddJobHelper.log("ODD-JOB, Hello World.");
    }
    // 在命令行中输出
    public void init(){
        logger.info("init");
    }
    public void destroy(){
        logger.info("destroy");
    }
}
