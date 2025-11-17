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
import java.util.UUID;

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
    public String login(HttpSession session) {
        // 生成state用于防止CSRF攻击
        String state = UUID.randomUUID().toString();
        session.setAttribute("oauth_state", state);
        
        String authUrl = giteeOAuthService.getAuthorizationUrl(state);
        log.info("生成OAuth state: {}", state);
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
            
            // 1. 验证state，防止CSRF攻击
            String savedState = (String) session.getAttribute("oauth_state");
            if (savedState == null || !savedState.equals(state)) {
                log.error("State验证失败: saved={}, received={}", savedState, state);
                throw new RuntimeException("State验证失败，可能存在CSRF攻击");
            }
            session.removeAttribute("oauth_state");
            
            // 2. 通过 code 获取 access_token
            String accessToken = giteeOAuthService.getAccessToken(code);
            log.info("获取到 access_token: {}", accessToken);
            
            // 2. 通过 access_token 获取用户信息
            GiteeUser giteeUser = giteeOAuthService.getUserInfo(accessToken);
            log.info("获取到 Gitee 用户信息: {}", giteeUser);
            
            // 3. 调用 User-server 创建或更新用户
            // 直接调用 User-server（端口 8082）
            String userServerUrl = "http://localhost:8082/api/v1/users/oauth/create-or-update";
            org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
            
            String requestUrl = String.format("%s?provider=%s&providerUserId=%s&username=%s&email=%s&avatarUrl=%s",
                userServerUrl,
                "gitee",
                String.valueOf(giteeUser.getId()),
                giteeUser.getLogin(),
                giteeUser.getEmail() != null ? giteeUser.getEmail() : "",
                giteeUser.getAvatarUrl() != null ? giteeUser.getAvatarUrl() : ""
            );
            
            Result<com.example.domain.dto.UserDetailsDTO> result = restTemplate.postForObject(
                requestUrl,
                null,
                Result.class
            );
            
            if (result == null || result.getCode() != 200 || result.getData() == null) {
                throw new RuntimeException("创建或更新用户失败");
            }
            
            // 从 result 中获取用户信息
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> userData = (java.util.Map<String, Object>) result.getData();
            
            UserVO userVO = new UserVO();
            userVO.setId(((Number) userData.get("userId")).longValue());
            userVO.setUsername((String) userData.get("username"));
            userVO.setEmail((String) userData.get("email"));
            
            log.info("Gitee用户登录成功: userId={}, username={}", userVO.getId(), userVO.getUsername());
                
            // 4. 保存用户信息到 Session
            session.setAttribute("user", userVO);
            session.setAttribute("loginType", "gitee");
            
            // 5. 重定向到前端应用的OAuth回调页面，传递用户信息
            // 使用URL参数传递，避免Session跨域问题
            String redirectUrl = String.format(
                "http://localhost:3000/oauth/callback?success=true&username=%s&email=%s&id=%d",
                userVO.getUsername(),
                userVO.getEmail() != null ? userVO.getEmail() : "",
                userVO.getId()
            );
            log.info("重定向到前端: {}", redirectUrl);
            mav.setViewName("redirect:" + redirectUrl);
            
        } catch (Exception e) {
            log.error("Gitee 登录失败", e);
            // 重定向到前端，传递错误信息
            String errorMsg = e.getMessage() != null ? e.getMessage() : "登录失败";
            mav.setViewName("redirect:http://localhost:3000/oauth/callback?success=false&error=" + errorMsg);
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
