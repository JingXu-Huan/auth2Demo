package org.example.imsearchservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.example.imsearchservice.domain.ImMessageDoc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 基础搜索服务：对 idx_im_messages 执行关键词 + channel 过滤查询。
 *
 * 为了避免依赖 Elasticsearch Java DSL，这里直接构造 JSON 查询请求。
 */
@Service
public class SearchService {

    private static final Logger log = LoggerFactory.getLogger(SearchService.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SearchService(RestClient restClient) {
        this.restClient = restClient;
    }

    public List<ImMessageDoc> searchMessages(String query, List<Long> channelIds) {
        if (query == null || query.isEmpty()) {
            return Collections.emptyList();
        }

        String safeQuery = query.replace("\"", "\\\"");

        String body;
        if (channelIds != null && !channelIds.isEmpty()) {
            String ids = channelIds.stream().map(String::valueOf).collect(Collectors.joining(","));
            body = "{" +
                    "\"size\":20," +
                    "\"query\":{\"bool\":{" +
                    "\"must\":[{\"match\":{\"content\":\"" + safeQuery + "\"}}]," +
                    "\"filter\":[{\"terms\":{\"channelId\":[" + ids + "]}}]" +
                    "}}}";
        } else {
            body = "{" +
                    "\"size\":20," +
                    "\"query\":{\"match\":{\"content\":\"" + safeQuery + "\"}}" +
                    "}";
        }

        try {
            Request request = new Request("GET", "/idx_im_messages/_search");
            request.setJsonEntity(body);
            Response response = restClient.performRequest(request);
            String responseBody = EntityUtils.toString(response.getEntity());

            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode hits = root.path("hits").path("hits");
            List<ImMessageDoc> result = new ArrayList<>();
            if (hits.isArray()) {
                for (JsonNode hit : hits) {
                    JsonNode source = hit.path("_source");
                    if (!source.isMissingNode()) {
                        ImMessageDoc doc = objectMapper.treeToValue(source, ImMessageDoc.class);
                        result.add(doc);
                    }
                }
            }
            return result;
        } catch (IOException e) {
            log.error("[search] 执行 Elasticsearch 查询失败", e);
            return Collections.emptyList();
        }
    }
}
