package com.example.file.controller;

import com.example.domain.vo.Result;
import com.example.file.entity.Folder;
import com.example.file.service.FolderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 文件夹管理接口
 */
@RestController
@RequestMapping("/api/file/folders")
@RequiredArgsConstructor
public class FolderController {
    
    private final FolderService folderService;
    
    /**
     * 创建文件夹
     */
    @PostMapping
    public Result<Folder> createFolder(
            @RequestParam String name,
            @RequestParam(required = false) Long parentId,
            @RequestHeader("X-User-Id") Long userId) {
        Folder folder = folderService.createFolder(userId, name, parentId);
        return Result.success(folder);
    }
    
    /**
     * 重命名文件夹
     */
    @PutMapping("/{folderId}/rename")
    public Result<Folder> renameFolder(
            @PathVariable Long folderId,
            @RequestParam String newName) {
        Folder folder = folderService.renameFolder(folderId, newName);
        return Result.success(folder);
    }
    
    /**
     * 删除文件夹
     */
    @DeleteMapping("/{folderId}")
    public Result<Void> deleteFolder(@PathVariable Long folderId) {
        folderService.deleteFolder(folderId);
        return Result.success(null);
    }
    
    /**
     * 获取文件夹详情
     */
    @GetMapping("/{folderId}")
    public Result<Folder> getFolder(@PathVariable Long folderId) {
        Folder folder = folderService.getFolderById(folderId);
        return Result.success(folder);
    }
    
    /**
     * 获取根文件夹列表
     */
    @GetMapping("/root")
    public Result<List<Folder>> getRootFolders(@RequestHeader("X-User-Id") Long userId) {
        List<Folder> folders = folderService.getRootFolders(userId);
        return Result.success(folders);
    }
    
    /**
     * 获取子文件夹列表
     */
    @GetMapping("/{folderId}/children")
    public Result<List<Folder>> getSubFolders(@PathVariable Long folderId) {
        List<Folder> folders = folderService.getSubFolders(folderId);
        return Result.success(folders);
    }
}
