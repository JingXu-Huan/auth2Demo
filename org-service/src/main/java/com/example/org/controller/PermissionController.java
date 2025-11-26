package com.example.org.controller;

import com.example.common.result.Result;
import com.example.org.entity.Permission;
import com.example.org.service.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

/**
 * 权限管理接口
 */
@RestController
@RequestMapping("/api/org/permissions")
@RequiredArgsConstructor
public class PermissionController {
    
    private final PermissionService permissionService;
    
    /**
     * 创建权限
     */
    @PostMapping
    public Result<Permission> createPermission(@RequestBody Permission permission) {
        Permission created = permissionService.createPermission(permission);
        return Result.success(created);
    }
    
    /**
     * 更新权限
     */
    @PutMapping("/{permissionId}")
    public Result<Permission> updatePermission(
            @PathVariable Long permissionId,
            @RequestBody Permission permission) {
        Permission updated = permissionService.updatePermission(permissionId, permission);
        return Result.success(updated);
    }
    
    /**
     * 删除权限
     */
    @DeleteMapping("/{permissionId}")
    public Result<Void> deletePermission(@PathVariable Long permissionId) {
        permissionService.deletePermission(permissionId);
        return Result.success(null);
    }
    
    /**
     * 获取权限详情
     */
    @GetMapping("/{permissionId}")
    public Result<Permission> getPermission(@PathVariable Long permissionId) {
        Permission permission = permissionService.getPermissionById(permissionId);
        return Result.success(permission);
    }
    
    /**
     * 获取所有权限
     */
    @GetMapping
    public Result<List<Permission>> getAllPermissions() {
        List<Permission> permissions = permissionService.getAllPermissions();
        return Result.success(permissions);
    }
    
    /**
     * 按模块获取权限
     */
    @GetMapping("/module/{module}")
    public Result<List<Permission>> getPermissionsByModule(@PathVariable String module) {
        List<Permission> permissions = permissionService.getPermissionsByModule(module);
        return Result.success(permissions);
    }
    
    /**
     * 获取用户权限编码列表
     */
    @GetMapping("/user/{userId}/org/{orgId}")
    public Result<Set<String>> getUserPermissions(
            @PathVariable Long userId,
            @PathVariable Long orgId) {
        Set<String> permissions = permissionService.getUserPermissionCodes(userId, orgId);
        return Result.success(permissions);
    }
    
    /**
     * 检查用户是否有权限
     */
    @GetMapping("/check")
    public Result<Boolean> checkPermission(
            @RequestParam Long userId,
            @RequestParam Long orgId,
            @RequestParam String permCode) {
        boolean hasPermission = permissionService.hasPermission(userId, orgId, permCode);
        return Result.success(hasPermission);
    }
}
