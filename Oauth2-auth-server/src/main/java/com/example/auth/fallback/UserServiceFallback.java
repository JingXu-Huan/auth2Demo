package com.example.auth.fallback;

import com.example.domain.dto.UserDetailsDTO;
import com.example.domain.vo.Result;
import com.example.domain.vo.UserVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author Junjie
 * @version 1.0.0
 * @date 2025-11-06
 * UserService 降级处理（备用）
 * 注意：现在使用 UserServiceClientFallback
 */
@Slf4j
@Component
public class UserServiceFallback {
    
    /**
     * @author Junjie
     * @version 1.0.0
     * @date 2025-11-06
     */
    public UserDetailsDTO getUserDetailsByEmail(String email) {
        log.error("User服务降级: getUserDetailsByEmail({})", email);
        return null;
    }
    
    /**
     * @author Junjie
     * @version 1.0.0
     * @date 2025-11-06
     */
    public UserDetailsDTO getUserDetailsByUsername(String username) {
        log.error("User服务降级: getUserDetailsByUsername({})", username);
        return null;
    }
    
    /**
     * @author Junjie
     * @version 1.0.0
     * @date 2025-11-06
     */
    public Result<Boolean> checkEmailExists(String email) {
        log.error("User服务降级: checkEmailExists({})", email);
        return Result.error(500, "服务暂时不可用，请稍后重试");
    }
    
    /**
     * @author Junjie
     * @version 1.0.0
     * @date 2025-11-06
     */
    public Result<Boolean> checkUsernameExists(String username) {
        log.error("User服务降级: checkUsernameExists({})", username);
        return Result.error(500, "服务暂时不可用，请稍后重试");
    }
    
    /**
     * @author Junjie
     * @version 1.0.0
     * @date 2025-11-06
     */
    public Result<UserVO> getUserById(Long userId) {
        log.error("User服务降级: getUserById({})", userId);
        return Result.error(500, "服务暂时不可用，请稍后重试");
    }
}
