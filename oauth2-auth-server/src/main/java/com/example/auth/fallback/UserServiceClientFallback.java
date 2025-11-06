package com.example.auth.fallback;

import com.example.auth.feign.UserServiceClient;
import com.example.domain.dto.UserDetailsDTO;
import com.example.domain.vo.Result;
import com.example.domain.vo.UserVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * UserServiceClient 降级处理
 * 当 User-server 服务不可用时的备用方案
 */
@Slf4j
@Component
public class UserServiceClientFallback implements UserServiceClient {
    
    @Override
    public UserDetailsDTO getUserDetailsByEmail(String email) {
        log.error("调用 User-server 失败，触发降级: getUserDetailsByEmail({})", email);
        return null;
    }
    
    @Override
    public UserDetailsDTO getUserDetailsByUsername(String username) {
        log.error("调用 User-server 失败，触发降级: getUserDetailsByUsername({})", username);
        return null;
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
}
