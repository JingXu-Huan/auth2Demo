package com.example.user.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * 调用 IM-relationship-server，在 Neo4j 中初始化用户节点
 */
@FeignClient(
        name = "IM-group-server",
        url = "${feign.group-service.url:http://localhost:8003}",
        path = "/api/v1/relationship/internal/users",
        configuration = FeignConfig.class
)
public interface RelationshipUserClient {

    @PostMapping("/{userId}/ensure-node")
    void ensureUserNode(@PathVariable("userId") Long userId);
}
