package com.example.org.controller;

import com.example.domain.vo.Result;
import com.example.org.entity.Permission;
import com.example.org.service.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

/**
 * ====================================================================
 * 权限管理控制器 (Permission Controller)
 * ====================================================================
 * 
 * 【RBAC权限模型中的位置】
 * 
 *   用户 (User)
 *      │
 *      ▼ 拥有
 *   角色 (Role)
 *      │
 *      ▼ 包含
 *   权限 (Permission) ← 本控制器管理
 * 
 * 【权限设计】
 * 权限是系统中最细粒度的访问控制单元：
 * - 每个权限对应一个具体的操作（如：创建用户、删除文章）
 * - 权限通过编码（permCode）标识，如：user:create, article:delete
 * - 权限按模块分组，便于管理
 * 
 * 【权限编码规范】
 * 格式：{模块}:{操作}
 * 示例：
 * - user:create   - 创建用户
 * - user:update   - 更新用户
 * - user:delete   - 删除用户
 * - user:view     - 查看用户
 * - article:*     - 文章所有权限
 * 
 * 【权限校验流程】
 * 1. 用户请求API
 * 2. Gateway/拦截器 提取用户角色
 * 3. 查询角色对应的权限列表
 * 4. 检查是否包含所需权限
 * 5. 有权限则放行，无权限返回403
 * 
 * @author 学习笔记
 * @see PermissionService 权限业务服务
 * @see RoleController 角色管理（权限与角色关联）
 */
@RestController
@RequestMapping("/api/org/permissions")
@RequiredArgsConstructor
public class PermissionController {
    
    /** 权限服务 - 处理权限的业务逻辑 */
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
