package org.example.imsearchservice.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Elasticsearch Java Client 基础配置。
 *
 * 通过 spring 配置项 elasticsearch.host 指定 ES 地址，默认 http://localhost:9200。
 */
@Configuration
public class ElasticsearchConfig {

    @Value("${elasticsearch.host:http://localhost:9200}")
    private String elasticsearchHost;

    @Bean
    public RestClient restClient() {
        return RestClient.builder(HttpHost.create(elasticsearchHost)).build();
    }

    @Bean
    public ElasticsearchClient elasticsearchClient(RestClient restClient) {
        ElasticsearchTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
        return new ElasticsearchClient(transport);
    }
}
