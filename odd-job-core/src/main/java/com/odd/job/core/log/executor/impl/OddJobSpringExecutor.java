package com.odd.job.core.log.executor.impl;

import com.odd.job.core.log.executor.OddJobExecutor;
import com.odd.job.core.glue.GlueFactory;
import com.odd.job.core.handler.annotation.OddJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.annotation.AnnotatedElementUtils;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * odd-job executor (for-spring)
 *
 * 实现了 ApplicationContextAware 接口，因此可以获取 Spring 应用上下文，在应用启动后执行相关初始化操作。
 * 作用是为了在 Spring Boot 应用中，将标注了 @OddJob 注解的方法注册为可执行的定时任务，
 * 使得这些方法可以通过 OddJob 的任务调度系统进行调度执行。
 *
 * @author oddity
 * @create 2023-12-05 16:11
 */
public class OddJobSpringExecutor extends OddJobExecutor implements ApplicationContextAware, SmartInitializingSingleton, DisposableBean {

    private static final Logger logger = LoggerFactory.getLogger(OddJobSpringExecutor.class);

    // SmartInitializingSingleton接口的方法
    // 在单例实例化后调用的方法，进行一些初始化操作
    @Override
    public void afterSingletonsInstantiated() {

        // init JobHandler Repository (for method) 注册JobHandler
        initJobHandlerMethodRepository(applicationContext);

        // refresh GlueFactory 将Glue文件转换为JobHandler
        GlueFactory.refreshInstance(1);

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

    //用于初始化作业处理器方法仓库，扫描 Spring 容器中带有 @OddJob 注解的方法，并将其注册为作业处理器
    private void initJobHandlerMethodRepository(ApplicationContext applicationContext){
        if (applicationContext == null){
            return;
        }
        // init job handler from method
        String[] beanDefinitionNames = applicationContext.getBeanNamesForType(Object.class, false, true);
        for (String beanDefinitionName : beanDefinitionNames){

            // get bean
            Object bean = null;
            Lazy onBean = applicationContext.findAnnotationOnBean(beanDefinitionName, Lazy.class);
            if (onBean != null){
                logger.debug("odd-job annotation scan, skip @Lazy Bean:{}", beanDefinitionName);
                continue;
            }else {
                bean = applicationContext.getBean(beanDefinitionName);
            }

            // filter method
            Map<Method, OddJob> annotationMethods = null; // referred to ：org.springframework.context.event.EventListenerMethodProcessor.processBean
            try {
                //MethodIntrospector.selectMethods() 方法查找带有 @OddJob 注解的方法。这个方法是 Spring 的一个工具方法，
                // 可以根据条件（在这里是查找带有 @OddJob 注解的方法）选择特定的方法。
                annotationMethods = MethodIntrospector.selectMethods(bean.getClass(),
                        new MethodIntrospector.MetadataLookup<OddJob>() {
                            @Override
                            public OddJob inspect(Method method) {
                                return AnnotatedElementUtils.findMergedAnnotation(method, OddJob.class);
                            }
                        });
            } catch (Throwable ex) {
                logger.error("odd-job method-jobhandler resolve error for bean[" + beanDefinitionName + "].", ex);
            }
            if (annotationMethods == null || annotationMethods.isEmpty()){
                continue;
            }

            // generate and registry method job handler
            for (Map.Entry<Method, OddJob> methodOddJobEntry : annotationMethods.entrySet()){
                Method executeMethod = methodOddJobEntry.getKey();
                OddJob oddJob = methodOddJobEntry.getValue();
                // registry
                registJobHandler(oddJob, bean, executeMethod);
            }
        }
    }

    // ---------------------- applicationContext ----------------------
    private static ApplicationContext applicationContext;

    //实现 ApplicationContextAware 接口，将 Spring 上下文设置到 OddJobSpringExecutor 类中
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        OddJobSpringExecutor.applicationContext = applicationContext;
    }

    public static ApplicationContext getApplicationContext(){
        return applicationContext;
    }
}
