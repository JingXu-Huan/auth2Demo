package com.example.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.domain.model.LoginLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 登录日志 Mapper
 */
@Mapper
public interface LoginLogMapper extends BaseMapper<LoginLog> {
    
    /**
     * 获取用户最后一次登录记录
     */
    @Select("SELECT * FROM login_logs WHERE user_id = #{userId} ORDER BY created_at DESC LIMIT 1")
    LoginLog getLastLoginLog(@Param("userId") Long userId);
    
    /**
     * 获取用户最后一次成功登录记录
     */
    @Select("SELECT * FROM login_logs WHERE user_id = #{userId} AND success = true ORDER BY created_at DESC LIMIT 1")
    LoginLog getLastSuccessLoginLog(@Param("userId") Long userId);
    
    /**
     * 统计指定时间段内的失败登录次数
     */
    @Select("SELECT COUNT(*) FROM login_logs WHERE user_id = #{userId} AND success = false AND created_at > #{since}")
    int countFailedLogins(@Param("userId") Long userId, @Param("since") LocalDateTime since);
    
    /**
     * 获取用户最近的登录记录
     */
    @Select("SELECT * FROM login_logs WHERE user_id = #{userId} ORDER BY created_at DESC LIMIT #{limit}")
    List<LoginLog> getRecentLoginLogs(@Param("userId") Long userId, @Param("limit") int limit);
    
    /**
     * 获取异常登录记录（不同IP）
     */
    @Select("SELECT * FROM login_logs WHERE user_id = #{userId} " +
            "AND ip_address != #{normalIp} " +
            "AND created_at > #{since} " +
            "ORDER BY created_at DESC")
    List<LoginLog> getAbnormalLoginLogs(@Param("userId") Long userId, 
                                        @Param("normalIp") String normalIp,
                                        @Param("since") LocalDateTime since);
}
