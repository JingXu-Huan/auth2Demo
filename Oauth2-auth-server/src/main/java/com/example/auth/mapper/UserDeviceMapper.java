package com.example.auth.mapper;

import com.example.auth.model.UserDevice;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户设备Mapper
 * 
 * @author Cascade AI
 * @date 2024-11-24
 */
@Mapper
public interface UserDeviceMapper {
    
    /**
     * 插入或更新设备（根据 user_id + device_id 唯一约束）
     */
    @Insert("INSERT INTO user_devices " +
            "(user_id, device_id, device_type, device_name, ip_address, user_agent, status, last_active_at, created_at) " +
            "VALUES (#{userId}, #{deviceId}, #{deviceType}, #{deviceName}, #{ipAddress}, #{userAgent}, " +
            "#{status}, #{lastActiveAt}, #{createdAt}) " +
            "ON CONFLICT (user_id, device_id) " +
            "DO UPDATE SET " +
            "device_type = EXCLUDED.device_type, " +
            "device_name = EXCLUDED.device_name, " +
            "ip_address = EXCLUDED.ip_address, " +
            "user_agent = EXCLUDED.user_agent, " +
            "status = EXCLUDED.status, " +
            "last_active_at = EXCLUDED.last_active_at")
    void insertOrUpdate(UserDevice device);
    
    /**
     * 根据用户ID查询所有设备
     */
    @Select("SELECT * FROM user_devices WHERE user_id = #{userId} ORDER BY last_active_at DESC")
    List<UserDevice> findByUserId(@Param("userId") Long userId);
    
    /**
     * 根据用户ID和设备ID查询
     */
    @Select("SELECT * FROM user_devices WHERE user_id = #{userId} AND device_id = #{deviceId}")
    UserDevice findByUserIdAndDeviceId(@Param("userId") Long userId, @Param("deviceId") String deviceId);
    
    /**
     * 更新设备状态
     */
    @Update("UPDATE user_devices SET status = #{status}, last_active_at = CURRENT_TIMESTAMP " +
            "WHERE user_id = #{userId} AND device_id = #{deviceId}")
    int updateStatus(@Param("userId") Long userId, @Param("deviceId") String deviceId, @Param("status") String status);
    
    /**
     * 更新所有设备状态（用于全部踢下线）
     */
    @Update("UPDATE user_devices SET status = #{status} WHERE user_id = #{userId} AND status = 'ACTIVE'")
    int updateAllStatus(@Param("userId") Long userId, @Param("status") String status);
    
    /**
     * 删除设备
     */
    @Delete("DELETE FROM user_devices WHERE user_id = #{userId} AND device_id = #{deviceId}")
    int delete(@Param("userId") Long userId, @Param("deviceId") String deviceId);
    
    /**
     * 更新最后活跃时间
     */
    @Update("UPDATE user_devices SET last_active_at = #{lastActiveAt}, status = 'ACTIVE' " +
            "WHERE user_id = #{userId} AND device_id = #{deviceId}")
    int updateLastActiveTime(@Param("userId") Long userId, @Param("deviceId") String deviceId, 
                             @Param("lastActiveAt") LocalDateTime lastActiveAt);
    
    /**
     * 统计用户活跃设备数
     */
    @Select("SELECT COUNT(*) FROM user_devices WHERE user_id = #{userId} AND status = 'ACTIVE'")
    int countActiveDevices(@Param("userId") Long userId);
}
