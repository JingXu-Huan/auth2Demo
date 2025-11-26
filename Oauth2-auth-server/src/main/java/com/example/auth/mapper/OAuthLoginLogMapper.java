package com.example.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.auth.entity.OAuthLoginLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * OAuth登录日志Mapper
 */
@Mapper
public interface OAuthLoginLogMapper extends BaseMapper<OAuthLoginLog> {

    @Select("SELECT * FROM oauth_login_logs WHERE user_id = #{userId} ORDER BY created_at DESC LIMIT #{limit}")
    List<OAuthLoginLog> findByUserId(@Param("userId") Long userId, @Param("limit") int limit);

    @Select("SELECT * FROM oauth_login_logs WHERE provider = #{provider} AND provider_user_id = #{providerUserId} " +
            "ORDER BY created_at DESC LIMIT #{limit}")
    List<OAuthLoginLog> findByProvider(@Param("provider") String provider, 
                                       @Param("providerUserId") String providerUserId,
                                       @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM oauth_login_logs WHERE user_id = #{userId} AND action = #{action} AND success = false " +
            "AND created_at > NOW() - INTERVAL '1 hour'")
    int countRecentFailures(@Param("userId") Long userId, @Param("action") String action);
}
