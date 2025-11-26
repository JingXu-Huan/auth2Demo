package com.example.auth.controller;

import com.example.auth.service.GiteeOAuthService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * OAuth登录控制器
 * 处理Gitee、GitHub等第三方OAuth登录
 */
@Slf4j
@RestController
@RequestMapping("/oauth")
@RequiredArgsConstructor
public class OAuthController {
    
    private final GiteeOAuthService giteeOAuthService;
    
    /**
     * 获取Gitee授权URL
     * 前端调用此接口获取授权URL，然后重定向到Gitee
     */
    @GetMapping("/gitee/authorize")
    public Map<String, String> getGiteeAuthorizeUrl() {
        String authorizeUrl = giteeOAuthService.getAuthorizeUrl();
        Map<String, String> result = new HashMap<>();
        result.put("authorizeUrl", authorizeUrl);
        return result;
    }
    
    /**
     * Gitee登录 - 重定向到Gitee授权页面
     * 前端直接跳转此地址即可
     */
    @GetMapping("/gitee/login")
    public void giteeLogin(HttpServletResponse response) throws Exception {
        String authorizeUrl = giteeOAuthService.getAuthorizeUrl();
        log.info("========== Gitee OAuth 调试信息 ==========");
        log.info("生成的授权URL: {}", authorizeUrl);
        log.info("请检查URL中的redirect_uri参数是否正确");
        log.info("==========================================");
        response.sendRedirect(authorizeUrl);
    }
    
    /**
     * Gitee回调处理
     * Gitee授权后会重定向到此接口，处理后重定向到前端
     * 
     * @param code 授权码
     * @param state 状态码（用于防止CSRF攻击）
     */
    @GetMapping("/gitee/callback")
    public void giteeCallback(
            @RequestParam("code") String code,
            @RequestParam(value = "state", required = false) String state,
            HttpServletResponse response) throws Exception {
        
        log.info("Gitee OAuth回调, code: {}, state: {}", code, state);
        
        try {
            Map<String, Object> result = giteeOAuthService.handleCallback(code, state);
            log.info("Gitee登录成功, userId: {}", result.get("user"));
            
            // 重定向到前端，携带token和用户信息
            String token = (String) result.get("token");
            @SuppressWarnings("unchecked")
            Map<String, Object> user = (Map<String, Object>) result.get("user");
            
            // 构建重定向URL，包含完整用户信息
            StringBuilder redirectUrl = new StringBuilder("http://localhost:3000/oauth/callback?");
            redirectUrl.append("token=").append(token);
            redirectUrl.append("&userId=").append(user.get("id"));
            redirectUrl.append("&username=").append(java.net.URLEncoder.encode(String.valueOf(user.get("username")), "UTF-8"));
            redirectUrl.append("&provider=gitee");
            
            // 添加昵称和头像
            if (user.get("nickname") != null) {
                redirectUrl.append("&nickname=").append(java.net.URLEncoder.encode(String.valueOf(user.get("nickname")), "UTF-8"));
            }
            if (user.get("avatar") != null) {
                redirectUrl.append("&avatar=").append(java.net.URLEncoder.encode(String.valueOf(user.get("avatar")), "UTF-8"));
            }
            if (user.get("email") != null) {
                redirectUrl.append("&email=").append(java.net.URLEncoder.encode(String.valueOf(user.get("email")), "UTF-8"));
            }
            
            response.sendRedirect(redirectUrl.toString());
        } catch (Exception e) {
            log.error("Gitee登录失败", e);
            // 重定向到前端登录页，携带错误信息
            String errorUrl = "http://localhost:3000/login?error=" + 
                java.net.URLEncoder.encode(e.getMessage(), "UTF-8");
            response.sendRedirect(errorUrl);
        }
    }
    
    /**
     * 绑定Gitee账号
     * 将Gitee账号绑定到当前已登录的用户
     */
    @PostMapping("/gitee/bind")
    public Map<String, Object> bindGiteeAccount(
            @RequestParam Long userId, // 实际应该从JWT token中获取
            @RequestParam String code) {
        
        try {
            giteeOAuthService.bindGiteeAccount(userId, code);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "绑定成功");
            return response;
        } catch (Exception e) {
            log.error("绑定Gitee账号失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return response;
        }
    }
    
    /**
     * 解绑Gitee账号
     */
    @PostMapping("/gitee/unbind")
    public Map<String, Object> unbindGiteeAccount(@RequestParam Long userId) {
        try {
            giteeOAuthService.unbindGiteeAccount(userId);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "解绑成功");
            return response;
        } catch (Exception e) {
            log.error("解绑Gitee账号失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return response;
        }
    }
    
    // 可以添加其他OAuth提供商的接口，如GitHub、微信等
    // @GetMapping("/github/authorize")
    // @GetMapping("/callback/github")
    // ...
}
