package com.example.user.controller;

import com.example.domain.model.UserProfile;
import com.example.domain.vo.Result;
import com.example.user.service.UserProfileService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

/**
 * 用户详情控制器
 * 提供用户详细信息管理的API
 */
@Slf4j
@Tag(name = "API")
@RestController
@RequestMapping("/api/v1/users/profile")
@Validated
public class UserProfileController {
    
    @Autowired
    private UserProfileService userProfileService;
    
    /**
     * 获取用户详情
     */
    @Operation(summary = "获取用户详情")
    @GetMapping("/{userId}")
    public ResponseEntity<Result<UserProfile>> getUserProfile(
            @Parameter(description = "用户ID")
            @PathVariable Long userId) {
        
        try {
            UserProfile profile = userProfileService.getUserProfile(userId);
            
            if (profile == null) {
                return ResponseEntity.ok(Result.error(404, "用户详情不存在"));
            }
            
            return ResponseEntity.ok(Result.success(profile));
            
        } catch (Exception e) {
            log.error("获取用户详情失败: userId={}", userId, e);
            return ResponseEntity.ok(Result.error(500, "获取用户详情失败"));
        }
    }
    
    /**
     * 更新用户基本信息
     */
    @Operation(summary = "更新用户基本信息")
    @PutMapping("/{userId}/basic")
    public ResponseEntity<Result<Void>> updateBasicInfo(
            @Parameter(description = "用户ID")
            @PathVariable Long userId,
            @Parameter(description = "基本信息")
            @Valid @RequestBody BasicInfoDTO basicInfo) {
        
        try {
            boolean success = userProfileService.updateBasicInfo(
                userId,
                basicInfo.getNickname(),
                basicInfo.getRealName(),
                basicInfo.getGender(),
                basicInfo.getBirthday(),
                basicInfo.getBio()
            );
            
            if (success) {
                return ResponseEntity.ok(Result.success("更新成功"));
            } else {
                return ResponseEntity.ok(Result.error(500, "更新失败"));
            }
            
        } catch (Exception e) {
            log.error("更新用户基本信息失败: userId={}", userId, e);
            return ResponseEntity.ok(Result.error(500, "更新失败: " + e.getMessage()));
        }
    }
    
    /**
     * 更新用户头像
     */
    @Operation(summary = "更新用户头像")
    @PutMapping("/{userId}/avatar")
    public ResponseEntity<Result<Void>> updateAvatar(
            @Parameter(description = "用户ID")
            @PathVariable Long userId,
            @Parameter(description = "头像URL")
            @RequestParam String avatarUrl) {
        
        try {
            boolean success = userProfileService.updateAvatar(userId, avatarUrl);
            
            if (success) {
                return ResponseEntity.ok(Result.success("头像更新成功"));
            } else {
                return ResponseEntity.ok(Result.error(500, "头像更新失败"));
            }
            
        } catch (Exception e) {
            log.error("更新用户头像失败: userId={}", userId, e);
            return ResponseEntity.ok(Result.error(500, "更新失败: " + e.getMessage()));
        }
    }
    
    /**
     * 更新工作信息
     */
    @Operation(summary = "更新工作信息")
    @PutMapping("/{userId}/work")
    public ResponseEntity<Result<Void>> updateWorkInfo(
            @Parameter(description = "用户ID")
            @PathVariable Long userId,
            @Parameter(description = "工作信息")
            @Valid @RequestBody WorkInfoDTO workInfo) {
        
        try {
            boolean success = userProfileService.updateWorkInfo(
                userId,
                workInfo.getCompany(),
                workInfo.getDepartment(),
                workInfo.getPosition(),
                workInfo.getEmployeeId()
            );
            
            if (success) {
                return ResponseEntity.ok(Result.success("工作信息更新成功"));
            } else {
                return ResponseEntity.ok(Result.error(500, "工作信息更新失败"));
            }
            
        } catch (Exception e) {
            log.error("更新工作信息失败: userId={}", userId, e);
            return ResponseEntity.ok(Result.error(500, "更新失败: " + e.getMessage()));
        }
    }
    
