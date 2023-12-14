package com.odd.job.admin.controller;

import com.odd.job.admin.core.complete.OddJobCompleter;
import com.odd.job.admin.core.exception.OddJobException;
import com.odd.job.admin.core.model.OddJobGroup;
import com.odd.job.admin.core.model.OddJobInfo;
import com.odd.job.admin.core.model.OddJobLog;
import com.odd.job.admin.core.scheduler.OddJobScheduler;
import com.odd.job.admin.core.util.I18nUtil;
import com.odd.job.admin.dao.OddJobGroupDao;
import com.odd.job.admin.dao.OddJobInfoDao;
import com.odd.job.admin.dao.OddJobLogDao;
import com.odd.job.core.biz.ExecutorBiz;
import com.odd.job.core.biz.model.KillParam;
import com.odd.job.core.biz.model.LogParam;
import com.odd.job.core.biz.model.LogResult;
import com.odd.job.core.biz.model.ReturnT;
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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author oddity
 * @create 2023-12-14 22:07
 */

@Controller
@RequestMapping("/joblog")
public class JobLogController {

    private static Logger logger = LoggerFactory.getLogger(JobLogController.class);

    @Resource
    private OddJobGroupDao oddJobGroupDao;
    @Resource
    public OddJobInfoDao oddJobInfoDao;
    @Resource
    public OddJobLogDao oddJobLogDao;

    @RequestMapping
    public String index(HttpServletRequest request, Model model, @RequestParam(required = false, defaultValue = "0") Integer jobId) {

        // 执行器列表
        List<OddJobGroup> jobGroupList_all =  oddJobGroupDao.findAll();

        // filter group
        List<OddJobGroup> jobGroupList = JobInfoController.filterJobGroupByRole(request, jobGroupList_all);
        if (jobGroupList==null || jobGroupList.size()==0) {
            throw new OddJobException(I18nUtil.getString("jobgroup_empty"));
        }

        model.addAttribute("JobGroupList", jobGroupList);

        // 任务
        if (jobId > 0) {
            OddJobInfo jobInfo = oddJobInfoDao.loadById(jobId);
            if (jobInfo == null) {
                throw new RuntimeException(I18nUtil.getString("jobinfo_field_id") + I18nUtil.getString("system_unvalid"));
            }

            model.addAttribute("jobInfo", jobInfo);

            // valid permission
            JobInfoController.validPermission(request, jobInfo.getJobGroup());
        }

        return "joblog/joblog.index";
    }

    @RequestMapping("/getJobsByGroup")
    @ResponseBody
    public ReturnT<List<OddJobInfo>> getJobsByGroup(int jobGroup){
        List<OddJobInfo> list = oddJobInfoDao.getJobsByGroup(jobGroup);
        return new ReturnT<List<OddJobInfo>>(list);
    }

    @RequestMapping("/pageList")
    @ResponseBody
    public Map<String, Object> pageList(HttpServletRequest request,
                                        @RequestParam(required = false, defaultValue = "0") int start,
                                        @RequestParam(required = false, defaultValue = "10") int length,
                                        int jobGroup, int jobId, int logStatus, String filterTime) {

        // valid permission
        JobInfoController.validPermission(request, jobGroup);	// 仅管理员支持查询全部；普通用户仅支持查询有权限的 jobGroup

        // parse param
        Date triggerTimeStart = null;
        Date triggerTimeEnd = null;
        if (filterTime!=null && filterTime.trim().length()>0) {
            String[] temp = filterTime.split(" - ");
            if (temp.length == 2) {
                triggerTimeStart = DateUtil.parseDateTime(temp[0]);
                triggerTimeEnd = DateUtil.parseDateTime(temp[1]);
            }
        }

        // page query
        List<OddJobLog> list = oddJobLogDao.pageList(start, length, jobGroup, jobId, triggerTimeStart, triggerTimeEnd, logStatus);
        int list_count = oddJobLogDao.pageListCount(start, length, jobGroup, jobId, triggerTimeStart, triggerTimeEnd, logStatus);

        // package result
        Map<String, Object> maps = new HashMap<String, Object>();
        maps.put("recordsTotal", list_count);		// 总记录数
        maps.put("recordsFiltered", list_count);	// 过滤后的总记录数
        maps.put("data", list);  					// 分页列表
        return maps;
    }

    @RequestMapping("/logDetailPage")
    public String logDetailPage(int id, Model model){

        // base check
        ReturnT<String> logStatue = ReturnT.SUCCESS;
        OddJobLog jobLog = oddJobLogDao.load(id);
        if (jobLog == null) {
            throw new RuntimeException(I18nUtil.getString("joblog_logid_unvalid"));
        }

        model.addAttribute("triggerCode", jobLog.getTriggerCode());
        model.addAttribute("handleCode", jobLog.getHandleCode());
        model.addAttribute("logId", jobLog.getId());
        return "joblog/joblog.detail";
    }

