package com.example.auth.controller;

import com.example.domain.model.GiteeUser;
import com.example.domain.vo.Result;
import com.example.domain.vo.UserVO;
import com.example.auth.feign.UserServiceClient;
import com.example.auth.service.GiteeOAuthService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpSession;
import java.util.*;

/**
 * @author Junjie
 * @version 1.0.0
 * @date 2025-11-06
 * Gitee OAuth 登录控制器
 * 处理 Gitee 第三方登录流程
 */
@Slf4j
@Api(tags = "Gitee OAuth", description = "Gitee 第三方登录")
@Controller
@RequestMapping("/oauth/gitee")
public class GiteeOAuthController {
    
    @Autowired
    private GiteeOAuthService giteeOAuthService;
    
    @Autowired
    private UserServiceClient userServiceClient;
    
    /**
     * 跳转到 Gitee 授权页面
     */
    @ApiOperation(value = "跳转到 Gitee 授权页面")
    @GetMapping("/login")
    public String login() {
        String state = String.valueOf(System.currentTimeMillis());
        String authUrl = giteeOAuthService.getAuthorizationUrl(state);
        log.info("重定向到 Gitee 授权页面: {}", authUrl);
        return "redirect:" + authUrl;
    }
    
    /**
     * Gitee 授权回调
     */
    @ApiOperation(value = "Gitee 授权回调")
    @GetMapping("/callback")
    public ModelAndView callback(
            @RequestParam("code") String code,
            @RequestParam(value = "state", required = false) String state,
            HttpSession session) {
        
        ModelAndView mav = new ModelAndView();
        
        try {
            log.info("收到 Gitee 回调: code={}, state={}", code, state);
            
            // 1. 通过 code 获取 access_token
            String accessToken = giteeOAuthService.getAccessToken(code);
            log.info("获取到 access_token: {}", accessToken);
            
            // 2. 通过 access_token 获取用户信息
            GiteeUser giteeUser = giteeOAuthService.getUserInfo(accessToken);
            log.info("获取到 Gitee 用户信息: {}", giteeUser);
            
            // 3. 调用 User-server 创建或更新用户
            // TODO: 实现真实的用户创建/更新逻辑
            // Result<UserVO> result = userServiceClient.createOrUpdateOAuthUser("gitee", 
            //     String.valueOf(giteeUser.getId()), giteeUser.getLogin(), giteeUser.getEmail(), giteeUser.getAvatarUrl());
            
            // 暂时模拟用户数据
            UserVO userVO = new UserVO();
            userVO.setId(1L);
            userVO.setUsername(giteeUser.getLogin());
            userVO.setEmail(giteeUser.getEmail());
            
            log.info("Gitee用户登录成功: userId={}, username={}", userVO.getId(), userVO.getUsername());
                
            // 4. 保存用户信息到 Session
            session.setAttribute("user", userVO);
            session.setAttribute("loginType", "gitee");
            
            // 5. 返回成功页面（使用内联HTML避免404）
            mav.setViewName("gitee-success");
            mav.addObject("username", userVO.getUsername());
            mav.addObject("email", userVO.getEmail());
            mav.addObject("message", "Gitee登录成功！");
            
        } catch (Exception e) {
            log.error("Gitee 登录失败", e);
            mav.setViewName("gitee-error");
            mav.addObject("error", e.getMessage());
        }
        
        return mav;
    }
    
    /**
     * 获取当前登录用户信息
     */
    @ApiOperation(value = "获取当前登录用户信息")
    @GetMapping("/user-info")
    public ResponseEntity<Result<UserVO>> getUserInfo(HttpSession session) {
        UserVO user = (UserVO) session.getAttribute("user");
        
        if (user == null) {
            return ResponseEntity.ok(Result.error(401, "未登录"));
        }
        
        return ResponseEntity.ok(Result.success(user));
    }
    
    /**
     * 退出登录
     */
    @ApiOperation(value = "退出登录")
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        log.info("用户已退出登录");
        return "redirect:/login.html";
    }
}
