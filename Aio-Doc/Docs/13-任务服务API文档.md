# 任务服务 API 文档

## 服务信息
- **服务名称**: task-service
- **端口**: 8013
- **基础路径**: /api/v1/tasks
- **版本**: v1.0.0

---

## 📋 服务功能说明

### 核心功能
任务服务提供完整的任务管理、协作、跟踪功能，支持个人任务和团队任务，帮助团队高效完成工作目标。

### 主要特性

#### 1. 任务管理
- ✅ **创建任务**: 个人任务、团队任务
- ✅ **任务分配**: 指派给成员
- ✅ **任务编辑**: 修改任务信息
- ✅ **任务删除**: 删除任务
- ✅ **任务复制**: 快速创建相似任务
- ✅ **子任务**: 支持多级子任务

#### 2. 任务状态
- ✅ **待办**: 未开始的任务
- ✅ **进行中**: 正在执行的任务
- ✅ **已完成**: 完成的任务
- ✅ **已关闭**: 关闭的任务
- ✅ **已取消**: 取消的任务

#### 3. 优先级管理
- ✅ **紧急**: P0级别
- ✅ **高**: P1级别
- ✅ **中**: P2级别
- ✅ **低**: P3级别

#### 4. 协作功能
- ✅ **多人协作**: 支持多人参与
- ✅ **任务评论**: 讨论任务细节
- ✅ **@提醒**: 提醒相关人员
- ✅ **文件附件**: 上传相关文件
- ✅ **任务关联**: 关联相关任务

#### 5. 进度跟踪
- ✅ **进度更新**: 实时更新进度
- ✅ **时间记录**: 记录工作时长
- ✅ **里程碑**: 设置关键节点
- ✅ **燃尽图**: 可视化进度

#### 6. 提醒通知
- ✅ **截止提醒**: 临近截止时间提醒
- ✅ **逾期提醒**: 超期任务提醒
- ✅ **状态变更**: 任务状态变更通知
- ✅ **评论提醒**: 新评论通知

#### 7. 任务看板
- ✅ **看板视图**: 可视化任务流
- ✅ **列表视图**: 列表展示任务
- ✅ **甘特图**: 时间线视图
- ✅ **日历视图**: 按日期查看

#### 8. 统计分析
- ✅ **完成率**: 任务完成统计
- ✅ **工时统计**: 工作时长统计
- ✅ **成员绩效**: 个人任务统计
- ✅ **趋势分析**: 任务趋势图表

### 业务场景

#### 场景1: 项目任务管理
```
项目经理创建项目
  ↓
分解为多个任务
  ↓
分配给团队成员
  ↓
成员更新任务进度
  ↓
项目经理跟踪进度
  ↓
任务完成，项目结项
```

#### 场景2: 个人GTD
```
用户创建个人任务
  ↓
设置优先级和截止时间
  ↓
系统提醒待办任务
  ↓
用户完成任务
  ↓
查看完成统计
```

### 技术特点
- 使用 Redis 缓存任务数据
- 使用 Elasticsearch 全文搜索
- 使用 RabbitMQ 异步通知
- 使用 MongoDB 存储任务历史

---

## 目录
1. [创建任务](#1-创建任务)
2. [获取任务详情](#2-获取任务详情)
3. [更新任务](#3-更新任务)
4. [删除任务](#4-删除任务)
5. [分配任务](#5-分配任务)
6. [更新任务状态](#6-更新任务状态)
7. [添加子任务](#7-添加子任务)
8. [添加评论](#8-添加评论)
9. [查询任务列表](#9-查询任务列表)
10. [任务统计](#10-任务统计)

---

## 1. 创建任务

### 接口信息
- **URL**: `/`
- **Method**: `POST`
- **功能**: 创建新任务
- **认证**: 需要 Bearer Token

### Request
```json
{
  "title": "完成用户模块开发",
  "description": "实现用户注册、登录、信息管理功能",
  "priority": "HIGH",
  "dueDate": "2025-11-20T18:00:00Z",
  "assigneeId": 10002,
  "projectId": "proj_001",
  "tags": ["开发", "后端"],
  "estimatedHours": 16,
  "attachments": [
    {
      "name": "需求文档.pdf",
      "url": "https://cdn.example.com/files/req.pdf"
    }
  ]
}
```

### Response - 成功 (200)
```json
{
  "code": 200,
  "message": "创建成功",
  "data": {
    "taskId": "task_123456",
    "title": "完成用户模块开发",
    "status": "TODO",
    "priority": "HIGH",
    "creatorId": 10001,
    "assigneeId": 10002,
    "createdAt": "2025-11-10T12:00:00Z"
  }
}
```

---

## 2. 获取任务详情

### 接口信息
- **URL**: `/{taskId}`
- **Method**: `GET`

### Response - 成功 (200)
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "taskId": "task_123456",
    "title": "完成用户模块开发",
    "description": "实现用户注册、登录、信息管理功能",
    "status": "IN_PROGRESS",
    "priority": "HIGH",
    "progress": 60,
    "creatorId": 10001,
    "creatorName": "张三",
    "assigneeId": 10002,
    "assigneeName": "李四",
    "dueDate": "2025-11-20T18:00:00Z",
    "estimatedHours": 16,
    "actualHours": 10,
    "tags": ["开发", "后端"],
    "subtasks": [
      {
        "taskId": "task_123457",
        "title": "用户注册接口",
        "status": "DONE",
        "progress": 100
      }
    ],
    "comments": [
      {
        "commentId": "cmt_001",
        "userId": 10002,
        "userName": "李四",
        "content": "已完成60%",
        "createdAt": "2025-11-12T10:00:00Z"
      }
    ],
    "createdAt": "2025-11-10T12:00:00Z",
    "updatedAt": "2025-11-12T10:00:00Z"
  }
}
```

---

## 6. 更新任务状态

### 接口信息
- **URL**: `/{taskId}/status`
- **Method**: `PUT`

### Request
```json
{
  "status": "DONE",
  "comment": "任务已完成"
}
```

### Response - 成功 (200)
```json
{
  "code": 200,
  "message": "状态更新成功",
  "data": {
    "taskId": "task_123456",
    "status": "DONE",
    "progress": 100,
    "completedAt": "2025-11-15T16:00:00Z"
  }
}
```

---

## 数据模型

### Task
```typescript
interface Task {
  taskId: string;               // 任务ID
  title: string;                // 标题
  description: string;          // 描述
  status: TaskStatus;           // 状态
  priority: Priority;           // 优先级
  progress: number;             // 进度(0-100)
  creatorId: number;            // 创建者ID
  assigneeId: number;           // 负责人ID
  dueDate: string;              // 截止时间
  estimatedHours: number;       // 预估工时
  actualHours: number;          // 实际工时
  tags: string[];               // 标签
  projectId: string;            // 项目ID
  parentTaskId: string;         // 父任务ID
  createdAt: string;            // 创建时间
  updatedAt: string;            // 更新时间
  completedAt: string;          // 完成时间
}

enum TaskStatus {
  TODO = 'TODO',                // 待办
  IN_PROGRESS = 'IN_PROGRESS',  // 进行中
  DONE = 'DONE',                // 已完成
  CLOSED = 'CLOSED',            // 已关闭
  CANCELLED = 'CANCELLED'       // 已取消
}

enum Priority {
  URGENT = 'URGENT',            // P0-紧急
  HIGH = 'HIGH',                // P1-高
  MEDIUM = 'MEDIUM',            // P2-中
  LOW = 'LOW'                   // P3-低
}
```

---

**文档版本**: v1.0.0  
**最后更新**: 2025-11-11  
**维护人**: 开发团队
