package com.example.org.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.org.entity.Role;
import com.example.org.entity.RolePermission;
import com.example.org.entity.UserRole;
import com.example.org.mapper.RoleMapper;
import com.example.org.mapper.RolePermissionMapper;
import com.example.org.mapper.UserRoleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 角色管理服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoleService {
    
    private final RoleMapper roleMapper;
    private final RolePermissionMapper rolePermissionMapper;
    private final UserRoleMapper userRoleMapper;
    
    /**
     * 创建角色
     */
    @Transactional
    public Role createRole(Role role) {
        // 检查角色编码是否已存在
        Role existing = roleMapper.selectByCode(role.getRoleCode(), role.getOrgId());
        if (existing != null) {
            throw new IllegalArgumentException("角色编码已存在: " + role.getRoleCode());
        }
        
        role.setCreatedAt(LocalDateTime.now());
        role.setUpdatedAt(LocalDateTime.now());
        roleMapper.insert(role);
        
        log.info("创建角色成功: orgId={}, roleCode={}", role.getOrgId(), role.getRoleCode());
        return role;
    }
    
    /**
     * 更新角色
     */
    @Transactional
    public Role updateRole(Long roleId, Role role) {
        Role existing = roleMapper.selectById(roleId);
        if (existing == null) {
            throw new IllegalArgumentException("角色不存在: " + roleId);
        }
        
        existing.setName(role.getName());
        existing.setDescription(role.getDescription());
        existing.setRoleLevel(role.getRoleLevel());
        existing.setMaxMembers(role.getMaxMembers());
        existing.setUpdatedAt(LocalDateTime.now());
        
        roleMapper.updateById(existing);
        log.info("更新角色成功: roleId={}", roleId);
        return existing;
    }
    
    /**
     * 删除角色
     */
    @Transactional
    public void deleteRole(Long roleId) {
        Role role = roleMapper.selectById(roleId);
        if (role == null) {
            throw new IllegalArgumentException("角色不存在: " + roleId);
        }
        
        // 检查是否有用户使用此角色
        List<UserRole> userRoles = userRoleMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<UserRole>()
                        .eq(UserRole::getRoleId, roleId)
                        .isNull(UserRole::getRevokedAt));
        if (!userRoles.isEmpty()) {
            throw new IllegalStateException("角色下还有用户，无法删除");
        }
        
        // 删除角色权限关联
        rolePermissionMapper.deleteByRoleId(roleId);
        
        // 删除角色
        roleMapper.deleteById(roleId);
        log.info("删除角色成功: roleId={}", roleId);
    }
    
    /**
     * 获取角色详情
     */
    public Role getRoleById(Long roleId) {
        return roleMapper.selectById(roleId);
    }
    
    /**
     * 获取组织下所有角色
     */
    public List<Role> getRolesByOrgId(Long orgId) {
        return roleMapper.selectByOrgId(orgId);
    }
    
    /**
     * 获取系统角色（无组织限制）
     */
    public List<Role> getSystemRoles() {
        return roleMapper.selectSystemRoles();
    }
    
    /**
     * 为角色分配权限
     */
    @Transactional
    public void assignPermissions(Long roleId, List<Long> permissionIds, Long grantedBy) {
        // 先删除旧的权限
        rolePermissionMapper.deleteByRoleId(roleId);
        
        // 添加新的权限
        LocalDateTime now = LocalDateTime.now();
        for (Long permissionId : permissionIds) {
            RolePermission rp = new RolePermission()
                    .setRoleId(roleId)
                    .setPermissionId(permissionId)
                    .setGrantedBy(grantedBy)
                    .setGrantedAt(now);
            rolePermissionMapper.insert(rp);
        }
        
        log.info("为角色分配权限成功: roleId={}, permissionCount={}", roleId, permissionIds.size());
    }
    
    /**
     * 获取角色的所有权限ID
     */
    public List<Long> getRolePermissionIds(Long roleId) {
        return rolePermissionMapper.selectPermissionIdsByRoleId(roleId);
    }
    
    /**
     * 为用户分配角色
     */
    @Transactional
    public void assignRoleToUser(Long userId, Long roleId, String scopeType, Long scopeId, Long grantedBy) {
        // 检查是否已分配
        LambdaQueryWrapper<UserRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserRole::getUserId, userId)
               .eq(UserRole::getRoleId, roleId)
               .isNull(UserRole::getRevokedAt);
        
        UserRole existing = userRoleMapper.selectOne(wrapper);
        if (existing != null) {
            log.warn("用户已拥有该角色: userId={}, roleId={}", userId, roleId);
            return;
        }
        
        UserRole userRole = new UserRole()
                .setUserId(userId)
                .setRoleId(roleId)
                .setScopeType(scopeType)
                .setScopeId(scopeId)
                .setGrantedBy(grantedBy)
                .setEffectiveDate(LocalDateTime.now().toLocalDate())
                .setGrantedAt(LocalDateTime.now());
        
        userRoleMapper.insert(userRole);
        log.info("为用户分配角色成功: userId={}, roleId={}", userId, roleId);
    }
    
    /**
     * 撤销用户角色
     */
    @Transactional
    public void revokeUserRole(Long userId, Long roleId) {
        userRoleMapper.revokeUserRole(userId, roleId);
        log.info("撤销用户角色成功: userId={}, roleId={}", userId, roleId);
    }
    
    /**
     * 获取用户的所有角色
     */
    public List<Role> getUserRoles(Long userId, Long orgId) {
        return roleMapper.selectUserRolesInOrg(userId, orgId);
    }
    
    /**
     * 获取拥有指定角色的所有用户ID
     */
    public List<Long> getUserIdsByRoleId(Long roleId) {
        return userRoleMapper.selectUserIdsByRoleId(roleId);
    }
}
