package com.example.collab.controller;

import com.example.collab.entity.Document;
import com.example.collab.service.DocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 文档控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/docs")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    /**
     * 创建文档
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createDocument(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam String title,
            @RequestParam(defaultValue = "text") String docType) {
        
        Document document = documentService.createDocument(userId, title, docType);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("docId", document.getDocId());
        result.put("title", document.getTitle());
        
        return ResponseEntity.ok(result);
    }

    /**
     * 获取文档
     */
    @GetMapping("/{docId}")
    public ResponseEntity<Map<String, Object>> getDocument(@PathVariable String docId) {
        Document document = documentService.getDocument(docId);
        
        if (document == null) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "文档不存在");
            return ResponseEntity.notFound().build();
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("document", document);
        
        return ResponseEntity.ok(result);
    }

    /**
     * 获取用户文档列表
     */
    @GetMapping("/my")
    public ResponseEntity<Map<String, Object>> getMyDocuments(
            @RequestHeader("X-User-Id") Long userId) {
        
        List<Document> documents = documentService.getUserDocuments(userId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("documents", documents);
        result.put("total", documents.size());
        
        return ResponseEntity.ok(result);
    }

    /**
     * 删除文档
     */
    @DeleteMapping("/{docId}")
    public ResponseEntity<Map<String, Object>> deleteDocument(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable String docId) {
        try {
            documentService.deleteDocument(docId, userId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("删除文档失败", e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * 获取协同锁
     */
    @PostMapping("/{docId}/lock")
    public ResponseEntity<Map<String, Object>> acquireLock(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable String docId,
            @RequestParam(defaultValue = "300") int ttlSeconds) {
        
        boolean acquired = documentService.acquireLock(docId, userId, ttlSeconds);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", acquired);
        
        return ResponseEntity.ok(result);
    }

    /**
     * 释放协同锁
     */
    @DeleteMapping("/{docId}/lock")
    public ResponseEntity<Map<String, Object>> releaseLock(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable String docId) {
        
        documentService.releaseLock(docId, userId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        
        return ResponseEntity.ok(result);
    }
}
