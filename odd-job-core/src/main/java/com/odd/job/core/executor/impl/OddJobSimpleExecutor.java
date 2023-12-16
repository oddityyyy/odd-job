package com.odd.job.core.executor.impl;

import com.odd.job.core.executor.OddJobExecutor;
import com.odd.job.core.handler.annotation.OddJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * odd-job executor (for frameless)
 * 
 * @author oddity
 * @create 2023-12-17 15:13
 */
public class OddJobSimpleExecutor extends OddJobExecutor {

    private static final Logger logger = LoggerFactory.getLogger(OddJobSimpleExecutor.class);


    private List<Object> oddJobBeanList = new ArrayList<>();
    public List<Object> getOddJobBeanList() {
        return oddJobBeanList;
    }
    public void setOddJobBeanList(List<Object> oddJobBeanList) {
        this.oddJobBeanList = oddJobBeanList;
    }


    @Override
    public void start() {

        // init JobHandler Repository (for method)
        initJobHandlerMethodRepository(oddJobBeanList);

        // super start
        try {
            super.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void destroy() {
        super.destroy();
    }


    private void initJobHandlerMethodRepository(List<Object> oddJobBeanList) {
        if (oddJobBeanList == null || oddJobBeanList.size() == 0) {
            return;
        }

        // init job handler from method
        for (Object bean : oddJobBeanList) {
            // method
            Method[] methods = bean.getClass().getDeclaredMethods();
            if (methods.length == 0) {
                continue;
            }
            for (Method executeMethod : methods) {
                OddJob oddJob = executeMethod.getAnnotation(OddJob.class);
                // registry
                registJobHandler(oddJob, bean, executeMethod);
            }

        }

    }
}
