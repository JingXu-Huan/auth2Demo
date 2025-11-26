package com.example.collab.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.collab.entity.DocFavorite;
import com.example.collab.mapper.DocFavoriteMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文档收藏服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocFavoriteService {
    
    private final DocFavoriteMapper docFavoriteMapper;
    
    /**
     * 收藏文档
     */
    public DocFavorite addFavorite(Long userId, Long docId, String folderName, String notes) {
        // 检查是否已收藏
        DocFavorite existing = docFavoriteMapper.selectByUserAndDoc(userId, docId);
        if (existing != null) {
            throw new IllegalStateException("已收藏该文档");
        }
        
        DocFavorite favorite = new DocFavorite()
                .setUserId(userId)
                .setDocId(docId)
                .setFolderName(folderName)
                .setNotes(notes)
                .setCreatedAt(LocalDateTime.now());
        
        docFavoriteMapper.insert(favorite);
        log.info("收藏文档: userId={}, docId={}", userId, docId);
        return favorite;
    }
    
    /**
     * 取消收藏
     */
    public void removeFavorite(Long userId, Long docId) {
        docFavoriteMapper.delete(new LambdaQueryWrapper<DocFavorite>()
                .eq(DocFavorite::getUserId, userId)
                .eq(DocFavorite::getDocId, docId));
        log.info("取消收藏: userId={}, docId={}", userId, docId);
    }
    
    /**
     * 获取用户的收藏列表
     */
    public List<DocFavorite> getUserFavorites(Long userId) {
        return docFavoriteMapper.selectByUserId(userId);
    }
    
    /**
     * 检查是否已收藏
     */
    public boolean isFavorite(Long userId, Long docId) {
        return docFavoriteMapper.selectByUserAndDoc(userId, docId) != null;
    }
    
    /**
     * 更新收藏备注
     */
    public void updateNotes(Long userId, Long docId, String notes) {
        DocFavorite favorite = docFavoriteMapper.selectByUserAndDoc(userId, docId);
        if (favorite == null) {
            throw new IllegalArgumentException("收藏不存在");
        }
        favorite.setNotes(notes);
        docFavoriteMapper.updateById(favorite);
    }
}
