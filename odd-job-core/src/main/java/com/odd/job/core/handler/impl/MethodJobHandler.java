package com.odd.job.core.handler.impl;

import com.odd.job.core.handler.IJobHandler;

import java.lang.reflect.Method;

/**
 * @author oddity
 * @create 2023-12-05 22:00
 */
public class MethodJobHandler extends IJobHandler {

    private final Object target; //bean
    private final Method method;
    private Method initMethod;
    private Method destroyMethod;

    public MethodJobHandler(Object target, Method method, Method initMethod, Method destroyMethod) {
        this.target = target;
        this.method = method;
        this.initMethod = initMethod;
        this.destroyMethod = destroyMethod;
    }

    @Override
    public void execute() throws Exception {
        Class<?>[] paramTypes = method.getParameterTypes();
        if (paramTypes.length > 0){
            //创建了一个空的对象数组，作为调用时的参数列表。这种方法主要是为了兼容方法的可变参数，但是不传递具体的参数值。
            method.invoke(target, new Object[paramTypes.length]);
        }else {
            method.invoke(target);
        }
    }

    @Override
    public void init() throws Exception {
        if (initMethod != null){
            initMethod.invoke(target);
        }
    }

    @Override
    public void destroy() throws Exception {
        if (destroyMethod != null){
            destroyMethod.invoke(target);
        }
    }

    @Override
    public String toString() {
        return super.toString()+"["+ target.getClass() + "#" + method.getName() +"]";
    }
}
