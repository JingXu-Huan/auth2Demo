package com.example.org.service;

import com.example.org.entity.Permission;
import com.example.org.mapper.PermissionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 权限管理服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionService {
    
    private final PermissionMapper permissionMapper;
    
    /**
     * 创建权限
     */
    @Transactional
    public Permission createPermission(Permission permission) {
        // 检查权限编码是否已存在
        Permission existing = permissionMapper.selectByCode(permission.getPermissionCode());
        if (existing != null) {
            throw new IllegalArgumentException("权限编码已存在: " + permission.getPermissionCode());
        }
        
        permission.setCreatedAt(LocalDateTime.now());
        permissionMapper.insert(permission);
        
        log.info("创建权限成功: permCode={}", permission.getPermissionCode());
        return permission;
    }
    
    /**
     * 更新权限
     */
    @Transactional
    public Permission updatePermission(Long permissionId, Permission permission) {
        Permission existing = permissionMapper.selectById(permissionId);
        if (existing == null) {
            throw new IllegalArgumentException("权限不存在: " + permissionId);
        }
        
        existing.setName(permission.getName());
        existing.setDescription(permission.getDescription());
        existing.setModule(permission.getModule());
        existing.setResource(permission.getResource());
        existing.setAction(permission.getAction());
        existing.setRiskLevel(permission.getRiskLevel());
        
        permissionMapper.updateById(existing);
        log.info("更新权限成功: permissionId={}", permissionId);
        return existing;
    }
    
    /**
     * 删除权限
     */
    @Transactional
    public void deletePermission(Long permissionId) {
        permissionMapper.deleteById(permissionId);
        log.info("删除权限成功: permissionId={}", permissionId);
    }
    
    /**
     * 获取权限详情
     */
    public Permission getPermissionById(Long permissionId) {
        return permissionMapper.selectById(permissionId);
    }
    
    /**
     * 根据权限编码获取
     */
    public Permission getPermissionByCode(String permCode) {
        return permissionMapper.selectByCode(permCode);
    }
    
    /**
     * 获取所有权限
     */
    public List<Permission> getAllPermissions() {
        return permissionMapper.selectList(null);
    }
    
    /**
     * 按模块获取权限
     */
    public List<Permission> getPermissionsByModule(String module) {
        return permissionMapper.selectByModule(module);
    }
    
    /**
     * 获取角色的所有权限
     */
    @Cacheable(value = "role_permissions", key = "#roleId")
    public List<Permission> getRolePermissions(Long roleId) {
        return permissionMapper.selectByRoleId(roleId);
    }
    
    /**
     * 获取用户的所有权限（合并所有角色的权限）
     */
    @Cacheable(value = "user_permissions", key = "#userId + '_' + #orgId")
    public Set<String> getUserPermissionCodes(Long userId, Long orgId) {
        List<Permission> permissions = permissionMapper.selectUserPermissions(userId);
        Set<String> codes = new HashSet<>();
        for (Permission p : permissions) {
            codes.add(p.getPermissionCode());
        }
        return codes;
    }
    
    /**
     * 检查用户是否拥有指定权限
     */
    public boolean hasPermission(Long userId, Long orgId, String permCode) {
        Set<String> userPerms = getUserPermissionCodes(userId, orgId);
        return userPerms.contains(permCode);
    }
    
    /**
     * 检查用户是否拥有所有指定权限
     */
    public boolean hasAllPermissions(Long userId, Long orgId, List<String> permCodes) {
        Set<String> userPerms = getUserPermissionCodes(userId, orgId);
        return userPerms.containsAll(permCodes);
    }
    
    /**
     * 检查用户是否拥有任一指定权限
     */
    public boolean hasAnyPermission(Long userId, Long orgId, List<String> permCodes) {
        Set<String> userPerms = getUserPermissionCodes(userId, orgId);
        for (String code : permCodes) {
            if (userPerms.contains(code)) {
                return true;
            }
        }
        return false;
    }
}
