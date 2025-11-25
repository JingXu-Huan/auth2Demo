package com.example.auth.fallback;

import com.example.auth.feign.UserServiceClient;
import com.example.domain.dto.UserDetailsDTO;
import com.example.domain.vo.Result;
import com.example.domain.vo.UserVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author Junjie
 * @version 1.0.0
 * @date 2025-11-06
 * UserServiceClient 降级处理
 * 适用于sentinel 等服务降级方案
 * 当 User-server 服务不可用时的备用方案
 */
@Slf4j
@Component
public class UserServiceClientFallback implements UserServiceClient {
    
    @Override
    public Result<UserDetailsDTO> getUserDetailsByEmail(String email) {
        log.error("调用 User-server 失败，触发降级: getUserDetailsByEmail({})", email);
        return Result.error(500, "用户服务暂时不可用");
    }
    
    @Override
    public Result<UserDetailsDTO> getUserDetailsByUsername(String username) {
        log.error("调用 User-server 失败，触发降级: getUserDetailsByUsername({})", username);
        return Result.error(500, "用户服务暂时不可用");
    }
    
    @Override
    public Result<Boolean> checkEmailExists(String email) {
        log.error("调用 User-server 失败，触发降级: checkEmailExists({})", email);
        return Result.error(500, "服务暂时不可用");
    }
    
    @Override
    public Result<Boolean> checkUsernameExists(String username) {
        log.error("调用 User-server 失败，触发降级: checkUsernameExists({})", username);
        return Result.error(500, "服务暂时不可用");
    }
    
    @Override
    public Result<UserVO> getUserById(Long userId) {
        log.error("调用 User-server 失败，触发降级: getUserById({})", userId);
        return Result.error(500, "服务暂时不可用");
    }
    
    @Override
    public void updateLastLoginTime(String email) {
        log.error("[Fallback] 更新最后登录时间失败: email={}", email);
    }
    
    @Override
    public Result<UserDetailsDTO> createOrUpdateOAuthUser(String provider, String providerUserId, 
                                                          String username, String email, String avatarUrl) {
        log.error("[Fallback] 创建或更新OAuth用户失败: provider={}, username={}", provider, username);
        return Result.error(500, "User服务暂时不可用，请稍后再试");
    }
}
