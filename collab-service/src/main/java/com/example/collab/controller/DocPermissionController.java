package com.example.collab.controller;

import com.example.collab.entity.DocPermission;
import com.example.collab.service.DocPermissionService;
import com.example.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 文档权限接口
 */
@RestController
@RequestMapping("/api/doc/permissions")
@RequiredArgsConstructor
public class DocPermissionController {
    
    private final DocPermissionService docPermissionService;
    
    /**
     * 授予权限
     */
    @PostMapping
    public Result<DocPermission> grantPermission(
            @RequestParam Long docId,
            @RequestParam String granteeType,
            @RequestParam Long granteeId,
            @RequestParam String level,
            @RequestHeader("X-User-Id") Long userId) {
        DocPermission permission = docPermissionService.grantPermission(docId, granteeType, granteeId, level, userId);
        return Result.success(permission);
    }
    
    /**
     * 撤销权限
     */
    @DeleteMapping
    public Result<Void> revokePermission(
            @RequestParam Long docId,
            @RequestParam String granteeType,
            @RequestParam Long granteeId) {
        docPermissionService.revokePermission(docId, granteeType, granteeId);
        return Result.success(null);
    }
    
    /**
     * 获取文档权限列表
     */
    @GetMapping("/doc/{docId}")
    public Result<List<DocPermission>> getDocPermissions(@PathVariable Long docId) {
        List<DocPermission> permissions = docPermissionService.getDocPermissions(docId);
        return Result.success(permissions);
    }
    
    /**
     * 检查用户权限
     */
    @GetMapping("/check")
    public Result<Boolean> checkPermission(
            @RequestParam Long docId,
            @RequestParam Long userId,
            @RequestParam String level) {
        boolean hasPermission = docPermissionService.hasPermission(docId, userId, level);
        return Result.success(hasPermission);
    }
    
    /**
     * 检查用户是否可以编辑
     */
    @GetMapping("/can-edit")
    public Result<Boolean> canEdit(
            @RequestParam Long docId,
            @RequestParam Long userId) {
        boolean canEdit = docPermissionService.canEdit(docId, userId);
        return Result.success(canEdit);
    }
}
