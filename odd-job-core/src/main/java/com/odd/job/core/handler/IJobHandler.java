package com.odd.job.core.handler;

/**
 * job handler
 *
 * @author oddity
 * @create 2023-12-05 17:21
 */
public abstract class IJobHandler {

    /**
     * execute handler, invoked when executor receives a scheduling request
     *
     * @throws Exception
     */
    public abstract void execute() throws Exception;

    /**
     * init handler, invoked when JobThread init
     */
    public void init() throws Exception {
        // do something
    }

    /**
     * destroy handler, invoke when JobThread destroy
     */
    public void destroy() throws Exception{
        // do something
    }
}
