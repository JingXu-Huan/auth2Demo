package com.example.domain.vo;

import lombok.Data;

/**
 * 用户登录响应 VO
 * 返回给前端的登录结果
 * 
 * @author Junjie
 * @version 1.0.0
 */
@Data
public class UserLoginVO {
    
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
    private String tokenType;
    
    /**
     * 过期时间（秒）
     */
    private Long expiresIn;
    
    /**
     * 授权范围
     */
    private String scope;
    
    /**
     * 用户信息
     */
    private UserVO user;
}
