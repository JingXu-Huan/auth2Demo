package com.example.org.service;

import com.example.org.dto.OrgEvent;
import com.example.org.entity.DeptUserRelation;
import com.example.org.mapper.DepartmentMapper;
import com.example.org.mapper.DeptUserRelationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 成员服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemberService {
    
    private final DeptUserRelationMapper relationMapper;
    private final DepartmentMapper deptMapper;
    private final RocketMQTemplate rocketMQTemplate;
    
    private static final String TOPIC_ORG_EVENT = "ORG_EVENT";
    
    /**
     * 添加成员到部门
     */
    @Transactional(rollbackFor = Exception.class)
    public void addMember(Long userId, Long deptId, String title, String employeeNo, boolean isPrimary) {
        // 检查是否已存在
        DeptUserRelation existing = relationMapper.findByUserAndDept(userId, deptId);
        if (existing != null) {
            throw new RuntimeException("成员已在该部门中");
        }
        
        // 如果是主属部门，先清除其他主属标记
        if (isPrimary) {
            relationMapper.clearPrimary(userId);
        }
        
        DeptUserRelation relation = new DeptUserRelation()
                .setUserId(userId)
                .setDeptId(deptId)
                .setTitle(title)
                .setEmployeeNo(employeeNo)
                .setIsPrimary(isPrimary)
                .setJoinedAt(LocalDateTime.now())
                .setCreatedAt(LocalDateTime.now());
        
        relationMapper.insert(relation);
        
        // 更新部门成员数
        deptMapper.incrementMemberCount(deptId);
        
        // 发送事件
        OrgEvent event = new OrgEvent(OrgEvent.MEMBER_ADDED, userId);
        event.setParentId(deptId);
        event.setPayload(Map.of("title", title != null ? title : "", "employeeNo", employeeNo != null ? employeeNo : ""));
        sendEvent(event);
        
        log.info("添加成员: userId={}, deptId={}", userId, deptId);
    }
    
    /**
     * 从部门移除成员
     */
    @Transactional(rollbackFor = Exception.class)
    public void removeMember(Long userId, Long deptId) {
        DeptUserRelation relation = relationMapper.findByUserAndDept(userId, deptId);
        if (relation == null) {
            return;
        }
        
        relationMapper.deleteById(relation.getId());
        deptMapper.decrementMemberCount(deptId);
        
        // 如果移除的是主属部门，设置另一个为主属
        if (Boolean.TRUE.equals(relation.getIsPrimary())) {
            List<DeptUserRelation> others = relationMapper.findByUserId(userId);
            if (!others.isEmpty()) {
                relationMapper.setPrimary(userId, others.get(0).getDeptId());
            }
        }
        
        OrgEvent event = new OrgEvent(OrgEvent.MEMBER_REMOVED, userId);
        event.setParentId(deptId);
        sendEvent(event);
        
        log.info("移除成员: userId={}, deptId={}", userId, deptId);
    }
    
    /**
     * 调整成员部门
     */
    @Transactional(rollbackFor = Exception.class)
    public void moveMember(Long userId, Long fromDeptId, Long toDeptId) {
        DeptUserRelation relation = relationMapper.findByUserAndDept(userId, fromDeptId);
        if (relation == null) {
            throw new RuntimeException("成员不在原部门中");
        }
        
        boolean isPrimary = Boolean.TRUE.equals(relation.getIsPrimary());
        String title = relation.getTitle();
        String employeeNo = relation.getEmployeeNo();
        
        // 移除原部门
        relationMapper.deleteById(relation.getId());
        deptMapper.decrementMemberCount(fromDeptId);
        
        // 添加到新部门
        DeptUserRelation newRelation = new DeptUserRelation()
                .setUserId(userId)
                .setDeptId(toDeptId)
                .setTitle(title)
                .setEmployeeNo(employeeNo)
                .setIsPrimary(isPrimary)
                .setJoinedAt(LocalDateTime.now())
                .setCreatedAt(LocalDateTime.now());
        relationMapper.insert(newRelation);
        deptMapper.incrementMemberCount(toDeptId);
        
        OrgEvent event = new OrgEvent(OrgEvent.MEMBER_MOVED, userId);
        event.setPayload(Map.of("fromDeptId", fromDeptId, "toDeptId", toDeptId));
        sendEvent(event);
        
        log.info("调整成员部门: userId={}, from={} to={}", userId, fromDeptId, toDeptId);
    }
    
    /**
     * 设置主属部门
     */
    @Transactional(rollbackFor = Exception.class)
    public void setPrimaryDepartment(Long userId, Long deptId) {
        DeptUserRelation relation = relationMapper.findByUserAndDept(userId, deptId);
        if (relation == null) {
            throw new RuntimeException("成员不在该部门中");
        }
        
        relationMapper.clearPrimary(userId);
        relationMapper.setPrimary(userId, deptId);
        
        log.info("设置主属部门: userId={}, deptId={}", userId, deptId);
    }
    
    /**
     * 获取部门成员
     */
    public List<DeptUserRelation> getDeptMembers(Long deptId) {
        return relationMapper.findByDeptId(deptId);
    }
    
    /**
     * 获取用户所属部门
     */
    public List<DeptUserRelation> getUserDepartments(Long userId) {
        return relationMapper.findByUserId(userId);
    }
    
    /**
     * 获取用户主属部门
     */
    public DeptUserRelation getPrimaryDepartment(Long userId) {
        return relationMapper.findPrimaryByUserId(userId);
    }
    
    private void sendEvent(OrgEvent event) {
        try {
            rocketMQTemplate.convertAndSend(TOPIC_ORG_EVENT, event);
        } catch (Exception e) {
            log.error("发送组织事件失败", e);
        }
    }
}
