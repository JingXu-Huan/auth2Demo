package com.example.org.controller;

import com.example.domain.vo.Result;
import com.example.org.entity.Organization;
import com.example.org.entity.OrgMember;
import com.example.org.service.OrganizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * ====================================================================
 * 组织管理控制器 (Organization Controller)
 * ====================================================================
 * 
 * 【业务场景】
 * 管理系统中的组织/团队/公司，类似于：
 * - 企业微信中的"企业"
 * - 钉钉中的"组织"
 * - Slack中的"Workspace"
 * 
 * 【组织架构层次】
 * ┌─────────────────────────────────────────────────────────┐
 * │                    组织 (Organization)                   │
 * │  ┌─────────────┐   ┌─────────────┐   ┌─────────────┐    │
 * │  │   部门A     │   │   部门B     │   │   部门C     │    │
 * │  │ (Department)│   │ (Department)│   │ (Department)│    │
 * │  └──────┬──────┘   └──────┬──────┘   └─────────────┘    │
 * │         ▼                 ▼                              │
 * │    ┌────────┐        ┌────────┐                         │
 * │    │ 子部门  │        │ 子部门  │                         │
 * │    └────────┘        └────────┘                         │
 * └─────────────────────────────────────────────────────────┘
 * 
 * 【组织与部门的区别】
 * - 组织(Organization): 顶层容器，代表一个独立的实体（公司/团队）
 * - 部门(Department): 组织内部的划分，有层级关系
 * 
 * 【成员管理】
 * - 用户通过 OrgMember 加入组织
 * - OrgMember 记录用户在组织中的：部门、职位、加入时间等
 * - 一个用户可以加入多个组织（如多个项目团队）
 * 
 * 【典型使用场景】
 * 1. 创建公司 → 创建各部门 → 邀请员工加入
 * 2. 创建项目组 → 添加项目成员 → 分配角色权限
 * 
 * @author 学习笔记
 * @see OrganizationService 组织业务服务
 * @see DepartmentController 部门管理（组织下的部门）
 * @see MemberController 成员管理（部门成员）
 */
@RestController
@RequestMapping("/api/org/organizations")
@RequiredArgsConstructor
public class OrganizationController {
    
    /** 组织服务 - 处理组织相关的业务逻辑 */
    private final OrganizationService organizationService;
    
    /**
     * 创建组织
     */
    @PostMapping
    public Result<Organization> createOrganization(
            @RequestBody Organization org,
            @RequestHeader("X-User-Id") Long userId) {
        Organization created = organizationService.createOrganization(org, userId);
        return Result.success(created);
    }
    
    /**
     * 更新组织
     */
    @PutMapping("/{orgId}")
    public Result<Organization> updateOrganization(
            @PathVariable Long orgId,
            @RequestBody Organization org) {
        Organization updated = organizationService.updateOrganization(orgId, org);
        return Result.success(updated);
    }
    
    /**
     * 获取组织详情
     */
    @GetMapping("/{orgId}")
    public Result<Organization> getOrganization(@PathVariable Long orgId) {
        Organization org = organizationService.getOrganizationById(orgId);
        return Result.success(org);
    }
    
    /**
     * 添加成员
     */
    @PostMapping("/{orgId}/members")
    public Result<OrgMember> addMember(
            @PathVariable Long orgId,
            @RequestParam Long userId,
            @RequestParam(required = false) Long deptId,
            @RequestParam(required = false) String position) {
        OrgMember member = organizationService.addMember(orgId, userId, deptId, position);
        return Result.success(member);
    }
    
    /**
     * 移除成员
     */
    @DeleteMapping("/{orgId}/members/{userId}")
    public Result<Void> removeMember(
            @PathVariable Long orgId,
            @PathVariable Long userId) {
        organizationService.removeMember(orgId, userId);
        return Result.success(null);
    }
    
    /**
     * 更新成员信息
     */
    @PutMapping("/members/{memberId}")
    public Result<OrgMember> updateMember(
            @PathVariable Long memberId,
            @RequestBody OrgMember member) {
        OrgMember updated = organizationService.updateMember(memberId, member);
        return Result.success(updated);
    }
    
    /**
     * 获取组织成员列表
     */
    @GetMapping("/{orgId}/members")
    public Result<List<OrgMember>> getOrgMembers(@PathVariable Long orgId) {
        List<OrgMember> members = organizationService.getOrgMembers(orgId);
        return Result.success(members);
    }
    
    /**
     * 获取用户的成员信息
     */
    @GetMapping("/{orgId}/members/user/{userId}")
    public Result<OrgMember> getMemberByUser(
            @PathVariable Long orgId,
            @PathVariable Long userId) {
        OrgMember member = organizationService.getMemberByUserId(orgId, userId);
        return Result.success(member);
    }
    
    /**
     * 获取用户加入的所有组织
     */
    @GetMapping("/user/{userId}")
    public Result<List<OrgMember>> getUserOrganizations(@PathVariable Long userId) {
        List<OrgMember> orgs = organizationService.getUserOrganizations(userId);
        return Result.success(orgs);
    }
    
    /**
     * 检查用户是否是组织成员
     */
    @GetMapping("/{orgId}/members/{userId}/check")
    public Result<Boolean> checkMembership(
            @PathVariable Long orgId,
            @PathVariable Long userId) {
        boolean isMember = organizationService.isMember(orgId, userId);
        return Result.success(isMember);
    }
}
