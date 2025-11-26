在企业级协作平台中，文件服务不仅仅是“上传下载”，它需要解决**海量数据存储成本（去重）、大文件传输稳定性（分片/直传）、安全性（权限管控）以及多媒体处理（缩略图/预览）**等核心问题。

本设计将采用 **CAS (Content Addressable Storage)** 核心思想，结合 **MinIO** 对象存储，构建一个高效、低成本的文件中台。

---

### 1. 核心定位与设计哲学
+ **核心职责**：
    - **元数据管理**：维护文件 Hash、大小、引用计数。
    - **签名中心**：生成 MinIO 的上传/下载预签名 URL (Presigned URL)，让客户端直接与 MinIO 交互，避免流量经过 Java 服务。
    - **去重引擎**：基于 SHA-256 实现“秒传”逻辑。
    - **资源回收**：基于引用计数 (Reference Counting) 的垃圾回收 (GC)。
+ **设计原则**：
    - **流量旁路 (Traffic Bypass)**：大文件流不经过 `file-service`，只走元数据。
    - **一次存储，多次引用**：物理上只存一份，逻辑上属于多人。
    - **私有读写**：MinIO Bucket 设置为 `Private`，一切访问必须通过签名。

---

### 2. 技术栈清单
+ **框架**: Spring Boot 3.x
+ **存储引擎**: MinIO (兼容 AWS S3 协议)
+ **数据库**: PostgreSQL (存储元数据)
+ **缓存**: Redis (缓存热点文件的下载 URL)
+ **图片处理**: `Thumbnailator` (Java 库) 或 `ImageMagick` (系统级)
+ **文件识别**: Apache Tika (MIME 类型探测)

---

### 3. 架构数据流图 (Data Flow)
```mermaid
graph TD
    Client[客户端 (Web/App)]
    FileService[File-Service]
    DB[(PostgreSQL)]
    MinIO[MinIO Cluster]
    Redis[Redis Cache]

    %% 上传流程
    Client -- 1. Pre-upload (SHA256) --> FileService
    FileService -- 2. Check Hash --> DB
    
    alt Hash Hit (秒传)
        DB -- 3a. Return Existing ID --> FileService
        FileService -- 4a. Upload Success --> Client
    else Hash Miss (新文件)
        FileService -- 3b. Gen Presigned PUT URL --> MinIO
        FileService -- 4b. Return URL --> Client
        Client -- 5b. PUT Binary Data --> MinIO
        Client -- 6b. Confirm Upload --> FileService
        FileService -- 7b. Save Metadata --> DB
    end

    %% 下载流程
    Client -- 8. Get Download URL --> FileService
    FileService -- 9. Check Perm & Gen Presigned GET URL --> MinIO
    FileService -- 10. Return URL --> Client
    Client -- 11. GET File --> MinIO
```

---

### 4. 核心功能模块详细设计
#### 4.1 CAS 秒传与上传流程 (The "Magic" of Deduplication)
这是节省存储空间的核心。

+ **步骤 1：预检 (Pre-upload)**
    - **接口**: `POST /api/file/pre-upload`
    - **参数**: `hash` (SHA-256), `size`, `fileName`, `ext`
    - **逻辑**:
        1. 查询 `file_metadata` 表是否存在该 `hash`。
        2. **命中 (Hit)**:
            + 引用计数 `ref_count + 1`。
            + 插入 `user_files` 表（用户逻辑文件）。
            + 返回 `{ status: "EXIST", url: "..." }`。客户端瞬间显示“上传成功”。
        3. **未命中 (Miss)**:
            + 生成对象键 `objectKey`：建议打散目录 `cas/{hash_prefix}/{hash}.{ext}`。
            + 调用 MinIO 生成 `Presigned PUT URL` (有效期 10分钟)。
            + 返回 `{ status: "MISS", uploadUrl: "...", objectKey: "..." }`。
+ **步骤 2：直传 (Direct Upload)**
    - 客户端使用 `PUT` 请求将文件二进制流发给 `uploadUrl`。
    - **关键**: 这一步不消耗应用服务器的带宽和 CPU。
