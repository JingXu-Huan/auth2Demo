# 文件服务 API 文档

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
