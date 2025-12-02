package com.example.org.controller;

import com.example.domain.vo.Result;
import com.example.org.entity.DeptUserRelation;
import com.example.org.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * ====================================================================
 * 成员管理控制器 (Member Controller)
 * ====================================================================
 * 
 * 【业务场景】
 * 管理组织中用户与部门的关系，支持：
 * - 一个用户可以属于多个部门（多部门任职）
 * - 每个用户有一个主属部门（primary department）
 * - 部门调动（从A部门调到B部门）
 * 
 * 【数据模型】
 * ┌────────────┐     ┌──────────────────┐     ┌────────────┐
 * │   用户     │     │  部门用户关系     │     │   部门     │
 * │  (User)    │ 1:N │ (DeptUserRelation)│ N:1 │(Department)│
 * ├────────────┤     ├──────────────────┤     ├────────────┤
 * │ id         │────►│ user_id          │     │ id         │
 * │ username   │     │ dept_id          │◄────│ name       │
 * └────────────┘     │ is_primary       │     │ parent_id  │
 *                    │ title (职位)      │     └────────────┘
 *                    │ employee_no      │
 *                    └──────────────────┘
 * 
 * 【主属部门概念】
 * - 用户可以属于多个部门，但只有一个主属部门
 * - 主属部门用于：工资发放、考勤统计、组织架构展示
 * - 通过 is_primary 字段标识
 * 
 * 【API设计特点】
 * - POST   /api/org/members/dept/{deptId}        → 添加成员到部门
 * - DELETE /api/org/members/dept/{deptId}/user/{userId} → 移除成员
 * - PUT    /api/org/members/move                 → 调整部门
 * - PUT    /api/org/members/primary              → 设置主属部门
 * 
 * @author 学习笔记
 * @see MemberService 成员业务服务
 * @see DeptUserRelation 部门用户关系实体
 */
@RestController
@RequestMapping("/api/org/members")
@RequiredArgsConstructor
public class MemberController {
    
    /** 成员服务 - 处理用户与部门关系的业务逻辑 */
    private final MemberService memberService;
    
    /**
     * 添加成员到部门
     */
    @PostMapping("/dept/{deptId}")
    public Result<Void> addMember(
            @PathVariable Long deptId,
            @RequestBody Map<String, Object> params,
            @RequestHeader("X-User-Id") Long operatorId) {
        Long userId = Long.valueOf(params.get("userId").toString());
        String title = (String) params.get("title");
        String employeeNo = (String) params.get("employeeNo");
        boolean isPrimary = params.get("isPrimary") != null && (Boolean) params.get("isPrimary");
        
        memberService.addMember(userId, deptId, title, employeeNo, isPrimary);
        return Result.success(null);
    }
    
    /**
     * 从部门移除成员
     */
    @DeleteMapping("/dept/{deptId}/user/{userId}")
    public Result<Void> removeMember(
            @PathVariable Long deptId,
            @PathVariable Long userId) {
        memberService.removeMember(userId, deptId);
        return Result.success(null);
    }
    
    /**
     * 调整成员部门
     */
    @PutMapping("/move")
    public Result<Void> moveMember(
            @RequestParam Long userId,
            @RequestParam Long fromDeptId,
            @RequestParam Long toDeptId) {
        memberService.moveMember(userId, fromDeptId, toDeptId);
        return Result.success(null);
    }
    
    /**
     * 设置主属部门
     */
    @PutMapping("/primary")
    public Result<Void> setPrimaryDepartment(
            @RequestParam Long userId,
            @RequestParam Long deptId) {
        memberService.setPrimaryDepartment(userId, deptId);
        return Result.success(null);
    }
    
    /**
     * 获取部门成员列表
     */
    @GetMapping("/dept/{deptId}")
    public Result<List<DeptUserRelation>> getDeptMembers(@PathVariable Long deptId) {
        List<DeptUserRelation> members = memberService.getDeptMembers(deptId);
        return Result.success(members);
    }
    
    /**
     * 获取用户所属部门
     */
    @GetMapping("/user/{userId}/departments")
    public Result<List<DeptUserRelation>> getUserDepartments(@PathVariable Long userId) {
        List<DeptUserRelation> depts = memberService.getUserDepartments(userId);
        return Result.success(depts);
    }
    
    /**
     * 获取用户主属部门
     */
    @GetMapping("/user/{userId}/primary")
    public Result<DeptUserRelation> getPrimaryDepartment(@PathVariable Long userId) {
        DeptUserRelation primary = memberService.getPrimaryDepartment(userId);
        return Result.success(primary);
    }
}
