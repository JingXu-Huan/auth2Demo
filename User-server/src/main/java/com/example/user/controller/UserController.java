package com.example.user.controller;

import com.example.common.converter.UserConverter;
import com.example.domain.dto.UserDTO;
import com.example.domain.model.User;
import com.example.domain.vo.Result;
import com.example.domain.vo.UserRegisterVO;
import com.example.domain.vo.UserVO;
import com.example.user.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;

/**
 * @author Junjie
 * @version 1.0.0
 * @date 2025-11-06
 * 用户控制器
 * 提供用户相关的 REST API
 */
@Slf4j
@Tag(name = "API")
@RestController
@RequestMapping("/api/v1/users")
@Validated
public class UserController {
    
    @Autowired
    private UserService userService;
    
    /**
     * 用户注册
     */
    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public ResponseEntity<Result<UserRegisterVO>> register(
            @Parameter(description = "用户注册信息")
            @Valid @RequestBody UserDTO userDTO) {
        
        try {
            // 1. 验证邮箱是否已存在
            if (userService.checkEmailExists(userDTO.getEmail())) {
                return ResponseEntity.ok(Result.error(400, "邮箱已被注册"));
            }
            
            // 2. 验证用户名是否已存在（安全改进）
            if (userService.checkUsernameExists(userDTO.getUsername())) {
                return ResponseEntity.ok(Result.error(400, "用户名已被使用"));
            }
            
            // 3. 创建用户
            User user = userService.createUser(
                userDTO.getUsername(),
                userDTO.getEmail(),
                userDTO.getPassword()
            );
            
            if (user == null) {
                return ResponseEntity.ok(Result.error(500, "注册失败"));
            }
            
            // 3. 返回注册结果
            UserRegisterVO vo = UserConverter.toRegisterVO(user);
            vo.setMessage("注册成功，请验证邮箱");
            vo.setRequiresEmailVerification(true);
            
            log.info("用户注册成功: userId={}, email={}", user.getId(), user.getEmail());
            return ResponseEntity.ok(Result.success(vo));
            
        } catch (Exception e) {
            log.error("用户注册失败", e);
            return ResponseEntity.ok(Result.error(500, "注册失败: " + e.getMessage()));
        }
    }
    
    /**
     * 根据用户ID获取用户信息
     */
    @Operation(summary = "获取用户信息")
    @GetMapping("/{userId}")
    public ResponseEntity<Result<UserVO>> getUserById(
            @Parameter(description = "用户ID")
            @PathVariable("userId") Long userId) {
        
        try {
            User user = userService.getUserById(userId);
            
            if (user == null) {
                return ResponseEntity.ok(Result.error(404, "用户不存在"));
            }
            
            UserVO userVO = UserConverter.toVO(user);
            return ResponseEntity.ok(Result.success(userVO));
            
        } catch (Exception e) {
            log.error("获取用户信息失败: userId={}", userId, e);
            return ResponseEntity.ok(Result.error(500, "获取用户信息失败"));
        }
    }
    
    /**
     * 检查邮箱是否存在
     */
    @Operation(summary = "检查邮箱是否存在")
    @GetMapping("/check-email")
    public Result<Boolean> checkEmailExists(
            @Parameter(description = "邮箱地址")
            @RequestParam(value = "email") @Email String email) {
        
        try {
            boolean exists = userService.checkEmailExists(email);
            return Result.success(exists);
        } catch (Exception e) {
            log.error("检查邮箱失败: email={}", email, e);
            return Result.error(500, "检查邮箱失败");
        }
    }
    
    /**
     * 检查用户名是否存在
     */
    @Operation(summary = "检查用户名是否存在")
    @GetMapping("/check-username")
    public Result<Boolean> checkUsernameExists(
            @Parameter(description = "用户名")
            @RequestParam(value = "username") @NotBlank String username) {
        
        try {
            boolean exists = userService.checkUsernameExists(username);
            return Result.success(exists);
        } catch (Exception e) {
            log.error("检查用户名失败: username={}", username, e);
            return Result.error(500, "检查用户名失败");
        }
    }
    
    /**
     * 根据邮箱获取用户详情（用于登录认证）
     */
    @Operation(summary = "根据邮箱获取用户详情")
    @GetMapping("/details/email/{email}")
    public ResponseEntity<Result<com.example.domain.dto.UserDetailsDTO>> getUserDetailsByEmail(
            @Parameter(description = "邮箱地址")
            @PathVariable("email") String email) {
        
        log.info("Controller收到请求: 获取用户详情, email={}", email);
        
        try {
            log.info("调用Service层查询用户");
            com.example.domain.dto.UserDetailsDTO userDetails = userService.getUserDetailsByEmail(email);
            log.info("Service层返回: userDetails={}", userDetails);
            
            if (userDetails == null) {
                log.warn("用户不存在: email={}", email);
                return ResponseEntity.ok(Result.error(404, "用户不存在或未注册"));
            }
            
            log.info("获取用户详情成功: email={}, userId={}", email, userDetails.getUserId());
            return ResponseEntity.ok(Result.success(userDetails));
            
        } catch (Throwable t) {
            log.error("获取用户详情异常: email={}, 异常类型={}, 错误信息={}", 
                     email, t.getClass().getName(), t.getMessage(), t);
            return ResponseEntity.ok(Result.error(500, "获取用户详情失败: " + t.getMessage()));
        }
    }
    
