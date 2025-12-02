package com.example.org.controller;

import com.example.domain.vo.Result;
import com.example.org.entity.Role;
import com.example.org.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * ====================================================================
 * 角色管理控制器 (Role Controller)
 * ====================================================================
 * 
 * 【功能概述】
 * 本控制器实现RBAC（基于角色的访问控制）中的角色管理功能，包括：
 * - 角色的CRUD操作（创建、读取、更新、删除）
 * - 角色与权限的关联管理
 * - 用户与角色的分配管理
 * 
 * 【RBAC权限模型说明】
 * ┌────────┐      ┌────────┐      ┌────────────┐
 * │  用户  │ ───► │  角色  │ ───► │    权限    │
 * │ User   │ N:M  │ Role   │ N:M  │ Permission │
 * └────────┘      └────────┘      └────────────┘
 * 
 * 一个用户可以拥有多个角色，一个角色可以拥有多个权限
 * 用户的最终权限 = 所有角色的权限并集
 * 
 * 【核心注解说明】
 * @RestController = @Controller + @ResponseBody
 *   - 标记这是一个REST风格的控制器
 *   - 所有方法返回值会自动序列化为JSON
 * 
 * @RequestMapping("/api/org/roles")
 *   - 定义本控制器的基础URL路径
 *   - 所有方法的路径都会以此为前缀
 * 
 * @RequiredArgsConstructor (Lombok)
 *   - 自动生成包含 final 字段的构造函数
 *   - 配合Spring的构造器注入，实现依赖注入
 *   - 比 @Autowired 更推荐，便于单元测试
 * 
 * 【API设计规范】
 * 遵循RESTful风格：
 * - POST   /api/org/roles           → 创建角色
 * - GET    /api/org/roles/{id}      → 获取角色
 * - PUT    /api/org/roles/{id}      → 更新角色
 * - DELETE /api/org/roles/{id}      → 删除角色
 * 
 * @author 学习笔记
 * @see RoleService 角色业务逻辑服务
 * @see Role 角色实体类
 */
@RestController
@RequestMapping("/api/org/roles")
@RequiredArgsConstructor
public class RoleController {
    
    /**
     * 角色服务 - 通过构造器注入
     * 
     * 【依赖注入方式对比】
     * 1. 字段注入 @Autowired (不推荐)
     *    - 无法声明为final，对象可变
     *    - 单元测试困难
     * 
     * 2. 构造器注入 (推荐) ✅
     *    - 可声明为final，保证不可变
     *    - 便于单元测试（可mock）
     *    - Spring官方推荐方式
     */
    private final RoleService roleService;
    
    /**
     * ================================================================
     * 创建角色 - POST /api/org/roles
     * ================================================================
     * 
     * 【注解说明】
     * @PostMapping - 映射HTTP POST请求，用于创建资源
     * @RequestBody - 将HTTP请求体中的JSON自动反序列化为Java对象
     * 
     * 【请求示例】
     * POST /api/org/roles
     * Content-Type: application/json
     * {
     *   "roleName": "项目经理",
     *   "roleCode": "PROJECT_MANAGER",
     *   "orgId": 1
     * }
     * 
     * 【返回值】Result<Role>
     * - 使用统一的响应封装类，包含 code, message, data
     * - Result.success(data) 返回成功响应
     */
    @PostMapping
    public Result<Role> createRole(@RequestBody Role role) {
        // 调用Service层处理业务逻辑
        Role created = roleService.createRole(role);
        // 封装成功响应返回
        return Result.success(created);
    }
    
    /**
     * ================================================================
     * 更新角色 - PUT /api/org/roles/{roleId}
     * ================================================================
     * 
     * 【注解说明】
     * @PutMapping("/{roleId}") - 映射HTTP PUT请求，用于更新资源
     * @PathVariable - 从URL路径中提取变量
     *   例如: PUT /api/org/roles/123 → roleId = 123
     * 
     * 【RESTful规范】
     * PUT请求应该是幂等的：多次执行同样的PUT请求，结果应该相同
     */
    @PutMapping("/{roleId}")
    public Result<Role> updateRole(@PathVariable Long roleId, @RequestBody Role role) {
        Role updated = roleService.updateRole(roleId, role);
        return Result.success(updated);
    }
    
    /**
     * ================================================================
     * 删除角色 - DELETE /api/org/roles/{roleId}
     * ================================================================
     * 
     * 【注解说明】
     * @DeleteMapping - 映射HTTP DELETE请求，用于删除资源
     * 
     * 【返回值】Result<Void>
     * - 删除操作通常不返回数据，使用Void泛型
     * - Result.success(null) 表示操作成功，无返回数据
     * 
     * 【软删除 vs 硬删除】
     * 本项目通常使用软删除（设置deleted_at字段），而非物理删除
     */
    @DeleteMapping("/{roleId}")
    public Result<Void> deleteRole(@PathVariable Long roleId) {
        roleService.deleteRole(roleId);
        return Result.success(null);
    }
    
    /**
     * ================================================================
     * 获取角色详情 - GET /api/org/roles/{roleId}
     * ================================================================
     * 
     * 【注解说明】
     * @GetMapping - 映射HTTP GET请求，用于查询资源
     * 
     * 【RESTful规范】
     * GET请求必须是安全的（不修改服务器状态）和幂等的
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
