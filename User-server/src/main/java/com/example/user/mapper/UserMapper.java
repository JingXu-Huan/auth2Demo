package com.example.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.domain.dto.UserDetailsDTO;
import com.example.domain.model.User;
import org.apache.ibatis.annotations.*;

/**
 * @author Junjie
 * @version 1.0.0
 * @date 2025-11-06
 * User Mapper 接口
 * 用户数据访问层
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
    
    /**
     * @author Junjie
     * @version 1.0.0
     * @date 2025-11-06
     * 根据邮箱获取用户详情（用于登录认证）
     * 只查询 provider='email' 的用户
     */
    @Select("SELECT u.id as userId, u.username, u.email, u.email_verified as emailVerified, " +
            "uc.password_hash as passwordHash, uc.provider " +
            "FROM users u " +
            "LEFT JOIN user_credentials uc ON u.id = uc.user_id " +
            "WHERE u.email = #{email} AND uc.provider = 'email'")
    UserDetailsDTO getUserDetailsByEmail(@Param("email") String email);
    
    /**
     * @author Junjie
     * @version 1.0.0
     * @date 2025-11-06
     * 根据用户名获取用户详情
     */
    @Select("SELECT u.id as userId, u.username, u.email, u.email_verified as emailVerified, " +
            "uc.password_hash as passwordHash, uc.provider " +
            "FROM users u " +
            "LEFT JOIN user_credentials uc ON u.id = uc.user_id " +
            "WHERE u.username = #{username}")
    UserDetailsDTO getUserDetailsByUsername(@Param("username") String username);
    
    /**
     * @author Junjie
     * @version 1.0.0
     * @date 2025-11-06
     * 检查邮箱是否存在
     */
    @Select("SELECT COUNT(*) FROM users WHERE email = #{email}")
    int checkEmailExists(@Param("email") String email);
    
    /**
     * @author Junjie
     * @version 1.0.0
     * @date 2025-11-06
     * 检查用户名是否存在
     */
    @Select("SELECT COUNT(*) FROM users WHERE username = #{username}")
    int checkUsernameExists(@Param("username") String username);
    
    /**
     * @author Junjie
     * @version 1.0.0
     * @date 2025-11-06
     * 根据第三方平台ID查询用户
     */
    @Select("SELECT u.* FROM users u " +
            "INNER JOIN user_credentials uc ON u.id = uc.user_id " +
            "WHERE uc.provider = #{provider} AND uc.provider_user_id = #{providerUserId}")
    User findByProviderAndProviderUserId(@Param("provider") String provider, 
                                         @Param("providerUserId") String providerUserId);
}
