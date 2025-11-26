package com.example.file.controller;

import com.example.common.result.Result;
import com.example.file.entity.UserStorageQuota;
import com.example.file.service.UserStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 存储配额接口
 */
@RestController
@RequestMapping("/api/file/storage")
@RequiredArgsConstructor
public class StorageController {
    
    private final UserStorageService userStorageService;
    
    /**
     * 获取存储配额信息
     */
    @GetMapping("/quota")
    public Result<UserStorageQuota> getQuota(@RequestHeader("X-User-Id") Long userId) {
        UserStorageQuota quota = userStorageService.getUserQuota(userId);
        return Result.success(quota);
    }
    
    /**
     * 获取存储使用统计
     */
    @GetMapping("/stats")
    public Result<Map<String, Object>> getStats(@RequestHeader("X-User-Id") Long userId) {
        UserStorageQuota quota = userStorageService.getUserQuota(userId);
        Map<String, Object> stats = new HashMap<>();
        stats.put("usedStorage", quota.getUsedStorage());
        stats.put("totalAvailable", quota.getTotalAvailable());
        stats.put("remainingStorage", quota.getRemainingSpace());
        stats.put("fileCount", quota.getFileCount());
        stats.put("usagePercent", userStorageService.getUsagePercent(userId));
        return Result.success(stats);
    }
    
    /**
     * 检查是否可以上传
     */
    @GetMapping("/can-upload")
    public Result<Boolean> canUpload(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam long fileSize) {
        boolean canUpload = userStorageService.canUpload(userId, fileSize);
        return Result.success(canUpload);
    }
}
