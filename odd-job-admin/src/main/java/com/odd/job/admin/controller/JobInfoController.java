package com.odd.job.admin.controller;

import com.odd.job.admin.core.exception.OddJobException;
import com.odd.job.admin.core.model.OddJobGroup;
import com.odd.job.admin.core.model.OddJobInfo;
import com.odd.job.admin.core.model.OddJobUser;
import com.odd.job.admin.core.route.ExecutorRouteStrategyEnum;
import com.odd.job.admin.core.scheduler.MisfireStrategyEnum;
import com.odd.job.admin.core.scheduler.ScheduleTypeEnum;
import com.odd.job.admin.core.thread.JobScheduleHelper;
import com.odd.job.admin.core.thread.JobTriggerPoolHelper;
import com.odd.job.admin.core.trigger.TriggerTypeEnum;
import com.odd.job.admin.core.util.I18nUtil;
import com.odd.job.admin.dao.OddJobGroupDao;
import com.odd.job.admin.service.LoginService;
import com.odd.job.admin.service.OddJobService;
import com.odd.job.core.biz.model.ReturnT;
import com.odd.job.core.enums.ExecutorBlockStrategyEnum;
import com.odd.job.core.glue.GlueTypeEnum;
import com.odd.job.core.util.DateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;

/**
 * index controller
 *
 * @author oddity
 * @create 2023-12-14 14:23
 */

@Controller
@RequestMapping("/jobinfo")
public class JobInfoController {

    private static Logger logger = LoggerFactory.getLogger(JobInfoController.class);

    @Resource
    private OddJobGroupDao oddJobGroupDao;
    @Resource
    private OddJobService oddJobService;

    @RequestMapping
    public String index(HttpServletRequest request, Model model, @RequestParam(required = false, defaultValue = "-1") int jobGroup) {

        // 枚举-字典
        model.addAttribute("ExecutorRouteStrategyEnum", ExecutorRouteStrategyEnum.values());	    // 路由策略-列表
        model.addAttribute("GlueTypeEnum", GlueTypeEnum.values());								// Glue类型-字典
        model.addAttribute("ExecutorBlockStrategyEnum", ExecutorBlockStrategyEnum.values());	    // 阻塞处理策略-字典
        model.addAttribute("ScheduleTypeEnum", ScheduleTypeEnum.values());	    				// 调度类型
        model.addAttribute("MisfireStrategyEnum", MisfireStrategyEnum.values());	    			// 调度过期策略

        // 执行器列表
        List<OddJobGroup> jobGroupList_all =  oddJobGroupDao.findAll();

        // filter group
        List<OddJobGroup> jobGroupList = filterJobGroupByRole(request, jobGroupList_all);
        if (jobGroupList==null || jobGroupList.size()==0) {
            throw new OddJobException(I18nUtil.getString("jobgroup_empty"));
        }

        model.addAttribute("JobGroupList", jobGroupList);
        model.addAttribute("jobGroup", jobGroup);

        return "jobinfo/jobinfo.index";
    }

    // 根据用户权限过滤可操作的执行器
    public static List<OddJobGroup> filterJobGroupByRole(HttpServletRequest request, List<OddJobGroup> jobGroupList_all){
        List<OddJobGroup> jobGroupList = new ArrayList<>();
        if (jobGroupList_all!=null && jobGroupList_all.size()>0) {
            OddJobUser loginUser = (OddJobUser) request.getAttribute(LoginService.LOGIN_IDENTITY_KEY);
            if (loginUser.getRole() == 1) {
                jobGroupList = jobGroupList_all;
            } else {
                List<String> groupIdStrs = new ArrayList<>();
                if (loginUser.getPermission()!=null && loginUser.getPermission().trim().length()>0) {
                    groupIdStrs = Arrays.asList(loginUser.getPermission().trim().split(","));
                }
                for (OddJobGroup groupItem : jobGroupList_all) {
                    if (groupIdStrs.contains(String.valueOf(groupItem.getId()))) {
                        jobGroupList.add(groupItem);
                    }
                }
            }
        }
        return jobGroupList;
    }

    public static void validPermission(HttpServletRequest request, int jobGroup) {
        OddJobUser loginUser = (OddJobUser) request.getAttribute(LoginService.LOGIN_IDENTITY_KEY);
        if (!loginUser.validPermission(jobGroup)) {
            throw new RuntimeException(I18nUtil.getString("system_permission_limit") + "[username="+ loginUser.getUsername() +"]");
        }
    }

    @RequestMapping("/pageList")
    @ResponseBody
    public Map<String, Object> pageList(@RequestParam(required = false, defaultValue = "0") int start,
                                        @RequestParam(required = false, defaultValue = "10") int length,
                                        int jobGroup, int triggerStatus, String jobDesc, String executorHandler, String author) {

        return oddJobService.pageList(start, length, jobGroup, triggerStatus, jobDesc, executorHandler, author);
    }

    @RequestMapping("/add")
    @ResponseBody
    public ReturnT<String> add(OddJobInfo jobInfo) {
        return oddJobService.add(jobInfo);
    }

    @RequestMapping("/update")
    @ResponseBody
    public ReturnT<String> update(OddJobInfo jobInfo) {
        return oddJobService.update(jobInfo);
    }

    @RequestMapping("/remove")
    @ResponseBody
    public ReturnT<String> remove(int id) {
        return oddJobService.remove(id);
    }

    @RequestMapping("/stop")
    @ResponseBody
    public ReturnT<String> pause(int id) {
        return oddJobService.stop(id);
    }

    @RequestMapping("/start")
    @ResponseBody
    public ReturnT<String> start(int id) {
        return oddJobService.start(id);
    }

    @RequestMapping("/trigger")
    @ResponseBody
    //@PermissionLimit(limit = false)
    public ReturnT<String> triggerJob(int id, String executorParam, String addressList) {
        // force cover job param
        if (executorParam == null) {
            executorParam = "";
        }

        JobTriggerPoolHelper.trigger(id, TriggerTypeEnum.MANUAL, -1, null, executorParam, addressList);
        return ReturnT.SUCCESS;
    }

    @RequestMapping("/nextTriggerTime")
    @ResponseBody
    public ReturnT<List<String>> nextTriggerTime(String scheduleType, String scheduleConf) {

        OddJobInfo paramOddJobInfo = new OddJobInfo();
        paramOddJobInfo.setScheduleType(scheduleType);
        paramOddJobInfo.setScheduleConf(scheduleConf);

        List<String> result = new ArrayList<>();
        try {
            Date lastTime = new Date();
            for (int i = 0; i < 5; i++) {
                lastTime = JobScheduleHelper.generateNextValidTime(paramOddJobInfo, lastTime);
                if (lastTime != null) {
                    result.add(DateUtil.formatDateTime(lastTime));
                } else {
                    break;
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return new ReturnT<List<String>>(ReturnT.FAIL_CODE, (I18nUtil.getString("schedule_type")+I18nUtil.getString("system_unvalid")) + e.getMessage());
        }
        return new ReturnT<List<String>>(result);

    }
}
