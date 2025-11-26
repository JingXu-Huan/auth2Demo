package com.example.org.service;

import com.example.org.entity.Organization;
import com.example.org.entity.OrgMember;
import com.example.org.mapper.OrganizationMapper;
import com.example.org.mapper.OrgMemberMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 组织管理服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrganizationService {
    
    private final OrganizationMapper organizationMapper;
    private final OrgMemberMapper orgMemberMapper;
    
    /**
     * 创建组织
     */
    @Transactional
    public Organization createOrganization(Organization org, Long creatorId) {
        org.setStatus(Organization.STATUS_ACTIVE);
        org.setCreatedAt(LocalDateTime.now());
        org.setUpdatedAt(LocalDateTime.now());
        organizationMapper.insert(org);
        
        // 创建者自动成为组织成员（管理员）
        OrgMember member = new OrgMember()
                .setOrgId(org.getId())
                .setUserId(creatorId)
                .setStatus(OrgMember.STATUS_ACTIVE)
                .setHireDate(java.time.LocalDate.now())
                .setCreatedAt(LocalDateTime.now());
        orgMemberMapper.insert(member);
        
        log.info("创建组织成功: orgId={}, orgCode={}", org.getId(), org.getOrgCode());
        return org;
    }
    
    /**
     * 更新组织信息
     */
    @Transactional
    public Organization updateOrganization(Long orgId, Organization org) {
        Organization existing = organizationMapper.selectById(orgId);
        if (existing == null) {
            throw new IllegalArgumentException("组织不存在: " + orgId);
        }
        
        existing.setName(org.getName());
        existing.setIndustry(org.getIndustry());
        existing.setContactEmail(org.getContactEmail());
        existing.setContactPhone(org.getContactPhone());
        existing.setAddress(org.getAddress());
        existing.setLogoUrl(org.getLogoUrl());
        existing.setUpdatedAt(LocalDateTime.now());
        
        organizationMapper.updateById(existing);
        log.info("更新组织成功: orgId={}", orgId);
        return existing;
    }
    
    /**
     * 获取组织详情
     */
    public Organization getOrganizationById(Long orgId) {
        return organizationMapper.selectById(orgId);
    }
    
    /**
     * 添加组织成员
     */
    @Transactional
    public OrgMember addMember(Long orgId, Long userId, Long deptId, String position) {
        // 检查是否已是成员
        OrgMember existing = orgMemberMapper.selectByOrgAndUser(orgId, userId);
        if (existing != null) {
            throw new IllegalArgumentException("用户已是组织成员");
        }
        
        OrgMember member = new OrgMember()
                .setOrgId(orgId)
                .setUserId(userId)
                .setDeptId(deptId)
                .setJobTitle(position)
                .setStatus(OrgMember.STATUS_ACTIVE)
                .setHireDate(java.time.LocalDate.now())
                .setCreatedAt(LocalDateTime.now());
        
        orgMemberMapper.insert(member);
        
        // 更新组织员工数
        organizationMapper.updateEmployeeCount(orgId, 1);
        
        log.info("添加组织成员成功: orgId={}, userId={}", orgId, userId);
        return member;
    }
    
    /**
     * 移除组织成员
     */
    @Transactional
    public void removeMember(Long orgId, Long userId) {
        OrgMember member = orgMemberMapper.selectByOrgAndUser(orgId, userId);
        if (member == null) {
            throw new IllegalArgumentException("用户不是组织成员");
        }
        
        member.setStatus(OrgMember.STATUS_LEFT);
        member.setLeftAt(LocalDateTime.now());
        member.setUpdatedAt(LocalDateTime.now());
        orgMemberMapper.updateById(member);
        
        // 更新组织员工数
        organizationMapper.updateEmployeeCount(orgId, -1);
        
        log.info("移除组织成员成功: orgId={}, userId={}", orgId, userId);
    }
    
    /**
     * 更新成员信息
     */
    @Transactional
    public OrgMember updateMember(Long memberId, OrgMember member) {
        OrgMember existing = orgMemberMapper.selectById(memberId);
        if (existing == null) {
            throw new IllegalArgumentException("成员不存在");
        }
        
        existing.setDeptId(member.getDeptId());
        existing.setJobTitle(member.getJobTitle());
        existing.setJobLevel(member.getJobLevel());
        existing.setWorkEmail(member.getWorkEmail());
        existing.setDirectManagerId(member.getDirectManagerId());
        existing.setOfficeLocation(member.getOfficeLocation());
        existing.setUpdatedAt(LocalDateTime.now());
        
        orgMemberMapper.updateById(existing);
        log.info("更新成员信息成功: memberId={}", memberId);
        return existing;
    }
    
    /**
     * 获取组织成员列表
     */
    public List<OrgMember> getOrgMembers(Long orgId) {
        return orgMemberMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<OrgMember>()
                        .eq(OrgMember::getOrgId, orgId)
                        .eq(OrgMember::getStatus, OrgMember.STATUS_ACTIVE));
    }
    
    /**
     * 获取部门成员列表
     */
    public List<OrgMember> getDeptMembers(Long deptId) {
        return orgMemberMapper.selectByDeptId(deptId);
    }
    
    /**
     * 获取用户的组织成员信息
     */
    public OrgMember getMemberByUserId(Long orgId, Long userId) {
        return orgMemberMapper.selectByOrgAndUser(orgId, userId);
    }
    
    /**
     * 获取用户加入的所有组织
     */
    public List<OrgMember> getUserOrganizations(Long userId) {
        return orgMemberMapper.selectByUserId(userId);
    }
    
    /**
     * 检查用户是否是组织成员
     */
    public boolean isMember(Long orgId, Long userId) {
        OrgMember member = orgMemberMapper.selectByOrgAndUser(orgId, userId);
        return member != null && member.getStatus() == OrgMember.STATUS_ACTIVE;
    }
}
