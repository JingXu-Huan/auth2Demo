### 第二部分：全量 API 接口清单 (The Complete List)
以下接口涵盖了 认证、用户、IM（含Thread）、文档（含评论）、文件、搜索、通知、日历 八大模块。

#### 1. 认证服务 (Auth Service)
**Base Path**: `/api/auth`

| **方法** | **路径** | **参数 (Body/Query)** | **描述** |
| --- | --- | --- | --- |
| POST | `/code/send` | `{email, type}` | 发送验证码 |
| POST | `/register` | `{email, code, nickname, pwd}` | 邮箱注册 |
| POST | `/login/email` | `{email, pwd, device_info}` | 账号密码登录 |
| POST | `/login/gitee` | `{code, device_info}` | Gitee OAuth 回调 |
| POST | `/token/refresh` | `{refresh_token}` | 换取新 Access Token |
| POST | `/logout` | - | 注销当前设备 |
| POST | `/password/reset` | `{old_pwd, new_pwd}` | 修改密码 |
| GET | `/devices` | - | 获取在线设备列表 |
| DELETE | `/devices/{id}` | - | 踢下线某设备 |


#### 2. 组织架构与用户 (User Service)
**Base Path**: `/api/org`

| **方法** | **路径** | **参数** | **描述** |
| --- | --- | --- | --- |
| GET | `/tree` | `?lazy=false` | 获取完整/懒加载组织树 |
| GET | `/users/me` | - | 获取当前用户详情 |
| PUT | `/users/me` | `{avatar, motto}` | 更新个人资料 |
| GET | `/users/{id}/card` | - | 获取他人名片 (含部门信息) |
| POST | `/depts` | `{name, parent_id}` | 创建部门 (Admin) |
| PUT | `/depts/{id}/move` | `{new_parent_id}` | 移动部门 (触发 Path 更新) |
| GET | `/members/search` | `?q=jack` | 全局搜索通讯录 (ES) |
| GET | `/notifications` | `?unread_only=true` | **获取通知中心列表** |
| PUT | `/notifications/read` | `{ids: [...]}` | 批量标记通知已读 |


#### 3. 即时通讯 (IM Service)
**Base Path**: `/api/im`

**核心消息：**

| **方法** | **路径** | **参数** | **描述** |
| --- | --- | --- | --- |
| POST | `/msg/send` | `{channel_id, type, content, root_id}` | 发送消息 (支持回复话题) |
| POST | `/msg/sync` | `{cursor_seq, limit}` | **信令同步 (核心拉取接口)** |
| GET | `/msg/history` | `?channel_id=x&before_seq=y` | 下拉加载历史消息 |
| POST | `/msg/revoke` | `{msg_id}` | 撤回消息 |
| POST | `/msg/react` | `{msg_id, emoji}` | 表情点赞 (Reaction) |
| GET | `/msg/{id}/read_status` | - | **查看已读/未读人员列表** |


**话题 (Threads) - 对标飞书核心：**

| **方法** | **路径** | **参数** | **描述** |
| --- | --- | --- | --- |
| GET | `/msg/{root_id}/thread` | `?page=1` | 获取某话题下的回复列表 |
| POST | `/thread/subscribe` | `{root_id}` | 关注话题 (有新回复推送通知) |


**会话管理：**

| **方法** | **路径** | **参数** | **描述** |
| --- | --- | --- | --- |
| GET | `/conversations` | - | 获取会话列表 (含置顶/未读数) |
| POST | `/conversations/ack` | `{channel_id, read_seq}` | 清除未读红点 |
| POST | `/groups` | `{members: [], name}` | 创建群组 |
| POST | `/groups/{id}/pin` | `{msg_id}` | **Pin 消息 (群置顶)** |


#### 4. 协同文档 (Collab Service)
**Base Path**: `/api/doc`

**元数据与管理：**

