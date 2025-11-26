package com.example.file.controller;

import com.example.file.entity.FileUploadSession;
import com.example.file.entity.UserFile;
import com.example.file.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

/**
 * 文件控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileController {

    private final FileStorageService fileStorageService;

    /**
     * 上传文件
     */
    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadFile(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "sourceType", defaultValue = "upload") String sourceType,
            @RequestParam(value = "sourceId", required = false) String sourceId) {
        try {
            UserFile userFile = fileStorageService.uploadFile(userId, file, sourceType, sourceId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("fileId", userFile.getId());
            result.put("fileHash", userFile.getFileHash());
            result.put("fileName", userFile.getOriginalName());
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("文件上传失败", e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * 检查文件是否存在（秒传）
     */
    @GetMapping("/check/{fileHash}")
    public ResponseEntity<Map<String, Object>> checkFile(@PathVariable String fileHash) {
        boolean exists = fileStorageService.checkFileExists(fileHash);
        
        Map<String, Object> result = new HashMap<>();
        result.put("exists", exists);
        result.put("fastUpload", exists);
        
        return ResponseEntity.ok(result);
    }

    /**
     * 获取文件下载链接
     */
    @GetMapping("/download/{fileHash}")
    public ResponseEntity<Map<String, Object>> getDownloadUrl(
            @PathVariable String fileHash,
            @RequestParam(value = "expireMinutes", defaultValue = "30") int expireMinutes) {
        try {
            String url = fileStorageService.getDownloadUrl(fileHash, expireMinutes);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("url", url);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("获取下载链接失败", e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * 初始化分片上传
     */
    @PostMapping("/multipart/init")
    public ResponseEntity<Map<String, Object>> initMultipartUpload(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam String fileName,
            @RequestParam Long fileSize,
            @RequestParam(defaultValue = "application/octet-stream") String mimeType,
            @RequestParam(defaultValue = "5242880") int chunkSize) {
        
        FileUploadSession session = fileStorageService.initMultipartUpload(
                userId, fileName, fileSize, mimeType, chunkSize);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("uploadId", session.getUploadId());
        result.put("chunkCount", session.getChunkCount());
        result.put("chunkSize", session.getChunkSize());
        
        return ResponseEntity.ok(result);
    }

    /**
     * 删除文件
     */
    @DeleteMapping("/{fileId}")
    public ResponseEntity<Map<String, Object>> deleteFile(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long fileId) {
        try {
            fileStorageService.deleteUserFile(userId, fileId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("删除文件失败", e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }
}
