package com.example.file.service;

import com.example.file.entity.FileShare;
import com.example.file.mapper.FileShareMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文件分享服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileShareService {
    
    private final FileShareMapper fileShareMapper;
    
    /**
     * 创建分享
     */
    @Transactional
    public FileShare createShare(Long fileId, Long userId, Integer shareType, 
                                  List<Long> targets, LocalDateTime expiresAt) {
        FileShare share = new FileShare()
                .setFileId(fileId)
                .setSharedBy(userId)
                .setShareType(shareType)
                .setShareTargets(targets)
                .setAllowDownload(true)
                .setAllowPreview(true)
                .setAllowSave(false)
                .setViewCount(0)
                .setDownloadCount(0)
                .setSaveCount(0)
                .setExpiresAt(expiresAt)
                .setCreatedAt(LocalDateTime.now());
        
        fileShareMapper.insert(share);
        log.info("创建文件分享成功: fileId={}, shareType={}", fileId, shareType);
        return share;
    }
    
    /**
     * 取消分享
     */
    @Transactional
    public void cancelShare(Long shareId) {
        fileShareMapper.deleteById(shareId);
        log.info("取消文件分享成功: shareId={}", shareId);
    }
    
    /**
     * 获取分享详情
     */
    public FileShare getShareById(Long shareId) {
        return fileShareMapper.selectById(shareId);
    }
    
    /**
     * 获取文件的所有分享
     */
    public List<FileShare> getFileShares(Long fileId) {
        return fileShareMapper.selectByFileId(fileId);
    }
    
    /**
     * 获取用户创建的分享
     */
    public List<FileShare> getUserShares(Long userId) {
        return fileShareMapper.selectBySharedBy(userId);
    }
    
    /**
     * 记录查看
     */
    @Transactional
    public void recordView(Long shareId) {
        fileShareMapper.incrementViewCount(shareId);
    }
    
    /**
     * 记录下载
     */
    @Transactional
    public void recordDownload(Long shareId) {
        fileShareMapper.incrementDownloadCount(shareId);
    }
    
    /**
     * 检查分享是否有效
     */
    public boolean isShareValid(FileShare share) {
        if (share == null) {
            return false;
        }
        if (share.getExpiresAt() != null && share.getExpiresAt().isBefore(LocalDateTime.now())) {
            return false;
        }
        return true;
    }
}
