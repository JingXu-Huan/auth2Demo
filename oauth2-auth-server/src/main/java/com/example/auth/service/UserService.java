package com.example.auth.service;

import com.example.auth.feign.UserServiceClient;
import com.example.common.model.GiteeUser;
import com.example.common.vo.Result;
import com.example.common.vo.UserVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 用户服务
 * 通过 Feign 调用 User-server 服务
 */
@Service
public class UserService {
    
    @Autowired
    private UserServiceClient userServiceClient;
    
    /**
     * 保存或更新 Gitee 用户
     * Gitee 登录成功后调用此方法
     * 返回 Result 让 Controller 层决定如何处理
     */
    public Result<UserVO> saveOrUpdateGiteeUser(GiteeUser giteeUser, String giteeAccessToken) {
        Map<String, String> giteeUserData = new HashMap<>();
        giteeUserData.put("giteeUserId", giteeUser.getId().toString());
        giteeUserData.put("login", giteeUser.getLogin());
        giteeUserData.put("name", giteeUser.getName());
        giteeUserData.put("email", giteeUser.getEmail());
        giteeUserData.put("avatarUrl", giteeUser.getAvatarUrl());
        
        return userServiceClient.saveOrUpdateGiteeUser(giteeUserData);
    }
    
    /**
     * 根据用户名查询用户
     * 返回 Result 让 Controller 层决定如何处理
     */
    public Result<UserVO> getUserByUsername(String username) {
        return userServiceClient.getUserByUsername(username);
    }
    
    /**
     * 检查用户名是否存在
     */
    public boolean usernameExists(String username) {
        Result<Boolean> result = userServiceClient.checkUsernameExists(username);
        return result.getData() != null && result.getData();
    }
    
    /**
     * 检查邮箱是否存在
     */
    public boolean checkEmailExists(String email) {
        Result<Boolean> result = userServiceClient.checkEmailExists(email);
        return result.getData() != null && result.getData();
    }
}