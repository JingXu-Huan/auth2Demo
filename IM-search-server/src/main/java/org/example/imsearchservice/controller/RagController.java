package org.example.imsearchservice.controller;

import org.example.imsearchservice.service.RagService;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * RAG (检索增强生成) 问答控制器。
 */
@RestController
@RequestMapping("/api/search")
public class RagController {

    private final RagService ragService;

    public RagController(RagService ragService) {
        this.ragService = ragService;
    }

    @PostMapping("/ask")
    public Map<String, Object> ask(@RequestBody AskRequest request) {
        return ragService.ask(request.getQuestion(), request.getUserId(), request.getChannelIds());
    }

    public static class AskRequest {
        private String question;
        private Long userId;
        private List<Long> channelIds;

        public String getQuestion() {
            return question;
        }

        public void setQuestion(String question) {
            this.question = question;
        }

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public List<Long> getChannelIds() {
            return channelIds != null ? channelIds : new ArrayList<>();
        }

        public void setChannelIds(List<Long> channelIds) {
            this.channelIds = channelIds;
        }
    }
}
