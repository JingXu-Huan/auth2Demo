package com.example.auth.controller;

import com.example.common.model.GiteeUser;
import com.example.common.vo.Result;
import com.example.common.vo.UserVO;

import com.example.auth.service.GiteeOAuthService;
import com.example.auth.service.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpSession;
import java.util.*;

/**
 * Gitee OAuth 控制器
 * 处理 Gitee 授权登录的请求
 */
@Api(tags = "Gitee OAuth", description = "Gitee 第三方登录接口")
@Controller
@RequestMapping("/oauth/gitee")
public class GiteeOAuthController {
    
    @Autowired
    private GiteeOAuthService giteeOAuthService;

    @Autowired
    private UserService userService;
    
    /**
     * 发起 Gitee 授权请求
     * 用户点击"Gitee登录"按钮时调用
     */
    @ApiOperation(value = "发起 Gitee 授权", notes = "重定向到 Gitee 授权页面")
    @GetMapping("/authorize")
    public String authorize(HttpSession session) {
        // 生成随机 state，防止 CSRF 攻击
        String state = UUID.randomUUID().toString();
        session.setAttribute("gitee_oauth_state", state);
        
        // 构建授权 URL 并重定向
        String authUrl = giteeOAuthService.getAuthorizationUrl(state);
        return "redirect:" + authUrl;
    }
    
    /**
     * Gitee 授权回调
     * 用户在 Gitee 授权后，会跳转到这个地址
     */
    @ApiOperation(value = "Gitee 授权回调", notes = "Gitee 授权后的回调地址，处理授权码并获取用户信息")
    @GetMapping("/callback")
    public String callback(
            @ApiParam(value = "授权码", required = true)
            @RequestParam("code") String code,
            @ApiParam(value = "防 CSRF 攻击的 state 参数", required = true)
            @RequestParam("state") String state,
            HttpSession session) {
        
        try {
            // 验证 state，防止 CSRF 攻击
            String savedState = (String) session.getAttribute("gitee_oauth_state");
            if (savedState == null || !savedState.equals(state)) {
                // 重定向到前端登录页，带错误信息
                return "redirect:http://localhost:5173/login?error=state_mismatch";
            }
            
            // 1. 用授权码换取 access_token
            String accessToken = giteeOAuthService.getAccessToken(code);
            
            // 2. 用 access_token 获取用户信息
            GiteeUser giteeUser = giteeOAuthService.getUserInfo(accessToken);
            
            // 3. 保存或更新用户到数据库
            Result<UserVO> result = userService.saveOrUpdateGiteeUser(giteeUser, accessToken);
            
            if (result.getCode() != 200 || result.getData() == null) {
                System.err.println("Gitee 用户保存失败: " + result.getMessage());
                return "redirect:http://localhost:5173/login?error=save_user_failed";
            }
            
            UserVO userVO = result.getData();
            
            // 4. 保存用户信息到 session
            session.setAttribute("user", userVO);
            session.setAttribute("gitee_access_token", accessToken);
            
            System.out.println("Gitee 登录成功，用户已保存到数据库: " + userVO.getUsername());
            
            // 5. 使用 Gitee access token 作为临时 token，重定向到前端回调页面
            // 前端回调页面会接收这个 token 并保存到 localStorage，然后跳转到 Dashboard
            return "redirect:http://localhost:5173/auth/callback?token=" + accessToken + "&token_type=Bearer&expires_in=86400";
            
        } catch (Exception e) {
            e.printStackTrace();
            // 登录失败，重定向到前端登录页，带错误信息
            return "redirect:http://localhost:5173/login?error=gitee_login_failed";
        }
    }
    
    /**
     * 获取当前登录用户信息
     */
    @ApiOperation(value = "获取当前用户", notes = "获取当前 Gitee 登录用户的信息")
    @GetMapping("/user")
    public ModelAndView getCurrentUser(HttpSession session) {
        ModelAndView mav = new ModelAndView("gitee-result");
        
        GiteeUser user = (GiteeUser) session.getAttribute("gitee_user");
        if (user != null) {
            mav.addObject("success", true);
            mav.addObject("user", user);
        } else {
            mav.addObject("error", "未登录");
        }
        
        return mav;
    }
    
    /**
     * 退出登录
     */
    @ApiOperation(value = "退出登录", notes = "清除 Gitee 登录会话")
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.removeAttribute("gitee_user");
        session.removeAttribute("gitee_access_token");
        session.removeAttribute("gitee_oauth_state");
        return "redirect:/login.html";
    }
}
