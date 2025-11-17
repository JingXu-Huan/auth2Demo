package com.example.user.controller;

import com.example.common.converter.UserConverter;
import com.example.domain.dto.UserDTO;
import com.example.domain.model.User;
import com.example.domain.vo.Result;
import com.example.domain.vo.UserRegisterVO;
import com.example.domain.vo.UserVO;
import com.example.user.service.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import java.util.Map;

/**
 * @author Junjie
 * @version 1.0.0
 * @date 2025-11-06
 * 用户控制器
 * 提供用户相关的 REST API
 */
@Slf4j
@Api(tags = "用户管理", description = "用户信息管理接口")
@RestController
@RequestMapping("/api/v1/users")
@Validated
public class UserController {
    
    @Autowired
    private UserService userService;
    
    /**
     * 用户注册
     */
    @ApiOperation(value = "用户注册", notes = "通过邮箱注册新用户")
    @PostMapping("/register")
    public ResponseEntity<Result<UserRegisterVO>> register(
            @ApiParam(value = "用户注册信息", required = true)
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
    @ApiOperation(value = "获取用户信息", notes = "根据用户ID获取用户详细信息")
    @GetMapping("/{userId}")
    public ResponseEntity<Result<UserVO>> getUserById(
            @ApiParam(value = "用户ID", required = true)
            @PathVariable Long userId) {
        
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
    @ApiOperation(value = "检查邮箱是否存在", notes = "用于注册时验证邮箱")
    @GetMapping("/check-email")
    public Result<Boolean> checkEmailExists(
            @ApiParam(value = "邮箱地址", required = true)
            @RequestParam @Email String email) {
        
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
    @ApiOperation(value = "检查用户名是否存在", notes = "用于注册时验证用户名")
    @GetMapping("/check-username")
    public Result<Boolean> checkUsernameExists(
            @ApiParam(value = "用户名", required = true)
            @RequestParam @NotBlank String username) {
        
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
    @ApiOperation(value = "根据邮箱获取用户详情", notes = "用于OAuth2认证服务器调用")
    @GetMapping("/details/email/{email}")
    public ResponseEntity<Result<com.example.domain.dto.UserDetailsDTO>> getUserDetailsByEmail(
            @ApiParam(value = "邮箱地址", required = true)
            @PathVariable String email) {
        
        log.info("Controller收到请求: 获取用户详情, email={}", email);
        
        try {
            log.info("调用Service层查询用户");
            com.example.domain.dto.UserDetailsDTO userDetails = userService.getUserDetailsByEmail(email);
            log.info("Service层返回: userDetails={}", userDetails != null ? "存在" : "null");
            
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
    @ApiOperation(value = "根据用户名获取用户详情", notes = "用于OAuth2认证服务器调用")
    @GetMapping("/details/username/{username}")
    public com.example.domain.dto.UserDetailsDTO getUserDetailsByUsername(
            @ApiParam(value = "用户名", required = true)
            @PathVariable String username) {
        
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
    @ApiOperation(value = "更新用户信息", notes = "更新用户的基本信息")
    @PutMapping("/{userId}")
    public ResponseEntity<Result<Void>> updateUser(
            @ApiParam(value = "用户ID", required = true)
            @PathVariable Long userId,
            @ApiParam(value = "用户信息", required = true)
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
    @ApiOperation(value = "修改密码", notes = "用户修改自己的密码")
    @PostMapping("/{userId}/change-password")
    public ResponseEntity<Result<Void>> changePassword(
            @ApiParam(value = "用户ID", required = true)
            @PathVariable Long userId,
            @ApiParam(value = "密码信息", required = true)
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
     * 创建或更新第三方 OAuth 用户
     * 此接口由 OAuth2-auth-server 在第三方登录成功后调用
     */
    @ApiOperation(value = "创建或更新OAuth用户", notes = "第三方登录时创建或更新用户信息")
    @PostMapping("/oauth/create-or-update")
    public ResponseEntity<Result<com.example.domain.dto.UserDetailsDTO>> createOrUpdateOAuthUser(
            @ApiParam(value = "OAuth提供商", required = true) @RequestParam String provider,
            @ApiParam(value = "提供商用户ID", required = true) @RequestParam String providerUserId,
            @ApiParam(value = "用户名", required = true) @RequestParam String username,
            @ApiParam(value = "邮箱") @RequestParam(required = false) String email,
            @ApiParam(value = "头像URL") @RequestParam(required = false) String avatarUrl) {
        
        try {
            log.info("创建或更新OAuth用户: provider={}, providerUserId={}, username={}, email={}", 
                    provider, providerUserId, username, email);
            
            User user = userService.createOrUpdateOAuthUser(provider, providerUserId, username, email, avatarUrl);
            
            com.example.domain.dto.UserDetailsDTO userDetails = new com.example.domain.dto.UserDetailsDTO();
            userDetails.setUserId(user.getId());
            userDetails.setUsername(user.getUsername());
            userDetails.setEmail(user.getEmail());
            userDetails.setDisplayName(user.getDisplayName());
            userDetails.setAvatarUrl(user.getAvatarUrl());
            userDetails.setProvider(provider);
            
            log.info("OAuth用户创建或更新成功: userId={}, username={}", user.getId(), user.getUsername());
            return ResponseEntity.ok(Result.success("操作成功", userDetails));
            
        } catch (Exception e) {
            log.error("创建或更新OAuth用户失败", e);
            return ResponseEntity.ok(Result.error(500, "操作失败: " + e.getMessage()));
        }
    }
    
    /**
     * 更新用户最后登录时间
     * 此接口由 OAuth2-auth-server 在用户登录成功后调用
     */
    @ApiOperation(value = "更新最后登录时间", notes = "用户登录成功后更新最后登录时间")
    @PostMapping("/update-login-time")
    public ResponseEntity<Result<Void>> updateLastLoginTime(
            @ApiParam(value = "用户邮箱", required = true)
            @RequestParam String email) {
        
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
    @ApiOperation(value = "搜索用户", notes = "通过邮箱或手机号搜索用户")
    @GetMapping("/search")
    public ResponseEntity<Result<UserVO>> searchUser(
            @ApiParam(value = "搜索类型：email 或 phone", required = true)
            @RequestParam String searchType,
            @ApiParam(value = "搜索关键词", required = true)
            @RequestParam String keyword) {
        
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
