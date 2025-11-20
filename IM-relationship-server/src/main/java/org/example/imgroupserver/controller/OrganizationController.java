package org.example.imgroupserver.controller;

import com.example.domain.dto.*;
import com.example.domain.vo.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.imgroupserver.service.OrganizationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * 组织 / 部门 控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/organization")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class OrganizationController {

    private final OrganizationService organizationService;

    /**
     * 创建部门
     */
    @PostMapping("/departments")
    public ResponseEntity<Result<DepartmentDTO>> createDepartment(@RequestBody CreateDepartmentRequest request) {
        try {
            DepartmentDTO dto = organizationService.createDepartment(request);
            return ResponseEntity.ok(Result.success("创建成功", dto));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(Result.error(400, e.getMessage()));
        } catch (Exception e) {
            log.error("创建部门失败", e);
            return ResponseEntity.ok(Result.error(500, "创建部门失败"));
        }
    }

    /**
     * 获取部门详情
     */
    @GetMapping("/departments/{departmentId}")
    public ResponseEntity<Result<DepartmentDTO>> getDepartment(@PathVariable("departmentId") String departmentId) {
        try {
            DepartmentDTO dto = organizationService.getDepartment(departmentId);
            return ResponseEntity.ok(Result.success("success", dto));
        } catch (NoSuchElementException e) {
            return ResponseEntity.ok(Result.error(404, e.getMessage()));
        } catch (Exception e) {
            log.error("获取部门详情失败", e);
            return ResponseEntity.ok(Result.error(500, "获取部门详情失败"));
        }
    }

    /**
     * 更新部门
     */
    @PutMapping("/departments/{departmentId}")
    public ResponseEntity<Result<DepartmentDTO>> updateDepartment(@PathVariable("departmentId") String departmentId,
                                                                  @RequestBody UpdateDepartmentRequest request) {
        try {
            DepartmentDTO dto = organizationService.updateDepartment(departmentId, request);
            return ResponseEntity.ok(Result.success("更新成功", dto));
        } catch (NoSuchElementException e) {
            return ResponseEntity.ok(Result.error(404, e.getMessage()));
        } catch (Exception e) {
            log.error("更新部门失败", e);
            return ResponseEntity.ok(Result.error(500, "更新部门失败"));
        }
    }

    /**
     * 删除部门（软删除）
     */
    @DeleteMapping("/departments/{departmentId}")
    public ResponseEntity<Result<Void>> deleteDepartment(@PathVariable("departmentId") String departmentId) {
        try {
            organizationService.deleteDepartment(departmentId);
            return ResponseEntity.ok(Result.success("删除成功"));
        } catch (NoSuchElementException e) {
            return ResponseEntity.ok(Result.error(404, e.getMessage()));
        } catch (Exception e) {
            log.error("删除部门失败", e);
            return ResponseEntity.ok(Result.error(500, "删除部门失败"));
        }
    }

    /**
     * 获取部门树（包含成员）
     */
    @GetMapping("/departments/tree")
    public ResponseEntity<Result<List<DepartmentTreeNodeDTO>>> getDepartmentTree() {
        try {
            List<DepartmentTreeNodeDTO> tree = organizationService.getDepartmentTree();
            return ResponseEntity.ok(Result.success("success", tree));
        } catch (Exception e) {
            log.error("获取部门树失败", e);
            return ResponseEntity.ok(Result.error(500, "获取部门树失败"));
        }
    }

    /**
     * 批量添加部门成员
     */
    @PostMapping("/departments/{departmentId}/members")
    public ResponseEntity<Result<Void>> addDepartmentMembers(@PathVariable("departmentId") String departmentId,
                                                             @RequestBody AddDepartmentMembersRequest request) {
        try {
            organizationService.addDepartmentMembers(departmentId, request);
            return ResponseEntity.ok(Result.success("添加成功"));
        } catch (NoSuchElementException e) {
            return ResponseEntity.ok(Result.error(404, e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(Result.error(400, e.getMessage()));
        } catch (Exception e) {
            log.error("添加部门成员失败", e);
            return ResponseEntity.ok(Result.error(500, "添加部门成员失败"));
        }
    }

    /**
     * 移除部门成员
     */
    @DeleteMapping("/departments/{departmentId}/members/{userId}")
    public ResponseEntity<Result<Void>> removeDepartmentMember(@PathVariable("departmentId") String departmentId,
                                                               @PathVariable("userId") Long userId) {
        try {
            organizationService.removeDepartmentMember(departmentId, userId);
            return ResponseEntity.ok(Result.success("已移除成员"));
        } catch (NoSuchElementException e) {
            return ResponseEntity.ok(Result.error(404, e.getMessage()));
        } catch (Exception e) {
            log.error("移除部门成员失败", e);
            return ResponseEntity.ok(Result.error(500, "移除部门成员失败"));
        }
    }

    /**
     * 获取部门成员列表
     */
    @GetMapping("/departments/{departmentId}/members")
    public ResponseEntity<Result<List<DepartmentMemberDTO>>> getDepartmentMembers(@PathVariable("departmentId") String departmentId) {
        try {
            List<DepartmentMemberDTO> members = organizationService.getDepartmentMembers(departmentId);
            return ResponseEntity.ok(Result.success("success", members));
        } catch (NoSuchElementException e) {
            return ResponseEntity.ok(Result.error(404, e.getMessage()));
        } catch (Exception e) {
            log.error("获取部门成员列表失败", e);
            return ResponseEntity.ok(Result.error(500, "获取部门成员列表失败"));
        }
    }

    /**
     * 设置部门负责人
     */
    @PutMapping("/departments/{departmentId}/leader")
    public ResponseEntity<Result<Void>> setDepartmentLeader(@PathVariable("departmentId") String departmentId,
                                                            @RequestBody SetDepartmentLeaderRequest request) {
        try {
            organizationService.setDepartmentLeader(departmentId, request);
            return ResponseEntity.ok(Result.success("设置成功"));
        } catch (NoSuchElementException e) {
            return ResponseEntity.ok(Result.error(404, e.getMessage()));
        } catch (Exception e) {
            log.error("设置部门负责人失败", e);
            return ResponseEntity.ok(Result.error(500, "设置部门负责人失败"));
        }
    }

    /**
     * 组织通讯录
     */
    @GetMapping("/contacts")
    public ResponseEntity<Result<List<DepartmentMemberDTO>>> getContacts() {
        try {
            List<DepartmentMemberDTO> contacts = organizationService.getContacts();
            return ResponseEntity.ok(Result.success("success", contacts));
        } catch (Exception e) {
            log.error("获取组织通讯录失败", e);
            return ResponseEntity.ok(Result.error(500, "获取组织通讯录失败"));
        }
    }
}
