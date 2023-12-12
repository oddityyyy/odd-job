package com.odd.job.admin.service;

import com.odd.job.core.biz.model.ReturnT;

import java.util.Date;
import java.util.Map;

/**
 * core job action for odd-job
 *
 * @author oddity
 * @create 2023-12-11 22:34
 */
public interface OddJobService {

    /**
     * page list
     *
     * @param start
     * @param length
     * @param jobGroup
     * @param jobDesc
     * @param executorHandler
     * @param author
     * @return
     */
    public Map<String, Object> pageList(int start, int length, int jobGroup, int triggerStatus, String jobDesc, String executorHandler, String author);

    /**
     * dashboard info
     *
     * @return
     */
    public Map<String,Object> dashboardInfo();

    /**
     * chart info
     *
     * @param startDate
     * @param endDate
     * @return
     */
    public ReturnT<Map<String,Object>> chartInfo(Date startDate, Date endDate);
}
