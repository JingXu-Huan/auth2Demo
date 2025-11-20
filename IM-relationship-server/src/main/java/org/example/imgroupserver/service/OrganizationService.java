package org.example.imgroupserver.service;

import com.example.domain.dto.*;
import com.example.domain.model.DepartmentNode;
import com.example.domain.model.OrganizationNode;
import com.example.domain.model.UserNode;
import com.example.domain.vo.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.imgroupserver.feign.UserServiceClient;
import org.example.imgroupserver.mapper.DepartmentNodeMapper;
import org.example.imgroupserver.mapper.OrganizationNodeMapper;
import org.example.imgroupserver.mapper.UserNodeMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 组织 / 部门 领域服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrganizationService {

    private final OrganizationNodeMapper organizationNodeMapper;
    private final DepartmentNodeMapper departmentNodeMapper;
    private final UserNodeMapper userNodeMapper;
    private final UserServiceClient userServiceClient;

    /**
     * 创建部门
     */
    @Transactional
    public DepartmentDTO createDepartment(CreateDepartmentRequest request) {
        if (request == null || request.getName() == null || request.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("部门名称不能为空");
        }

        String orgId = (request.getOrgId() != null && !request.getOrgId().trim().isEmpty())
                ? request.getOrgId().trim()
                : "default_org";

        // 确保组织节点存在
        organizationNodeMapper.findByOrgId(orgId).orElseGet(() -> {
            OrganizationNode org = new OrganizationNode();
            org.setOrgId(orgId);
            org.setName("Lantis");
            org.setEnabled(true);
            org.setCreatedAt(LocalDateTime.now());
            org.setUpdatedAt(LocalDateTime.now());
            return organizationNodeMapper.save(org);
        });

        DepartmentNode dept = new DepartmentNode();
        dept.setDeptId(generateDeptId());
        dept.setOrgId(orgId);
        dept.setName(request.getName().trim());
        dept.setParentDeptId(request.getParentDeptId());
        dept.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
        dept.setLeaderUserId(request.getLeaderUserId());
        dept.setDeleted(false);
        dept.setCreatedAt(LocalDateTime.now());
        dept.setUpdatedAt(LocalDateTime.now());

        // 计算层级和路径
        if (request.getParentDeptId() != null) {
            departmentNodeMapper.findByDeptId(request.getParentDeptId()).ifPresent(parent -> {
                int parentLevel = parent.getLevel() != null ? parent.getLevel() : 1;
                dept.setLevel(parentLevel + 1);
                String parentPath = parent.getFullPathName() != null ? parent.getFullPathName() : parent.getName();
                dept.setFullPathName(parentPath + "/" + dept.getName());
            });
        }

        if (dept.getLevel() == null) {
            dept.setLevel(1);
        }
        if (dept.getFullPathName() == null) {
            dept.setFullPathName(dept.getName());
        }

        DepartmentNode saved = departmentNodeMapper.save(dept);

        // 维护组织-部门关系
        departmentNodeMapper.bindDepartmentToOrganization(saved.getOrgId(), saved.getDeptId());
        // 维护父子部门关系
        if (saved.getParentDeptId() != null) {
            departmentNodeMapper.bindSubDepartment(saved.getParentDeptId(), saved.getDeptId());
        }

        Long memberCount = departmentNodeMapper.countMembers(saved.getDeptId());
        saved.setMemberCount(memberCount != null ? memberCount.intValue() : 0);
        return toDepartmentDTO(saved);
    }

    /**
     * 获取部门详情
     */
    public DepartmentDTO getDepartment(String deptId) {
        DepartmentNode node = departmentNodeMapper.findByDeptId(deptId)
                .orElseThrow(() -> new NoSuchElementException("部门不存在: " + deptId));
        Long memberCount = departmentNodeMapper.countMembers(node.getDeptId());
        node.setMemberCount(memberCount != null ? memberCount.intValue() : 0);
        return toDepartmentDTO(node);
    }

    /**
     * 更新部门
     */
    @Transactional
    public DepartmentDTO updateDepartment(String deptId, UpdateDepartmentRequest request) {
        DepartmentNode dept = departmentNodeMapper.findByDeptId(deptId)
                .orElseThrow(() -> new NoSuchElementException("部门不存在: " + deptId));

        if (request.getName() != null) {
            dept.setName(request.getName().trim());
        }
        if (request.getParentDeptId() != null) {
            dept.setParentDeptId(request.getParentDeptId());
        }
        if (request.getSortOrder() != null) {
            dept.setSortOrder(request.getSortOrder());
        }
        if (request.getLeaderUserId() != null) {
            dept.setLeaderUserId(request.getLeaderUserId());
        }
        if (request.getDeleted() != null) {
            dept.setDeleted(request.getDeleted());
        }

        // 根据父部门重新计算层级和路径
        if (dept.getParentDeptId() != null) {
            departmentNodeMapper.findByDeptId(dept.getParentDeptId()).ifPresent(parent -> {
                int parentLevel = parent.getLevel() != null ? parent.getLevel() : 1;
                dept.setLevel(parentLevel + 1);
                String parentPath = parent.getFullPathName() != null ? parent.getFullPathName() : parent.getName();
                dept.setFullPathName(parentPath + "/" + dept.getName());
            });
        } else {
            dept.setLevel(1);
            dept.setFullPathName(dept.getName());
        }

        dept.setUpdatedAt(LocalDateTime.now());
        DepartmentNode saved = departmentNodeMapper.save(dept);

        // 维护组织-部门关系
        departmentNodeMapper.bindDepartmentToOrganization(saved.getOrgId(), saved.getDeptId());
        // 先清理旧的父子部门关系，再根据当前父部门重建
        departmentNodeMapper.clearParentRelation(saved.getDeptId());
        if (saved.getParentDeptId() != null) {
            departmentNodeMapper.bindSubDepartment(saved.getParentDeptId(), saved.getDeptId());
        }

        Long memberCount = departmentNodeMapper.countMembers(saved.getDeptId());
        saved.setMemberCount(memberCount != null ? memberCount.intValue() : 0);
        return toDepartmentDTO(saved);
    }

    /**
     * 删除部门（软删除）
     */
    @Transactional
    public void deleteDepartment(String deptId) {
        DepartmentNode dept = departmentNodeMapper.findByDeptId(deptId)
                .orElseThrow(() -> new NoSuchElementException("部门不存在: " + deptId));
        dept.setDeleted(true);
        dept.setUpdatedAt(LocalDateTime.now());
        departmentNodeMapper.save(dept);
        // 移除成员关系
        departmentNodeMapper.removeAllMembers(deptId);
    }

    /**
     * 获取部门树（含成员）
     */
    public List<DepartmentTreeNodeDTO> getDepartmentTree() {
        List<DepartmentNode> departments = departmentNodeMapper.findAllActive();
        if (departments.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, DepartmentTreeNodeDTO> deptNodeMap = new HashMap<>();
        for (DepartmentNode dept : departments) {
            DepartmentTreeNodeDTO node = new DepartmentTreeNodeDTO();
            node.setId(dept.getDeptId());
            node.setName(dept.getName());
            node.setType("dept");
            deptNodeMap.put(dept.getDeptId(), node);
        }

        // 先挂成员
        for (DepartmentNode dept : departments) {
            DepartmentTreeNodeDTO parentNode = deptNodeMap.get(dept.getDeptId());
            List<DepartmentMemberDTO> members = getDepartmentMembersInternal(dept.getDeptId());
            for (DepartmentMemberDTO member : members) {
                DepartmentTreeNodeDTO userNode = new DepartmentTreeNodeDTO();
                String userIdStr = member.getUserId() != null ? String.valueOf(member.getUserId()) : UUID.randomUUID().toString();
                userNode.setId(userIdStr);
                userNode.setName(member.getName());
                userNode.setType("user");
                userNode.setAvatar(member.getAvatar());
                userNode.setDepartment(member.getDepartment());
                userNode.setPosition(member.getTitle());
                userNode.setEmail(member.getEmail());
                userNode.setPhone(member.getPhone());
                userNode.setEmployeeId(member.getEmployeeId());
                parentNode.getChildren().add(userNode);
            }
        }

        // 构建部门树
        List<DepartmentTreeNodeDTO> roots = new ArrayList<>();
        for (DepartmentNode dept : departments) {
            DepartmentTreeNodeDTO node = deptNodeMap.get(dept.getDeptId());
            if (dept.getParentDeptId() != null && deptNodeMap.containsKey(dept.getParentDeptId())) {
                DepartmentTreeNodeDTO parent = deptNodeMap.get(dept.getParentDeptId());
                parent.getChildren().add(node);
            } else {
                roots.add(node);
            }
        }

        return roots;
    }

    /**
     * 添加部门成员
     */
    @Transactional
    public void addDepartmentMembers(String deptId, AddDepartmentMembersRequest request) {
        if (request == null || request.getUserIds() == null || request.getUserIds().isEmpty()) {
            throw new IllegalArgumentException("userIds 不能为空");
        }
        // 校验部门是否存在
        departmentNodeMapper.findByDeptId(deptId)
                .orElseThrow(() -> new NoSuchElementException("部门不存在: " + deptId));

        boolean primary = request.getPrimaryDepartment() != null && request.getPrimaryDepartment();
        String title = request.getTitle();
        int sort = 0;
        for (Long userId : request.getUserIds()) {
            ensureUserNodeExists(userId);
            departmentNodeMapper.addOrUpdateMember(deptId, userId, primary, title, sort++);
        }
    }

    /**
     * 移除部门成员
     */
    @Transactional
    public void removeDepartmentMember(String deptId, Long userId) {
        // 确保部门存在
        departmentNodeMapper.findByDeptId(deptId)
                .orElseThrow(() -> new NoSuchElementException("部门不存在: " + deptId));
        departmentNodeMapper.removeMember(deptId, userId);
    }

    /**
     * 获取部门成员列表
     */
    public List<DepartmentMemberDTO> getDepartmentMembers(String deptId) {
        // 确保部门存在
        departmentNodeMapper.findByDeptId(deptId)
                .orElseThrow(() -> new NoSuchElementException("部门不存在: " + deptId));
        return getDepartmentMembersInternal(deptId);
    }

    /**
     * 设置部门负责人
     */
    @Transactional
    public void setDepartmentLeader(String deptId, SetDepartmentLeaderRequest request) {
        DepartmentNode dept = departmentNodeMapper.findByDeptId(deptId)
                .orElseThrow(() -> new NoSuchElementException("部门不存在: " + deptId));
        dept.setLeaderUserId(request.getLeaderUserId());
        dept.setUpdatedAt(LocalDateTime.now());
        DepartmentNode saved = departmentNodeMapper.save(dept);

        // 同步维护图关系：User-[:MANAGES]->Department
        departmentNodeMapper.clearDepartmentManager(saved.getDeptId());
        if (saved.getLeaderUserId() != null) {
            ensureUserNodeExists(saved.getLeaderUserId());
            departmentNodeMapper.bindDepartmentManager(saved.getDeptId(), saved.getLeaderUserId());
        }
    }

    /**
     * 组织通讯录：返回所有部门成员的去重列表
     */
    public List<DepartmentMemberDTO> getContacts() {
        List<DepartmentNode> departments = departmentNodeMapper.findAllActive();
        if (departments.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, DepartmentMemberDTO> memberMap = new LinkedHashMap<>();
        for (DepartmentNode dept : departments) {
            List<DepartmentMemberDTO> members = getDepartmentMembersInternal(dept.getDeptId());
            for (DepartmentMemberDTO member : members) {
                if (member.getUserId() == null) {
                    continue;
                }
                memberMap.putIfAbsent(member.getUserId(), member);
            }
        }
        return new ArrayList<>(memberMap.values());
    }

    private DepartmentDTO toDepartmentDTO(DepartmentNode node) {
        if (node == null) {
            return null;
        }
        DepartmentDTO dto = new DepartmentDTO();
        dto.setId(node.getId());
        dto.setDeptId(node.getDeptId());
        dto.setOrgId(node.getOrgId());
        dto.setName(node.getName());
        dto.setFullPathName(node.getFullPathName());
        dto.setParentDeptId(node.getParentDeptId());
        dto.setLevel(node.getLevel());
        dto.setSortOrder(node.getSortOrder());
        dto.setLeaderUserId(node.getLeaderUserId());
        dto.setDeleted(node.getDeleted());
        dto.setMemberCount(node.getMemberCount());
        dto.setCreatedAt(node.getCreatedAt());
        dto.setUpdatedAt(node.getUpdatedAt());
        return dto;
    }

    @SuppressWarnings("unchecked")
    private List<DepartmentMemberDTO> getDepartmentMembersInternal(String deptId) {
        List<Object> queryResult = departmentNodeMapper.findDepartmentMembers(deptId);
        if (queryResult == null || queryResult.isEmpty()) {
            return new ArrayList<>();
        }

        List<Map<String, Object>> membersMap = new ArrayList<>();
        Object first = queryResult.get(0);
        try {
            if (first instanceof org.neo4j.driver.Value) {
                org.neo4j.driver.Value value = (org.neo4j.driver.Value) first;
                List<Object> innerList = value.asList(v -> v.asMap());
                for (Object item : innerList) {
                    if (item instanceof Map) {
                        membersMap.add((Map<String, Object>) item);
                    }
                }
            } else if (first instanceof List) {
                List<?> innerList = (List<?>) first;
                for (Object item : innerList) {
                    if (item instanceof Map) {
                        membersMap.add((Map<String, Object>) item);
                    }
                }
            }
        } catch (Exception e) {
            log.error("解析部门成员结果失败: deptId={}", deptId, e);
        }

        List<DepartmentMemberDTO> members = new ArrayList<>();
        for (Map<String, Object> map : membersMap) {
            try {
                DepartmentMemberDTO dto = new DepartmentMemberDTO();
                dto.setUserId(map.get("userId") != null ? ((Number) map.get("userId")).longValue() : null);
                dto.setName((String) map.get("name"));
                dto.setAvatar((String) map.get("avatar"));
                dto.setDepartment((String) map.get("department"));
                dto.setTitle((String) map.get("title"));
                dto.setPrimaryDepartment(map.get("primaryDepartment") != null ? (Boolean) map.get("primaryDepartment") : Boolean.FALSE);
                dto.setSortOrder(map.get("sortOrder") != null ? ((Number) map.get("sortOrder")).intValue() : 0);
                dto.setJoinedAt((String) map.get("joinedAt"));
                // 目前 Neo4j 中未存储邮件/电话/员工号/状态，这里先留空
                dto.setEmail(null);
                dto.setPhone(null);
                dto.setEmployeeId(null);
                dto.setStatus(null);
                members.add(dto);
            } catch (Exception e) {
                log.error("转换部门成员数据失败: {}", map, e);
            }
        }
        return members;
    }

    private void ensureUserNodeExists(Long userId) {
        Long count = userNodeMapper.countByUserId(userId);
        UserNode user = null;
        if (count != null && count > 0) {
            user = userNodeMapper.findByUserId(userId).orElse(null);
        }
        if (user == null) {
            user = new UserNode();
            user.setUserId(userId);
        }

        try {
            Result<Map<String, Object>> userResult = userServiceClient.getUserById(userId);
            if (userResult != null && userResult.getData() != null) {
                Map<String, Object> userData = userResult.getData();
                String username = (String) userData.getOrDefault("username", "用户" + userId);
                user.setNickname(username);
                log.info("从 User-server 获取用户信息成功: userId={}, username={}", userId, username);
            } else {
                user.setNickname("用户" + userId);
                log.warn("User-server 返回空数据, userId={}", userId);
            }
        } catch (Exception e) {
            log.warn("获取用户信息失败, userId={}, 使用默认昵称", userId, e);
            user.setNickname("用户" + userId);
        }

        user.setStatus("ONLINE");
        userNodeMapper.save(user);
    }

    private String generateDeptId() {
        return "dept_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }
}
