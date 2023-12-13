package com.odd.job.core.glue.impl;

import com.odd.job.core.log.executor.impl.OddJobSpringExecutor;
import com.odd.job.core.glue.GlueFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.annotation.AnnotationUtils;

import javax.annotation.Resource;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * 用于将 Spring 中的 Bean 注入到作业处理类（JobHandler）的实例中。
 * 它检查实例中声明的字段，如果字段上标注了 @Resource 或 @Autowired 注解，
 * 就尝试从 Spring 容器中查找相应的 Bean，并将其注入到字段中。
 *
 * @author oddity
 * @create 2023-12-05 22:55
 */
public class SpringGlueFactory extends GlueFactory {

    private static Logger logger = LoggerFactory.getLogger(SpringGlueFactory.class);

    /**
     * inject action of spring
     * @param instance
     */
    @Override
    public void injectService(Object instance) {
        if (instance == null){
            return;
        }
        if (OddJobSpringExecutor.getApplicationContext() == null){
            return;
        }

        Field[] fields = instance.getClass().getDeclaredFields();
        for (Field field : fields){
            if (Modifier.isStatic(field.getModifiers())){
                continue;
            }

            Object fieldBean = null;
            // with bean-id, bean could be found by both @Resource and @Autowired, or bean could only be found by @Autowired

            if (AnnotationUtils.getAnnotation(field, Resource.class) != null){
                try {
                    Resource resource = AnnotationUtils.getAnnotation(field, Resource.class);
                    if (resource.name() != null && resource.name().length() > 0){
                        fieldBean = OddJobSpringExecutor.getApplicationContext().getBean(resource.name());
                    } else {
                        fieldBean = OddJobSpringExecutor.getApplicationContext().getBean(field.getName());
                    }
                } catch (BeansException e) {
                }
                if (fieldBean == null){
                    fieldBean = OddJobSpringExecutor.getApplicationContext().getBean(field.getType());
                }
            } else if (AnnotationUtils.getAnnotation(field, Autowired.class) != null){
                Qualifier qualifier = AnnotationUtils.getAnnotation(field, Qualifier.class);
                if (qualifier != null && qualifier.value() != null && qualifier.value().length() > 0){
                    fieldBean = OddJobSpringExecutor.getApplicationContext().getBean(qualifier.value());
                } else {
                    fieldBean = OddJobSpringExecutor.getApplicationContext().getBean(field.getType());
                }
            }

            if (fieldBean != null){
                field.setAccessible(true);
                try {
                    field.set(instance, fieldBean);
                } catch (IllegalArgumentException e) {
                    logger.error(e.getMessage(), e);
                } catch (IllegalAccessException e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }
    }
}
