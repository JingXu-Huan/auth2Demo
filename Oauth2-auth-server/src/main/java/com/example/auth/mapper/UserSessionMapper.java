package com.example.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.domain.model.UserSession;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户会话 Mapper
 */
@Mapper
public interface UserSessionMapper extends BaseMapper<UserSession> {
    
    /**
     * 更新最后访问时间
     */
    @Update("UPDATE user_sessions SET last_accessed_at = NOW() WHERE id = #{sessionId}")
    int updateLastAccessTime(@Param("sessionId") String sessionId);
    
    /**
     * 获取用户的活跃会话
     */
    @Select("SELECT * FROM user_sessions WHERE user_id = #{userId} AND expires_at > NOW() ORDER BY last_accessed_at DESC")
    List<UserSession> getActiveSessionsByUserId(@Param("userId") Long userId);
    
    /**
     * 批量删除过期会话
     */
    @Delete("DELETE FROM user_sessions WHERE expires_at < NOW()")
    int deleteExpiredSessions();
    
    /**
     * 延长会话时间
     */
    @Update("UPDATE user_sessions SET expires_at = #{expiresAt}, last_accessed_at = NOW() WHERE id = #{sessionId}")
    int extendSession(@Param("sessionId") String sessionId, @Param("expiresAt") LocalDateTime expiresAt);
    
    /**
     * 获取用户会话数量
     */
    @Select("SELECT COUNT(*) FROM user_sessions WHERE user_id = #{userId} AND expires_at > NOW()")
    int countActiveSessionsByUserId(@Param("userId") Long userId);
}