+ **步骤 3：确认 (Confirmation)**
    - **接口**: `POST /api/file/confirm`
    - **参数**: `hash`, `objectKey`, `fileName`
    - **逻辑**:
        1. **校验**: (可选) `file-service` 调用 MinIO API `statObject` 确认文件真的存在且大小一致。
        2. **落库**:
            + 插入 `file_metadata` (status=1, ref_count=1)。
            + 插入 `user_files`。
        3. **异步任务**: 发送 MQ 消息触发“图片缩略图生成”或“文档转码”。

#### 4.2 安全下载与预览 (Secure Access)
MinIO 中的文件是私有的，不能直接通过 URL 访问。

+ **接口**: `GET /api/file/url/{fileHash}`
+ **逻辑**:
    1. **鉴权**: 校验 Access Token。判断用户是否有权访问该文件（通常这步比较弱，只要登录即可；严格权限由业务层 IM/Doc 控制，FileService 只负责给 URL）。
    2. **缓存查取**: Redis `file:url:{hash}` 是否存在且未过期？
    3. **生成签名**:
        * 调用 MinIO `getPresignedObjectUrl`。
        * **设置过期时间**: 如 1 小时。
        * **设置 Content-Disposition**: 决定是“下载”还是“浏览器预览”。
    4. **缓存**: 将生成的 URL 存入 Redis (TTL 50分钟)。
    5. **返回**: 客户端拿到 URL 后，直接从 MinIO 下载。

#### 4.3 引用计数与垃圾回收 (GC)
+ **引用计数维护**:
    - **IM 发消息**: `im-service` 调用 `file-service` -> `ref_count++`。
    - **撤回/删除消息**: `im-service` 调用 `file-service` -> `ref_count--`。
+ **软删除**:
    - 当 `ref_count <= 0` 时，不立即删除 MinIO 文件。
    - 更新 `file_metadata.status = 2 (WAIT_GC)`。
+ **GC 定时任务**:
    - 每晚凌晨运行。
    - 扫描 `status=2` 且 `update_time < (NOW - 24h)` 的记录。
    - 调用 MinIO `removeObject` 物理删除。
    - 删除数据库记录。

#### 4.4 图片处理 (Image Processing)
+ **需求**: 用户上传 10MB 的 4K 原图，但在聊天列表只需显示 20KB 的缩略图。
+ **方案**:
    1. 上传确认后，`file-service` 下载原图的前 N KB（或全量）。
    2. 使用 `Thumbnailator` 生成缩略图 (200x200)。
    3. 上传缩略图至 MinIO，路径为 `cas/.../{hash}_thumb.jpg`。
    4. 更新 `file_metadata` 的 `extra` 字段：`{ "thumbnail": "cas/..._thumb.jpg" }`。

---

### 5. 数据库设计 (PostgreSQL)
基于之前的 Schema 进行微调，增加业务细节。

```sql
-- 1. 物理文件元数据表 (Global Unique)
CREATE TABLE file_metadata (
    file_hash VARCHAR(64) PRIMARY KEY, -- SHA-256
    file_size BIGINT NOT NULL,
    ext VARCHAR(20),                   -- 扩展名: "jpg", "pdf"
    content_type VARCHAR(100),         -- MIME: "image/jpeg"
    
    storage_bucket VARCHAR(64) NOT NULL,
    storage_path VARCHAR(255) NOT NULL,-- MinIO Key: "cas/a1/b2/hash.jpg"
    
    ref_count INT DEFAULT 0,           -- 引用计数
    status INT DEFAULT 1,              -- 1:正常, 2:待GC
    
    extra JSONB,                       -- 扩展: {"width": 1024, "height": 768, "duration": 60, "thumb_path": "..."}
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- 2. 用户逻辑文件表 (User View)
CREATE TABLE user_files (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,           -- 上传者
    file_hash VARCHAR(64) NOT NULL,    -- 关联物理文件
    file_name VARCHAR(255) NOT NULL,   -- 用户命名的文件名 (原名)
    
    source_type INT,                   -- 来源: 1=IM, 2=Doc, 3=Avatar
    source_id VARCHAR(64),             -- 关联的业务ID (如 msg_id)
    
    created_at TIMESTAMPTZ DEFAULT NOW(),
    deleted_at TIMESTAMPTZ,            -- 用户层面的删除(软删)
    
    CONSTRAINT fk_user_file_hash FOREIGN KEY (file_hash) REFERENCES file_metadata(file_hash)
);
```

---

