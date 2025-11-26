package com.example.auth.controller;

import com.example.auth.model.UserPunishment;
import com.example.auth.service.RiskControlService;
import com.example.common.util.JwtUtil;
import com.example.domain.vo.Result;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 风控管理Controller（管理员专用）
 * 
 * @author Cascade AI
 * @date 2024-11-24
 */
@Slf4j
@Tag(name = "API")
@RestController
@RequestMapping("/api/admin/risk")
public class RiskController {
    
    @Autowired
    private RiskControlService riskControlService;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    /**
     * 封禁用户
     */
    @Operation(summary = "封禁用户")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/ban")
    public Result<Void> banUser(@RequestBody BanUserRequest request, HttpServletRequest httpRequest) {
        try {
            log.warn("管理员封禁用户: userId={}, duration={}, reason={}", 
                request.getUserId(), request.getDuration(), request.getReason());
            
            // 从JWT中获取当前管理员ID
            Long operatorId = getCurrentUserId(httpRequest);
            if (operatorId == null) {
                return Result.error(401, "未登录或Token无效");
            }
            
            riskControlService.banUser(
                request.getUserId(), 
                request.getDuration(), 
                request.getReason(), 
                operatorId
            );
            
            return Result.success("封禁成功");
        } catch (Exception e) {
            log.error("封禁用户失败: userId={}", request.getUserId(), e);
            return Result.error(500, "封禁失败: " + e.getMessage());
        }
    }
    
    /**
     * 解封用户
     */
    @Operation(summary = "解封用户")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/unban")
    public Result<Void> unbanUser(@RequestBody UnbanUserRequest request) {
        try {
            log.info("管理员解封用户: userId={}, reason={}", 
                request.getUserId(), request.getReason());
            
            riskControlService.unbanUser(request.getUserId(), request.getReason());
            
            return Result.success("解封成功");
        } catch (Exception e) {
            log.error("解封用户失败: userId={}", request.getUserId(), e);
            return Result.error(500, "解封失败: " + e.getMessage());
        }
    }
    
    /**
     * 禁言用户
     */
    @Operation(summary = "禁言用户")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/mute")
    public Result<Void> muteUser(@RequestBody MuteUserRequest request, HttpServletRequest httpRequest) {
        try {
            log.warn("管理员禁言用户: userId={}, duration={}, reason={}", 
                request.getUserId(), request.getDuration(), request.getReason());
            
            Long operatorId = getCurrentUserId(httpRequest);
            if (operatorId == null) {
                return Result.error(401, "未登录或Token无效");
            }
            
            riskControlService.muteUser(
                request.getUserId(), 
                request.getDuration(), 
                request.getReason(), 
                operatorId
            );
            
            return Result.success("禁言成功");
        } catch (Exception e) {
            log.error("禁言用户失败: userId={}", request.getUserId(), e);
            return Result.error(500, "禁言失败: " + e.getMessage());
        }
    }
    
    /**
     * 取消禁言
     */
    @Operation(summary = "取消禁言")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/unmute")
    public Result<Void> unmuteUser(@RequestBody UnmuteUserRequest request) {
        try {
            log.info("管理员取消禁言: userId={}, reason={}", 
                request.getUserId(), request.getReason());
            
            riskControlService.unmuteUser(request.getUserId(), request.getReason());
            
            return Result.success("取消禁言成功");
        } catch (Exception e) {
            log.error("取消禁言失败: userId={}", request.getUserId(), e);
            return Result.error(500, "取消禁言失败: " + e.getMessage());
        }
    }
    
    /**
     * 检查用户状态
     */
    @Operation(summary = "检查用户状态")
    @GetMapping("/status/{userId}")
    public Result<UserStatusVO> checkUserStatus(
            @Parameter(description = "用户ID")
            @PathVariable Long userId) {
        try {
            UserStatusVO status = new UserStatusVO();
            status.setUserId(userId);
            status.setBanned(riskControlService.isUserBanned(userId));
            status.setMuted(riskControlService.isUserMuted(userId));
            
            // 获取当前有效的封禁记录
            UserPunishment activeBan = riskControlService.getActiveBan(userId);
            if (activeBan != null) {
                status.setBanReason(activeBan.getReason());
                status.setBanExpiresAt(activeBan.getExpiresAt());
            }
            
            return Result.success(status);
        } catch (Exception e) {
            log.error("检查用户状态失败: userId={}", userId, e);
            return Result.error(500, "检查失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取用户惩罚记录
     */
    @Operation(summary = "获取用户惩罚记录")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/punishments/{userId}")
    public Result<List<UserPunishment>> getUserPunishments(
            @Parameter(description = "用户ID")
            @PathVariable Long userId) {
        try {
            List<UserPunishment> punishments = riskControlService.getUserPunishments(userId);
            return Result.success(punishments);
        } catch (Exception e) {
            log.error("获取惩罚记录失败: userId={}", userId, e);
            return Result.error(500, "获取失败: " + e.getMessage());
        }
    }
    
    // ==================== Request DTOs ====================
    
    @Data
    public static class BanUserRequest {
        private Long userId;
        private Integer duration;  // 秒，null表示永久
        private String reason;
    }
    
    @Data
    public static class UnbanUserRequest {
        private Long userId;
        private String reason;
    }
    
    @Data
    public static class MuteUserRequest {
        private Long userId;
        private Integer duration;  // 秒
        private String reason;
    }
    
    @Data
    public static class UnmuteUserRequest {
        private Long userId;
        private String reason;
    }
    
    @Data
    public static class UserStatusVO {
        private Long userId;
        private Boolean banned;
        private Boolean muted;
        private String banReason;
        private java.time.LocalDateTime banExpiresAt;
    }
    
    /**
     * 从请求中获取当前用户ID（管理员ID）
     */
    private Long getCurrentUserId(HttpServletRequest request) {
        try {
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.warn("未找到有效的Authorization Header");
                return null;
            }
            
            String token = authHeader.substring(7);
            Long userId = jwtUtil.getUserIdFromToken(token);
            if (userId == null) {
                log.warn("Token解析失败或userId为空");
                return null;
            }
            
            return userId;
        } catch (Exception e) {
            log.error("获取当前用户ID失败", e);
            return null;
        }
    }
}
