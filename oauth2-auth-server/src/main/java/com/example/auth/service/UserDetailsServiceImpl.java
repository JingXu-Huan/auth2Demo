package com.example.auth.service;

import com.example.common.dto.UserDetailsDTO;
import com.example.auth.feign.UserServiceClient;
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
 * 自定义用户详情服务
 * 通过 Feign 从 User-server 加载用户信息
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {
    
    @Autowired
    private UserServiceClient userServiceClient;
    
    @Override
    public UserDetails loadUserByUsername(String emailOrUsername) throws UsernameNotFoundException {
        UserDetailsDTO userDetails = null;
        
        // 1. 密码登录只支持邮箱，必须包含@符号
        if (!emailOrUsername.contains("@")) {
            throw new UsernameNotFoundException("密码登录仅支持邮箱，请使用邮箱登录: " + emailOrUsername);
        }
        
        // 2. 通过邮箱查询（只查询 provider='email' 的凭证）
        userDetails = userServiceClient.getUserDetailsByEmail(emailOrUsername);
        
        if (userDetails == null) {
            throw new UsernameNotFoundException("用户不存在: " + emailOrUsername);
        }
        
        // 3. 检查密码（必须是邮箱登录方式才有密码）
        if (userDetails.getPasswordHash() == null) {
            throw new UsernameNotFoundException("该账户未设置密码，请使用其他登录方式: " + emailOrUsername);
        }
        
        // 4. 验证是否为邮箱登录凭证
        if (!"email".equals(userDetails.getProvider())) {
            throw new UsernameNotFoundException("该账户不支持密码登录，请使用 " + userDetails.getProvider() + " 登录");
        }
        
        // 3. 构建权限列表
        List<GrantedAuthority> authorities = new ArrayList<>();
        if ("admin".equals(userDetails.getUsername()) || "admin@example.com".equals(userDetails.getEmail())) {
            authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        } else {
            authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        }
        
        // 4. 返回 UserDetails（使用邮箱作为username）
        return org.springframework.security.core.userdetails.User.builder()
                .username(userDetails.getEmail() != null ? userDetails.getEmail() : userDetails.getUsername())
                .password(userDetails.getPasswordHash())
                .authorities(authorities)
                .build();
    }
}