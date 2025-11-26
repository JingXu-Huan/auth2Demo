package com.example.org.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.org.entity.Role;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 角色Mapper
 */
@Mapper
public interface RoleMapper extends BaseMapper<Role> {
    
    /**
     * 查询组织下的所有角色
     */
    @Select("SELECT * FROM roles WHERE org_id = #{orgId} OR org_id IS NULL ORDER BY role_level")
    List<Role> selectByOrgId(@Param("orgId") Long orgId);
    
    /**
     * 查询系统角色
     */
    @Select("SELECT * FROM roles WHERE org_id IS NULL AND role_type = 2")
    List<Role> selectSystemRoles();
    
    /**
     * 根据角色编码查询
     */
    @Select("SELECT * FROM roles WHERE role_code = #{roleCode} AND (org_id = #{orgId} OR org_id IS NULL) LIMIT 1")
    Role selectByCode(@Param("roleCode") String roleCode, @Param("orgId") Long orgId);
    
    /**
     * 查询用户在组织中的角色
     */
    @Select("SELECT r.* FROM roles r " +
            "JOIN user_roles ur ON r.id = ur.role_id " +
            "WHERE ur.user_id = #{userId} AND ur.revoked_at IS NULL " +
            "AND (ur.scope_id = #{orgId} OR ur.scope_type = 'global')")
    List<Role> selectUserRolesInOrg(@Param("userId") Long userId, @Param("orgId") Long orgId);
}
