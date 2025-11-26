package com.example.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.auth.entity.MfaLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * MFA日志Mapper
 */
@Mapper
public interface MfaLogMapper extends BaseMapper<MfaLog> {
    
    /**
     * 查询用户最近的MFA记录
     */
    @Select("SELECT * FROM mfa_logs WHERE user_id = #{userId} ORDER BY created_at DESC LIMIT #{limit}")
    List<MfaLog> selectRecentByUser(@Param("userId") Long userId, @Param("limit") int limit);
    
    /**
     * 查询用户指定时间内的失败次数
     */
    @Select("SELECT COUNT(*) FROM mfa_logs WHERE user_id = #{userId} AND success = FALSE " +
            "AND created_at > NOW() - INTERVAL '#{minutes} minutes'")
    int countRecentFailures(@Param("userId") Long userId, @Param("minutes") int minutes);
}
