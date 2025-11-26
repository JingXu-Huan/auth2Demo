package com.example.auth.service;

import com.example.domain.dto.UserDetailsDTO;
import com.example.domain.vo.Result;
import com.example.auth.feign.UserServiceClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Junjie
 * @version 1.0.0
 * @date 2025-11-06
 * 用户详情服务实现
 * 通过 Feign 调用 User-server 获取用户信息
 */
@Slf4j
@Service
public class UserDetailsServiceImpl implements UserDetailsService {
    
    @Autowired
    private UserServiceClient userServiceClient;
    
    @Override
    public UserDetails loadUserByUsername(String emailOrUsername) throws UsernameNotFoundException {
        log.info("======== 开始加载用户详情 ========");
        log.info("输入参数: emailOrUsername={}", emailOrUsername);
        
        UserDetailsDTO userDetails = null;
        
        // 1. 判断是邮箱还是用户名（包含@符号）
        if (!emailOrUsername.contains("@")) {
            log.warn("拒绝登录: 不是邮箱格式");
            throw new UsernameNotFoundException("请使用邮箱登录，不支持用户名登录: " + emailOrUsername);
        }
        
        // 2. 通过邮箱查询用户
        try {
            log.info("调用 UserServiceClient.getUserDetailsByEmail...");
            Result<UserDetailsDTO> result = userServiceClient.getUserDetailsByEmail(emailOrUsername);
            log.info("Feign 调用返回: result={}", result);
            
            if (result != null) {
                log.info("Result.code={}, Result.message={}, Result.success={}", 
                    result.getCode(), result.getMessage(), result.isSuccess());
                
                if (result.getData() != null) {
                    userDetails = result.getData();
                    log.info("Data:{}",result.getData());
                    log.info("获取到用户详情: userId={}, username={}, email={}", 
                        userDetails.getUserId(), userDetails.getUsername(), userDetails.getEmail());
                    log.info("emailVerified={}, passwordHash长度={}", 
                        userDetails.getEmailVerified(), 
                        userDetails.getPasswordHash() != null ? userDetails.getPasswordHash().length() : "null");
                } else {
                    log.warn("Result.getData() 返回 null");
                }
            } else {
                log.warn("Feign 调用返回 null");
            }
        } catch (Exception e) {
            log.error("调用用户服务失败: email={}, 异常类型={}, 异常信息={}", 
                emailOrUsername, e.getClass().getName(), e.getMessage(), e);
            throw new UsernameNotFoundException("系统错误，请稍后重试");
        }
        
        if (userDetails == null) {
            log.warn("用户不存在: {}", emailOrUsername);
            throw new UsernameNotFoundException("用户不存在: " + emailOrUsername);
        }
        
        // 3. 检查邮箱是否已验证
        log.info("检查邮箱验证状态: emailVerified={}", userDetails.getEmailVerified());
        if (userDetails.getEmailVerified() == null || !userDetails.getEmailVerified()) {
            log.warn("邮箱未验证: {}", emailOrUsername);
            throw new UsernameNotFoundException("邮箱未验证，请先验证邮箱后再登录: " + emailOrUsername);
        }
        
        // 4. 检查是否有密码（第三方登录用户没有密码）
        log.info("检查密码: passwordHash={}", 
            userDetails.getPasswordHash() != null ? "存在(长度:" + userDetails.getPasswordHash().length() + ")" : "NULL");
        if (userDetails.getPasswordHash() == null || userDetails.getPasswordHash().isEmpty()) {
            log.warn("用户没有密码: {}", emailOrUsername);
            throw new UsernameNotFoundException("该账户不支持密码登录，请使用第三方登录");
        }
        
        // 5. 构建权限
        List<GrantedAuthority> authorities = new ArrayList<>();
        if ("admin".equals(userDetails.getUsername()) || "admin@example.com".equals(userDetails.getEmail())) {
            authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        } else {
            authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        }
        log.info("用户权限: {}", authorities);
        
        // 6. 返回 UserDetails（注意：username 使用邮箱）
        String loginUsername = userDetails.getEmail() != null ? userDetails.getEmail() : userDetails.getUsername();
        log.info("构建 Spring Security UserDetails: loginUsername={}", loginUsername);
        log.info("======== 用户详情加载完成，准备进行密码验证 ========");
        
        return org.springframework.security.core.userdetails.User.builder()
                .username(loginUsername)
                .password(userDetails.getPasswordHash())
                .authorities(authorities)
                .build();
    }
}
