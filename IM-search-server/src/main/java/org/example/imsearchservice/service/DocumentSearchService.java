package org.example.imsearchservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.example.imsearchservice.domain.DocumentChunkDoc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 文档搜索服务：支持权限过滤的文档索引查询。
 *
 * 实现 Read-Time Filtering：在查询时动态注入用户的权限条件。
 */
@Service
public class DocumentSearchService {

    private static final Logger log = LoggerFactory.getLogger(DocumentSearchService.class);

    private final RestClient restClient;
    private final PermissionService permissionService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public DocumentSearchService(RestClient restClient, PermissionService permissionService) {
        this.restClient = restClient;
        this.permissionService = permissionService;
    }

    public List<DocumentChunkDoc> searchDocuments(String query, Long userId) {
        if (query == null || query.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> userChannelIds = permissionService.getUserChannelIds(userId);
        List<Long> userDeptIds = permissionService.getUserDeptIds(userId);

        String safeQuery = query.replace("\"", "\\\"");

        // 构建权限过滤条件（Security Trimming）
        StringBuilder filterBuilder = new StringBuilder();
        filterBuilder.append("{\"bool\":{\"should\":[");
        filterBuilder.append("{\"term\":{\"isPublic\":true}}");
        if (userId != null) {
            filterBuilder.append(",{\"terms\":{\"allowUsers\":[").append(userId).append("]}}");
        }
        if (userDeptIds != null && !userDeptIds.isEmpty()) {
            String depts = userDeptIds.stream().map(String::valueOf).collect(Collectors.joining(","));
            filterBuilder.append(",{\"terms\":{\"allowDepts\":[").append(depts).append("]}}");
        }
        filterBuilder.append("]}}");

        String body = "{" +
                "\"size\":20," +
                "\"query\":{\"bool\":{" +
                "\"must\":[{\"match\":{\"contentChunk\":\"" + safeQuery + "\"}}]," +
                "\"filter\":[" + filterBuilder.toString() + "]" +
                "}}}";

        try {
            Request request = new Request("GET", "/idx_documents/_search");
            request.setJsonEntity(body);
            Response response = restClient.performRequest(request);
            String responseBody = EntityUtils.toString(response.getEntity());

            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode hits = root.path("hits").path("hits");
            List<DocumentChunkDoc> result = new ArrayList<>();
            if (hits.isArray()) {
                for (JsonNode hit : hits) {
                    JsonNode source = hit.path("_source");
                    if (!source.isMissingNode()) {
                        DocumentChunkDoc doc = objectMapper.treeToValue(source, DocumentChunkDoc.class);
                        result.add(doc);
                    }
                }
            }
            return result;
        } catch (IOException e) {
            log.error("[document-search] 执行 Elasticsearch 查询失败", e);
            return Collections.emptyList();
        }
    }
}
