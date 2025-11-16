# 文件服务 API 文档

## 📋 服务功能说明

文件服务负责处理IM系统中所有文件的上传、下载、存储和管理。本服务支持多种文件类型，提供安全的文件传输、高效的存储方案和完善的文件管理功能，是消息服务、群组服务等的重要支撑。

### 核心功能

#### 1. 文件上传
- **多文件上传**: 支持单文件、批量文件上传
- **断点续传**: 大文件分片上传，支持断点续传
- **文件类型**: 图片、视频、音频、文档、压缩包等
- **格式限制**: 可配置允许/禁止的文件格式
- **大小限制**: 单文件最大100MB，可配置
- **秒传功能**: 文件MD5校验，已存在文件直接返回

#### 2. 文件下载
- **直接下载**: 通过URL直接下载文件
- **预览支持**: 图片、PDF等文件在线预览
- **断点下载**: 支持断点续传下载
- **下载限速**: 防止带宽占用过高
- **权限控制**: 验证用户下载权限

#### 3. 图片处理
- **缩略图**: 自动生成多种尺寸缩略图
- **图片压缩**: 智能压缩，减少存储空间
- **格式转换**: 支持WebP、JPEG、PNG等格式转换
- **水印添加**: 可选添加水印
- **EXIF信息**: 提取图片元数据

#### 4. 文件管理
- **文件列表**: 查询用户上传的所有文件
- **文件搜索**: 按文件名、类型、时间搜索
- **文件删除**: 删除文件（标记删除，定期清理）
- **文件分类**: 按类型自动分类（图片、视频、文档等）
- **存储统计**: 用户存储空间使用统计

#### 5. 安全控制
- **病毒扫描**: 上传文件自动病毒扫描
- **内容审核**: 图片、视频内容安全审核
- **访问控制**: 基于Token的访问权限控制
- **防盗链**: CDN防盗链配置
- **加密存储**: 敏感文件加密存储

#### 6. 存储优化
- **CDN加速**: 文件CDN分发，加快访问速度
- **冷热分离**: 热数据SSD，冷数据归档
- **自动清理**: 过期临时文件自动清理
- **去重存储**: 相同文件只存储一份

### 技术特性
- **对象存储**: 阿里云OSS / 腾讯云COS
- **CDN**: 全球CDN加速
- **数据库**: MongoDB（文件元数据）
- **缓存**: Redis（热点文件缓存）
- **消息队列**: RabbitMQ（异步处理）
- **文件扫描**: ClamAV（病毒扫描）

---

## 服务信息
- **服务名称**: file-service
- **端口**: 8005
- **基础路径**: /api/v1/files
- **版本**: v1.0.0

---

## 核心接口

### 1. 上传文件
- **URL**: `/upload`
- **Method**: `POST`
- **Content-Type**: `multipart/form-data`
- **功能**: 上传文件到服务器

**Request**:
```
POST /api/v1/files/upload
Content-Type: multipart/form-data

file: [binary data]
userId: 10001
fileType: IMAGE
```

**Response**:
```json
{
  "code": 200,
  "message": "上传成功",
  "data": {
    "fileId": "file_123456",
    "fileName": "image.jpg",
    "fileSize": 1024000,
    "fileType": "IMAGE",
    "url": "https://cdn.example.com/files/file_123456.jpg",
    "thumbnailUrl": "https://cdn.example.com/thumbnails/file_123456_thumb.jpg",
    "uploadTime": "2025-11-09T15:00:00Z"
  }
}
```

---

### 2. 下载文件
- **URL**: `/{fileId}`
- **Method**: `GET`
- **功能**: 下载文件

**Response**: 文件二进制流

---

### 3. 获取文件信息
- **URL**: `/{fileId}/info`
- **Method**: `GET`
- **功能**: 获取文件元数据

**Response**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "fileId": "file_123456",
    "fileName": "image.jpg",
    "fileSize": 1024000,
    "fileType": "IMAGE",
    "mimeType": "image/jpeg",
    "url": "https://cdn.example.com/files/file_123456.jpg",
    "uploaderId": 10001,
    "uploadTime": "2025-11-09T15:00:00Z"
  }
}
```

---

### 4. 删除文件
- **URL**: `/{fileId}`
- **Method**: `DELETE`
- **功能**: 删除文件

**Response**:
```json
{
  "code": 200,
  "message": "删除成功",
  "data": null
}
```

---

### 5. 图片压缩
- **URL**: `/image/compress`
- **Method**: `POST`
- **功能**: 压缩图片

**Request**:
```json
{
  "fileId": "file_123456",
  "quality": 80,
  "maxWidth": 1920,
  "maxHeight": 1080
}
```

**Response**:
```json
{
  "code": 200,
  "message": "压缩成功",
  "data": {
    "originalFileId": "file_123456",
    "compressedFileId": "file_123457",
    "originalSize": 2048000,
    "compressedSize": 512000,
    "compressionRatio": "75%",
    "url": "https://cdn.example.com/files/file_123457.jpg"
  }
}
```

---

## 文件类型

| 类型 | 说明 | 支持格式 |
|------|------|----------|
| IMAGE | 图片 | jpg, png, gif, webp |
| VIDEO | 视频 | mp4, avi, mov |
| AUDIO | 音频 | mp3, wav, aac |
| DOCUMENT | 文档 | pdf, doc, docx, xls, xlsx |
| OTHER | 其他 | 所有其他格式 |

---

## 文件大小限制

| 类型 | 最大大小 |
|------|----------|
| 图片 | 10MB |
| 视频 | 100MB |
| 音频 | 20MB |
| 文档 | 50MB |
| 其他 | 20MB |

---

## 数据模型

```typescript
interface FileInfo {
  fileId: string;
  fileName: string;
  fileSize: number;
  fileType: FileType;
  mimeType: string;
  url: string;
  thumbnailUrl?: string;
  uploaderId: number;
  uploadTime: string;
}

type FileType = 'IMAGE' | 'VIDEO' | 'AUDIO' | 'DOCUMENT' | 'OTHER';
```

---

**文档版本**: v1.0.0  
**最后更新**: 2025-11-09  
**维护人**: 开发团队