### 6. 核心代码逻辑 (Java)
#### 6.1 MinIO 工具类封装
```java
@Component
public class MinioTemplate {

    @Autowired
    private MinioClient minioClient;
    @Value("${minio.bucket}")
    private String bucket;

    /**
     * 生成上传预签名 URL
     */
    public String getPresignedPutUrl(String objectKey) {
        try {
            return minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                    .method(Method.PUT)
                    .bucket(bucket)
                    .object(objectKey)
                    .expiry(10, TimeUnit.MINUTES)
                    .build());
        } catch (Exception e) {
            throw new BizException("MinIO Error", e);
        }
    }

    /**
     * 生成下载预签名 URL
     */
    public String getPresignedGetUrl(String objectKey, String fileName) {
        // 设置 Content-Disposition 使得浏览器下载时使用正确的文件名
        Map<String, String> reqParams = new HashMap<>();
        reqParams.put("response-content-disposition", 
                      "attachment; filename=\"" + fileName + "\"");
        
        return minioClient.getPresignedObjectUrl(
             GetPresignedObjectUrlArgs.builder()
                .method(Method.GET)
                .bucket(bucket)
                .object(objectKey)
                .extraQueryParams(reqParams)
                .expiry(1, TimeUnit.HOURS)
                .build());
    }
}
```

#### 6.2 文件上传业务逻辑
```java
@Service
public class FileService {

    @Autowired
    private FileMetadataMapper metaMapper;
    @Autowired
    private UserFileMapper userFileMapper;
    @Autowired
    private MinioTemplate minioTemplate;

    // 1. 预检接口
    public PreUploadResp preUpload(PreUploadReq req, Long userId) {
        String hash = req.getHash();
        
        // 1.1 检查是否秒传
        FileMetadata meta = metaMapper.selectById(hash);
        if (meta != null) {
            // 秒传逻辑
            saveUserFile(userId, meta, req.getFileName());
            incrementRefCount(hash); // 引用+1
            return new PreUploadResp("EXIST", meta.getStoragePath());
        }

        // 1.2 未命中，生成路径
        // 路径打散: cas/ab/cd/abcd123...jpg
        String objectKey = generateObjectKey(hash, req.getExt());
        String uploadUrl = minioTemplate.getPresignedPutUrl(objectKey);
        
        return new PreUploadResp("MISS", uploadUrl, objectKey);
    }

    // 2. 确认接口
    @Transactional
    public void confirmUpload(ConfirmReq req, Long userId) {
        // 防止并发确认
        if (metaMapper.exists(req.getHash())) return;

        // 2.1 插入元数据
        FileMetadata meta = new FileMetadata();
        meta.setFileHash(req.getHash());
        meta.setStoragePath(req.getObjectKey());
        meta.setRefCount(1);
        meta.setStatus(1);
        metaMapper.insert(meta);

        // 2.2 插入用户文件记录
        saveUserFile(userId, meta, req.getFileName());
        
        // 2.3 触发异步处理 (生成缩略图)
        // Spring Event 或 MQ
    }
}
```

---

### 7. 性能优化与大文件处理
#### 7.1 分片上传 (Multipart Upload)
对于超过 100MB 的文件，MinIO 支持 S3 协议的分片上传。

+ **流程**：
    1. `initiateMultipartUpload`: 后端向 MinIO 申请 UploadID。
    2. 后端生成 N 个 Presigned URL（对应 N 个分片）。
    3. 前端并发上传分片。
    4. `completeMultipartUpload`: 前端通知后端，后端通知 MinIO 合并分片。

#### 7.2 CDN 加速
对于公开或半公开资源（如群头像、表情包），可以在 MinIO 前面挂一个 **Nginx** 或 **CDN**。

+ **头像逻辑**: 将头像 Bucket 设为 `Public Read`，直接通过 CDN 域名访问，减轻 MinIO 压力。

### 8. 总结
`file-service` 的设计关键在于**剥离了数据流与控制流**：

+ Java 服务只处理轻量级的 Hash 和 URL 签名。
+ 沉重的文件传输压力全部甩给了 MinIO 集群。
+ 通过 CAS 实现了高效的存储去重，特别适合 IM 场景（群里大家都在转发同一个文件）。

这套架构既能抗住高并发，又能有效控制存储成本，是标准的企业级网盘/文件设计模式。

