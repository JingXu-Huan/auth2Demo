package com.example.org.controller;

import com.example.common.result.Result;
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
 * 组织架构控制器
 */
@Tag(name = "组织架构管理")
@RestController
@RequestMapping("/api/org")
@RequiredArgsConstructor
public class OrgController {
    
    private final DepartmentService departmentService;
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
