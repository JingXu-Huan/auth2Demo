package com.example.org.controller;

import com.example.domain.vo.Result;
import com.example.org.dto.DepartmentNode;
import com.example.org.entity.Department;
import com.example.org.service.DepartmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 部门管理接口
 */
@RestController
@RequestMapping("/api/org/departments")
@RequiredArgsConstructor
public class DepartmentController {
    
    private final DepartmentService departmentService;
    
    /**
     * 创建部门
     */
    @PostMapping
    public Result<Department> createDepartment(
            @RequestBody Department department,
            @RequestHeader("X-User-Id") Long userId) {
        Department created = departmentService.createDepartment(
                department.getName(), department.getParentId(), department.getManagerId());
        return Result.success(created);
    }
    
    /**
     * 更新部门
     */
    @PutMapping("/{deptId}")
    public Result<Department> updateDepartment(
            @PathVariable Long deptId,
            @RequestBody Department department) {
        departmentService.updateDepartment(deptId, department.getName(), department.getManagerId());
        return Result.success(departmentService.getDepartment(deptId));
    }
    
    /**
     * 删除部门
     */
    @DeleteMapping("/{deptId}")
    public Result<Void> deleteDepartment(@PathVariable Long deptId) {
        departmentService.deleteDepartment(deptId);
        return Result.success(null);
    }
    
    /**
     * 获取部门详情
     */
    @GetMapping("/{deptId}")
    public Result<Department> getDepartment(@PathVariable Long deptId) {
        Department department = departmentService.getDepartment(deptId);
        return Result.success(department);
    }
    
    /**
     * 获取组织的部门树
     */
    @GetMapping("/tree/{orgId}")
    public Result<List<DepartmentNode>> getDepartmentTree(@PathVariable Long orgId) {
        List<DepartmentNode> tree = departmentService.getTree(true);
        return Result.success(tree);
    }
    
    /**
     * 获取子部门列表
     */
    @GetMapping("/children/{parentId}")
    public Result<List<DepartmentNode>> getChildDepartments(@PathVariable Long parentId) {
        List<DepartmentNode> children = departmentService.getChildren(parentId);
        return Result.success(children);
    }
    
    /**
     * 移动部门
     */
    @PutMapping("/{deptId}/move")
    public Result<Void> moveDepartment(
            @PathVariable Long deptId,
            @RequestParam Long newParentId) {
        departmentService.moveDepartment(deptId, newParentId);
        return Result.success(null);
    }
    
    /**
     * 搜索部门
     */
    @GetMapping("/search")
    public Result<List<Department>> searchDepartments(
            @RequestParam Long orgId,
            @RequestParam String keyword) {
        // 搜索功能待实现
        return Result.success(java.util.Collections.emptyList());
    }
}
