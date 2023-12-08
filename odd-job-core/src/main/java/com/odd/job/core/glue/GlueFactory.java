package com.odd.job.core.glue;

import com.odd.job.core.glue.impl.SpringGlueFactory;
import com.odd.job.core.handler.IJobHandler;
import groovy.lang.GroovyClassLoader;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * glue factory, product class/object by name
 *
 * 动态加载代码并生成相应的类实例
 *
 * @author oddity
 * @create 2023-12-05 22:49
 */
public class GlueFactory {

    private static GlueFactory glueFactory = new GlueFactory();
    public static GlueFactory getInstance() {
        return glueFactory;
    }
    public static void refreshInstance(int type){
        if (type == 0){
            glueFactory = new GlueFactory();
        } else if (type == 1){
            glueFactory = new SpringGlueFactory();
        }
    }

    /**
     * groovy class loader
     */
    private GroovyClassLoader groovyClassLoader = new GroovyClassLoader();
    private ConcurrentMap<String, Class<?>> CLASS_CACHE = new ConcurrentHashMap<>();

    /**
     * load new instance, prototype
     * 根据提供的代码源加载新的类实例（这里通过给定的代码字符串来生成类实例）
     *
     * @param codeSource
     * @return
     * @throws Exception
     */
    public IJobHandler loadNewInstance(String codeSource) throws Exception{
        if (codeSource != null && codeSource.trim().length() > 0){
            Class<?> clazz = getCodeSourceClass(codeSource);
            if (clazz != null){
                Object instance = clazz.newInstance();
                if (instance != null){
                    if (instance instanceof IJobHandler){
                        // 从SpringContext中注入此JobHandler所需的bean
                        this.injectService(instance);
                        return (IJobHandler) instance;
                    } else {
                        //必须是IJobHandler的子类
                        throw new IllegalArgumentException(">>>>>>>>>>> odd-glue, loadNewInstance error, "
                                + "cannot convert from instance["+ instance.getClass() +"] to IJobHandler");
                    }
                }
            }
        }
        throw new IllegalArgumentException(">>>>>>>>>>> odd-glue, loadNewInstance error, instance is null");
    }

    //根据代码源获取对应的类。它首先将代码源进行 MD5 加密得到唯一标识，然后检查类缓存中是否存在该标识的类，如果存在则直接返回缓存中的类，否则通过 GroovyClassLoader 解析代码并返回对应的类。
    private Class<?> getCodeSourceClass(String codeSource){
        try {
            byte[] md5 = MessageDigest.getInstance("MD5").digest(codeSource.getBytes());
            String md5Str = new BigInteger(1, md5).toString(16);

            Class<?> clazz = CLASS_CACHE.get(md5Str);
            if (clazz == null){
                clazz = groovyClassLoader.parseClass(codeSource);
                CLASS_CACHE.putIfAbsent(md5Str, clazz);
            }
            return clazz;
        } catch (Exception e) {
            return groovyClassLoader.parseClass(codeSource);
        }
    }

    /**
     * inject service of bean field
     *
     * @param instance
     */
    public void injectService(Object instance){
        // do something
    }
}
