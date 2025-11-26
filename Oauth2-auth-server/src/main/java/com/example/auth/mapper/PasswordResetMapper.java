package com.example.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.auth.entity.PasswordReset;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 密码重置Mapper
 */
@Mapper
public interface PasswordResetMapper extends BaseMapper<PasswordReset> {
    
    /**
     * 根据令牌查询有效的重置记录
     */
    @Select("SELECT * FROM password_resets WHERE token = #{token} AND used = FALSE AND expires_at > NOW()")
    PasswordReset selectValidByToken(@Param("token") String token);
    
    /**
     * 标记令牌已使用
     */
    @Update("UPDATE password_resets SET used = TRUE, used_at = NOW() WHERE token = #{token}")
    int markAsUsed(@Param("token") String token);
    
    /**
     * 使用户之前的所有重置令牌失效
     */
    @Update("UPDATE password_resets SET used = TRUE WHERE user_id = #{userId} AND used = FALSE")
    int invalidateAllByUser(@Param("userId") Long userId);
}
