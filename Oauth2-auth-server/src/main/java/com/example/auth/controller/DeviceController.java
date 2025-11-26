package com.example.auth.controller;

import com.example.auth.model.UserDevice;
import com.example.auth.service.DeviceService;
import com.example.common.util.JwtUtil;
import com.example.domain.vo.Result;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 设备管理Controller
 * 
 * @author Cascade AI
 * @date 2024-11-24
 */
@Slf4j
@Tag(name = "API")
@RestController
@RequestMapping("/api/auth/devices")
public class DeviceController {
    
    @Autowired
    private DeviceService deviceService;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    /**
     * 获取当前用户的所有登录设备
     */
    @Operation(summary = "获取当前登录设备列表")
    @GetMapping
    public Result<List<UserDevice>> getDevices(HttpServletRequest request) {
        try {
            // 从JWT获取当前用户ID
            Long userId = getCurrentUserId(request);
            if (userId == null) {
                return Result.error(401, "未登录或Token无效");
            }
            
            List<UserDevice> devices = deviceService.getUserDevices(userId);
            return Result.success(devices);
        } catch (Exception e) {
            log.error("获取设备列表失败", e);
            return Result.error(500, "获取失败: " + e.getMessage());
        }
    }
    
    /**
     * 踢下线指定设备
     */
    @Operation(summary = "踢下线指定设备")
    @DeleteMapping("/{deviceId}")
    public Result<Void> kickDevice(
            @Parameter(description = "设备ID")
            @PathVariable String deviceId,
            HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            if (userId == null) {
                return Result.error(401, "未登录或Token无效");
            }
            
            deviceService.kickDevice(userId, deviceId);
            return Result.success("设备已踢下线");
        } catch (Exception e) {
            log.error("踢设备失败: deviceId={}", deviceId, e);
            return Result.error(500, "踢下线失败: " + e.getMessage());
        }
    }
    
    /**
     * 踢下线所有其他设备（保留当前设备）
     */
    @Operation(summary = "踢下线所有其他设备")
    @PostMapping("/kick-others")
    public Result<Void> kickOtherDevices(@RequestBody KickOthersRequest kickRequest, 
                                         HttpServletRequest httpRequest) {
        try {
            Long userId = getCurrentUserId(httpRequest);
            if (userId == null) {
                return Result.error(401, "未登录或Token无效");
            }
            
            // 获取所有设备
            List<UserDevice> devices = deviceService.getUserDevices(userId);
            
            // 踢掉除当前设备外的所有设备
            int kickedCount = 0;
            for (UserDevice device : devices) {
                if (!device.getDeviceId().equals(kickRequest.getCurrentDeviceId()) 
                    && UserDevice.DeviceStatus.ACTIVE.name().equals(device.getStatus())) {
                    deviceService.kickDevice(userId, device.getDeviceId());
                    kickedCount++;
                }
            }
            
            return Result.success("已踢下线 " + kickedCount + " 个设备");
        } catch (Exception e) {
            log.error("踢下线其他设备失败", e);
            return Result.error(500, "操作失败: " + e.getMessage());
        }
    }
    
    /**
     * 统计活跃设备数
     */
    @Operation(summary = "统计活跃设备数")
    @GetMapping("/count")
    public Result<Integer> countActiveDevices(HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            if (userId == null) {
                return Result.error(401, "未登录或Token无效");
            }
            
            int count = deviceService.countActiveDevices(userId);
            return Result.success(count);
        } catch (Exception e) {
            log.error("统计设备数失败", e);
            return Result.error(500, "统计失败: " + e.getMessage());
        }
    }
    
    /**
     * 从请求中获取当前用户ID
     * 从Authorization Header中提取JWT Token并解析userId
     */
    private Long getCurrentUserId(HttpServletRequest request) {
        try {
            // 1. 从Header中获取Token
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.warn("未找到有效的Authorization Header");
                return null;
            }
            
            // 2. 提取Token（去除"Bearer "前缀）
            String token = authHeader.substring(7);
            
            // 3. 使用JwtUtil解析userId
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
    
    @Data
    public static class KickOthersRequest {
        private String currentDeviceId;
    }
}
