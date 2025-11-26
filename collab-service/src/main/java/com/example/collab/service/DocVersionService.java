package com.example.collab.service;

import com.example.collab.entity.DocumentVersion;
import com.example.collab.mapper.DocumentVersionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文档版本服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocVersionService {
    
    private final DocumentVersionMapper documentVersionMapper;
    
    /**
     * 创建新版本
     */
    @Transactional
    public DocumentVersion createVersion(Long docId, Long authorId, String title, 
                                          byte[] content, String changeSummary, boolean isMajor) {
        // 获取当前最大版本号
        Long maxVersion = documentVersionMapper.selectMaxVersionNumber(docId);
        long newVersion = (maxVersion == null ? 0 : maxVersion) + 1;
        
        DocumentVersion version = new DocumentVersion()
                .setDocId(docId)
                .setVersionNumber(newVersion)
                .setTitle(title)
                .setContent(content)
                .setAuthorId(authorId)
                .setChangeSummary(changeSummary)
                .setIsMajor(isMajor)
                .setIsPublished(false)
                .setCreatedAt(LocalDateTime.now());
        
        documentVersionMapper.insert(version);
        log.info("创建文档版本: docId={}, version={}", docId, newVersion);
        return version;
    }
    
    /**
     * 获取文档的所有版本
     */
    public List<DocumentVersion> getVersions(Long docId) {
        return documentVersionMapper.selectByDocId(docId);
    }
    
    /**
     * 获取最新版本
     */
    public DocumentVersion getLatestVersion(Long docId) {
        return documentVersionMapper.selectLatestByDocId(docId);
    }
    
    /**
     * 获取指定版本
     */
    public DocumentVersion getVersion(Long versionId) {
        return documentVersionMapper.selectById(versionId);
    }
    
    /**
     * 发布版本
     */
    public void publishVersion(Long versionId) {
        DocumentVersion version = documentVersionMapper.selectById(versionId);
        if (version == null) {
            throw new IllegalArgumentException("版本不存在");
        }
        version.setIsPublished(true);
        documentVersionMapper.updateById(version);
        log.info("发布版本: versionId={}", versionId);
    }
    
    /**
     * 回滚到指定版本
     */
    @Transactional
    public DocumentVersion rollbackToVersion(Long docId, Long versionId, Long userId) {
        DocumentVersion targetVersion = documentVersionMapper.selectById(versionId);
        if (targetVersion == null || !targetVersion.getDocId().equals(docId)) {
            throw new IllegalArgumentException("目标版本不存在");
        }
        
        // 创建新版本作为回滚记录
        return createVersion(docId, userId, targetVersion.getTitle(), 
                targetVersion.getContent(), "回滚到版本 " + targetVersion.getVersionNumber(), true);
    }
}
