package com.example.file.controller;

import com.example.common.result.Result;
import com.example.file.entity.FileShare;
import com.example.file.service.FileShareService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * 文件分享接口
 */
@RestController
@RequestMapping("/api/file/shares")
@RequiredArgsConstructor
public class FileShareController {
    
    private final FileShareService fileShareService;
    
    /**
     * 创建分享
     */
    @PostMapping
    public Result<FileShare> createShare(
            @RequestParam Long fileId,
            @RequestParam String shareType,
            @RequestParam(required = false) Long targetId,
            @RequestParam(required = false) String password,
            @RequestParam(required = false) Integer expireDays,
            @RequestHeader("X-User-Id") Long userId) {
        LocalDateTime expiresAt = expireDays != null ? LocalDateTime.now().plusDays(expireDays) : null;
        List<Long> targets = targetId != null ? Collections.singletonList(targetId) : null;
        int type = "public".equals(shareType) ? 1 : 2;
        FileShare share = fileShareService.createShare(fileId, userId, type, targets, expiresAt);
        return Result.success(share);
    }
    
    /**
     * 取消分享
     */
    @DeleteMapping("/{shareId}")
    public Result<Void> cancelShare(
            @PathVariable Long shareId,
            @RequestHeader("X-User-Id") Long userId) {
        fileShareService.cancelShare(shareId);
        return Result.success(null);
    }
    
    /**
     * 获取我的分享列表
     */
    @GetMapping("/my")
    public Result<List<FileShare>> getMyShares(@RequestHeader("X-User-Id") Long userId) {
        List<FileShare> shares = fileShareService.getUserShares(userId);
        return Result.success(shares);
    }
    
    /**
     * 获取文件的分享信息
     */
    @GetMapping("/file/{fileId}")
    public Result<List<FileShare>> getFileShares(@PathVariable Long fileId) {
        List<FileShare> shares = fileShareService.getFileShares(fileId);
        return Result.success(shares);
    }
    
    /**
     * 验证分享有效性
     */
    @GetMapping("/{shareId}/validate")
    public Result<Boolean> validateShare(
            @PathVariable Long shareId,
            @RequestParam(required = false) String password) {
        FileShare share = fileShareService.getShareById(shareId);
        boolean valid = fileShareService.isShareValid(share);
        return Result.success(valid);
    }
    
    /**
     * 记录查看
     */
    @PostMapping("/{shareId}/view")
    public Result<Void> recordView(@PathVariable Long shareId) {
        fileShareService.recordView(shareId);
        return Result.success(null);
    }
    
    /**
     * 记录下载
     */
    @PostMapping("/{shareId}/download")
    public Result<Void> recordDownload(@PathVariable Long shareId) {
        fileShareService.recordDownload(shareId);
        return Result.success(null);
    }
}
