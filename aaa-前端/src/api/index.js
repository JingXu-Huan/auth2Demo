import request from '../utils/request'
import axios from 'axios'

// ==================== 认证相关 ====================
export const authApi = {
  // 登录
  login: async (email, password) => {
    const authUrl = import.meta.env.VITE_AUTH_BASE_URL || 'http://localhost:8080'
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
  getChannels: () => request.get('/im/channels/user'),
  
  // 获取频道详情
  getChannel: (channelId) => request.get(`/im/channels/${channelId}`),
  
  // 创建频道
  createChannel: (data) => request.post('/im/channels', data),
  
  // 获取或创建私聊频道
  getPrivateChannel: (targetUserId) => request.post(`/im/channels/private/${targetUserId}`),
  
  // 获取频道成员
  getMembers: (channelId) => request.get(`/im/channels/${channelId}/members`),
  
  // 添加成员
  addMembers: (channelId, memberIds) => request.post(`/im/channels/${channelId}/members`, memberIds),
  
  // 退出频道
  leave: (channelId) => request.post(`/im/channels/${channelId}/leave`)
}

// ==================== 消息相关 ====================
export const messageApi = {
  // 发送消息
  send: (data) => request.post('/v1/chat/send', data),
  
  // 获取历史消息
  getHistory: (params) => request.get('/v1/chat/history', { params }),
  
  // 撤回消息
  recall: (messageId) => request.post(`/v1/chat/recall/${messageId}`),
  
  // 标记已读
  markRead: (data) => request.post('/v1/chat/messages/read', data),
  
  // 获取已读回执
  getReadReceipts: (messageId) => request.get(`/v1/chat/messages/${messageId}/reads`),
  
  // 添加表情反应
  addReaction: (messageId, emoji) => request.post(`/v1/chat/messages/${messageId}/reactions`, { emoji }),
  
  // 移除表情反应
  removeReaction: (messageId, emoji) => request.delete(`/v1/chat/messages/${messageId}/reactions`, { params: { emoji } }),
  
  // 搜索消息
  search: (params) => request.get('/v1/chat/messages/search', { params }),
  
  // 转发消息
  forward: (messageId, data) => request.post(`/v1/chat/messages/${messageId}/forward`, data)
}

// ==================== 好友相关 ====================
export const friendApi = {
  // 获取好友列表
  getFriends: (userId) => request.get('/v1/friends', { params: { userId } }),
  
  // 发送好友请求
  sendRequest: (data) => request.post('/v1/friends/request', data),
  
  // 获取待处理请求
  getPendingRequests: (userId) => request.get('/v1/friends/pending', { params: { userId } }),
  
  // 接受好友请求
  accept: (userId, requesterId) => request.post('/v1/friends/accept', { userId, requesterId }),
  
  // 拒绝好友请求
  reject: (userId, requesterId) => request.post('/v1/friends/reject', { userId, requesterId }),
  
  // 删除好友
  remove: (userId, friendId) => request.delete(`/v1/friends/${friendId}`, { params: { userId } })
}

// ==================== 群组相关 ====================
export const groupApi = {
  // 创建群组
  create: (data) => request.post('/v1/groups', data),
  
  // 获取群组信息
  getGroup: (groupId) => request.get(`/v1/groups/${groupId}`),
  
  // 获取用户群组列表
  getUserGroups: (userId) => request.get(`/v1/groups/user/${userId}`),
  
  // 添加成员
  addMembers: (groupId, data) => request.post(`/v1/groups/${groupId}/members`, data),
  
  // 移除成员
  removeMember: (groupId, userId) => request.delete(`/v1/groups/${groupId}/members/${userId}`),
  
  // 退出群组
  leave: (groupId, userId) => request.delete(`/v1/groups/${groupId}/members/leave`, { params: { userId } }),
  
  // 解散群组
  dissolve: (groupId) => request.delete(`/v1/groups/${groupId}`)
}

// ==================== 文档相关 ====================
export const docApi = {
  // 获取我的文档
  getMyDocs: () => request.get('/v1/docs/my'),
  
  // 创建文档
  create: (title, docType = 'text') => request.post('/v1/docs', null, { params: { title, docType } }),
  
  // 获取文档
  getDoc: (docId) => request.get(`/v1/docs/${docId}`),
  
  // 删除文档
  remove: (docId) => request.delete(`/v1/docs/${docId}`)
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
