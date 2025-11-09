package com.example.domain.model;

import lombok.Data;

/**
 * Gitee 用户信息
 * 从 Gitee OAuth2 API 获取的用户数据
 * 
 * @author Junjie
 * @version 1.0.0
 */
@Data
public class GiteeUser {
    
    /**
     * Gitee 用户ID
     */
    private String id;
    
    /**
     * Gitee 登录名
     */
    private String login;
    
    /**
     * 用户名称
     */
    private String name;
    
    /**
     * 邮箱
     */
    private String email;
    
    /**
     * 头像URL
     */
    private String avatarUrl;
    
    /**
     * 个人主页
     */
    private String htmlUrl;
    
    /**
     * 个人简介
     */
    private String bio;
}
