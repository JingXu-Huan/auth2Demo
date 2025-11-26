package org.example.imsearchservice.ingest;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.RestClient;
import org.example.imsearchservice.domain.ImMessageDoc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/**
 * IM 消息写入 Elasticsearch 的摄入监听器。
 *
 * 监听 RocketMQ 主题 IM_MSG_SEND，将消息内容向量化并写入 idx_im_messages 索引。
 */
@Component
@RocketMQMessageListener(topic = "IM_MSG_SEND", consumerGroup = "SEARCH_INGEST_GROUP")
public class IngestListener implements RocketMQListener<String> {

    private static final Logger log = LoggerFactory.getLogger(IngestListener.class);

    private final RestClient restClient;
    private final EmbeddingModel embeddingModel;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public IngestListener(RestClient restClient, EmbeddingModel embeddingModel) {
        this.restClient = restClient;
        this.embeddingModel = embeddingModel;
    }

    @Override
    public void onMessage(String message) {
        log.info("[search-ingest] 收到 IM_MSG_SEND 消息: {}", message);

        MsgEvent event;
        try {
            event = objectMapper.readValue(message, MsgEvent.class);
        } catch (Exception e) {
            log.error("[search-ingest] 解析 MsgEvent 失败", e);
            return;
        }

        String text = event.getContent();
        if (text == null || text.trim().isEmpty()) {
            log.debug("[search-ingest] 空内容消息，跳过 msgId={}", event.getMsgId());
            return;
        }

        try {
            Response<Embedding> embeddingResponse = embeddingModel.embed(text);
            List<Float> vector = embeddingResponse.content().vectorAsList();

            ImMessageDoc doc = new ImMessageDoc();
            doc.setMsgId(event.getMsgId());
            doc.setChannelId(event.getChannelId());
            doc.setSenderId(event.getSenderId());
            doc.setContent(text);
            doc.setVector(vector);
            Instant ts = event.getTimestamp() != null ? event.getTimestamp() : Instant.now();
            doc.setCreatedAt(ts);

            String json = objectMapper.writeValueAsString(doc);

            Request request = new Request("PUT", "/idx_im_messages/_doc/" + doc.getMsgId());
            request.setJsonEntity(json);
            org.elasticsearch.client.Response esResponse = restClient.performRequest(request);
            log.debug("[search-ingest] ES index status: {}", esResponse.getStatusLine());
        } catch (Exception e) {
            log.error("[search-ingest] 写入 Elasticsearch 失败", e);
            throw new RuntimeException("Elasticsearch indexing failed", e);
        }
    }
}
