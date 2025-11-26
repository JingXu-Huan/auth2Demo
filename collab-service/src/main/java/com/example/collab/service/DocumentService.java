package com.example.collab.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.collab.entity.Document;
import com.example.collab.mapper.DocumentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 文档服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentMapper documentMapper;

    /**
     * 创建文档
     */
    @Transactional
    public Document createDocument(Long ownerId, String title, String docType) {
        Document document = new Document();
        document.setDocId(UUID.randomUUID().toString());
        document.setTitle(title);
        document.setDocType(docType);
        document.setOwnerId(ownerId);
        document.setContentVersion(1L);
        document.setContentSize(0L);
        document.setStorageType("database");
        document.setVisibility("private");
        document.setAllowComment(true);
        document.setAllowCopy(true);
        document.setAllowDownload(true);
        document.setStatus(1);
        document.setPublished(false);
        document.setViewCount(0);
        document.setEditCount(0);
        document.setCommentCount(0);
        document.setStarCount(0);
        document.setCreatedAt(OffsetDateTime.now());
        document.setUpdatedAt(OffsetDateTime.now());

        documentMapper.insert(document);
        log.info("文档创建成功: docId={}, title={}", document.getDocId(), title);

        return document;
    }

    /**
     * 获取文档
     */
    public Document getDocument(String docId) {
        return documentMapper.selectOne(
                new LambdaQueryWrapper<Document>()
                        .eq(Document::getDocId, docId)
                        .ne(Document::getStatus, 4) // 排除已删除
        );
    }

    /**
     * 获取用户的文档列表
     */
    public List<Document> getUserDocuments(Long userId) {
        return documentMapper.selectList(
                new LambdaQueryWrapper<Document>()
                        .eq(Document::getOwnerId, userId)
                        .eq(Document::getStatus, 1)
                        .orderByDesc(Document::getUpdatedAt)
        );
    }

    /**
     * 更新文档内容
     */
    @Transactional
    public void updateContent(String docId, byte[] content, byte[] yjsState) {
        Document document = getDocument(docId);
        if (document == null) {
            throw new RuntimeException("文档不存在");
        }

        document.setContent(content);
        document.setYjsState(yjsState);
        document.setContentSize((long) content.length);
        document.setContentVersion(document.getContentVersion() + 1);
        document.setEditCount(document.getEditCount() + 1);
        document.setUpdatedAt(OffsetDateTime.now());

        documentMapper.updateById(document);
        log.debug("文档内容更新: docId={}, version={}", docId, document.getContentVersion());
    }

    /**
     * 删除文档（软删除）
     */
    @Transactional
    public void deleteDocument(String docId, Long userId) {
        Document document = getDocument(docId);
        if (document == null) {
            throw new RuntimeException("文档不存在");
        }
        if (!document.getOwnerId().equals(userId)) {
            throw new RuntimeException("无权限删除");
        }

        document.setStatus(4); // 已删除
        document.setDeletedAt(OffsetDateTime.now());
        documentMapper.updateById(document);

        log.info("文档删除成功: docId={}", docId);
    }

    /**
     * 获取或创建协同锁
     */
    @Transactional
    public boolean acquireLock(String docId, Long userId, int ttlSeconds) {
        Document document = getDocument(docId);
        if (document == null) {
            return false;
        }

        OffsetDateTime now = OffsetDateTime.now();

        // 检查锁是否已被占用
        if (document.getLockUserId() != null && 
            document.getLockExpiresAt() != null &&
            document.getLockExpiresAt().isAfter(now) &&
            !document.getLockUserId().equals(userId)) {
            return false; // 锁被其他用户占用
        }

        // 获取锁
        document.setLockUserId(userId);
        document.setLockToken(UUID.randomUUID().toString());
        document.setLockExpiresAt(now.plusSeconds(ttlSeconds));
        documentMapper.updateById(document);

        return true;
    }

    /**
     * 释放协同锁
     */
    @Transactional
    public void releaseLock(String docId, Long userId) {
        Document document = getDocument(docId);
        if (document != null && userId.equals(document.getLockUserId())) {
            document.setLockUserId(null);
            document.setLockToken(null);
            document.setLockExpiresAt(null);
            documentMapper.updateById(document);
        }
    }
}
