package com.example.auth.mapper;

import com.example.auth.model.UserPunishment;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户惩罚Mapper
 * 
 * @author Cascade AI
 * @date 2024-11-24
 */
@Mapper
public interface UserPunishmentMapper {
    
    /**
     * 插入惩罚记录
     */
    @Insert("INSERT INTO user_punishments " +
            "(user_id, type, duration, reason, operator_id, created_at, expires_at) " +
            "VALUES (#{userId}, #{type}, #{duration}, #{reason}, #{operatorId}, " +
            "#{createdAt}, #{expiresAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(UserPunishment punishment);
    
    /**
     * 查询用户的有效惩罚记录（未过期且未撤销）
     */
    @Select("SELECT * FROM user_punishments " +
            "WHERE user_id = #{userId} " +
            "AND type = #{type} " +
            "AND revoked = FALSE " +
            "AND (expires_at IS NULL OR expires_at > CURRENT_TIMESTAMP) " +
            "ORDER BY created_at DESC " +
            "LIMIT 1")
    UserPunishment findActivePunishment(@Param("userId") Long userId, @Param("type") String type);
    
    /**
     * 查询用户所有惩罚记录
     */
    @Select("SELECT * FROM user_punishments WHERE user_id = #{userId} ORDER BY created_at DESC")
    List<UserPunishment> findByUserId(@Param("userId") Long userId);
    
    /**
     * 撤销惩罚
     */
    @Update("UPDATE user_punishments " +
            "SET revoked = TRUE, revoked_at = #{revokedAt}, revoke_reason = #{revokeReason} " +
            "WHERE user_id = #{userId} AND type = #{type} AND revoked = FALSE")
    int revoke(@Param("userId") Long userId, @Param("type") String type, 
               @Param("revokedAt") LocalDateTime revokedAt, @Param("revokeReason") String revokeReason);
    
    /**
     * 根据ID撤销惩罚
     */
    @Update("UPDATE user_punishments " +
            "SET revoked = TRUE, revoked_at = #{revokedAt}, revoke_reason = #{revokeReason} " +
            "WHERE id = #{id}")
    int revokeById(@Param("id") Long id, @Param("revokedAt") LocalDateTime revokedAt, 
                   @Param("revokeReason") String revokeReason);
    
    /**
     * 检查用户是否有有效的惩罚
     */
    @Select("SELECT EXISTS(" +
            "SELECT 1 FROM user_punishments " +
            "WHERE user_id = #{userId} " +
            "AND type = #{type} " +
            "AND revoked = FALSE " +
            "AND (expires_at IS NULL OR expires_at > CURRENT_TIMESTAMP)" +
            ")")
    boolean hasActivePunishment(@Param("userId") Long userId, @Param("type") String type);
    
    /**
     * 删除过期的惩罚记录（定时清理）
     */
    @Delete("DELETE FROM user_punishments " +
            "WHERE expires_at < #{beforeDate} " +
            "AND created_at < #{beforeDate}")
    int deleteExpired(@Param("beforeDate") LocalDateTime beforeDate);
}
