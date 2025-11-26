package com.example.org.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.org.entity.UserRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 用户角色关联Mapper
 */
@Mapper
public interface UserRoleMapper extends BaseMapper<UserRole> {
    
    /**
     * 查询用户的所有有效角色关联
     */
    @Select("SELECT * FROM user_roles WHERE user_id = #{userId} AND revoked_at IS NULL")
    List<UserRole> selectByUserId(@Param("userId") Long userId);
    
    /**
     * 查询用户在特定范围的角色
     */
    @Select("SELECT * FROM user_roles WHERE user_id = #{userId} " +
            "AND scope_type = #{scopeType} AND scope_id = #{scopeId} AND revoked_at IS NULL")
    List<UserRole> selectByUserAndScope(@Param("userId") Long userId, 
                                        @Param("scopeType") String scopeType,
                                        @Param("scopeId") Long scopeId);
    
    /**
     * 撤销用户角色
     */
    @Update("UPDATE user_roles SET revoked_at = NOW() WHERE user_id = #{userId} AND role_id = #{roleId} AND revoked_at IS NULL")
    int revokeUserRole(@Param("userId") Long userId, @Param("roleId") Long roleId);
    
    /**
     * 查询拥有特定角色的所有用户
     */
    @Select("SELECT user_id FROM user_roles WHERE role_id = #{roleId} AND revoked_at IS NULL")
    List<Long> selectUserIdsByRoleId(@Param("roleId") Long roleId);
}
