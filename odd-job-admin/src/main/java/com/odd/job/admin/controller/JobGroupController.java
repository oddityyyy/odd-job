package com.odd.job.admin.controller;

import com.odd.job.admin.controller.annotation.PermissionLimit;
import com.odd.job.admin.core.model.OddJobGroup;
import com.odd.job.admin.core.model.OddJobRegistry;
import com.odd.job.admin.core.util.I18nUtil;
import com.odd.job.admin.dao.OddJobGroupDao;
import com.odd.job.admin.dao.OddJobInfoDao;
import com.odd.job.admin.dao.OddJobRegistryDao;
import com.odd.job.core.biz.model.ReturnT;
import com.odd.job.core.enums.RegistryConfig;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;

/**
 * job group controller
 * 
 * @author oddity
 * @create 2023-12-14 1:38
 */

@Controller
@RequestMapping("/jobgroup")
public class JobGroupController {

    @Resource
    public OddJobInfoDao oddJobInfoDao;
    @Resource
    public OddJobGroupDao oddJobGroupDao;
    @Resource
    private OddJobRegistryDao oddJobRegistryDao;

    @RequestMapping
    @PermissionLimit(adminuser = true)
    public String index(Model model) {
        return "jobgroup/jobgroup.index";
    }

    @RequestMapping("/pageList")
    @ResponseBody
    @PermissionLimit(adminuser = true)
    public Map<String, Object> pageList(HttpServletRequest request,
                                        @RequestParam(required = false, defaultValue = "0") int start,
                                        @RequestParam(required = false, defaultValue = "10") int length,
                                        String appname, String title) {

        // page query
        List<OddJobGroup> list = oddJobGroupDao.pageList(start, length, appname, title);
        int list_count = oddJobGroupDao.pageListCount(start, length, appname, title);

        // package result
        Map<String, Object> maps = new HashMap<String, Object>();
        maps.put("recordsTotal", list_count);		// 总记录数
        maps.put("recordsFiltered", list_count);	// 过滤后的总记录数
        maps.put("data", list);  					// 分页列表
        return maps;
    }

    @RequestMapping("/save")
    @ResponseBody
    @PermissionLimit(adminuser = true)
    public ReturnT<String> save(OddJobGroup oddJobGroup){

        // valid
        if (oddJobGroup.getAppname()==null || oddJobGroup.getAppname().trim().length()==0) {
            return new ReturnT<String>(500, (I18nUtil.getString("system_please_input")+"AppName") );
        }
        if (oddJobGroup.getAppname().length()<4 || oddJobGroup.getAppname().length()>64) {
            return new ReturnT<String>(500, I18nUtil.getString("jobgroup_field_appname_length") );
        }
        if (oddJobGroup.getAppname().contains(">") || oddJobGroup.getAppname().contains("<")) {
            return new ReturnT<String>(500, "AppName"+I18nUtil.getString("system_unvalid") );
        }
        if (oddJobGroup.getTitle()==null || oddJobGroup.getTitle().trim().length()==0) {
            return new ReturnT<String>(500, (I18nUtil.getString("system_please_input") + I18nUtil.getString("jobgroup_field_title")) );
        }
        if (oddJobGroup.getTitle().contains(">") || oddJobGroup.getTitle().contains("<")) {
            return new ReturnT<String>(500, I18nUtil.getString("jobgroup_field_title")+I18nUtil.getString("system_unvalid") );
        }
        if (oddJobGroup.getAddressType()!=0) { // 手动注册
            if (oddJobGroup.getAddressList()==null || oddJobGroup.getAddressList().trim().length()==0) {
                return new ReturnT<String>(500, I18nUtil.getString("jobgroup_field_addressType_limit") );
            }
            if (oddJobGroup.getAddressList().contains(">") || oddJobGroup.getAddressList().contains("<")) {
                return new ReturnT<String>(500, I18nUtil.getString("jobgroup_field_registryList")+I18nUtil.getString("system_unvalid") );
            }

            String[] addresss = oddJobGroup.getAddressList().split(",");
            for (String item : addresss) {
                if (item==null || item.trim().length()==0) {
                    return new ReturnT<String>(500, I18nUtil.getString("jobgroup_field_registryList_unvalid") );
                }
            }
        }

        // process
        oddJobGroup.setUpdateTime(new Date());

        int ret = oddJobGroupDao.save(oddJobGroup);
        return (ret>0)?ReturnT.SUCCESS:ReturnT.FAIL;
    }