    /**
     * 根据用户名获取用户详情
     */
    @Operation(summary = "根据用户名获取用户详情")
    @GetMapping("/details/username/{username}")
    public com.example.domain.dto.UserDetailsDTO getUserDetailsByUsername(
            @Parameter(description = "用户名")
            @PathVariable("username") String username) {
        
        try {
            return userService.getUserDetailsByUsername(username);
        } catch (Exception e) {
            log.error("获取用户详情失败: username={}", username, e);
            return null;
        }
    }
    
    /**
     * 更新用户信息
     */
    @Operation(summary = "更新用户信息")
    @PutMapping("/{userId}")
    public ResponseEntity<Result<Void>> updateUser(
            @Parameter(description = "用户ID")
            @PathVariable("userId") Long userId,
            @Parameter(description = "用户信息")
            @Valid @RequestBody UserDTO userDTO) {
        
        try {
            User user = userService.getUserById(userId);
            if (user == null) {
                return ResponseEntity.ok(Result.error(404, "用户不存在"));
            }
            
            // 更新用户信息
            user.setUsername(userDTO.getUsername());
            
            boolean success = userService.updateUser(user);
            
            if (success) {
                return ResponseEntity.ok(Result.success("更新成功"));
            } else {
                return ResponseEntity.ok(Result.error(500, "更新失败"));
            }
            
        } catch (Exception e) {
            log.error("更新用户信息失败: userId={}", userId, e);
            return ResponseEntity.ok(Result.error(500, "更新失败"));
        }
    }
    
    /**
     * 修改密码
     */
    @Operation(summary = "修改密码")
    @PostMapping("/{userId}/change-password")
    public ResponseEntity<Result<Void>> changePassword(
            @Parameter(description = "用户ID")
            @PathVariable("userId") Long userId,
            @Parameter(description = "密码信息")
            @RequestBody Map<String, String> passwordData) {
        
        try {
            String oldPassword = passwordData.get("oldPassword");
            String newPassword = passwordData.get("newPassword");
            
            if (oldPassword == null || newPassword == null) {
                return ResponseEntity.ok(Result.error(400, "参数不完整"));
            }
            
            boolean success = userService.changePassword(userId, oldPassword, newPassword);
            
            if (success) {
                return ResponseEntity.ok(Result.success("密码修改成功"));
            } else {
                return ResponseEntity.ok(Result.error(400, "旧密码错误"));
            }
            
        } catch (Exception e) {
            log.error("修改密码失败: userId={}", userId, e);
            return ResponseEntity.ok(Result.error(500, "修改密码失败"));
        }
    }
    
    
    /**
     * 更新用户最后登录时间
     * 此接口由 OAuth2-auth-server 在用户登录成功后调用
     */
    @Operation(summary = "更新最后登录时间")
    @PostMapping("/update-login-time")
    public ResponseEntity<Result<Void>> updateLastLoginTime(
            @Parameter(description = "用户邮箱")
            @RequestParam(value = "email") String email) {
        
        try {
            userService.updateLastLoginTime(email);
            log.info("更新最后登录时间成功: email={}", email);
            return ResponseEntity.ok(Result.success("更新成功"));
            
        } catch (Exception e) {
            log.error("更新最后登录时间失败: email={}", email, e);
            return ResponseEntity.ok(Result.error(500, "更新失败"));
        }
    }
    
    /**
     * 搜索用户（通过邮箱或手机号）
     */
    @Operation(summary = "搜索用户")
    @GetMapping("/search")
    public ResponseEntity<Result<UserVO>> searchUser(
            @Parameter(description = "搜索类型：email 或 phone")
            @RequestParam(value = "searchType") String searchType,
            @Parameter(description = "搜索关键词")
            @RequestParam(value = "keyword") String keyword) {
        
        try {
            User user = null;
            
            if ("email".equals(searchType)) {
                user = userService.getUserByEmail(keyword);
            } else if ("phone".equals(searchType)) {
                // TODO: 手机号搜索待实现
                return ResponseEntity.ok(Result.error(400, "手机号搜索功能待实现"));
            } else {
                return ResponseEntity.ok(Result.error(400, "不支持的搜索类型"));
            }
            
            if (user == null) {
                return ResponseEntity.ok(Result.error(404, "未找到用户"));
            }
            
            UserVO userVO = UserConverter.toVO(user);
            log.info("搜索用户成功: searchType={}, keyword={}, userId={}", searchType, keyword, user.getId());
            return ResponseEntity.ok(Result.success("搜索成功", userVO));
            
        } catch (Exception e) {
            log.error("搜索用户失败: searchType={}, keyword={}", searchType, keyword, e);
            return ResponseEntity.ok(Result.error(500, "搜索失败"));
        }
    }
}
