package com.example.org.service;

import com.example.org.entity.PermissionPolicy;
import com.example.org.mapper.PermissionPolicyMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 权限策略服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionPolicyService {

    private final PermissionPolicyMapper policyMapper;

    /**
     * 创建权限策略
     */
    @Transactional
    public PermissionPolicy createPolicy(String name, String effect, 
                                          Map<String, Object> principals,
                                          Map<String, Object> resources,
                                          List<String> actions,
                                          Integer priority) {
        PermissionPolicy policy = new PermissionPolicy()
                .setName(name)
                .setEffect(effect)
                .setPrincipals(principals)
                .setResources(resources)
                .setActions(actions)
                .setPriority(priority != null ? priority : 100)
                .setEnabled(true)
                .setCreatedAt(LocalDateTime.now());
        
        policyMapper.insert(policy);
        log.info("创建权限策略: policyId={}, name={}", policy.getId(), name);
        return policy;
    }

    /**
     * 获取所有启用的策略
     */
    public List<PermissionPolicy> getAllEnabledPolicies() {
        return policyMapper.findAllEnabled();
    }

    /**
     * 获取允许策略
     */
    public List<PermissionPolicy> getAllowPolicies() {
        return policyMapper.findByEffect("allow");
    }

    /**
     * 获取拒绝策略
     */
    public List<PermissionPolicy> getDenyPolicies() {
        return policyMapper.findByEffect("deny");
    }

    /**
     * 启用/禁用策略
     */
    @Transactional
    public void setEnabled(Long policyId, boolean enabled) {
        PermissionPolicy policy = policyMapper.selectById(policyId);
        if (policy != null) {
            policy.setEnabled(enabled);
            policyMapper.updateById(policy);
            log.info("{}权限策略: policyId={}", enabled ? "启用" : "禁用", policyId);
        }
    }

    /**
     * 删除策略
     */
    @Transactional
    public void deletePolicy(Long policyId) {
        policyMapper.deleteById(policyId);
        log.info("删除权限策略: policyId={}", policyId);
    }

    /**
     * 评估用户对资源的操作是否允许
     */
    public boolean evaluate(Long userId, List<Long> roleIds, String resourceType, 
                           String resourceId, String action) {
        List<PermissionPolicy> policies = getAllEnabledPolicies();
        
        // 先检查拒绝策略
        for (PermissionPolicy policy : policies) {
            if ("deny".equals(policy.getEffect()) && matchPolicy(policy, userId, roleIds, resourceType, resourceId, action)) {
                log.debug("策略拒绝: policyId={}, userId={}, action={}", policy.getId(), userId, action);
                return false;
            }
        }
        
        // 再检查允许策略
        for (PermissionPolicy policy : policies) {
            if ("allow".equals(policy.getEffect()) && matchPolicy(policy, userId, roleIds, resourceType, resourceId, action)) {
                log.debug("策略允许: policyId={}, userId={}, action={}", policy.getId(), userId, action);
                return true;
            }
        }
        
        return false; // 默认拒绝
    }

    private boolean matchPolicy(PermissionPolicy policy, Long userId, List<Long> roleIds,
                                String resourceType, String resourceId, String action) {
        // 检查操作匹配
        if (policy.getActions() != null && !policy.getActions().contains(action) && !policy.getActions().contains("*")) {
            return false;
        }
        
        // 检查资源类型匹配
        Map<String, Object> resources = policy.getResources();
        if (resources != null) {
            @SuppressWarnings("unchecked")
            List<String> types = (List<String>) resources.get("types");
            if (types != null && !types.isEmpty() && !types.contains(resourceType) && !types.contains("*")) {
                return false;
            }
        }
        
        // 检查主体匹配
        Map<String, Object> principals = policy.getPrincipals();
        if (principals != null) {
            @SuppressWarnings("unchecked")
            List<Long> users = (List<Long>) principals.get("users");
            @SuppressWarnings("unchecked")
            List<Long> roles = (List<Long>) principals.get("roles");
            
            boolean userMatch = users == null || users.isEmpty() || users.contains(userId);
            boolean roleMatch = roles == null || roles.isEmpty() || 
                    (roleIds != null && roleIds.stream().anyMatch(roles::contains));
            
            if (!userMatch && !roleMatch) {
                return false;
            }
        }
        
        return true;
    }
}
