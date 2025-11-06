package com.example.common.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * @author Junjie
 * @version 1.0.0
 * @date 2025-11-06
 * 用户登录响应 VO
 */
@Data
public class UserLoginVO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 访问令牌
     */
    private String accessToken;
    
    /**
     * 刷新令牌
     */
    private String refreshToken;
    
    /**
     * 令牌类型
     */
    private String tokenType = "Bearer";
    
    /**
     * 过期时间（秒）
     */
    private Long expiresIn;
    
    /**
     * 用户信息
     */
    private UserVO userInfo;
    
    public UserLoginVO() {
    }
    
    public UserLoginVO(String accessToken, String refreshToken, Long expiresIn, UserVO userInfo) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expiresIn = expiresIn;
        this.userInfo = userInfo;
    }
}
