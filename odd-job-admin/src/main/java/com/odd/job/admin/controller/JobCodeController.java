package com.odd.job.admin.controller;

import com.odd.job.admin.core.model.OddJobInfo;
import com.odd.job.admin.core.model.OddJobLogGlue;
import com.odd.job.admin.core.util.I18nUtil;
import com.odd.job.admin.dao.OddJobInfoDao;
import com.odd.job.admin.dao.OddJobLogGlueDao;
import com.odd.job.core.biz.model.ReturnT;
import com.odd.job.core.glue.GlueTypeEnum;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.List;

/**
 * job code controller
 *
 * @author oddity
 * @create 2023-12-14 21:16
 */

@Controller
@RequestMapping("/jobcode")
public class JobCodeController {

    @Resource
    private OddJobInfoDao oddJobInfoDao;
    @Resource
    private OddJobLogGlueDao oddJobLogGlueDao;

    @RequestMapping
    public String index(HttpServletRequest request, Model model, int jobId) {
        OddJobInfo jobInfo = oddJobInfoDao.loadById(jobId);
        List<OddJobLogGlue> jobLogGlues = oddJobLogGlueDao.findByJobId(jobId);

        if (jobInfo == null) {
            throw new RuntimeException(I18nUtil.getString("jobinfo_glue_jobid_unvalid"));
        }
        if (GlueTypeEnum.BEAN == GlueTypeEnum.match(jobInfo.getGlueType())) {
            throw new RuntimeException(I18nUtil.getString("jobinfo_glue_gluetype_unvalid"));
        }

        // valid permission
        JobInfoController.validPermission(request, jobInfo.getJobGroup());

        // Glue类型-字典
        model.addAttribute("GlueTypeEnum", GlueTypeEnum.values());

        model.addAttribute("jobInfo", jobInfo);
        model.addAttribute("jobLogGlues", jobLogGlues);
        return "jobcode/jobcode.index";
    }

    @RequestMapping("/save")
    @ResponseBody
    public ReturnT<String> save(Model model, int id, String glueSource, String glueRemark) {
        // valid
        if (glueRemark==null) {
            return new ReturnT<String>(500, (I18nUtil.getString("system_please_input") + I18nUtil.getString("jobinfo_glue_remark")) );
        }
        if (glueRemark.length()<4 || glueRemark.length()>100) {
            return new ReturnT<String>(500, I18nUtil.getString("jobinfo_glue_remark_limit"));
        }
        OddJobInfo exists_jobInfo = oddJobInfoDao.loadById(id);
        if (exists_jobInfo == null) {
            return new ReturnT<String>(500, I18nUtil.getString("jobinfo_glue_jobid_unvalid"));
        }

        // update new code
        exists_jobInfo.setGlueSource(glueSource);
        exists_jobInfo.setGlueRemark(glueRemark);
        exists_jobInfo.setGlueUpdatetime(new Date());

        exists_jobInfo.setUpdateTime(new Date());
        oddJobInfoDao.update(exists_jobInfo);

        // log old code
        OddJobLogGlue oddJobLogGlue = new OddJobLogGlue();
        oddJobLogGlue.setJobId(exists_jobInfo.getId());
        oddJobLogGlue.setGlueType(exists_jobInfo.getGlueType());
        oddJobLogGlue.setGlueSource(glueSource);
        oddJobLogGlue.setGlueRemark(glueRemark);

        oddJobLogGlue.setAddTime(new Date());
        oddJobLogGlue.setUpdateTime(new Date());
        oddJobLogGlueDao.save(oddJobLogGlue);

        // remove code backup more than 30
        oddJobLogGlueDao.removeOld(exists_jobInfo.getId(), 30); // 只保留最新的30版

        return ReturnT.SUCCESS;
    }
}
