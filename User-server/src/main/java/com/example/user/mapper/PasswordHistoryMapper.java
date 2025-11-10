package com.example.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.domain.model.PasswordHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * @author Junjie
 * @version 1.0.0
 * @date 2025-11-10
 * 密码历史 Mapper
 */
@Mapper
public interface PasswordHistoryMapper extends BaseMapper<PasswordHistory> {
    
    /**
     * 获取用户最近N次密码历史
     */
    @Select("SELECT * FROM password_history WHERE user_id = #{userId} " +
            "ORDER BY created_at DESC LIMIT #{limit}")
    List<PasswordHistory> getRecentPasswords(@Param("userId") Long userId, @Param("limit") int limit);
}
