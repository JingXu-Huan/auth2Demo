package com.example.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.auth.entity.PasswordHistory;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 密码历史Mapper
 */
@Mapper
public interface PasswordHistoryMapper extends BaseMapper<PasswordHistory> {
    
    /**
     * 查询用户最近的密码历史
     */
    @Select("SELECT * FROM password_history WHERE user_id = #{userId} ORDER BY created_at DESC LIMIT #{limit}")
    List<PasswordHistory> selectRecentByUser(@Param("userId") Long userId, @Param("limit") int limit);
    
    /**
     * 删除用户旧的密码历史（保留最近N条）
     */
    @Delete("DELETE FROM password_history WHERE user_id = #{userId} AND id NOT IN " +
            "(SELECT id FROM password_history WHERE user_id = #{userId} ORDER BY created_at DESC LIMIT #{keepCount})")
    int deleteOldHistory(@Param("userId") Long userId, @Param("keepCount") int keepCount);
}
