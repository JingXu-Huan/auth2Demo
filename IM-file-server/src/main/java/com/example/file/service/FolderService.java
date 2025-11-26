package com.example.file.service;

import com.example.file.entity.Folder;
import com.example.file.mapper.FolderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文件夹管理服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FolderService {
    
    private final FolderMapper folderMapper;
    
    /**
     * 创建文件夹
     */
    @Transactional
    public Folder createFolder(Long userId, String name, Long parentId) {
        Folder folder = new Folder()
                .setUserId(userId)
                .setName(name)
                .setParentId(parentId)
                .setFileCount(0)
                .setFolderCount(0)
                .setTotalSize(0L)
                .setCreatedAt(LocalDateTime.now())
                .setUpdatedAt(LocalDateTime.now());
        
        // 计算路径和层级
        if (parentId != null) {
            Folder parent = folderMapper.selectById(parentId);
            if (parent == null) {
                throw new IllegalArgumentException("父文件夹不存在");
            }
            folder.setPath(parent.getPath() + "/" + name);
            folder.setLevel(parent.getLevel() + 1);
            
            // 更新父文件夹的子文件夹数
            parent.setFolderCount(parent.getFolderCount() + 1);
            parent.setUpdatedAt(LocalDateTime.now());
            folderMapper.updateById(parent);
        } else {
            folder.setPath("/" + name);
            folder.setLevel(1);
        }
        
        folderMapper.insert(folder);
        log.info("创建文件夹成功: userId={}, path={}", userId, folder.getPath());
        return folder;
    }
    
    /**
     * 重命名文件夹
     */
    @Transactional
    public Folder renameFolder(Long folderId, String newName) {
        Folder folder = folderMapper.selectById(folderId);
        if (folder == null) {
            throw new IllegalArgumentException("文件夹不存在");
        }
        
        String oldPath = folder.getPath();
        String newPath = oldPath.substring(0, oldPath.lastIndexOf("/") + 1) + newName;
        
        folder.setName(newName);
        folder.setPath(newPath);
        folder.setUpdatedAt(LocalDateTime.now());
        folderMapper.updateById(folder);
        
        // 更新子文件夹路径
        updateChildrenPath(folder.getUserId(), oldPath, newPath);
        
        log.info("重命名文件夹成功: folderId={}, newName={}", folderId, newName);
        return folder;
    }
    
    /**
     * 删除文件夹（软删除）
     */
    @Transactional
    public void deleteFolder(Long folderId) {
        Folder folder = folderMapper.selectById(folderId);
        if (folder == null) {
            throw new IllegalArgumentException("文件夹不存在");
        }
        
        // 检查是否有内容
        if (folder.getFileCount() > 0 || folder.getFolderCount() > 0) {
            throw new IllegalStateException("文件夹非空，无法删除");
        }
        
        folder.setDeletedAt(LocalDateTime.now());
        folderMapper.updateById(folder);
        
        // 更新父文件夹的子文件夹数
        if (folder.getParentId() != null) {
            Folder parent = folderMapper.selectById(folder.getParentId());
            if (parent != null) {
                parent.setFolderCount(parent.getFolderCount() - 1);
                parent.setUpdatedAt(LocalDateTime.now());
                folderMapper.updateById(parent);
            }
        }
        
        log.info("删除文件夹成功: folderId={}", folderId);
    }
    
    /**
     * 获取文件夹详情
     */
    public Folder getFolderById(Long folderId) {
        return folderMapper.selectById(folderId);
    }
    
    /**
     * 获取用户根文件夹
     */
    public List<Folder> getRootFolders(Long userId) {
        return folderMapper.selectRootFolders(userId);
    }
    
    /**
     * 获取子文件夹
     */
    public List<Folder> getSubFolders(Long parentId) {
        return folderMapper.selectByParentId(parentId);
    }
    
    /**
     * 更新子文件夹路径
     */
    private void updateChildrenPath(Long userId, String oldPath, String newPath) {
        List<Folder> children = folderMapper.selectByPathPrefix(userId, oldPath + "/");
        for (Folder child : children) {
            child.setPath(child.getPath().replace(oldPath, newPath));
            child.setUpdatedAt(LocalDateTime.now());
            folderMapper.updateById(child);
        }
    }
}
