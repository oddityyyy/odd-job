package com.odd.job.admin.service.impl;

import com.odd.job.admin.core.model.OddJobGroup;
import com.odd.job.admin.core.model.OddJobInfo;
import com.odd.job.admin.core.model.OddJobLogReport;
import com.odd.job.admin.dao.*;
import com.odd.job.admin.service.OddJobService;
import com.odd.job.core.biz.model.ReturnT;
import com.odd.job.core.util.DateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import java.util.*;

/**
 * core job action for odd-job
 * 
 * @author oddity
 * @create 2023-12-12 14:44
 */
public class OddJobServiceImpl implements OddJobService {

    private static Logger logger = LoggerFactory.getLogger(OddJobServiceImpl.class);

    @Resource
    private OddJobGroupDao oddJobGroupDao;
    @Resource
    private OddJobInfoDao oddJobInfoDao;
    @Resource
    public OddJobLogDao oddJobLogDao;
    @Resource
    private OddJobLogGlueDao oddJobLogGlueDao;
    @Resource
    private OddJobLogReportDao oddJobLogReportDao;
    
    @Override
    public Map<String, Object> pageList(int start, int length, int jobGroup, int triggerStatus, String jobDesc, String executorHandler, String author) {
        // page list
        List<OddJobInfo> list = oddJobInfoDao.pageList(start, length, jobGroup, triggerStatus, jobDesc, executorHandler, author);
        int list_count = oddJobInfoDao.pageListCount(start, length, jobGroup, triggerStatus, jobDesc, executorHandler, author);

        // package result
        Map<String, Object> maps = new HashMap<String, Object>();
        maps.put("recordsTotal", list_count);		// 总记录数
        maps.put("recordsFiltered", list_count);	// 过滤后的总记录数
        maps.put("data", list);  					// 分页列表
        return maps;
    }

    @Override
    public Map<String, Object> dashboardInfo() {

        int jobInfoCount = oddJobInfoDao.findAllCount();
        int jobLogCount = 0;
        int jobLogSuccessCount = 0;
        OddJobLogReport oddJobLogReport = oddJobLogReportDao.queryLogReportTotal();
        if (oddJobLogReport != null) {
            jobLogCount = oddJobLogReport.getRunningCount() + oddJobLogReport.getSucCount() + oddJobLogReport.getFailCount();
            jobLogSuccessCount = oddJobLogReport.getSucCount();
        }

        // executor count
        Set<String> executorAddressSet = new HashSet<String>();
        List<OddJobGroup> groupList = oddJobGroupDao.findAll();

        if (groupList != null && !groupList.isEmpty()) {
            for (OddJobGroup group : groupList) {
                if (group.getRegistryList() != null && !group.getRegistryList().isEmpty()) {
                    executorAddressSet.addAll(group.getRegistryList());
                }
            }
        }

        int executorCount = executorAddressSet.size();

        Map<String, Object> dashboardMap = new HashMap<String, Object>();
        dashboardMap.put("jobInfoCount", jobInfoCount);
        dashboardMap.put("jobLogCount", jobLogCount);
        dashboardMap.put("jobLogSuccessCount", jobLogSuccessCount);
        dashboardMap.put("executorCount", executorCount);
        return dashboardMap;
    }

    @Override
    public ReturnT<Map<String, Object>> chartInfo(Date startDate, Date endDate) {

        // process
        List<String> triggerDayList = new ArrayList<String>();
        List<Integer> triggerDayCountRunningList = new ArrayList<Integer>();
        List<Integer> triggerDayCountSucList = new ArrayList<Integer>();
        List<Integer> triggerDayCountFailList = new ArrayList<Integer>();
        int triggerCountRunningTotal = 0;
        int triggerCountSucTotal = 0;
        int triggerCountFailTotal = 0;

        List<OddJobLogReport> logReportList = oddJobLogReportDao.queryLogReport(startDate, endDate);

        if (logReportList != null && logReportList.size() > 0) {
            for (OddJobLogReport item : logReportList) {
                String day = DateUtil.formatDate(item.getTriggerDay());
                int triggerDayCountRunning = item.getRunningCount();
                int triggerDayCountSuc = item.getSucCount();
                int triggerDayCountFail = item.getFailCount();

                triggerDayList.add(day);
                triggerDayCountRunningList.add(triggerDayCountRunning);
                triggerDayCountSucList.add(triggerDayCountSuc);
                triggerDayCountFailList.add(triggerDayCountFail);

                triggerCountRunningTotal += triggerDayCountRunning;
                triggerCountSucTotal += triggerDayCountSuc;
                triggerCountFailTotal += triggerDayCountFail;
            }
        } else {
            for (int i = -6; i <= 0; i++) {
                triggerDayList.add(DateUtil.formatDate(DateUtil.addDays(new Date(), i)));
                triggerDayCountRunningList.add(0);
                triggerDayCountSucList.add(0);
                triggerDayCountFailList.add(0);
            }
        }

        Map<String, Object> result = new HashMap<String, Object>();
        result.put("triggerDayList", triggerDayList);
        result.put("triggerDayCountRunningList", triggerDayCountRunningList);
        result.put("triggerDayCountSucList", triggerDayCountSucList);
        result.put("triggerDayCountFailList", triggerDayCountFailList);

        result.put("triggerCountRunningTotal", triggerCountRunningTotal);
        result.put("triggerCountSucTotal", triggerCountSucTotal);
        result.put("triggerCountFailTotal", triggerCountFailTotal);

        return new ReturnT<Map<String, Object>>(result);
    }
}
