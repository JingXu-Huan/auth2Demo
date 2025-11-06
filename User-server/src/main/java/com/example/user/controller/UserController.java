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
import java.util.HashMap;
import java.util.Map;

/**
 * 用户控制器
 * 提供用户相关的 REST API
 */
@Slf4j
@Api(tags = "用户管理", description = "用户信息管理接口")
@RestController
@RequestMapping("/api/users")
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
            
            // 2. 创建用户
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
    public com.example.domain.dto.UserDetailsDTO getUserDetailsByEmail(
            @ApiParam(value = "邮箱地址", required = true)
            @PathVariable String email) {
        
        try {
            return userService.getUserDetailsByEmail(email);
        } catch (Exception e) {
            log.error("获取用户详情失败: email={}", email, e);
            return null;
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
}
