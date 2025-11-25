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
        UserDetailsDTO userDetails = null;
        
        // 1. 判断是邮箱还是用户名（包含@符号）
        if (!emailOrUsername.contains("@")) {
            throw new UsernameNotFoundException("请使用邮箱登录，不支持用户名登录: " + emailOrUsername);
        }
        
        // 2. 通过邮箱查询用户
        try {
            Result<UserDetailsDTO> result = userServiceClient.getUserDetailsByEmail(emailOrUsername);
            if (result != null && result.getData() != null) {
                userDetails = result.getData();
            }
        } catch (Exception e) {
            log.error("调用用户服务失败: email={}", emailOrUsername, e);
            throw new UsernameNotFoundException("系统错误，请稍后重试");
        }
        
        if (userDetails == null) {
            throw new UsernameNotFoundException("用户不存在: " + emailOrUsername);
        }
        
        // 3. 检查邮箱是否已验证
        if (userDetails.getEmailVerified() == null || !userDetails.getEmailVerified()) {
            throw new UsernameNotFoundException("邮箱未验证，请先验证邮箱后再登录: " + emailOrUsername);
        }
        
        // 4. 检查是否有密码（第三方登录用户没有密码）
        if (userDetails.getPasswordHash() == null || userDetails.getPasswordHash().isEmpty()) {
            throw new UsernameNotFoundException("该账户不支持密码登录，请使用第三方登录");
        }
        
        // 5. 构建权限
        List<GrantedAuthority> authorities = new ArrayList<>();
        if ("admin".equals(userDetails.getUsername()) || "admin@example.com".equals(userDetails.getEmail())) {
            authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        } else {
            authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        }
        
        // 6. 返回 UserDetails（注意：username 使用邮箱）
        return org.springframework.security.core.userdetails.User.builder()
                .username(userDetails.getEmail() != null ? userDetails.getEmail() : userDetails.getUsername())
                .password(userDetails.getPasswordHash())
                .authorities(authorities)
                .build();
    }
}
