package org.example.imsearchservice.service;

import org.example.imsearchservice.domain.DocumentChunkDoc;
import org.example.imsearchservice.domain.ImMessageDoc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * RAG (检索增强生成) 服务骨架。
 *
 * 当前实现：检索相关文档片段并返回上下文，不调用 LLM。
 * 后续可接入 LangChain4j 的 ChatLanguageModel 实现真正的问答。
 */
@Service
public class RagService {

    private static final Logger log = LoggerFactory.getLogger(RagService.class);

    private final SearchService searchService;
    private final DocumentSearchService documentSearchService;

    public RagService(SearchService searchService, DocumentSearchService documentSearchService) {
        this.searchService = searchService;
        this.documentSearchService = documentSearchService;
    }

    public Map<String, Object> ask(String question, Long userId, List<Long> channelIds) {
        log.info("[rag] 收到问题: question={}, userId={}", question, userId);

        // Step 1: Retrieve - 从 IM 消息和文档中检索相关内容
        List<ImMessageDoc> messages = searchService.searchMessages(question, channelIds);
        List<DocumentChunkDoc> documents = documentSearchService.searchDocuments(question, userId);

        // Step 2: 组装上下文
        List<String> contexts = new ArrayList<>();
        for (ImMessageDoc msg : messages) {
            contexts.add("[IM消息] " + msg.getContent());
        }
        for (DocumentChunkDoc doc : documents) {
            contexts.add("[文档:" + doc.getTitle() + "] " + doc.getContentChunk());
        }

        // Step 3: 当前只返回检索结果，不调用 LLM（演示版本）
        Map<String, Object> result = new HashMap<>();
        result.put("question", question);
        result.put("contexts", contexts);
        result.put("answer", "[演示版本] 请基于以上 " + contexts.size() + " 条上下文信息自行判断答案。后续可接入 LLM 自动生成。");

        // TODO: 接入 LangChain4j ChatLanguageModel，构造 Prompt 并调用 LLM
        // String prompt = buildPrompt(question, contexts);
        // String answer = chatModel.generate(prompt);
        // result.put("answer", answer);

        return result;
    }
}
