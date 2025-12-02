package com.example.org.controller;

import com.example.domain.vo.Result;
import com.example.org.dto.DepartmentNode;
import com.example.org.entity.Department;
import com.example.org.entity.DeptUserRelation;
import com.example.org.service.DepartmentService;
import com.example.org.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * ====================================================================
 * 组织架构控制器 (Organization Structure Controller)
 * ====================================================================
 * 
 * 【功能概述】
 * 整合了部门管理和成员管理的综合API，是前端组织架构模块的主要入口。
 * 
 * 【API分组】
 * ┌─────────────────────────────────────────────────────────────┐
 * │  /api/org/depts/*    - 部门管理API                          │
 * │  ├── POST   /depts           创建部门                       │
 * │  ├── PUT    /depts/{id}      修改部门                       │
 * │  ├── PUT    /depts/{id}/move 移动部门                       │
 * │  ├── DELETE /depts/{id}      删除部门                       │
 * │  ├── GET    /tree            获取组织架构树                  │
 * │  └── GET    /depts/{id}/children 获取子部门（懒加载）        │
 * ├─────────────────────────────────────────────────────────────┤
 * │  /api/org/members/*  - 成员管理API                          │
 * │  ├── POST   /members         添加成员到部门                  │
 * │  ├── DELETE /members         移除成员                       │
 * │  ├── PUT    /members/{uid}/dept    调整部门                 │
 * │  └── PUT    /members/{uid}/primary 设置主属部门             │
 * └─────────────────────────────────────────────────────────────┘
 * 
 * 【Swagger注解说明】
 * @Tag         - 接口分组标签，用于API文档分类
 * @Operation   - 描述单个API操作
 * @Parameter   - 描述请求参数
 * 
 * 【懒加载树说明】
 * 组织架构树可能非常大（数万部门），提供两种加载方式：
 * 1. 全量加载 - GET /tree (小规模组织)
 * 2. 懒加载   - GET /depts/{id}/children (大规模组织)
 * 
 * @author 学习笔记
 * @see DepartmentService 部门业务服务
 * @see MemberService 成员业务服务
 */
@Tag(name = "组织架构管理")  // Swagger分组标签
@RestController
@RequestMapping("/api/org")
@RequiredArgsConstructor
public class OrgController {
    
    /** 部门服务 */
    private final DepartmentService departmentService;
    
    /** 成员服务 */
    private final MemberService memberService;
    
    // ==================== 部门管理 ====================
    
    @Operation(summary = "创建部门")
    @PostMapping("/depts")
    public Result<Department> createDepartment(
            @RequestParam String name,
            @RequestParam(required = false) Long parentId,
            @RequestParam(required = false) Long leaderId) {
        Department dept = departmentService.createDepartment(name, parentId, leaderId);
        return Result.success(dept);
    }
    
    @Operation(summary = "修改部门信息")
    @PutMapping("/depts/{id}")
    public Result<Void> updateDepartment(
            @PathVariable Long id,
            @RequestParam String name,
            @RequestParam(required = false) Long leaderId) {
        departmentService.updateDepartment(id, name, leaderId);
        return Result.success();
    }
    
    @Operation(summary = "移动部门")
    @PutMapping("/depts/{id}/move")
    public Result<Void> moveDepartment(
            @PathVariable Long id,
            @RequestParam Long newParentId) {
        departmentService.moveDepartment(id, newParentId);
        return Result.success();
    }
    
    @Operation(summary = "删除部门")
    @DeleteMapping("/depts/{id}")
    public Result<Void> deleteDepartment(@PathVariable Long id) {
        departmentService.deleteDepartment(id);
        return Result.success();
    }
    
    @Operation(summary = "获取组织架构树")
    @GetMapping("/tree")
    public Result<List<DepartmentNode>> getTree(
            @RequestParam(defaultValue = "true") boolean useCache) {
        return Result.success(departmentService.getTree(useCache));
    }
    
    @Operation(summary = "获取子部门(懒加载)")
    @GetMapping("/depts/{id}/children")
    public Result<List<DepartmentNode>> getChildren(@PathVariable Long id) {
        return Result.success(departmentService.getChildren(id));
    }
    
    @Operation(summary = "获取部门详情")
    @GetMapping("/depts/{id}")
    public Result<Department> getDepartment(@PathVariable Long id) {
        return Result.success(departmentService.getDepartment(id));
    }
    
    // ==================== 成员管理 ====================
    
    @Operation(summary = "添加成员到部门")
    @PostMapping("/members")
    public Result<Void> addMember(
            @RequestParam Long userId,
            @RequestParam Long deptId,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String employeeNo,
            @RequestParam(defaultValue = "true") boolean isPrimary) {
        memberService.addMember(userId, deptId, title, employeeNo, isPrimary);
        return Result.success();
    }
    
    @Operation(summary = "从部门移除成员")
    @DeleteMapping("/members")
    public Result<Void> removeMember(
            @RequestParam Long userId,
            @RequestParam Long deptId) {
        memberService.removeMember(userId, deptId);
        return Result.success();
    }
    
    @Operation(summary = "调整成员部门")
    @PutMapping("/members/{uid}/dept")
    public Result<Void> moveMember(
            @PathVariable Long uid,
            @RequestParam Long fromDeptId,
            @RequestParam Long toDeptId) {
        memberService.moveMember(uid, fromDeptId, toDeptId);
        return Result.success();
    }
    
    @Operation(summary = "设置主属部门")
    @PutMapping("/members/{uid}/primary")
    public Result<Void> setPrimaryDept(
            @PathVariable Long uid,
            @RequestParam Long deptId) {
        memberService.setPrimaryDepartment(uid, deptId);
        return Result.success();
    }
    
    @Operation(summary = "获取部门成员")
    @GetMapping("/depts/{id}/members")
    public Result<List<DeptUserRelation>> getDeptMembers(@PathVariable Long id) {
        return Result.success(memberService.getDeptMembers(id));
    }
    
    @Operation(summary = "获取用户所属部门")
    @GetMapping("/members/{uid}/depts")
    public Result<List<DeptUserRelation>> getUserDepts(@PathVariable Long uid) {
        return Result.success(memberService.getUserDepartments(uid));
    }
}
