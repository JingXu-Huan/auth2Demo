package com.example.common.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * Gitee 用户信息
 */
@Getter
public class GiteeUser {

    // Getter和Setter方法
    /**
     * 用户ID
     */
    private Long id;
    
    /**
     * 用户名
     */
    private String login;
    
    /**
     * 用户昵称
     */
    private String name;
    
    /**
     * 头像URL
     */
    @JsonProperty("avatar_url")
    private String avatarUrl;
    
    /**
     * 邮箱
     */
    private String email;
    
    /**
     * 个人主页
     */
    @JsonProperty("html_url")
    private String htmlUrl;
    
    /**
     * 个人简介
     */
    private String bio;

    public void setId(Long id) {
        this.id = id;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setHtmlUrl(String htmlUrl) {
        this.htmlUrl = htmlUrl;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }
    
    @Override
    public String toString() {
        return "GiteeUser{" +
                "id=" + id +
                ", login='" + login + '\'' +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                '}';
    }
}