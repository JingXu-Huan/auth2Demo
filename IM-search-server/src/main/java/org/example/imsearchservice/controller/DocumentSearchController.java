package org.example.imsearchservice.controller;

import org.example.imsearchservice.domain.DocumentChunkDoc;
import org.example.imsearchservice.service.DocumentSearchService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 文档搜索控制器：提供带权限过滤的文档检索接口。
 */
@RestController
@RequestMapping("/api/search")
public class DocumentSearchController {

    private final DocumentSearchService documentSearchService;

    public DocumentSearchController(DocumentSearchService documentSearchService) {
        this.documentSearchService = documentSearchService;
    }

    @GetMapping("/documents")
    public List<DocumentChunkDoc> searchDocuments(@RequestParam("q") String query,
                                                   @RequestParam(value = "userId", required = false) Long userId) {
        return documentSearchService.searchDocuments(query, userId);
    }
}
