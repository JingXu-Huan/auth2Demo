package com.example.org.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.org.entity.Permission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 权限Mapper
 */
@Mapper
public interface PermissionMapper extends BaseMapper<Permission> {
    
    /**
     * 根据模块查询权限
     */
    @Select("SELECT * FROM permissions WHERE module = #{module} ORDER BY permission_code")
    List<Permission> selectByModule(@Param("module") String module);
    
    /**
     * 根据权限编码查询
     */
    @Select("SELECT * FROM permissions WHERE permission_code = #{code}")
    Permission selectByCode(@Param("code") String code);
    
    /**
     * 查询角色的所有权限
     */
    @Select("SELECT p.* FROM permissions p " +
            "JOIN role_permissions rp ON p.id = rp.permission_id " +
            "WHERE rp.role_id = #{roleId}")
    List<Permission> selectByRoleId(@Param("roleId") Long roleId);
    
    /**
     * 查询用户的所有权限（包括角色继承）
     */
    @Select("SELECT DISTINCT p.* FROM permissions p " +
            "JOIN role_permissions rp ON p.id = rp.permission_id " +
            "JOIN user_roles ur ON rp.role_id = ur.role_id " +
            "WHERE ur.user_id = #{userId} AND ur.revoked_at IS NULL")
    List<Permission> selectUserPermissions(@Param("userId") Long userId);
}