    @RequestMapping("/update")
    @ResponseBody
    @PermissionLimit(adminuser = true)
    public ReturnT<String> update(OddJobGroup oddJobGroup){
        // valid
        if (oddJobGroup.getAppname()==null || oddJobGroup.getAppname().trim().length()==0) {
            return new ReturnT<String>(500, (I18nUtil.getString("system_please_input")+"AppName") );
        }
        if (oddJobGroup.getAppname().length()<4 || oddJobGroup.getAppname().length()>64) {
            return new ReturnT<String>(500, I18nUtil.getString("jobgroup_field_appname_length") );
        }
        if (oddJobGroup.getTitle()==null || oddJobGroup.getTitle().trim().length()==0) {
            return new ReturnT<String>(500, (I18nUtil.getString("system_please_input") + I18nUtil.getString("jobgroup_field_title")) );
        }
        if (oddJobGroup.getAddressType() == 0) {
            // 0=自动注册
            List<String> registryList = findRegistryByAppName(oddJobGroup.getAppname());
            String addressListStr = null;
            if (registryList!=null && !registryList.isEmpty()) {
                Collections.sort(registryList);
                addressListStr = "";
                for (String item : registryList) {
                    addressListStr += item + ",";
                }
                addressListStr = addressListStr.substring(0, addressListStr.length()-1);
            }
            oddJobGroup.setAddressList(addressListStr);
        } else {
            // 1=手动录入
            if (oddJobGroup.getAddressList()==null || oddJobGroup.getAddressList().trim().length()==0) {
                return new ReturnT<String>(500, I18nUtil.getString("jobgroup_field_addressType_limit") );
            }
            String[] addresss = oddJobGroup.getAddressList().split(",");
            for (String item : addresss) {
                if (item==null || item.trim().length()==0) {
                    return new ReturnT<String>(500, I18nUtil.getString("jobgroup_field_registryList_unvalid") );
                }
            }
        }

        // process
        oddJobGroup.setUpdateTime(new Date());

        int ret = oddJobGroupDao.update(oddJobGroup);
        return (ret>0)?ReturnT.SUCCESS:ReturnT.FAIL;
    }

    // 0=自动注册
    private List<String> findRegistryByAppName(String appnameParam){
        HashMap<String, List<String>> appAddressMap = new HashMap<String, List<String>>();
        List<OddJobRegistry> list = oddJobRegistryDao.findAll(RegistryConfig.DEAD_TIMEOUT, new Date());
        if (list != null) {
            for (OddJobRegistry item : list) {
                if (RegistryConfig.RegistType.EXECUTOR.name().equals(item.getRegistryGroup())) {
                    String appname = item.getRegistryKey();
                    List<String> registryList = appAddressMap.get(appname);
                    if (registryList == null) {
                        registryList = new ArrayList<String>();
                    }

                    if (!registryList.contains(item.getRegistryValue())) {
                        registryList.add(item.getRegistryValue());
                    }
                    appAddressMap.put(appname, registryList);
                }
            }
        }
        return appAddressMap.get(appnameParam);
    }

    @RequestMapping("/remove")
    @ResponseBody
    @PermissionLimit(adminuser = true)
    public ReturnT<String> remove(int id){

        // valid
        int count = oddJobInfoDao.pageListCount(0, 10, id, -1,  null, null, null);
        if (count > 0) {
            return new ReturnT<String>(500, I18nUtil.getString("jobgroup_del_limit_0") ); //Refuse to delete, the executor is being used
        }

        List<OddJobGroup> allList = oddJobGroupDao.findAll();
        if (allList.size() == 1) {
            return new ReturnT<String>(500, I18nUtil.getString("jobgroup_del_limit_1") ); //Refuses to delete, the system retains at least one executor
        }

        int ret = oddJobGroupDao.remove(id);
        return (ret>0)?ReturnT.SUCCESS:ReturnT.FAIL;
    }

    @RequestMapping("/loadById")
    @ResponseBody
    @PermissionLimit(adminuser = true)
    public ReturnT<OddJobGroup> loadById(int id){
        OddJobGroup jobGroup = oddJobGroupDao.load(id);
        return jobGroup!=null?new ReturnT<OddJobGroup>(jobGroup):new ReturnT<OddJobGroup>(ReturnT.FAIL_CODE, null);
    }
}
