package com.example.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.common.dto.UserDetailsDTO;
import com.example.common.model.User;
import org.apache.ibatis.annotations.*;

/**
 * 用户 Mapper
 * 继承 MyBatis-Plus 的 BaseMapper，自动拥有 CRUD 方法
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
    
    /**
     * 根据用户名查询用户详情（包含凭证信息）
     */
    @Select("SELECT u.id as userId, u.username, u.display_name as displayName, u.email, u.email_verified as emailVerified, " +
            "u.avatar_url as avatarUrl, uc.password_hash as passwordHash, uc.provider " +
            "FROM users u " +
            "LEFT JOIN user_credentials uc ON u.id = uc.user_id AND uc.provider = 'email' " +
            "WHERE u.username = #{username}")
    UserDetailsDTO findUserDetailsByUsername(String username);
    
    /**
     * 根据邮箱查询用户详情（包含凭证信息）
     */
    @Select("SELECT u.id as userId, u.username, u.display_name as displayName, u.email, u.email_verified as emailVerified, " +
            "u.avatar_url as avatarUrl, uc.password_hash as passwordHash, uc.provider " +
            "FROM users u " +
            "LEFT JOIN user_credentials uc ON u.id = uc.user_id AND uc.provider = 'email' " +
            "WHERE u.email = #{email}")
    UserDetailsDTO findUserDetailsByEmail(String email);
}