    /**
     * 更新地址信息
     */
    @Operation(summary = "更新地址信息")
    @PutMapping("/{userId}/address")
    public ResponseEntity<Result<Void>> updateAddressInfo(
            @Parameter(description = "用户ID")
            @PathVariable Long userId,
            @Parameter(description = "地址信息")
            @Valid @RequestBody AddressInfoDTO addressInfo) {
        
        try {
            boolean success = userProfileService.updateAddressInfo(
                userId,
                addressInfo.getCountry(),
                addressInfo.getProvince(),
                addressInfo.getCity(),
                addressInfo.getAddress()
            );
            
            if (success) {
                return ResponseEntity.ok(Result.success("地址信息更新成功"));
            } else {
                return ResponseEntity.ok(Result.error(500, "地址信息更新失败"));
            }
            
        } catch (Exception e) {
            log.error("更新地址信息失败: userId={}", userId, e);
            return ResponseEntity.ok(Result.error(500, "更新失败: " + e.getMessage()));
        }
    }
    
    /**
     * 更新偏好设置
     */
    @Operation(summary = "更新偏好设置")
    @PutMapping("/{userId}/preferences")
    public ResponseEntity<Result<Void>> updatePreferences(
            @Parameter(description = "用户ID")
            @PathVariable Long userId,
            @Parameter(description = "时区")
            @RequestParam String timezone,
            @Parameter(description = "语言")
            @RequestParam String locale) {
        
        try {
            boolean success = userProfileService.updatePreferences(userId, timezone, locale);
            
            if (success) {
                return ResponseEntity.ok(Result.success("偏好设置更新成功"));
            } else {
                return ResponseEntity.ok(Result.error(500, "偏好设置更新失败"));
            }
            
        } catch (Exception e) {
            log.error("更新偏好设置失败: userId={}", userId, e);
            return ResponseEntity.ok(Result.error(500, "更新失败: " + e.getMessage()));
        }
    }
    
    /**
     * 更新扩展字段
     */
    @Operation(summary = "更新扩展字段")
    @PutMapping("/{userId}/extra")
    public ResponseEntity<Result<Void>> updateExtra(
            @Parameter(description = "用户ID")
            @PathVariable Long userId,
            @Parameter(description = "扩展字段")
            @RequestBody Map<String, Object> extra) {
        
        try {
            boolean success = userProfileService.updateExtra(userId, extra);
            
            if (success) {
                return ResponseEntity.ok(Result.success("扩展字段更新成功"));
            } else {
                return ResponseEntity.ok(Result.error(500, "扩展字段更新失败"));
            }
            
        } catch (Exception e) {
            log.error("更新扩展字段失败: userId={}", userId, e);
            return ResponseEntity.ok(Result.error(500, "更新失败: " + e.getMessage()));
        }
    }
    
    /**
     * 检查用户资料是否完善
     */
    @Operation(summary = "检查资料完整性")
    @GetMapping("/{userId}/complete")
    public ResponseEntity<Result<Boolean>> isProfileComplete(
            @Parameter(description = "用户ID")
            @PathVariable Long userId) {
        
        try {
            boolean complete = userProfileService.isProfileComplete(userId);
            return ResponseEntity.ok(Result.success(complete));
            
        } catch (Exception e) {
            log.error("检查资料完整性失败: userId={}", userId, e);
            return ResponseEntity.ok(Result.error(500, "检查失败"));
        }
    }
    
    /**
     * 基本信息DTO
     */
    @Data
    public static class BasicInfoDTO {
        private String nickname;
        private String realName;
        private Integer gender;
        private String birthday;
        private String bio;
    }
    
    /**
     * 工作信息DTO
     */
    @Data
    public static class WorkInfoDTO {
        private String company;
        private String department;
        private String position;
        private String employeeId;
    }
    
    /**
     * 地址信息DTO
     */
    @Data
    public static class AddressInfoDTO {
        private String country;
        private String province;
        private String city;
        private String address;
    }
}
