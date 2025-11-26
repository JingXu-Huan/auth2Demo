package com.example.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.auth.entity.OAuth2Token;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * OAuth2令牌Mapper
 */
@Mapper
public interface OAuth2TokenMapper extends BaseMapper<OAuth2Token> {
    
    /**
     * 根据访问令牌查询
     */
    @Select("SELECT * FROM oauth2_tokens WHERE access_token = #{accessToken}")
    OAuth2Token selectByAccessToken(@Param("accessToken") String accessToken);
    
    /**
     * 根据刷新令牌查询
     */
    @Select("SELECT * FROM oauth2_tokens WHERE refresh_token = #{refreshToken}")
    OAuth2Token selectByRefreshToken(@Param("refreshToken") String refreshToken);
    
    /**
     * 查询用户的所有令牌
     */
    @Select("SELECT * FROM oauth2_tokens WHERE user_id = #{userId} ORDER BY created_at DESC")
    List<OAuth2Token> selectByUserId(@Param("userId") Long userId);
    
    /**
     * 删除过期令牌
     */
    @Delete("DELETE FROM oauth2_tokens WHERE expires_at < NOW()")
    int deleteExpiredTokens();
}
