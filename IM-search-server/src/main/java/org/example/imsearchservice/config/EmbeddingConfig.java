package org.example.imsearchservice.config;

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 向量模型配置：基于 LangChain4j 的 OpenAI Embedding 模型。
 *
 * 可通过配置项控制：
 * - ai.embedding.api-key  : OpenAI API Key
 * - ai.embedding.model    : 模型名称，例如 text-embedding-3-small
 */
@Configuration
public class EmbeddingConfig {

    @Value("${ai.embedding.api-key:}")
    private String apiKey;

    @Value("${ai.embedding.model:text-embedding-3-small}")
    private String modelName;

    @Bean
    public EmbeddingModel embeddingModel() {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalStateException("ai.embedding.api-key must be configured for EmbeddingModel");
        }
        return OpenAiEmbeddingModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .build();
    }
}
