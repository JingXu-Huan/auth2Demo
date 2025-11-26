package com.example.org.controller;

import com.example.common.result.Result;
import com.example.org.entity.Organization;
import com.example.org.entity.OrgMember;
import com.example.org.service.OrganizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 组织管理接口
 */
@RestController
@RequestMapping("/api/org/organizations")
@RequiredArgsConstructor
public class OrganizationController {
    
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
