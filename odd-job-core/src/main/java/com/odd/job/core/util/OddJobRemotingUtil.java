package com.odd.job.core.util;

import com.odd.job.core.biz.model.ReturnT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.*;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * 这段代码是通过 Java 原生的 HttpURLConnection 来发送 HTTP POST 请求的，
 * 处理了连接、设置请求头、写入请求体、读取响应数据等步骤。它使用了 Java 原生的方法来进行网络请求和数据处理。
 *
 * @author oddity
 * @create 2023-12-06 16:38
 */
public class OddJobRemotingUtil {

    private static Logger logger = LoggerFactory.getLogger(OddJobRemotingUtil.class);

    public static final String ODD_JOB_ACCESS_TOKEN = "ODD-JOB-ACCESS-TOKEN";

    // trust-https start
    private static void trustAllHosts(HttpsURLConnection connection){
        try {
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new SecureRandom());
            SSLSocketFactory newFactory = sc.getSocketFactory();

            connection.setSSLSocketFactory(newFactory);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        connection.setHostnameVerifier(new HostnameVerifier() {
            @Override
            public boolean verify(String hostname, SSLSession session) {
                return true;
            }
        });
    }
    private static final TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[]{};
        }
        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        }
        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        }
    }};


    public static ReturnT postBody(String url, String accessToken, int timeout, Object requestObj, Class returnTargClassOfT){

        HttpURLConnection connection = null;
        BufferedReader bufferedReader = null;

        try {
            // connection
            URL realUrl = new URL(url);
            connection = (HttpURLConnection) realUrl.openConnection();

            // trust-https
            boolean useHttps = url.startsWith("https");
            if (useHttps){
                HttpsURLConnection https = (HttpsURLConnection) connection;
                trustAllHosts(https);
            }

            // connection setting
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setUseCaches(false);
            connection.setReadTimeout(timeout * 1000);
            connection.setConnectTimeout(3 * 1000);
            connection.setRequestProperty("connection", "Keep-Alive");
            connection.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
            connection.setRequestProperty("Accept-Charset", "application/json;charset=UTF-8");

            if (accessToken != null && accessToken.trim().length() > 0){
                connection.setRequestProperty(ODD_JOB_ACCESS_TOKEN, accessToken);
            }

            // do connection
            connection.connect();

            // write requestBody
            if (requestObj != null){
                String requestBody = GsonTool.toJson(requestObj);

                DataOutputStream dataOutputStream = new DataOutputStream(connection.getOutputStream());
                dataOutputStream.write(requestBody.getBytes("UTF-8"));
                dataOutputStream.flush();
                dataOutputStream.close();
            }

            int statusCode = connection.getResponseCode();
            if (statusCode != 200){
                return new ReturnT<String>(ReturnT.FAIL_CODE, "odd-job remoting fail, StatusCode("+ statusCode +") invalid. for url : " + url);
            }

            // result
            bufferedReader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
            StringBuilder result = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null){
                result.append(line);
            }
            String resultJson = result.toString();

            // parse returnT
            try {
                ReturnT returnT = GsonTool.fromJson(resultJson, ReturnT.class, returnTargClassOfT);
                return returnT;
            } catch (Exception e) {
                logger.error("odd-job remoting (url="+url+") response content invalid("+ resultJson +").", e);
                return new ReturnT<String>(ReturnT.FAIL_CODE, "odd-job remoting (url="+url+") response content invalid("+ resultJson +").");
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return new ReturnT<String>(ReturnT.FAIL_CODE, "odd-job remoting error("+ e.getMessage() +"), for url : " + url);
        } finally {
            try {
                if (bufferedReader != null) {
                    bufferedReader.close();
                }
                if (connection != null) {
                    connection.disconnect();
                }
            } catch (Exception e2) {
                logger.error(e2.getMessage(), e2);
            }
        }

    }
}
