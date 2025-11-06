package com.example.auth.feign;

import com.example.common.dto.UserDetailsDTO;
import com.example.common.vo.Result;
import com.example.common.vo.UserVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * User 服务 Feign 客户端
 * 通过 OpenFeign 远程调用 User-server 服务
 */
@FeignClient(name = "user-server")
public interface UserServiceClient {
    
    /**
     * 根据用户名查询用户详情（包含凭证信息）
     */
    @GetMapping("/api/users/details/{username}")
    UserDetailsDTO getUserDetails(@PathVariable("username") String username);
    
    /**
     * 根据邮箱查询用户详情（包含凭证信息）
     */
    @GetMapping("/api/users/details/email/{email}")
    UserDetailsDTO getUserDetailsByEmail(@PathVariable("email") String email);
    
    /**
     * 根据用户名查询用户（新格式：Result<UserVO>）
     */
    @GetMapping("/api/users/{username}")
    Result<UserVO> getUserByUsername(@PathVariable("username") String username);
    
    /**
     * 根据ID查询用户（新格式：Result<UserVO>）
     */
    @GetMapping("/api/users/id/{id}")
    Result<UserVO> getUserById(@PathVariable("id") Long id);
    
    /**
     * 检查用户名是否存在（新格式：Result<Boolean>）
     */
    @GetMapping("/api/users/exists/{username}")
    Result<Boolean> checkUsernameExists(@PathVariable("username") String username);
    
    /**
     * 检查邮箱是否存在（新格式：Result<Boolean>）
     */
    @GetMapping("/api/users/exists/email/{email}")
    Result<Boolean> checkEmailExists(@PathVariable("email") String email);
    
    /**
     * 保存或更新 Gitee 用户（新格式：Result<UserVO>）
     */
    @PostMapping("/api/users/gitee")
    Result<UserVO> saveOrUpdateGiteeUser(@RequestBody Map<String, String> giteeUserData);
    
    /**
     * 创建新用户（新格式：Result<UserRegisterVO>）
     */
    @PostMapping("/api/users/register")
    Result<Map<String, Object>> createUser(@RequestBody Map<String, String> userData);
    
    /**
     * 验证密码是否正确
     */
    @PostMapping("/api/security/validate-password")
    Map<String, Object> validatePassword(@RequestBody Map<String, String> request);
    
    /**
     * 检查是否需要安全验证
     */
    @GetMapping("/api/security/check-verification")
    Map<String, Object> checkSecurityVerification(@RequestParam("email") String email);
    
    /**
     * 发送安全验证码
     */
    @PostMapping("/api/security/send-code")
    Map<String, Object> sendSecurityCode(@RequestBody Map<String, String> request);
    
    /**
     * 验证安全验证码（返回 Result<Boolean>）
     */
    @PostMapping("/api/security/verify-code")
    Result<Boolean> verifySecurityCode(@RequestBody Map<String, String> request);
}
