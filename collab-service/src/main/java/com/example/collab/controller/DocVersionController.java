package com.example.collab.controller;

import com.example.collab.entity.DocumentVersion;
import com.example.collab.service.DocVersionService;
import com.example.domain.vo.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 文档版本接口
 */
@RestController
@RequestMapping("/api/doc/versions")
@RequiredArgsConstructor
public class DocVersionController {
    
    private final DocVersionService docVersionService;
    
    /**
     * 创建版本
     */
    @PostMapping
    public Result<DocumentVersion> createVersion(
            @RequestParam Long docId,
            @RequestParam String title,
            @RequestParam(required = false) String changeSummary,
            @RequestParam(defaultValue = "false") boolean isMajor,
            @RequestHeader("X-User-Id") Long userId) {
        DocumentVersion version = docVersionService.createVersion(docId, userId, title, null, changeSummary, isMajor);
        return Result.success(version);
    }
    
    /**
     * 获取文档版本列表
     */
    @GetMapping("/doc/{docId}")
    public Result<List<DocumentVersion>> getVersions(@PathVariable Long docId) {
        List<DocumentVersion> versions = docVersionService.getVersions(docId);
        return Result.success(versions);
    }
    
    /**
     * 获取最新版本
     */
    @GetMapping("/doc/{docId}/latest")
    public Result<DocumentVersion> getLatestVersion(@PathVariable Long docId) {
        DocumentVersion version = docVersionService.getLatestVersion(docId);
        return Result.success(version);
    }
    
    /**
     * 获取指定版本
     */
    @GetMapping("/{versionId}")
    public Result<DocumentVersion> getVersion(@PathVariable Long versionId) {
        DocumentVersion version = docVersionService.getVersion(versionId);
        return Result.success(version);
    }
    
    /**
     * 发布版本
     */
    @PostMapping("/{versionId}/publish")
    public Result<Void> publishVersion(@PathVariable Long versionId) {
        docVersionService.publishVersion(versionId);
        return Result.success(null);
    }
    
    /**
     * 回滚到指定版本
     */
    @PostMapping("/doc/{docId}/rollback/{versionId}")
    public Result<DocumentVersion> rollback(
            @PathVariable Long docId,
            @PathVariable Long versionId,
            @RequestHeader("X-User-Id") Long userId) {
        DocumentVersion newVersion = docVersionService.rollbackToVersion(docId, versionId, userId);
        return Result.success(newVersion);
    }
}
