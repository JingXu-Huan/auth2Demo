import request from '../utils/request'
import axios from 'axios'

// ==================== 认证相关 ====================
export const authApi = {
  // 登录
  login: async (email, password) => {
    const authUrl = import.meta.env.VITE_AUTH_BASE_URL || 'http://127.0.0.1:8012'
    const params = new URLSearchParams()
    params.append('username', email)
    params.append('password', password)
    
    const response = await axios.post(`${authUrl}/login`, params, {
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      withCredentials: true
    })
    return response.data
  },
  
  // 检查邮箱
  checkEmail: (email) => request.get('/v1/users/check-email', { params: { email } }),
  
  // 检查用户名
  checkUsername: (username) => request.get('/v1/users/check-username', { params: { username } })
}

// ==================== 用户相关 ====================
export const userApi = {
  // 注册
  register: (data) => request.post('/v1/users/register', data),
  
  // 检查邮箱是否已注册
  checkEmail: (email) => request.get('/v1/users/check-email', { params: { email } }),
  
  // 检查用户名是否已存在
  checkUsername: (username) => request.get('/v1/users/check-username', { params: { username } }),
  
  // 获取用户信息
  getUser: (userId) => request.get(`/v1/users/${userId}`),
  
  // 更新用户信息
  updateUser: (userId, data) => request.put(`/v1/users/${userId}`, data),
  
  // 修改密码
  changePassword: (userId, data) => request.post(`/v1/users/${userId}/change-password`, data),
  
  // 搜索用户
  searchUser: (keyword) => {
    const searchType = keyword.includes('@') ? 'email' : 'phone'
    return request.get('/v1/users/search', { params: { searchType, keyword } })
  },
  
  // 发送验证码
  sendVerifyCode: (email) => request.post('/v1/email/send-code', { email }),
  
  // 验证邮箱验证码
  verifyCode: (email, code) => request.post('/v1/email/verify-code', { email, code })
}

// ==================== 频道/会话相关 ====================
export const channelApi = {
  // 获取用户频道列表
  getChannels: () => request.get('/v1/im/channels/user'),
  
  // 获取频道详情
  getChannel: (channelId) => request.get(`/v1/im/channels/${channelId}`),
  
  // 创建频道
  createChannel: (data) => request.post('/v1/im/channels', data),
  
  // 获取或创建私聊频道
  getPrivateChannel: (targetUserId) => request.post(`/v1/im/channels/private/${targetUserId}`),
  
  // 获取频道成员
  getMembers: (channelId) => request.get(`/v1/im/channels/${channelId}/members`),
  
  // 添加成员
  addMembers: (channelId, memberIds) => request.post(`/v1/im/channels/${channelId}/members`, memberIds),
  
  // 退出频道
  leave: (channelId) => request.post(`/v1/im/channels/${channelId}/leave`)
}

// ==================== 消息相关 ====================
// 后端路径: /api/v1/chat/**
export const messageApi = {
  // 发送消息
  send: (data) => request.post('/v1/chat/messages', data),
  
  // 获取历史消息（同步消息）
  getHistory: (params) => request.get('/v1/chat/messages/sync', { params }),
  
  // 撤回消息
  recall: (messageId) => request.post(`/v1/chat/messages/${messageId}/recall`),
  
  // 标记已读
  markRead: (channelId, messageId) => request.post('/v1/chat/messages/read', null, { params: { channelId, messageId } }),
  
  // 获取已读回执
  getReadReceipts: (messageId, channelId) => request.get(`/v1/chat/messages/${messageId}/reads`, { params: { channelId } }),
  
  // 添加表情反应
  addReaction: (messageId, emoji) => request.post(`/v1/chat/messages/${messageId}/reactions`, null, { params: { emoji } }),
  
  // 移除表情反应
  removeReaction: (messageId, emoji) => request.delete(`/v1/chat/messages/${messageId}/reactions`, { params: { emoji } }),
  
  // 搜索消息
  search: (params) => request.get('/v1/chat/messages/search', { params }),
  
  // 转发消息
  forward: (messageId, targetChannelId) => request.post(`/v1/chat/messages/${messageId}/forward`, null, { params: { targetChannelId } })
}

// ==================== 好友相关 ====================
// 后端路径: /api/v1/relations/**
export const friendApi = {
  // 获取好友列表 (需要 X-User-Id header，由 request 拦截器自动添加)
  getFriends: () => request.get('/v1/relations/friends'),
  
  // 发送好友请求
  sendRequest: (data) => request.post('/v1/relations/friend/apply', data),
  
  // 获取待处理请求
  getPendingRequests: () => request.get('/v1/relations/friend/requests/pending'),
  
  // 审核好友请求 (接受/拒绝)
  auditRequest: (data) => request.put('/v1/relations/friend/audit', data),
  
  // 删除好友
  remove: (friendId) => request.delete(`/v1/relations/friends/${friendId}`),
  
  // 检查是否是好友
  checkFriend: (targetId) => request.get('/v1/relations/check', { params: { targetId } }),
  
  // 获取好友详情
  getFriendDetail: (friendId) => request.get(`/v1/relations/friends/${friendId}`),
  
  // 更新好友备注
  updateRemark: (friendId, remark) => request.put(`/v1/relations/friends/${friendId}/remark`, null, { params: { remark } }),
  
  // 获取黑名单
  getBlacklist: () => request.get('/v1/relations/blacklist'),
  
  // 拉黑用户
  blockUser: (targetId, reason) => request.post(`/v1/relations/blacklist/${targetId}`, null, { params: { reason } }),
  
  // 取消拉黑
  unblockUser: (targetId) => request.delete(`/v1/relations/blacklist/${targetId}`)
}

// ==================== 好友分组相关 ====================
// 后端路径: /api/v1/relations/groups/**
export const groupApi = {
  // 创建分组
  create: (name) => request.post('/v1/relations/groups', null, { params: { name } }),
  
  // 获取分组列表
  getGroups: () => request.get('/v1/relations/groups'),
  
  // 删除分组
  remove: (groupId) => request.delete(`/v1/relations/groups/${groupId}`),
  
  // 移动好友到分组
  moveFriendToGroup: (friendId, groupId) => request.put(`/v1/relations/friends/${friendId}/group`, null, { params: { groupId } })
}

// ==================== 文档相关 ====================
// 后端路径: /api/v1/docs/**
export const docApi = {
  // 获取我的文档列表
  getMyDocs: () => request.get('/v1/docs/my'),
  
  // 创建文档
  create: (title, docType = 'text') => request.post('/v1/docs', null, { params: { title, docType } }),
  
  // 获取文档详情
  getDoc: (docId) => request.get(`/v1/docs/${docId}`),
  
  // 删除文档
  remove: (docId) => request.delete(`/v1/docs/${docId}`),
  
  // 获取协同锁
  acquireLock: (docId, ttlSeconds = 300) => request.post(`/v1/docs/${docId}/lock`, null, { params: { ttlSeconds } }),
  
  // 释放协同锁
  releaseLock: (docId) => request.delete(`/v1/docs/${docId}/lock`)
}

// ==================== 文件相关 ====================
export const fileApi = {
  // 上传文件
  upload: (formData) => request.post('/v1/files/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  }),
  
  // 获取下载链接
  getDownloadUrl: (fileId) => request.get(`/v1/files/${fileId}/download-url`),
  
  // 删除文件
  remove: (fileId) => request.delete(`/v1/files/${fileId}`)
}

export default {
  auth: authApi,
  user: userApi,
  channel: channelApi,
  message: messageApi,
  friend: friendApi,
  group: groupApi,
  doc: docApi,
  file: fileApi
}
