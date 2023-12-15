package com.odd.job.admin.service;

import com.odd.job.admin.core.model.OddJobUser;
import com.odd.job.admin.core.util.CookieUtil;
import com.odd.job.admin.core.util.I18nUtil;
import com.odd.job.admin.core.util.JacksonUtil;
import com.odd.job.admin.dao.OddJobUserDao;
import com.odd.job.core.biz.model.ReturnT;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.math.BigInteger;

/**
 * 登录管理业务事宜
 *
 * @author oddity
 * @create 2023-12-11 22:35
 */

@Configuration
public class LoginService {

    public static final String LOGIN_IDENTITY_KEY = "ODD_JOB_LOGIN_IDENTITY";

    @Resource
    private OddJobUserDao oddJobUserDao;

    private String makeToken(OddJobUser oddJobUser){
        String tokenJson = JacksonUtil.writeValueAsString(oddJobUser);
        String tokenHex = new BigInteger(tokenJson.getBytes()).toString(16);
        return tokenHex;
    }
    private OddJobUser parseToken(String tokenHex){
        OddJobUser oddJobUser = null;
        if (tokenHex != null) {
            String tokenJson = new String(new BigInteger(tokenHex, 16).toByteArray());      // username_password(md5)
            oddJobUser = JacksonUtil.readValue(tokenJson, OddJobUser.class);
        }
        return oddJobUser;
    }

    public ReturnT<String> login(HttpServletRequest request, HttpServletResponse response, String username, String password, boolean ifRemember){

        // param
        if (username == null || username.trim().length() == 0 || password == null || password.trim().length() == 0){
            return new ReturnT<String>(500, I18nUtil.getString("login_param_empty"));
        }

        // valid passowrd
        OddJobUser oddJobUser = oddJobUserDao.loadByUserName(username);
        if (oddJobUser == null) {
            return new ReturnT<String>(500, I18nUtil.getString("login_param_unvalid"));
        }
        String passwordMd5 = DigestUtils.md5DigestAsHex(password.getBytes());
        if (!passwordMd5.equals(oddJobUser.getPassword())) {
            return new ReturnT<String>(500, I18nUtil.getString("login_param_unvalid"));
        }

        String loginToken = makeToken(oddJobUser);

        // do login
        CookieUtil.set(response, LOGIN_IDENTITY_KEY, loginToken, ifRemember);
        return ReturnT.SUCCESS;
    }

    /**
     * logout
     *
     * @param request
     * @param response
     */
    public ReturnT<String> logout(HttpServletRequest request, HttpServletResponse response){
        CookieUtil.remove(request, response, LOGIN_IDENTITY_KEY);
        return ReturnT.SUCCESS;
    }

    /**
     * logout
     *
     * @param request
     * @return
     */
    public OddJobUser ifLogin(HttpServletRequest request, HttpServletResponse response){
        String cookieToken = CookieUtil.getValue(request, LOGIN_IDENTITY_KEY);
        if (cookieToken != null) {
            OddJobUser cookieUser = null;
            try {
                cookieUser = parseToken(cookieToken);
            } catch (Exception e) {
                logout(request, response);
            }
            if (cookieUser != null) {
                OddJobUser dbUser = oddJobUserDao.loadByUserName(cookieUser.getUsername());
                if (dbUser != null) {
                    if (cookieUser.getPassword().equals(dbUser.getPassword())) {
                        return dbUser;
                    }
                }
            }
        }
        return null;
    }
}
