package com.example.user.controller;

import com.example.common.converter.UserConverter;
import com.example.common.dto.UserDetailsDTO;
import com.example.common.model.User;
import com.example.common.vo.Result;
import com.example.common.vo.UserRegisterVO;
import com.example.common.vo.UserVO;
import com.example.user.service.UserService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Junjie
 * @version 1.0.0
 * @date 2025-11-06
 * 用户控制器
 * 提供用户相关的 REST API
 */
@Slf4j
@RestController
@Api(tags = "用户", description = "用户相关接口")
@RequestMapping("/api/users")
// CORS 由网关统一处理
public class UserController {
    
    /*  
     * @auther Junjie 
     * @version 1.0.0
     * @date 2025-11-06
     * 自动装配 UserService
     */
    @Autowired
    private UserService userService;
    
    /**
     * @auther Junjie 
     * @version 2.0.0
     * @date 2025-11-06
     * 根据用户名查询用户详情（仅供内部服务调用，需要认证）
     * ⚠️ 此接口包含敏感信息（passwordHash），仅用于服务间认证，不应暴露给前端
     */
    @GetMapping("/details/{username}")
    @ApiOperation(value = "根据用户名查询用户详情（内部接口）", notes = "⚠️ 包含敏感信息，仅供服务间调用")
    public UserDetailsDTO getUserDetails(@ApiParam(value = "username:用户名",required = true) @PathVariable String username) {
        // TODO: 添加服务间认证，确保只有授权的服务可以调用此接口
        return userService.getUserDetailsByUsername(username);
    }
    
    /**
     * @auther Junjie 
     * @version 2.0.0
     * @date 2025-11-06
     * 根据邮箱查询用户详情（仅供内部服务调用，需要认证）
     * ⚠️ 此接口包含敏感信息（passwordHash），仅用于服务间认证，不应暴露给前端
     */
    @GetMapping("/details/email/{email}")
    @ApiOperation(value = "根据邮箱查询用户详情（内部接口）", notes = "⚠️ 包含敏感信息，仅供服务间调用")
    public UserDetailsDTO getUserDetailsByEmail(@ApiParam(value = "email:邮箱",required = true) @PathVariable String email) {
        // TODO: 添加服务间认证，确保只有授权的服务可以调用此接口
        return userService.getUserDetailsByEmail(email);
    }
    
    /**
     * @auther Junjie 
     * @version 3.0.0
     * @date 2025-11-06
     * 根据用户名查询用户（返回 VO）
     */
    @GetMapping("/{username}")
    @ApiOperation(value = "根据用户名查询用户")
    public Result<UserVO> getUserByUsername(@ApiParam(value = "username:用户名",required = true) @PathVariable String username) {
        User user = userService.getUserByUsername(username);
        UserVO vo = UserConverter.toVO(user);
        return Result.success(vo);
    }
    
    /**
     * @auther Junjie 
     * @version 3.0.0
     * @date 2025-11-06
     * 根据ID查询用户（返回 VO）
     */
    @GetMapping("/id/{id}")
    @ApiOperation(value = "根据ID查询用户")
    public Result<UserVO> getUserById(@ApiParam(value = "id:用户ID",required = true) @PathVariable Long id) {
        User user = userService.getUserById(id);
        UserVO vo = UserConverter.toVO(user);
        return Result.success(vo);
    }
    
    /**
     * @auther Junjie 
     * @version 2.0.0
     * @date 2025-11-06
     * 检查用户名是否存在
     */
    @GetMapping("/exists/{username}")
    @ApiOperation(value = "检查用户名是否存在")
    public Result<Boolean> checkUsernameExists(@ApiParam(value = "username:用户名",required = true) @PathVariable String username) {
        boolean exists = userService.usernameExists(username);
        return Result.success(exists);
    }
    
    /**
     * @auther Junjie 
     * @version 2.0.0
     * @date 2025-11-06
     * 检查邮箱是否存在
     */
    @GetMapping("/exists/email/{email}")
    @ApiOperation(value = "检查邮箱是否存在")
    public Result<Boolean> checkEmailExists(@ApiParam(value = "email:邮箱",required = true) @PathVariable String email) {
        boolean exists = userService.emailExists(email);
        return Result.success(exists);
    }
    
    /**
     * @auther Junjie 
     * @version 3.0.0
     * @date 2025-11-06
     * 保存或更新 Gitee 用户（返回 VO）
     */
    @PostMapping("/gitee")
    @ApiOperation(value = "保存或更新 Gitee 用户")
    public Result<UserVO> saveOrUpdateGiteeUser(@ApiParam(value = "giteeUserData:Gitee用户数据(1:giteeUserId,2:login,3:name,4:email,5:avatarUrl)",required = true) @RequestBody Map<String, String> giteeUserData) {
        User user = userService.saveOrUpdateGiteeUser(
            giteeUserData.get("giteeUserId"),
            giteeUserData.get("login"),
            giteeUserData.get("name"),
            giteeUserData.get("email"),
            giteeUserData.get("avatarUrl")
        );
        UserVO vo = UserConverter.toVO(user);
        return Result.success("操作成功", vo);
    }
    
    /**
     * @auther Junjie 
     * @version 3.0.0
     * @date 2025-11-06
     * 创建新用户（邮箱密码方式）
     * 返回注册 VO，不包含敏感信息
     */
    @PostMapping("/register")
    @ApiOperation(value = "创建新用户", notes = "注册成功后会发送验证邮件到用户邮箱")
    public Result<UserRegisterVO> createUser(@ApiParam(value = "userData:用户数据(1:username,2:email,3:passwordHash)",required = true) @RequestBody Map<String, String> userData) {
        User user = userService.createUser(
            userData.get("username"),
            userData.get("email"),
            userData.get("passwordHash")
        );
        
        UserRegisterVO vo = UserConverter.toRegisterVO(user);
        return Result.success("注册成功", vo);
    }

    /**
     * @auther Junjie 
     * @version 1.0.0
     * @date 2025-11-06
     * 确认邮箱验证
     * 用户点击邮件中的链接后，调用此接口激活账户
     */
    @GetMapping("/confirm")
    @ApiOperation(value = "确认邮箱验证")
    public ResponseEntity<String> confirmEmailVerify(@ApiParam(value = "token:邮箱验证令牌",required = true) @RequestParam("token") String token) {
        if(token == null || token.isEmpty()) {
            return ResponseEntity.badRequest().body("错误: token不能为空");
        }
        
        // 调用 UserService 的 confirmToken 方法进行验证
        return userService.confirmToken(token);
    }
}
