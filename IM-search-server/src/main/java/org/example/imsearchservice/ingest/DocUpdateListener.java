package org.example.imsearchservice.ingest;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.apache.tika.Tika;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.RestClient;
import org.example.imsearchservice.domain.DocumentChunkDoc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Component
@RocketMQMessageListener(topic = "DOC_UPDATE", consumerGroup = "SEARCH_DOC_INGEST_GROUP")
public class DocUpdateListener implements RocketMQListener<String> {

    private static final Logger log = LoggerFactory.getLogger(DocUpdateListener.class);

    private final RestClient restClient;
    private final EmbeddingModel embeddingModel;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Tika tika = new Tika();

    public DocUpdateListener(RestClient restClient, EmbeddingModel embeddingModel) {
        this.restClient = restClient;
        this.embeddingModel = embeddingModel;
    }

    @Override
    public void onMessage(String message) {
        log.info("[search-doc-ingest] 收到 DOC_UPDATE 消息: {}", message);

        DocUpdateEvent event;
        try {
            event = objectMapper.readValue(message, DocUpdateEvent.class);
        } catch (Exception e) {
            log.error("[search-doc-ingest] 解析 DocUpdateEvent 失败", e);
            return;
        }

        String text = loadContent(event);
        if (text == null || text.trim().isEmpty()) {
            log.debug("[search-doc-ingest] 文档内容为空, docId={}", event.getDocId());
            return;
        }

        List<String> chunks = splitText(text, 1000, 200);
        int index = 0;
        for (String chunk : chunks) {
            try {
                Response<Embedding> embeddingResponse = embeddingModel.embed(chunk);
                List<Float> vector = embeddingResponse.content().vectorAsList();

                DocumentChunkDoc doc = new DocumentChunkDoc();
                doc.setChunkId(event.getDocId() + "-" + index);
                doc.setParentDocId(event.getDocId());
                doc.setTitle(event.getTitle());
                doc.setContentChunk(chunk);
                doc.setVector(vector);
                doc.setAllowUsers(event.getAllowUsers());
                doc.setAllowDepts(event.getAllowDepts());
                doc.setPublic(event.getPublic());
                doc.setCreatedAt(Instant.now());

                String json = objectMapper.writeValueAsString(doc);
                Request request = new Request("PUT", "/idx_documents/_doc/" + doc.getChunkId());
                request.setJsonEntity(json);
                org.elasticsearch.client.Response esResponse = restClient.performRequest(request);
                log.debug("[search-doc-ingest] ES index status: {}", esResponse.getStatusLine());
            } catch (Exception e) {
                log.error("[search-doc-ingest] 写入文档索引失败, docId=" + event.getDocId(), e);
            }
            index++;
        }
    }

    private String loadContent(DocUpdateEvent event) {
        // 1) 如果上游已经提供了纯文本内容，直接使用
        if (event.getContentText() != null && !event.getContentText().trim().isEmpty()) {
            return event.getContentText();
        }

        // 2) 否则如果有文件地址，则下载文件并交给 Tika 提取文本
        if (event.getFileUrl() == null || event.getFileUrl().isEmpty()) {
            return null;
        }
        try {
            URI uri = URI.create(event.getFileUrl());
            try (InputStream in = uri.toURL().openStream()) {
                return tika.parseToString(in);
            }
        } catch (Exception e) {
            log.error("[search-doc-ingest] 从 fileUrl 加载并解析文档失败", e);
        }
        return null;
    }

    private List<String> splitText(String text, int maxLen, int overlap) {
        List<String> result = new ArrayList<>();
        if (text == null || text.isEmpty() || maxLen <= 0) {
            return result;
        }
        int len = text.length();
        int start = 0;
        while (start < len) {
            int end = Math.min(len, start + maxLen);
            result.add(text.substring(start, end));
            if (end == len) {
                break;
            }
            start = Math.max(end - overlap, start + 1);
        }
        return result;
    }
}
