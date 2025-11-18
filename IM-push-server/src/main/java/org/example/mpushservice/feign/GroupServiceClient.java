package org.example.mpushservice.feign;

import com.example.domain.vo.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

/**
 * 群组服务 Feign 客户端
 * 用于获取群成员列表
 */
@FeignClient(
    name = "IM-group-server",
    url = "${feign.group-service.url:http://localhost:8003}",
    configuration = FeignConfig.class
)
public interface GroupServiceClient {

    /**
     * 获取群组成员列表
     * @param groupId 群组ID
     * @param page 页码（从1开始）
     * @param size 每页大小
     * @return 群组成员列表
     */
    @GetMapping("/api/v1/groups/{groupId}/members")
    Result<Map<String, Object>> getGroupMembers(
        @PathVariable("groupId") String groupId,
        @RequestParam(value = "page", defaultValue = "1") int page,
        @RequestParam(value = "size", defaultValue = "1000") int size,
        @RequestHeader("X-Internal-Service") String internalService
    );
}
