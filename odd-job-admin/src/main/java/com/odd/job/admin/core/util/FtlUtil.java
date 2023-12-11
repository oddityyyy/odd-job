package com.odd.job.admin.core.util;

import freemarker.ext.beans.BeansWrapper;
import freemarker.ext.beans.BeansWrapperBuilder;
import freemarker.template.Configuration;
import freemarker.template.TemplateHashModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ftl util
 *
 * @author oddity
 * @create 2023-12-11 19:19
 */
public class FtlUtil {

    private static Logger logger = LoggerFactory.getLogger(FtlUtil.class);

    // BeansWrapper 负责将 Java 对象包装成 FreeMarker 可以识别的模板模型对象，使其可以在模板中使用
    private static BeansWrapper wrapper = new BeansWrapperBuilder(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS).build();     //BeansWrapper.getDefaultInstance();

    // 允许通过指定包名获取静态类的模型对象，以便在模板中进行访问和操作
    public static TemplateHashModel generateStaticModel(String packageName) {
        try {
            TemplateHashModel staticModels = wrapper.getStaticModels();
            TemplateHashModel fileStatics = (TemplateHashModel) staticModels.get(packageName);
            return fileStatics;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }
}
