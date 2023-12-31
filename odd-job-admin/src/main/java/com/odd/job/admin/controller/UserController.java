package com.odd.job.admin.controller;

import com.odd.job.admin.controller.annotation.PermissionLimit;
import com.odd.job.admin.core.model.OddJobGroup;
import com.odd.job.admin.core.model.OddJobUser;
import com.odd.job.admin.core.util.I18nUtil;
import com.odd.job.admin.dao.OddJobGroupDao;
import com.odd.job.admin.dao.OddJobUserDao;
import com.odd.job.admin.service.LoginService;
import com.odd.job.core.biz.model.ReturnT;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author oddity
 * @create 2023-12-13 23:27
 */

@Controller
@RequestMapping("/user")
public class UserController {

    @Resource
    private OddJobUserDao oddJobUserDao;
    @Resource
    private OddJobGroupDao oddJobGroupDao;

    // 向ModelAndView中缓存执行器列表
    @RequestMapping
    @PermissionLimit(adminuser = true)
    public String index(Model model) {

        // 执行器列表
        List<OddJobGroup> groupList = oddJobGroupDao.findAll();
        model.addAttribute("groupList", groupList);

        return "user/user.index";
    }

    @RequestMapping("/pageList")
    @ResponseBody
    @PermissionLimit(adminuser = true)
    public Map<String, Object> pageList(@RequestParam(required = false, defaultValue = "0") int start,
                                        @RequestParam(required = false, defaultValue = "10") int length,
                                        String username, int role) {

        // page list
        List<OddJobUser> list = oddJobUserDao.pageList(start, length, username, role);
        int list_count = oddJobUserDao.pageListCount(start, length, username, role);

        // filter
        if (list != null && list.size() > 0) {
            for (OddJobUser item : list) {
                item.setPassword(null);
            }
        }

        // package result
        Map<String, Object> maps = new HashMap<String, Object>();
        maps.put("recordsTotal", list_count);		// 总记录数
        maps.put("recordsFiltered", list_count);	// 过滤后的总记录数
        maps.put("data", list);  					// 分页列表
        return maps;
    }

    @RequestMapping("/add")
    @ResponseBody
    @PermissionLimit(adminuser = true)
    public ReturnT<String> add(OddJobUser oddJobUser) {

        // valid username
        if (!StringUtils.hasText(oddJobUser.getUsername())) {
            return new ReturnT<String>(ReturnT.FAIL_CODE, I18nUtil.getString("system_please_input")+I18nUtil.getString("user_username") );
        }
        oddJobUser.setUsername(oddJobUser.getUsername().trim());
        if (!(oddJobUser.getUsername().length()>=4 && oddJobUser.getUsername().length()<=20)) {
            return new ReturnT<String>(ReturnT.FAIL_CODE, I18nUtil.getString("system_lengh_limit")+"[4-20]" );
        }
        // valid password
        if (!StringUtils.hasText(oddJobUser.getPassword())) {
            return new ReturnT<String>(ReturnT.FAIL_CODE, I18nUtil.getString("system_please_input")+I18nUtil.getString("user_password") );
        }
        oddJobUser.setPassword(oddJobUser.getPassword().trim());
        if (!(oddJobUser.getPassword().length()>=4 && oddJobUser.getPassword().length()<=20)) {
            return new ReturnT<String>(ReturnT.FAIL_CODE, I18nUtil.getString("system_lengh_limit")+"[4-20]" );
        }
        // md5 password
        oddJobUser.setPassword(DigestUtils.md5DigestAsHex(oddJobUser.getPassword().getBytes()));

        // check repeat
        OddJobUser existUser = oddJobUserDao.loadByUserName(oddJobUser.getUsername());
        if (existUser != null) {
            return new ReturnT<String>(ReturnT.FAIL_CODE, I18nUtil.getString("user_username_repeat") );
        }

        // write
        oddJobUserDao.save(oddJobUser);
        return ReturnT.SUCCESS;
    }

    @RequestMapping("/update")
    @ResponseBody
    @PermissionLimit(adminuser = true)
    public ReturnT<String> update(HttpServletRequest request, OddJobUser oddJobUser) {

        // avoid opt login seft
        OddJobUser loginUser = (OddJobUser) request.getAttribute(LoginService.LOGIN_IDENTITY_KEY);
        if (loginUser.getUsername().equals(oddJobUser.getUsername())) {
            return new ReturnT<String>(ReturnT.FAIL.getCode(), I18nUtil.getString("user_update_loginuser_limit"));
        }

        // valid password
        if (StringUtils.hasText(oddJobUser.getPassword())) {
            oddJobUser.setPassword(oddJobUser.getPassword().trim());
            if (!(oddJobUser.getPassword().length()>=4 && oddJobUser.getPassword().length()<=20)) {
                return new ReturnT<String>(ReturnT.FAIL_CODE, I18nUtil.getString("system_lengh_limit")+"[4-20]" );
            }
            // md5 password
            oddJobUser.setPassword(DigestUtils.md5DigestAsHex(oddJobUser.getPassword().getBytes()));
        } else {
            oddJobUser.setPassword(null);
        }

        // write
        oddJobUserDao.update(oddJobUser);
        return ReturnT.SUCCESS;
    }

    @RequestMapping("/remove")
    @ResponseBody
    @PermissionLimit(adminuser = true)
    public ReturnT<String> remove(HttpServletRequest request, int id) {

        // avoid opt login seft
        OddJobUser loginUser = (OddJobUser) request.getAttribute(LoginService.LOGIN_IDENTITY_KEY);
        if (loginUser.getId() == id) {
            return new ReturnT<String>(ReturnT.FAIL.getCode(), I18nUtil.getString("user_update_loginuser_limit"));
        }

        oddJobUserDao.delete(id);
        return ReturnT.SUCCESS;
    }

    @RequestMapping("/updatePwd")
    @ResponseBody
    public ReturnT<String> updatePwd(HttpServletRequest request, String password){

        // valid password
        if (password==null || password.trim().length()==0){
            return new ReturnT<String>(ReturnT.FAIL.getCode(), "密码不可为空");
        }
        password = password.trim();
        if (!(password.length()>=4 && password.length()<=20)) {
            return new ReturnT<String>(ReturnT.FAIL_CODE, I18nUtil.getString("system_lengh_limit")+"[4-20]" );
        }

        // md5 password
        String md5Password = DigestUtils.md5DigestAsHex(password.getBytes());

        // update pwd
        OddJobUser loginUser = (OddJobUser) request.getAttribute(LoginService.LOGIN_IDENTITY_KEY);

        // do write
        OddJobUser existUser = oddJobUserDao.loadByUserName(loginUser.getUsername());
        existUser.setPassword(md5Password);
        oddJobUserDao.update(existUser);

        return ReturnT.SUCCESS;
    }
}
