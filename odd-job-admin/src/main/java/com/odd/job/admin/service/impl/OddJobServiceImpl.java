package com.odd.job.admin.service.impl;

import com.odd.job.admin.core.model.OddJobGroup;
import com.odd.job.admin.core.model.OddJobInfo;
import com.odd.job.admin.core.model.OddJobLogReport;
import com.odd.job.admin.core.route.ExecutorRouteStrategyEnum;
import com.odd.job.admin.core.scheduler.MisfireStrategyEnum;
import com.odd.job.admin.core.scheduler.ScheduleTypeEnum;
import com.odd.job.admin.core.thread.JobScheduleHelper;
import com.odd.job.admin.core.util.I18nUtil;
import com.odd.job.admin.dao.*;
import com.odd.job.admin.service.OddJobService;
import com.odd.job.core.biz.model.ReturnT;
import com.odd.job.core.enums.ExecutorBlockStrategyEnum;
import com.odd.job.core.glue.GlueTypeEnum;
import com.odd.job.core.util.DateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.text.MessageFormat;
import java.util.*;

/**
 * core job action for odd-job
 * 
 * @author oddity
 * @create 2023-12-12 14:44
 */

@Service
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
    public ReturnT<String> add(OddJobInfo jobInfo) {

        // valid base
        OddJobGroup group = oddJobGroupDao.load(jobInfo.getJobGroup());
        if (group == null) {
            return new ReturnT<String>(ReturnT.FAIL_CODE, (I18nUtil.getString("system_please_choose")+I18nUtil.getString("jobinfo_field_jobgroup")) );
        }
        if (jobInfo.getJobDesc()==null || jobInfo.getJobDesc().trim().length()==0) {
            return new ReturnT<String>(ReturnT.FAIL_CODE, (I18nUtil.getString("system_please_input")+I18nUtil.getString("jobinfo_field_jobdesc")) );
        }
        if (jobInfo.getAuthor()==null || jobInfo.getAuthor().trim().length()==0) {
            return new ReturnT<String>(ReturnT.FAIL_CODE, (I18nUtil.getString("system_please_input")+I18nUtil.getString("jobinfo_field_author")) );
        }

        // valid trigger
        ScheduleTypeEnum scheduleTypeEnum = ScheduleTypeEnum.match(jobInfo.getScheduleType(), null);
        if (scheduleTypeEnum == null) {
            return new ReturnT<String>(ReturnT.FAIL_CODE, (I18nUtil.getString("schedule_type")+I18nUtil.getString("system_unvalid")) );
        }
        if (scheduleTypeEnum == ScheduleTypeEnum.CRON) {
            if (jobInfo.getScheduleConf()==null || !CronExpression.isValidExpression(jobInfo.getScheduleConf())) {
                return new ReturnT<String>(ReturnT.FAIL_CODE, "Cron"+I18nUtil.getString("system_unvalid"));
            }
        } else if (scheduleTypeEnum == ScheduleTypeEnum.FIX_RATE/* || scheduleTypeEnum == ScheduleTypeEnum.FIX_DELAY*/) {
            if (jobInfo.getScheduleConf() == null) {
                return new ReturnT<String>(ReturnT.FAIL_CODE, (I18nUtil.getString("schedule_type")) );
            }
            try {
                int fixSecond = Integer.valueOf(jobInfo.getScheduleConf());
                if (fixSecond < 1) {
                    return new ReturnT<String>(ReturnT.FAIL_CODE, (I18nUtil.getString("schedule_type")+I18nUtil.getString("system_unvalid")) );
                }
            } catch (Exception e) {
                return new ReturnT<String>(ReturnT.FAIL_CODE, (I18nUtil.getString("schedule_type")+I18nUtil.getString("system_unvalid")) );
            }
        }

        // valid job
        if (GlueTypeEnum.match(jobInfo.getGlueType()) == null) {
            return new ReturnT<String>(ReturnT.FAIL_CODE, (I18nUtil.getString("jobinfo_field_gluetype")+I18nUtil.getString("system_unvalid")) );
        }
        if (GlueTypeEnum.BEAN==GlueTypeEnum.match(jobInfo.getGlueType()) && (jobInfo.getExecutorHandler()==null || jobInfo.getExecutorHandler().trim().length()==0) ) {
            return new ReturnT<String>(ReturnT.FAIL_CODE, (I18nUtil.getString("system_please_input")+"JobHandler") );
        }
        // 》fix "\r" in shell
        if (GlueTypeEnum.GLUE_SHELL==GlueTypeEnum.match(jobInfo.getGlueType()) && jobInfo.getGlueSource()!=null) {
            jobInfo.setGlueSource(jobInfo.getGlueSource().replaceAll("\r", ""));
        }

        // valid advanced
        if (ExecutorRouteStrategyEnum.match(jobInfo.getExecutorRouteStrategy(), null) == null) {
            return new ReturnT<String>(ReturnT.FAIL_CODE, (I18nUtil.getString("jobinfo_field_executorRouteStrategy")+I18nUtil.getString("system_unvalid")) );
        }
        if (MisfireStrategyEnum.match(jobInfo.getMisfireStrategy(), null) == null) {
            return new ReturnT<String>(ReturnT.FAIL_CODE, (I18nUtil.getString("misfire_strategy")+I18nUtil.getString("system_unvalid")) );
        }
        if (ExecutorBlockStrategyEnum.match(jobInfo.getExecutorBlockStrategy(), null) == null) {
            return new ReturnT<String>(ReturnT.FAIL_CODE, (I18nUtil.getString("jobinfo_field_executorBlockStrategy")+I18nUtil.getString("system_unvalid")) );
        }

        // 》ChildJobId valid
        if (jobInfo.getChildJobId()!=null && jobInfo.getChildJobId().trim().length()>0) {
            String[] childJobIds = jobInfo.getChildJobId().split(",");
            for (String childJobIdItem : childJobIds) {
                if (childJobIdItem!=null && childJobIdItem.trim().length()>0 && isNumeric(childJobIdItem)) {
                    OddJobInfo childJobInfo = oddJobInfoDao.loadById(Integer.parseInt(childJobIdItem));
                    if (childJobInfo==null) {
                        return new ReturnT<String>(ReturnT.FAIL_CODE,
                                MessageFormat.format((I18nUtil.getString("jobinfo_field_childJobId")+"({0})"+I18nUtil.getString("system_not_found")), childJobIdItem));
                    }
                } else {
                    return new ReturnT<String>(ReturnT.FAIL_CODE,
                            MessageFormat.format((I18nUtil.getString("jobinfo_field_childJobId")+"({0})"+I18nUtil.getString("system_unvalid")), childJobIdItem));
                }
            }

            // join , avoid "xxx,,"
            String temp = "";
            for (String item : childJobIds) {
                temp += item + ",";
            }
            temp = temp.substring(0, temp.length()-1);

            jobInfo.setChildJobId(temp);
        }

        // add in db
        jobInfo.setAddTime(new Date());
        jobInfo.setUpdateTime(new Date());
        jobInfo.setGlueUpdatetime(new Date());
        oddJobInfoDao.save(jobInfo);
        if (jobInfo.getId() < 1) {
            return new ReturnT<String>(ReturnT.FAIL_CODE, (I18nUtil.getString("jobinfo_field_add")+I18nUtil.getString("system_fail")) );
        }

        return new ReturnT<String>(String.valueOf(jobInfo.getId()));
    }

    private boolean isNumeric(String str){
        try {
            int result = Integer.valueOf(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @Override
    public ReturnT<String> update(OddJobInfo jobInfo) {

        // valid base
        if (jobInfo.getJobDesc()==null || jobInfo.getJobDesc().trim().length()==0) {
            return new ReturnT<String>(ReturnT.FAIL_CODE, (I18nUtil.getString("system_please_input")+I18nUtil.getString("jobinfo_field_jobdesc")) );
        }
        if (jobInfo.getAuthor()==null || jobInfo.getAuthor().trim().length()==0) {
            return new ReturnT<String>(ReturnT.FAIL_CODE, (I18nUtil.getString("system_please_input")+I18nUtil.getString("jobinfo_field_author")) );
        }

        // valid trigger
        ScheduleTypeEnum scheduleTypeEnum = ScheduleTypeEnum.match(jobInfo.getScheduleType(), null);
        if (scheduleTypeEnum == null) {
            return new ReturnT<String>(ReturnT.FAIL_CODE, (I18nUtil.getString("schedule_type")+I18nUtil.getString("system_unvalid")) );
        }
        if (scheduleTypeEnum == ScheduleTypeEnum.CRON) {
            if (jobInfo.getScheduleConf()==null || !CronExpression.isValidExpression(jobInfo.getScheduleConf())) {
                return new ReturnT<String>(ReturnT.FAIL_CODE, "Cron"+I18nUtil.getString("system_unvalid") );
            }
        } else if (scheduleTypeEnum == ScheduleTypeEnum.FIX_RATE /*|| scheduleTypeEnum == ScheduleTypeEnum.FIX_DELAY*/) {
            if (jobInfo.getScheduleConf() == null) {
                return new ReturnT<String>(ReturnT.FAIL_CODE, (I18nUtil.getString("schedule_type")+I18nUtil.getString("system_unvalid")) );
            }
            try {
                int fixSecond = Integer.valueOf(jobInfo.getScheduleConf());
                if (fixSecond < 1) {
                    return new ReturnT<String>(ReturnT.FAIL_CODE, (I18nUtil.getString("schedule_type")+I18nUtil.getString("system_unvalid")) );
                }
            } catch (Exception e) {
                return new ReturnT<String>(ReturnT.FAIL_CODE, (I18nUtil.getString("schedule_type")+I18nUtil.getString("system_unvalid")) );
            }
        }

        // valid advanced
        if (ExecutorRouteStrategyEnum.match(jobInfo.getExecutorRouteStrategy(), null) == null) {
            return new ReturnT<String>(ReturnT.FAIL_CODE, (I18nUtil.getString("jobinfo_field_executorRouteStrategy")+I18nUtil.getString("system_unvalid")) );
        }
        if (MisfireStrategyEnum.match(jobInfo.getMisfireStrategy(), null) == null) {
            return new ReturnT<String>(ReturnT.FAIL_CODE, (I18nUtil.getString("misfire_strategy")+I18nUtil.getString("system_unvalid")) );
        }
        if (ExecutorBlockStrategyEnum.match(jobInfo.getExecutorBlockStrategy(), null) == null) {
            return new ReturnT<String>(ReturnT.FAIL_CODE, (I18nUtil.getString("jobinfo_field_executorBlockStrategy")+I18nUtil.getString("system_unvalid")) );
        }

        // 》ChildJobId valid
        if (jobInfo.getChildJobId()!=null && jobInfo.getChildJobId().trim().length()>0) {
            String[] childJobIds = jobInfo.getChildJobId().split(",");
            for (String childJobIdItem: childJobIds) {
                if (childJobIdItem!=null && childJobIdItem.trim().length()>0 && isNumeric(childJobIdItem)) {
                    OddJobInfo childJobInfo = oddJobInfoDao.loadById(Integer.parseInt(childJobIdItem));
                    if (childJobInfo==null) {
                        return new ReturnT<String>(ReturnT.FAIL_CODE,
                                MessageFormat.format((I18nUtil.getString("jobinfo_field_childJobId")+"({0})"+I18nUtil.getString("system_not_found")), childJobIdItem));
                    }
                } else {
                    return new ReturnT<String>(ReturnT.FAIL_CODE,
                            MessageFormat.format((I18nUtil.getString("jobinfo_field_childJobId")+"({0})"+I18nUtil.getString("system_unvalid")), childJobIdItem));
                }
            }

            // join , avoid "xxx,,"
            String temp = "";
            for (String item:childJobIds) {
                temp += item + ",";
            }
            temp = temp.substring(0, temp.length()-1);

            jobInfo.setChildJobId(temp);
        }

        // group valid
        OddJobGroup jobGroup = oddJobGroupDao.load(jobInfo.getJobGroup());
        if (jobGroup == null) {
            return new ReturnT<String>(ReturnT.FAIL_CODE, (I18nUtil.getString("jobinfo_field_jobgroup")+I18nUtil.getString("system_unvalid")) );
        }

        // stage job info
        OddJobInfo exists_jobInfo = oddJobInfoDao.loadById(jobInfo.getId());
        if (exists_jobInfo == null) {
            return new ReturnT<String>(ReturnT.FAIL_CODE, (I18nUtil.getString("jobinfo_field_id")+I18nUtil.getString("system_not_found")) );
        }

        // next trigger time (5s后生效，避开预读周期)
        long nextTriggerTime = exists_jobInfo.getTriggerNextTime();
        boolean scheduleDataNotChanged = jobInfo.getScheduleType().equals(exists_jobInfo.getScheduleType()) && jobInfo.getScheduleConf().equals(exists_jobInfo.getScheduleConf());
        // 正在运行的任务，调度周期发生变化了
        if (exists_jobInfo.getTriggerStatus() == 1 && !scheduleDataNotChanged) {
            try {
                Date nextValidTime = JobScheduleHelper.generateNextValidTime(jobInfo, new Date(System.currentTimeMillis() + JobScheduleHelper.PRE_READ_MS));
                if (nextValidTime == null) {
                    return new ReturnT<String>(ReturnT.FAIL_CODE, (I18nUtil.getString("schedule_type")+I18nUtil.getString("system_unvalid")) );
                }
                nextTriggerTime = nextValidTime.getTime();
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                return new ReturnT<String>(ReturnT.FAIL_CODE, (I18nUtil.getString("schedule_type")+I18nUtil.getString("system_unvalid")) );
            }
        }

        exists_jobInfo.setJobGroup(jobInfo.getJobGroup());
        exists_jobInfo.setJobDesc(jobInfo.getJobDesc());
        exists_jobInfo.setAuthor(jobInfo.getAuthor());
        exists_jobInfo.setAlarmEmail(jobInfo.getAlarmEmail());
        exists_jobInfo.setScheduleType(jobInfo.getScheduleType());
        exists_jobInfo.setScheduleConf(jobInfo.getScheduleConf());
        exists_jobInfo.setMisfireStrategy(jobInfo.getMisfireStrategy());
        exists_jobInfo.setExecutorRouteStrategy(jobInfo.getExecutorRouteStrategy());
        exists_jobInfo.setExecutorHandler(jobInfo.getExecutorHandler());
        exists_jobInfo.setExecutorParam(jobInfo.getExecutorParam());
        exists_jobInfo.setExecutorBlockStrategy(jobInfo.getExecutorBlockStrategy());
        exists_jobInfo.setExecutorTimeout(jobInfo.getExecutorTimeout());
        exists_jobInfo.setExecutorFailRetryCount(jobInfo.getExecutorFailRetryCount());
        exists_jobInfo.setChildJobId(jobInfo.getChildJobId());
        exists_jobInfo.setTriggerNextTime(nextTriggerTime);

        exists_jobInfo.setUpdateTime(new Date());
        oddJobInfoDao.update(exists_jobInfo);


        return ReturnT.SUCCESS;
    }

    @Override
    public ReturnT<String> remove(int id) {
        OddJobInfo oddJobInfo = oddJobInfoDao.loadById(id);
        if (oddJobInfo == null) {
            return ReturnT.SUCCESS;
        }

        oddJobInfoDao.delete(id);
        oddJobLogDao.delete(id);
        oddJobLogGlueDao.deleteByJobId(id);
        return ReturnT.SUCCESS;
    }

    @Override
    public ReturnT<String> start(int id) {
        OddJobInfo oddJobInfo = oddJobInfoDao.loadById(id);

        // valid
        ScheduleTypeEnum scheduleTypeEnum = ScheduleTypeEnum.match(oddJobInfo.getScheduleType(), ScheduleTypeEnum.NONE);
        if (ScheduleTypeEnum.NONE == scheduleTypeEnum) {
            return new ReturnT<String>(ReturnT.FAIL_CODE, (I18nUtil.getString("schedule_type_none_limit_start")) );
        }

        // next trigger time (5s后生效，避开预读周期)
        long nextTriggerTime = 0;
        try {
            Date nextValidTime = JobScheduleHelper.generateNextValidTime(oddJobInfo, new Date(System.currentTimeMillis() + JobScheduleHelper.PRE_READ_MS));
            if (nextValidTime == null) {
                return new ReturnT<String>(ReturnT.FAIL_CODE, (I18nUtil.getString("schedule_type")+I18nUtil.getString("system_unvalid")) );
            }
            nextTriggerTime = nextValidTime.getTime();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return new ReturnT<String>(ReturnT.FAIL_CODE, (I18nUtil.getString("schedule_type")+I18nUtil.getString("system_unvalid")) );
        }

        oddJobInfo.setTriggerStatus(1);
        oddJobInfo.setTriggerLastTime(0);
        oddJobInfo.setTriggerNextTime(nextTriggerTime);

        oddJobInfo.setUpdateTime(new Date());
        oddJobInfoDao.update(oddJobInfo);
        return ReturnT.SUCCESS;
    }

    @Override
    public ReturnT<String> stop(int id) {
        OddJobInfo oddJobInfo = oddJobInfoDao.loadById(id);

        oddJobInfo.setTriggerStatus(0);
        oddJobInfo.setTriggerLastTime(0);
        oddJobInfo.setTriggerNextTime(0);

        oddJobInfo.setUpdateTime(new Date());
        oddJobInfoDao.update(oddJobInfo);
        return ReturnT.SUCCESS;
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