    @RequestMapping("/logDetailCat")
    @ResponseBody
    public ReturnT<LogResult> logDetailCat(long logId, int fromLineNum){
        try {
            // valid
            OddJobLog jobLog = oddJobLogDao.load(logId);	// todo, need to improve performance
            if (jobLog == null) {
                return new ReturnT<LogResult>(ReturnT.FAIL_CODE, I18nUtil.getString("joblog_logid_unvalid"));
            }

            // log cat
            ExecutorBiz executorBiz = OddJobScheduler.getExecutorBiz(jobLog.getExecutorAddress());
            ReturnT<LogResult> logResult = executorBiz.log(new LogParam(jobLog.getTriggerTime().getTime(), logId, fromLineNum));

            // is end
            if (logResult.getContent()!=null && logResult.getContent().getFromLineNum() > logResult.getContent().getToLineNum()) {
                if (jobLog.getHandleCode() > 0) {
                    logResult.getContent().setEnd(true);
                }
            }

            return logResult;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return new ReturnT<LogResult>(ReturnT.FAIL_CODE, e.getMessage());
        }
    }

    @RequestMapping("/logKill")
    @ResponseBody
    public ReturnT<String> logKill(int id){
        // base check
        OddJobLog log = oddJobLogDao.load(id);
        OddJobInfo jobInfo = oddJobInfoDao.loadById(log.getJobId());
        if (jobInfo==null) {
            return new ReturnT<String>(500, I18nUtil.getString("jobinfo_glue_jobid_unvalid"));
        }
        if (ReturnT.SUCCESS_CODE != log.getTriggerCode()) {
            return new ReturnT<String>(500, I18nUtil.getString("joblog_kill_log_limit"));
        }

        // request of kill
        ReturnT<String> runResult = null;
        try {
            ExecutorBiz executorBiz = OddJobScheduler.getExecutorBiz(log.getExecutorAddress());
            runResult = executorBiz.kill(new KillParam(jobInfo.getId()));
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            runResult = new ReturnT<String>(500, e.getMessage());
        }

        if (ReturnT.SUCCESS_CODE == runResult.getCode()) {
            log.setHandleCode(ReturnT.FAIL_CODE);
            log.setHandleMsg( I18nUtil.getString("joblog_kill_log_byman")+":" + (runResult.getMsg()!=null?runResult.getMsg():""));
            log.setHandleTime(new Date());
            OddJobCompleter.updateHandleInfoAndFinish(log);
            return new ReturnT<String>(runResult.getMsg());
        } else {
            return new ReturnT<String>(500, runResult.getMsg());
        }
    }

    @RequestMapping("/clearLog")
    @ResponseBody
    public ReturnT<String> clearLog(int jobGroup, int jobId, int type){

        Date clearBeforeTime = null;
        int clearBeforeNum = 0; //要保留的条数
        if (type == 1) {
            clearBeforeTime = DateUtil.addMonths(new Date(), -1);	// 清理一个月之前日志数据
        } else if (type == 2) {
            clearBeforeTime = DateUtil.addMonths(new Date(), -3);	// 清理三个月之前日志数据
        } else if (type == 3) {
            clearBeforeTime = DateUtil.addMonths(new Date(), -6);	// 清理六个月之前日志数据
        } else if (type == 4) {
            clearBeforeTime = DateUtil.addYears(new Date(), -1);	// 清理一年之前日志数据
        } else if (type == 5) {
            clearBeforeNum = 1000;		// 清理一千条以前日志数据
        } else if (type == 6) {
            clearBeforeNum = 10000;		// 清理一万条以前日志数据
        } else if (type == 7) {
            clearBeforeNum = 30000;		// 清理三万条以前日志数据
        } else if (type == 8) {
            clearBeforeNum = 100000;	// 清理十万条以前日志数据
        } else if (type == 9) {
            clearBeforeNum = 0;			// 清理所有日志数据
        } else {
            return new ReturnT<String>(ReturnT.FAIL_CODE, I18nUtil.getString("joblog_clean_type_unvalid"));
        }

        List<Long> logIds = null;
        do {
            logIds = oddJobLogDao.findClearLogIds(jobGroup, jobId, clearBeforeTime, clearBeforeNum, 1000);
            if (logIds!=null && logIds.size()>0) {
                oddJobLogDao.clearLog(logIds);
            }
        } while (logIds!=null && logIds.size()>0);

        return ReturnT.SUCCESS;
    }
}
