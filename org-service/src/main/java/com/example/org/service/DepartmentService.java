package com.example.org.service;

import com.example.org.dto.DepartmentNode;
import com.example.org.dto.OrgEvent;
import com.example.org.entity.Department;
import com.example.org.mapper.DepartmentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 部门服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DepartmentService {
    
    private final DepartmentMapper deptMapper;
    private final RocketMQTemplate rocketMQTemplate;
    private final RedisTemplate<String, Object> redisTemplate;
    
    private static final String CACHE_TREE_KEY = "org:tree";
    private static final String TOPIC_ORG_EVENT = "ORG_EVENT";
    
    /**
     * 创建部门
     */
    @Transactional(rollbackFor = Exception.class)
    public Department createDepartment(String name, Long parentId, Long managerId) {
        Department parent = null;
        String path = "/";
        int level = 0;
        
        if (parentId != null && parentId > 0) {
            parent = deptMapper.selectById(parentId);
            if (parent == null) {
                throw new RuntimeException("父部门不存在");
            }
            path = parent.getPath() + parent.getId() + "/";
            level = parent.getLevel() + 1;
        }
        
        Department dept = new Department()
                .setName(name)
                .setParentId(parentId == null ? 0L : parentId)
                .setPath(path)
                .setLevel(level)
                .setSortOrder(0)
                .setManagerId(managerId)
                .setMemberCount(0)
                .setStatus(1)
                .setCreatedAt(LocalDateTime.now())
                .setUpdatedAt(LocalDateTime.now());
        
        deptMapper.insert(dept);
        
        // 清除缓存
        clearTreeCache();
        
        // 发送事件
        sendEvent(new OrgEvent(OrgEvent.DEPT_CREATED, dept.getId()));
        
        log.info("创建部门: id={}, name={}, parentId={}", dept.getId(), name, parentId);
        return dept;
    }
    
    /**
     * 更新部门信息
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateDepartment(Long deptId, String name, Long managerId) {
        Department dept = deptMapper.selectById(deptId);
        if (dept == null) {
            throw new RuntimeException("部门不存在");
        }
        
        dept.setName(name);
        dept.setManagerId(managerId);
        dept.setUpdatedAt(LocalDateTime.now());
        deptMapper.updateById(dept);
        
        clearTreeCache();
        sendEvent(new OrgEvent(OrgEvent.DEPT_UPDATED, deptId));
        
        log.info("更新部门: id={}, name={}", deptId, name);
    }
    
    /**
     * 移动部门 (核心难点)
     */
    @Transactional(rollbackFor = Exception.class)
    public void moveDepartment(Long deptId, Long newParentId) {
        Department dept = deptMapper.selectById(deptId);
        Department newParent = deptMapper.selectById(newParentId);
        
        if (dept == null) {
            throw new RuntimeException("部门不存在");
        }
        if (newParent == null) {
            throw new RuntimeException("目标父部门不存在");
        }
        
        // 校验环路 (不能移动到自己的子节点下)
        String currentSubtreePath = dept.getPath() + dept.getId() + "/";
        if (newParent.getPath().startsWith(currentSubtreePath)) {
            throw new RuntimeException("不能移动到自己的子部门下");
        }
        
        // 计算路径变更
        String oldPathPrefix = currentSubtreePath;
        String newPathPrefix = newParent.getPath() + newParent.getId() + "/";
        
        // 更新自身
        Long oldParentId = dept.getParentId();
        dept.setParentId(newParentId);
        dept.setPath(newParent.getPath() + newParent.getId() + "/");
        dept.setLevel(newParent.getLevel() + 1);
        dept.setUpdatedAt(LocalDateTime.now());
        deptMapper.updateById(dept);
        
        // 批量更新子孙节点路径
        deptMapper.updateChildPaths(oldPathPrefix, newPathPrefix);
        
        // 清除缓存
        clearTreeCache();
        
        // 发送事件
        OrgEvent event = new OrgEvent(OrgEvent.DEPT_MOVED, deptId, oldPathPrefix, newPathPrefix);
        event.setParentId(newParentId);
        event.setPayload(Map.of("oldParentId", oldParentId, "newParentId", newParentId));
        sendEvent(event);
        
        log.info("移动部门: id={}, from={} to={}", deptId, oldParentId, newParentId);
    }
    
    /**
     * 删除部门
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteDepartment(Long deptId) {
        // 校验
        if (deptMapper.countMembers(deptId) > 0) {
            throw new RuntimeException("部门下存在成员，无法删除");
        }
        if (deptMapper.countChildren(deptId) > 0) {
            throw new RuntimeException("部门下存在子部门，无法删除");
        }
        
        Department dept = deptMapper.selectById(deptId);
        if (dept == null) {
            return;
        }
        
        // 软删除
        dept.setStatus(0);
        dept.setUpdatedAt(LocalDateTime.now());
        deptMapper.updateById(dept);
        
        clearTreeCache();
        sendEvent(new OrgEvent(OrgEvent.DEPT_DELETED, deptId));
        
        log.info("删除部门: id={}", deptId);
    }
    
    /**
     * 获取组织架构树
     */
    @SuppressWarnings("unchecked")
    public List<DepartmentNode> getTree(boolean useCache) {
        if (useCache) {
            List<DepartmentNode> cached = (List<DepartmentNode>) redisTemplate.opsForValue().get(CACHE_TREE_KEY);
            if (cached != null) {
                return cached;
            }
        }
        
        // 全量查询
        List<Department> allDepts = deptMapper.selectList(null);
        
        // 内存构建树
        Map<Long, DepartmentNode> nodeMap = new HashMap<>();
        List<DepartmentNode> roots = new ArrayList<>();
        
        // 转换节点
        for (Department d : allDepts) {
            if (d.getStatus() == 1) {
                nodeMap.put(d.getId(), convertToNode(d));
            }
        }
        
        // 组装树
        for (Department d : allDepts) {
            if (d.getStatus() != 1) continue;
            
            DepartmentNode node = nodeMap.get(d.getId());
            if (d.getParentId() == null || d.getParentId() == 0) {
                roots.add(node);
            } else {
                DepartmentNode parent = nodeMap.get(d.getParentId());
                if (parent != null) {
                    parent.getChildren().add(node);
                }
            }
        }
        
        // 排序
        sortTree(roots);
        
        // 缓存
        redisTemplate.opsForValue().set(CACHE_TREE_KEY, roots, 1, TimeUnit.HOURS);
        
        return roots;
    }
    
    /**
     * 获取子部门 (懒加载)
     */
    public List<DepartmentNode> getChildren(Long parentId) {
        List<Department> children = deptMapper.findByParentId(parentId);
        return children.stream().map(this::convertToNode).toList();
    }
    
    /**
     * 获取部门详情
     */
    public Department getDepartment(Long deptId) {
        return deptMapper.selectById(deptId);
    }
    
    private DepartmentNode convertToNode(Department d) {
        DepartmentNode node = new DepartmentNode();
        node.setId(d.getId());
        node.setName(d.getName());
        node.setParentId(d.getParentId());
        node.setPath(d.getPath());
        node.setLevel(d.getLevel());
        node.setSortOrder(d.getSortOrder());
        node.setManagerId(d.getManagerId());
        node.setMemberCount(d.getMemberCount());
        return node;
    }
    
    private void sortTree(List<DepartmentNode> nodes) {
        nodes.sort(Comparator.comparingInt(n -> n.getSortOrder() == null ? 0 : n.getSortOrder()));
        for (DepartmentNode node : nodes) {
            if (!node.getChildren().isEmpty()) {
                sortTree(node.getChildren());
            }
        }
    }
    
    private void clearTreeCache() {
        redisTemplate.delete(CACHE_TREE_KEY);
    }
    
    private void sendEvent(OrgEvent event) {
        try {
            rocketMQTemplate.convertAndSend(TOPIC_ORG_EVENT, event);
        } catch (Exception e) {
            log.error("发送组织事件失败", e);
        }
    }
}
