package org.example.imserver.feign;

import com.example.domain.vo.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

/**
 * 会话服务 Feign 客户端
 * 调用 IM-relationship-server 管理会话与成员关系
 */
@FeignClient(
    name = "IM-group-server",
    contextId = "conversationServiceClient",
    url = "${feign.group-service.url:http://localhost:8003}",
    configuration = FeignConfig.class
)
public interface ConversationServiceClient {

    /**
     * 创建或获取单聊会话
     */
    @PostMapping("/api/v1/conversations/p2p")
    Result<Map<String, Object>> createOrGetP2PConversation(
            @RequestParam("userAId") Long userAId,
            @RequestParam("userBId") Long userBId);

    /**
     * 创建或获取群聊会话
     */
    @PostMapping("/api/v1/conversations/group/{groupId}")
    Result<Map<String, Object>> createOrGetGroupConversation(
            @PathVariable("groupId") String groupId);

    /**
     * 获取会话成员列表
     */
    @GetMapping("/api/v1/conversations/{conversationId}/members")
    Result<Map<String, Object>> getConversationMembers(
            @PathVariable("conversationId") String conversationId);
}
