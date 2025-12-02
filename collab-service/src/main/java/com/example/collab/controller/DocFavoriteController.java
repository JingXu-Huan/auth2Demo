package com.example.collab.controller;

import com.example.collab.entity.DocFavorite;
import com.example.collab.service.DocFavoriteService;
import com.example.domain.vo.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 文档收藏接口
 */
@RestController
@RequestMapping("/api/doc/favorites")
@RequiredArgsConstructor
public class DocFavoriteController {
    
    private final DocFavoriteService docFavoriteService;
    
    /**
     * 收藏文档
     */
    @PostMapping
    public Result<DocFavorite> addFavorite(
            @RequestParam Long docId,
            @RequestParam(required = false) String folderName,
            @RequestParam(required = false) String notes,
            @RequestHeader("X-User-Id") Long userId) {
        DocFavorite favorite = docFavoriteService.addFavorite(userId, docId, folderName, notes);
        return Result.success(favorite);
    }
    
    /**
     * 取消收藏
     */
    @DeleteMapping("/{docId}")
    public Result<Void> removeFavorite(
            @PathVariable Long docId,
            @RequestHeader("X-User-Id") Long userId) {
        docFavoriteService.removeFavorite(userId, docId);
        return Result.success(null);
    }
    
    /**
     * 获取收藏列表
     */
    @GetMapping
    public Result<List<DocFavorite>> getFavorites(@RequestHeader("X-User-Id") Long userId) {
        List<DocFavorite> favorites = docFavoriteService.getUserFavorites(userId);
        return Result.success(favorites);
    }
    
    /**
     * 检查是否已收藏
     */
    @GetMapping("/check/{docId}")
    public Result<Boolean> isFavorite(
            @PathVariable Long docId,
            @RequestHeader("X-User-Id") Long userId) {
        boolean isFavorite = docFavoriteService.isFavorite(userId, docId);
        return Result.success(isFavorite);
    }
    
    /**
     * 更新收藏备注
     */
    @PutMapping("/{docId}/notes")
    public Result<Void> updateNotes(
            @PathVariable Long docId,
            @RequestParam String notes,
            @RequestHeader("X-User-Id") Long userId) {
        docFavoriteService.updateNotes(userId, docId, notes);
        return Result.success(null);
    }
}
