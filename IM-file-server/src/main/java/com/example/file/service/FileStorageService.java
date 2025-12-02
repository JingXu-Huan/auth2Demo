package com.example.file.service;

import com.example.file.entity.FileMetadata;
import com.example.file.entity.FileUploadSession;
import com.example.file.entity.UserFile;
import com.example.file.mapper.FileMetadataMapper;
import com.example.file.mapper.FileUploadSessionMapper;
import com.example.file.mapper.UserFileMapper;
import io.minio.*;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * ====================================================================
 * 文件存储服务 (File Storage Service with CAS Deduplication)
 * ====================================================================
 * 
 * 【核心功能】
 * 实现企业级文件存储功能：
 * - 文件上传/下载
 * - CAS去重（秒传）
 * - 分片上传（大文件）
 * - 引用计数管理
 * 
 * 【CAS（内容寻址存储）原理】
 * ┌─────────────────────────────────────────────────────────┐
 * │  CAS = Content-Addressable Storage                      │
 * │                                                         │
 * │  传统存储：文件名 → 文件内容                             │
 * │  CAS存储：SHA-256(内容) → 文件内容                       │
 * │                                                         │
 * │  优点：                                                  │
 * │  1. 自动去重：相同内容只存一份                           │
 * │  2. 秒传：已存在的文件无需重传                           │
 * │  3. 完整性校验：哈希值可验证文件是否损坏                 │
 * └─────────────────────────────────────────────────────────┘
 * 
 * 【文件上传流程】
 * 1. 客户端计算文件SHA-256哈希
 * 2. 查询数据库是否已存在该哈希
 * 3. 存在 → 秒传成功（仅增加引用计数）
 * 4. 不存在 → 上传到MinIO → 保存元数据
 * 
 * 【引用计数机制】
 * - 每个文件有 ref_count 字段
 * - 用户上传相同文件：ref_count++
 * - 用户删除文件：ref_count--
 * - ref_count = 0 时，才真正删除物理文件
 * 
 * 【数据模型】
 * FileMetadata（物理文件，用哈希做主键）
 *      ↑
 *      │ N:1（多个用户可以引用同一个物理文件）
 *      │
 * UserFile（用户的文件引用，包含文件名、路径等）
 * 
 * @author 学习笔记
 * @see MinioClient MinIO对象存储客户端
 * @see FileMetadata 物理文件元数据
 * @see UserFile 用户文件引用
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageService {

    /** MinIO客户端 - 对象存储操作 */
    private final MinioClient minioClient;
    
    /** 文件元数据访问 - 物理文件信息（CAS表） */
    private final FileMetadataMapper fileMetadataMapper;
    
    /** 用户文件访问 - 用户与文件的关联关系 */
    private final UserFileMapper userFileMapper;
    
    /** 上传会话访问 - 分片上传状态管理 */
    private final FileUploadSessionMapper uploadSessionMapper;

    /** MinIO存储桶名称 */
    @Value("${minio.bucket-name:files}")
    private String bucketName;

    /**
     * 上传文件（支持秒传）
     */
    @Transactional
    public UserFile uploadFile(Long userId, MultipartFile file, String sourceType, String sourceId) throws Exception {
        // 1. 计算文件哈希
        String fileHash = calculateSHA256(file.getInputStream());
        
        // 2. 检查是否已存在（秒传）
        FileMetadata existingFile = fileMetadataMapper.selectById(fileHash);
        
        if (existingFile != null) {
            // 文件已存在，增加引用计数
            fileMetadataMapper.incrementRefCount(fileHash);
            log.info("秒传成功: fileHash={}", fileHash);
        } else {
            // 3. 上传到MinIO
            String objectKey = buildObjectKey(fileHash, getExtension(file.getOriginalFilename()));
            
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectKey)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());

            // 4. 保存元数据
            FileMetadata metadata = new FileMetadata();
            metadata.setFileHash(fileHash);
            metadata.setFileSize(file.getSize());
            metadata.setFileType(getFileType(file.getContentType()));
            metadata.setMimeType(file.getContentType());
            metadata.setExt(getExtension(file.getOriginalFilename()));
            metadata.setStorageBackend("minio");
            metadata.setBucketName(bucketName);
            metadata.setObjectKey(objectKey);
            metadata.setRefCount(1);
            metadata.setStatus(1);
            metadata.setVirusScanStatus(0);
            metadata.setContentAuditStatus(0);
            metadata.setCreatedAt(LocalDateTime.now());
            metadata.setUpdatedAt(LocalDateTime.now());
            
            fileMetadataMapper.insert(metadata);
            log.info("文件上传成功: fileHash={}, objectKey={}", fileHash, objectKey);
        }

        // 5. 创建用户文件关联
        UserFile userFile = new UserFile();
        userFile.setUserId(userId);
        userFile.setFileHash(fileHash);
        userFile.setOriginalName(file.getOriginalFilename());
        userFile.setDisplayName(file.getOriginalFilename());
        userFile.setSourceType(sourceType);
        userFile.setSourceId(sourceId);
        userFile.setIsPublic(false);
        userFile.setDownloadCount(0);
        userFile.setCreatedAt(LocalDateTime.now());
        
        userFileMapper.insert(userFile);
        
        return userFile;
    }

    /**
     * 检查文件是否存在（用于秒传）
     */
    public boolean checkFileExists(String fileHash) {
        return fileMetadataMapper.selectById(fileHash) != null;
    }

    /**
     * 获取文件下载URL（预签名）
     */
    public String getDownloadUrl(String fileHash, int expireMinutes) throws Exception {
        FileMetadata metadata = fileMetadataMapper.selectById(fileHash);
        if (metadata == null) {
            throw new RuntimeException("文件不存在");
        }

        return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                .bucket(metadata.getBucketName())
                .object(metadata.getObjectKey())
                .method(Method.GET)
                .expiry(expireMinutes, TimeUnit.MINUTES)
                .build());
    }

    /**
     * 初始化分片上传
     */
    public FileUploadSession initMultipartUpload(Long userId, String fileName, Long fileSize, 
                                                  String mimeType, int chunkSize) {
        int chunkCount = (int) Math.ceil((double) fileSize / chunkSize);
        
        FileUploadSession session = new FileUploadSession();
        session.setUploadId(UUID.randomUUID().toString());
        session.setUserId(userId);
        session.setFileName(fileName);
        session.setFileSize(fileSize);
        session.setMimeType(mimeType);
        session.setChunkSize(chunkSize);
        session.setChunkCount(chunkCount);
        session.setUploadedChunks(0);
        session.setStatus(1); // 上传中
        session.setExpiresAt(LocalDateTime.now().plusHours(24));
        session.setCreatedAt(LocalDateTime.now());
        
        uploadSessionMapper.insert(session);
        
        log.info("初始化分片上传: uploadId={}, fileName={}, chunkCount={}", 
                session.getUploadId(), fileName, chunkCount);
        
        return session;
    }

    /**
     * 删除用户文件
     */
    @Transactional
    public void deleteUserFile(Long userId, Long fileId) {
        UserFile userFile = userFileMapper.selectById(fileId);
        if (userFile == null || !userFile.getUserId().equals(userId)) {
            throw new RuntimeException("文件不存在或无权限");
        }

        // 软删除用户文件
        userFile.setDeletedAt(LocalDateTime.now());
        userFileMapper.updateById(userFile);

        // 减少引用计数
        fileMetadataMapper.decrementRefCount(userFile.getFileHash());
        
        log.info("文件删除成功: userId={}, fileId={}", userId, fileId);
    }

    /**
     * 计算SHA-256哈希
     */
    private String calculateSHA256(InputStream inputStream) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] buffer = new byte[8192];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            digest.update(buffer, 0, bytesRead);
        }
        byte[] hashBytes = digest.digest();
        
        StringBuilder hexString = new StringBuilder();
        for (byte b : hashBytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }

    /**
     * 构建CAS对象键
     */
    private String buildObjectKey(String fileHash, String ext) {
        // CAS路径: cas/a1/b2/hash.ext
        String prefix = fileHash.substring(0, 2) + "/" + fileHash.substring(2, 4);
        return "cas/" + prefix + "/" + fileHash + (ext != null ? "." + ext : "");
    }

    /**
     * 获取文件扩展名
     */
    private String getExtension(String fileName) {
        if (fileName == null) return null;
        int dotIndex = fileName.lastIndexOf('.');
        return dotIndex > 0 ? fileName.substring(dotIndex + 1).toLowerCase() : null;
    }

    /**
     * 获取文件类型
     */
    private String getFileType(String mimeType) {
        if (mimeType == null) return "other";
        if (mimeType.startsWith("image/")) return "image";
        if (mimeType.startsWith("video/")) return "video";
        if (mimeType.startsWith("audio/")) return "audio";
        if (mimeType.contains("pdf") || mimeType.contains("document") || 
            mimeType.contains("word") || mimeType.contains("excel")) return "document";
        return "other";
    }
}
