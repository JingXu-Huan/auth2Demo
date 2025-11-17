package org.example.imgroupserver.feign;

import com.example.domain.vo.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

/**
 * User 服务 Feign 客户端
 */
@FeignClient(name = "user-server", path = "/api/v1/users")
public interface UserServiceClient {
    
    /**
     * 根据用户ID获取用户信息
     */
    @GetMapping("/{userId}")
    Result<Map<String, Object>> getUserById(@PathVariable("userId") Long userId);
}
