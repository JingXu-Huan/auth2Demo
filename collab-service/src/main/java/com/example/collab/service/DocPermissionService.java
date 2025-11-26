package com.example.collab.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.collab.entity.DocPermission;
import com.example.collab.mapper.DocPermissionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文档权限服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocPermissionService {
    
    private final DocPermissionMapper docPermissionMapper;
    
    // 权限级别常量
    public static final String LEVEL_VIEW = "view";
    public static final String LEVEL_COMMENT = "comment";
    public static final String LEVEL_EDIT = "edit";
    public static final String LEVEL_ADMIN = "admin";
    
    /**
     * 授予权限
     */
    @Transactional
    public DocPermission grantPermission(Long docId, String granteeType, Long granteeId, 
                                          String level, Long grantedBy) {
        // 检查是否已存在权限
        DocPermission existing = docPermissionMapper.selectByGrantee(docId, granteeType, granteeId);
        if (existing != null) {
            // 更新权限级别
            existing.setPermissionLevel(level);
            existing.setGrantedBy(grantedBy);
            existing.setGrantedAt(LocalDateTime.now());
            docPermissionMapper.updateById(existing);
            log.info("更新文档权限: docId={}, granteeType={}, granteeId={}, level={}", 
                    docId, granteeType, granteeId, level);
            return existing;
        }
        
        DocPermission permission = new DocPermission()
                .setDocId(docId)
                .setGranteeType(granteeType)
                .setGranteeId(granteeId)
                .setPermissionLevel(level)
                .setCanView(true)
                .setCanComment(level.equals(LEVEL_COMMENT) || level.equals(LEVEL_EDIT) || level.equals(LEVEL_ADMIN))
                .setCanEdit(level.equals(LEVEL_EDIT) || level.equals(LEVEL_ADMIN))
                .setCanDelete(level.equals(LEVEL_ADMIN))
                .setCanShare(level.equals(LEVEL_ADMIN))
                .setGrantedBy(grantedBy)
                .setGrantedAt(LocalDateTime.now());
        
        docPermissionMapper.insert(permission);
        log.info("授予文档权限: docId={}, granteeType={}, granteeId={}, level={}", 
                docId, granteeType, granteeId, level);
        return permission;
    }
    
    /**
     * 撤销权限
     */
    @Transactional
    public void revokePermission(Long docId, String granteeType, Long granteeId) {
        docPermissionMapper.delete(new LambdaQueryWrapper<DocPermission>()
                .eq(DocPermission::getDocId, docId)
                .eq(DocPermission::getGranteeType, granteeType)
                .eq(DocPermission::getGranteeId, granteeId));
        log.info("撤销文档权限: docId={}, granteeType={}, granteeId={}", docId, granteeType, granteeId);
    }
    
    /**
     * 获取文档的所有权限
     */
    public List<DocPermission> getDocPermissions(Long docId) {
        return docPermissionMapper.selectByDocId(docId);
    }
    
    /**
     * 获取用户对文档的权限
     */
    public DocPermission getUserPermission(Long docId, Long userId) {
        return docPermissionMapper.selectByGrantee(docId, "user", userId);
    }
    
    /**
     * 检查用户是否有权限
     */
    public boolean hasPermission(Long docId, Long userId, String requiredLevel) {
        DocPermission permission = getUserPermission(docId, userId);
        if (permission == null) {
            return false;
        }
        
        // 检查权限是否过期
        if (permission.getExpiresAt() != null && permission.getExpiresAt().isBefore(LocalDateTime.now())) {
            return false;
        }
        
        return checkPermissionLevel(permission.getPermissionLevel(), requiredLevel);
    }
    
    /**
     * 检查权限级别是否满足要求
     */
    private boolean checkPermissionLevel(String actual, String required) {
        int actualLevel = getLevelValue(actual);
        int requiredLevel = getLevelValue(required);
        return actualLevel >= requiredLevel;
    }
    
    private int getLevelValue(String level) {
        return switch (level) {
            case LEVEL_VIEW -> 1;
            case LEVEL_COMMENT -> 2;
            case LEVEL_EDIT -> 3;
            case LEVEL_ADMIN -> 4;
            default -> 0;
        };
    }
    
    /**
     * 检查用户是否可以编辑文档
     */
    public boolean canEdit(Long docId, Long userId) {
        return hasPermission(docId, userId, LEVEL_EDIT);
    }
    
    /**
     * 检查用户是否可以查看文档
     */
    public boolean canView(Long docId, Long userId) {
        return hasPermission(docId, userId, LEVEL_VIEW);
    }
}
