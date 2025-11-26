package com.example.admin.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.admin.entity.Report;
import com.example.admin.entity.SensitiveWord;
import com.example.admin.entity.SysConfig;
import com.example.admin.service.DashboardService;
import com.example.admin.service.ReportService;
import com.example.admin.service.SensitiveWordService;
import com.example.admin.service.SysConfigService;
import com.example.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 管理后台控制器
 */
@Tag(name = "管理后台")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {
    
    private final SensitiveWordService sensitiveWordService;
    private final ReportService reportService;
    private final SysConfigService sysConfigService;
    private final DashboardService dashboardService;
    
    // ==================== 仪表盘 ====================
    
    @Operation(summary = "获取仪表盘统计")
    @GetMapping("/dashboard/stats")
    public Result<Map<String, Object>> getDashboardStats() {
        return Result.success(dashboardService.getStats());
    }
    
    // ==================== 敏感词管理 ====================
    
    @Operation(summary = "添加敏感词")
    @PostMapping("/sensitive-words")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'OPERATOR')")
    public Result<Void> addSensitiveWord(
            @RequestHeader("X-Admin-Id") Long adminId,
            @RequestParam String word,
            @RequestParam(defaultValue = "GENERAL") String category,
            @RequestParam(defaultValue = "1") int actionType) {
        sensitiveWordService.addWord(word, category, actionType, adminId);
        return Result.success();
    }
    
    @Operation(summary = "批量添加敏感词")
    @PostMapping("/sensitive-words/batch")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'OPERATOR')")
    public Result<Void> batchAddSensitiveWords(
            @RequestHeader("X-Admin-Id") Long adminId,
            @RequestBody List<String> words,
            @RequestParam(defaultValue = "GENERAL") String category,
            @RequestParam(defaultValue = "1") int actionType) {
        sensitiveWordService.addWords(words, category, actionType, adminId);
        return Result.success();
    }
    
    @Operation(summary = "删除敏感词")
    @DeleteMapping("/sensitive-words/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'OPERATOR')")
    public Result<Void> deleteSensitiveWord(
            @RequestHeader("X-Admin-Id") Long adminId,
            @PathVariable Long id) {
        sensitiveWordService.deleteWord(id, adminId);
        return Result.success();
    }
    
    @Operation(summary = "分页查询敏感词")
    @GetMapping("/sensitive-words")
    public Result<Page<SensitiveWord>> listSensitiveWords(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String category) {
        return Result.success(sensitiveWordService.listWords(page, size, category));
    }
    
    // ==================== 举报管理 ====================
    
    @Operation(summary = "分页查询举报")
    @GetMapping("/reports")
    public Result<Page<Report>> listReports(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Integer status) {
        return Result.success(reportService.listReports(page, size, status));
    }
    
    @Operation(summary = "处理举报")
    @PutMapping("/reports/{id}/handle")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'IT_ADMIN')")
    public Result<Void> handleReport(
            @RequestHeader("X-Admin-Id") Long adminId,
            @PathVariable Long id,
            @RequestParam int status,
            @RequestParam(required = false) String comment) {
        reportService.handleReport(id, status, comment, adminId);
        return Result.success();
    }
    
    // ==================== 系统配置 ====================
    
    @Operation(summary = "获取所有配置")
    @GetMapping("/configs")
    public Result<List<SysConfig>> getAllConfigs() {
        return Result.success(sysConfigService.getAllConfigs());
    }
    
    @Operation(summary = "更新配置")
    @PutMapping("/configs/{key}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public Result<Void> updateConfig(
            @RequestHeader("X-Admin-Id") Long adminId,
            @PathVariable String key,
            @RequestParam String value,
            @RequestParam(required = false) String description) {
        sysConfigService.setConfig(key, value, description, adminId);
        return Result.success();
    }
}
