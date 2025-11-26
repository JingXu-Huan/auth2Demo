package com.example.org.controller;

import com.example.common.result.Result;
import com.example.org.entity.DeptUserRelation;
import com.example.org.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 成员管理接口
 */
@RestController
@RequestMapping("/api/org/members")
@RequiredArgsConstructor
public class MemberController {
    
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
