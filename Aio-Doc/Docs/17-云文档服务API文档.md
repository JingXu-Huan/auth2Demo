# 云文档服务 API 文档

## 服务信息
- **服务名称**: document-service
- **端口**: 8017
- **基础路径**: /api/v1/documents
- **版本**: v1.0.0

---

## 📋 服务功能说明

### 核心功能
云文档服务提供在线文档创建、编辑、协作、分享等功能，支持多人实时协作编辑，类似飞书文档、腾讯文档。

### 主要特性

#### 1. 文档管理
- ✅ **创建文档**: 文档、表格、演示文稿
- ✅ **编辑文档**: 在线编辑
- ✅ **删除文档**: 删除到回收站
- ✅ **恢复文档**: 从回收站恢复
- ✅ **文档模板**: 预定义模板

#### 2. 协作编辑
- ✅ **实时协作**: 多人同时编辑
- ✅ **光标同步**: 显示他人光标
- ✅ **冲突解决**: 自动合并冲突
- ✅ **版本历史**: 查看历史版本
- ✅ **评论功能**: 添加评论

#### 3. 权限管理
- ✅ **查看权限**: 只读
- ✅ **编辑权限**: 可编辑
- ✅ **评论权限**: 可评论
- ✅ **管理权限**: 可管理
- ✅ **分享链接**: 生成分享链接

#### 4. 文档导出
- ✅ **导出PDF**: 导出为PDF
- ✅ **导出Word**: 导出为Word
- ✅ **导出Markdown**: 导出为MD

---

## 1. 创建文档

### Request
```json
{
  "title": "产品需求文档",
  "type": "DOCUMENT",
  "folderId": "folder_001",
  "content": {
    "blocks": [
      {
        "type": "heading",
        "level": 1,
        "text": "产品需求"
      },
      {
        "type": "paragraph",
        "text": "这是需求描述..."
      }
    ]
  }
}
```

### Response
```json
{
  "code": 200,
  "message": "创建成功",
  "data": {
    "documentId": "doc_123456",
    "title": "产品需求文档",
    "type": "DOCUMENT",
    "ownerId": 10001,
    "shareUrl": "https://docs.example.com/doc_123456",
    "createdAt": "2025-11-10T12:00:00Z"
  }
}
```

---

## 2. 协作编辑

### WebSocket连接
```javascript
const ws = new WebSocket('wss://docs.example.com/ws/doc_123456?token=xxx');

ws.onmessage = (event) => {
  const data = JSON.parse(event.data);
  // 处理协作更新
};

// 发送编辑操作
ws.send(JSON.stringify({
  type: 'EDIT',
  operation: {
    type: 'INSERT',
    position: 10,
    content: 'Hello'
  }
}));
```

---

## 数据模型

### Document
```typescript
interface Document {
  documentId: string;           // 文档ID
  title: string;                // 标题
  type: DocumentType;           // 类型
  ownerId: number;              // 所有者ID
  content: object;              // 文档内容
  collaborators: Collaborator[]; // 协作者
  shareUrl: string;             // 分享链接
  version: number;              // 版本号
  createdAt: string;            // 创建时间
  updatedAt: string;            // 更新时间
}

enum DocumentType {
  DOCUMENT = 'DOCUMENT',        // 文档
  SPREADSHEET = 'SPREADSHEET',  // 表格
  PRESENTATION = 'PRESENTATION' // 演示文稿
}

enum Permission {
  VIEW = 'VIEW',                // 查看
  COMMENT = 'COMMENT',          // 评论
  EDIT = 'EDIT',                // 编辑
  MANAGE = 'MANAGE'             // 管理
}
```

---

**文档版本**: v1.0.0  
**最后更新**: 2025-11-11  
**维护人**: 开发团队