| **方法** | **路径** | **参数** | **描述** |
| --- | --- | --- | --- |
| POST | `/create` | `{title, type, space_id}` | 创建空文档 |
| GET | `/{id}/meta` | - | 获取标题、权限、Owner |
| GET | `/{id}/export` | `?format=pdf` | 导出文档 |
| POST | `/{id}/perm` | `{public_scope, members}` | 修改权限设置 |


**划词评论 (Comments) - 对标飞书核心：**

| **方法** | **路径** | **参数** | **描述** |
| --- | --- | --- | --- |
| POST | `/{id}/comments` | `{anchor_data, content}` | 发表划词评论 |
| GET | `/{id}/comments` | `?status=OPEN` | 获取文档内所有评论 |
| POST | `/comments/{cid}/reply` | `{content}` | 回复某条评论 |
| PUT | `/comments/{cid}/resolve` | - | 解决评论 (不再高亮) |


#### 5. 日程日历 (Calendar Service) - _新补充模块_
**Base Path**: `/api/calendar`

| **方法** | **路径** | **参数** | **描述** |
| --- | --- | --- | --- |
| POST | `/events` | `{title, start, end, attendees}` | 创建日程 |
| GET | `/events` | `?start=x&end=y` | 获取某段时间的日程视图 |
| POST | `/free-busy` | `{user_ids: [], time_range}` | **忙闲查询 (预约会议用)** |
| POST | `/events/{id}/rsvp` | `{status: ACCEPT/DECLINE}` | 接受/拒绝会议邀请 |


#### 6. 文件服务 (File Service)
**Base Path**: `/api/file`

| **方法** | **路径** | **参数** | **描述** |
| --- | --- | --- | --- |
| POST | `/pre-upload` | `{hash, size, ext}` | 秒传预检，返回 MinIO URL |
| POST | `/confirm` | `{hash, key, name}` | 上传完成确认 |
| GET | `/url/{hash}` | - | 获取下载/预览签名链接 |
| GET | `/avatar/{uid}` | - | 获取用户头像 (CDN加速) |


#### 7. 智能搜索 (Search Service)
**Base Path**: `/api/search`

| **方法** | **路径** | **参数** | **描述** |
| --- | --- | --- | --- |
| GET | `/general` | `?q=Key&filter_type=MSG` | 全局混合搜索 (ES) |
| POST | `/ai/ask` | `{prompt, context_doc_id}` | **RAG 对话 (流式返回)** |
| POST | `/mcp/tool/call` | `{tool_name, args}` | 供 AI Agent 调用的接口 |


#### 8. 管理后台 (Admin Service)
**Base Path**: `/api/admin`

| **方法** | **路径** | **参数** | **描述** |
| --- | --- | --- | --- |
| POST | `/users/ban` | `{uid, reason, duration}` | 封禁用户 |
| GET | `/audit/logs` | `?action=DELETE` | 查询审计日志 |
| POST | `/risk/words` | `{word, action}` | 添加敏感词 |
| GET | `/stats/daily` | `?date=2025-11-24` | 获取日活/消息量报表 |


---

### 第三部分：关键交互数据结构 (Data Structures)
#### 1. 话题回复的 WebSocket 推送
当有人在话题里回复时，Gateway 推送的 JSON：

JSON

```plain
{
  "cmd": "PUSH_MSG",
  "data": {
    "channel_id": 888,
    "msg": {
      "id": "2001",
      "root_id": "1001", // 标识这是某条消息的回复
      "content": "I agree",
      "seq": 56
    },
    "thread_summary": {
      "root_id": "1001",
      "reply_count": 5,
      "last_reply_ts": 1710009999
    }
  }
}
```

#### 2. 文档评论的 WebSocket 推送
当有人发表划词评论时，Node.js Sidecar 广播的 JSON：

JSON

```plain
{
  "type": "COMMENT_ADDED",
  "data": {
    "comment_id": "c_123",
    "creator": { "id": "u1", "name": "Alice" },
    "anchor": { "y_change": "base64...", "quote": "Revenue" },
    "content": "Data needs verification",
    "status": "OPEN"
  }
}
```

