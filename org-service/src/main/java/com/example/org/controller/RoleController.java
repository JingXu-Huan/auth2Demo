package com.example.org.controller;

import com.example.common.result.Result;
import com.example.org.entity.Role;
import com.example.org.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 角色管理接口
 */
@RestController
@RequestMapping("/api/org/roles")
@RequiredArgsConstructor
public class RoleController {
    
    private final RoleService roleService;
    
    /**
     * 创建角色
     */
    @PostMapping
    public Result<Role> createRole(@RequestBody Role role) {
        Role created = roleService.createRole(role);
        return Result.success(created);
    }
    
    /**
     * 更新角色
     */
    @PutMapping("/{roleId}")
    public Result<Role> updateRole(@PathVariable Long roleId, @RequestBody Role role) {
        Role updated = roleService.updateRole(roleId, role);
        return Result.success(updated);
    }
    
    /**
     * 删除角色
     */
    @DeleteMapping("/{roleId}")
    public Result<Void> deleteRole(@PathVariable Long roleId) {
        roleService.deleteRole(roleId);
        return Result.success(null);
    }
    
    /**
     * 获取角色详情
     */
    @GetMapping("/{roleId}")
    public Result<Role> getRole(@PathVariable Long roleId) {
        Role role = roleService.getRoleById(roleId);
        return Result.success(role);
    }
    
    /**
     * 获取组织下所有角色
     */
    @GetMapping("/org/{orgId}")
    public Result<List<Role>> getOrgRoles(@PathVariable Long orgId) {
        List<Role> roles = roleService.getRolesByOrgId(orgId);
        return Result.success(roles);
    }
    
    /**
     * 获取系统角色
     */
    @GetMapping("/system")
    public Result<List<Role>> getSystemRoles() {
        List<Role> roles = roleService.getSystemRoles();
        return Result.success(roles);
    }
    
    /**
     * 为角色分配权限
     */
    @PostMapping("/{roleId}/permissions")
    public Result<Void> assignPermissions(
            @PathVariable Long roleId,
            @RequestBody List<Long> permissionIds,
            @RequestHeader("X-User-Id") Long userId) {
        roleService.assignPermissions(roleId, permissionIds, userId);
        return Result.success(null);
    }
    
    /**
     * 获取角色权限
     */
    @GetMapping("/{roleId}/permissions")
    public Result<List<Long>> getRolePermissions(@PathVariable Long roleId) {
        List<Long> permissionIds = roleService.getRolePermissionIds(roleId);
        return Result.success(permissionIds);
    }
    
    /**
     * 为用户分配角色
     */
    @PostMapping("/{roleId}/users/{userId}")
    public Result<Void> assignRoleToUser(
            @PathVariable Long roleId,
            @PathVariable Long userId,
            @RequestParam(required = false) String scopeType,
            @RequestParam(required = false) Long scopeId,
            @RequestHeader("X-User-Id") Long grantedBy) {
        roleService.assignRoleToUser(userId, roleId, scopeType, scopeId, grantedBy);
        return Result.success(null);
    }
    
    /**
     * 撤销用户角色
     */
    @DeleteMapping("/{roleId}/users/{userId}")
    public Result<Void> revokeUserRole(
            @PathVariable Long roleId,
            @PathVariable Long userId) {
        roleService.revokeUserRole(userId, roleId);
        return Result.success(null);
    }
    
    /**
     * 获取用户角色
     */
    @GetMapping("/user/{userId}/org/{orgId}")
    public Result<List<Role>> getUserRoles(
            @PathVariable Long userId,
            @PathVariable Long orgId) {
        List<Role> roles = roleService.getUserRoles(userId, orgId);
        return Result.success(roles);
    }
}
