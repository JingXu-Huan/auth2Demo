import axios from 'axios'
import { ElMessage } from 'element-plus'

// 创建axios实例
const api = axios.create({
  baseURL: '/api',
  timeout: 10000
})

// 请求拦截器
api.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

// 响应拦截器
api.interceptors.response.use(
  response => {
    // 直接返回响应数据，让调用方自己处理
    return response.data
  },
  error => {
    // 不在拦截器中显示错误，让调用方自己处理
    // 只记录日志
    console.error('API Error:', error.response?.status, error.message)
    return Promise.reject(error)
  }
)

// 认证服务API (OAuth2-auth-server)
export const authAPI = {
  // 使用 Spring Security 表单登录
  async login(email, password) {
    // 使用 application/x-www-form-urlencoded 格式
    const params = new URLSearchParams();
    params.append('username', email);  // Spring Security 默认字段名是 username
    params.append('password', password);
    
    const response = await axios.post('http://localhost:8080/login', params, {
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded'
      },
      withCredentials: true  // 携带 Cookie（Session）
    });
    
    // 返回处理后的数据（与api拦截器保持一致）
    return response.data;
  },
  
  // 检查邮箱是否存在
  checkEmail: (email) => api.get('/v1/auth/check-email', { params: { email } }),
  // 检查用户名是否存在
  checkUsername: (username) => api.get('/v1/auth/check-username', { params: { username } })
}

// 用户服务API
export const userAPI = {
  // 用户注册
  register: (data) => api.post('/v1/users/register', data),
  
  // 获取用户信息
  getUserInfo: (userId) => api.get(`/v1/users/${userId}`),
  
  // 检查邮箱是否存在
  checkEmail: (email) => api.get('/v1/users/check-email', { params: { email } }),
  
  // 检查用户名是否存在
  checkUsername: (username) => api.get('/v1/users/check-username', { params: { username } }),
  
  // 根据邮箱获取用户详情（用于登录）
  getUserByEmail: (email) => api.get(`/v1/users/details/email/${email}`),
  
  // 发送邮箱验证码
  sendVerificationCode: (email) => api.post('/v1/email/send-code', { email }),
  
  // 验证邮箱验证码
  verifyEmailCode: (email, code) => api.post('/v1/email/verify-code', { email, code }),
  
  // 验证邮箱并激活用户
  verifyAndActivate: (email, code) => api.post('/v1/email/verify-and-activate', { email, code }),
  
  // 检查是否需要安全验证（30天未登录）
  checkSecurityVerification: (email) => api.get('/v1/security/check', { params: { email } }),
  
  // 发送安全验证码
  sendSecurityCode: (email) => api.post('/v1/security/send-code', { email }),
  
  // 验证安全验证码
  verifySecurityCode: (email, code) => api.post('/v1/security/verify-code', { email, code }),
  
  // 更新用户信息
  updateUser: (userId, data) => api.put(`/v1/users/${userId}`, data),
  
  // 修改密码
  changePassword: (userId, data) => api.post(`/v1/users/${userId}/change-password`, data),
  
  // 更新最后登录时间
  updateLastLoginTime: (email) => api.post('/v1/users/update-login-time', null, { params: { email } })
}

// 聊天相关API
export const chatAPI = {
  // 发送消息
  sendMessage: (data) => api.post('/v1/chat/send', data),
  
  // 获取聊天历史
  getChatHistory: (params) => api.get('/v1/chat/history', { params }),
  
  // 标记已读
  markAsRead: (data) => api.post('/v1/chat/read', data),
  
  // 撤回消息
  recallMessage: (messageId) => api.post(`/v1/chat/recall/${messageId}`),
  
  // 批量已读
  batchRead: (data) => api.post('/v1/chat/batch-read', data),
  
  // 获取未读摘要
  getUnreadSummary: (userId) => api.get(`/v1/chat/unread-summary/${userId}`),
  
  // 转发消息
  forwardMessage: (data) => api.post('/v1/chat/forward', data),
  
  // 收藏消息
  favoriteMessage: (data) => api.post('/v1/chat/favorite', data),
  
  // 置顶消息
  pinMessage: (data) => api.post('/v1/chat/pin', data),
  
  // 正在输入
  typing: (data) => api.post('/v1/chat/typing', data),
  
  // 获取在线用户数
  getOnlineUsers: () => api.get('/v1/chat/online-users')
}

// 组织架构服务API (8011端口)
export const organizationAPI = {
  // 部门管理
  createDepartment: (data) => api.post('/v1/organization/departments', data),
  getDepartment: (departmentId) => api.get(`/v1/organization/departments/${departmentId}`),
  updateDepartment: (departmentId, data) => api.put(`/v1/organization/departments/${departmentId}`, data),
  deleteDepartment: (departmentId) => api.delete(`/v1/organization/departments/${departmentId}`),
  getDepartmentTree: () => api.get('/v1/organization/departments/tree'),
  
  // 部门成员管理
  addDepartmentMember: (departmentId, data) => api.post(`/v1/organization/departments/${departmentId}/members`, data),
  removeDepartmentMember: (departmentId, userId) => api.delete(`/v1/organization/departments/${departmentId}/members/${userId}`),
  getDepartmentMembers: (departmentId, params) => api.get(`/v1/organization/departments/${departmentId}/members`, { params }),
  setDepartmentLeader: (departmentId, data) => api.put(`/v1/organization/departments/${departmentId}/leader`, data),
  
  // 组织通讯录
  getContacts: (params) => api.get('/v1/organization/contacts', { params })
}

// 日程服务API (8012端口)
export const calendarAPI = {
  // 日程管理
  createEvent: (data) => api.post('/v1/calendar/events', data),
  getEvent: (eventId) => api.get(`/v1/calendar/events/${eventId}`),
  updateEvent: (eventId, data) => api.put(`/v1/calendar/events/${eventId}`, data),
  deleteEvent: (eventId) => api.delete(`/v1/calendar/events/${eventId}`),
  getEvents: (params) => api.get('/v1/calendar/events', { params }),
  
  // 日程参与者
  inviteParticipants: (eventId, data) => api.post(`/v1/calendar/events/${eventId}/participants`, data),
  respondToInvitation: (eventId, data) => api.put(`/v1/calendar/events/${eventId}/response`, data),
  
  // 日历视图
  getCalendarView: (params) => api.get('/v1/calendar/view', { params }),
  
  // 忙碌状态
  getBusyStatus: (params) => api.get('/v1/calendar/busy', { params })
}

// 任务服务API (8013端口)
export const taskAPI = {
  // 任务管理
  createTask: (data) => api.post('/v1/tasks', data),
  getTask: (taskId) => api.get(`/v1/tasks/${taskId}`),
  updateTask: (taskId, data) => api.put(`/v1/tasks/${taskId}`, data),
  deleteTask: (taskId) => api.delete(`/v1/tasks/${taskId}`),
  getTasks: (params) => api.get('/v1/tasks', { params }),
  
  // 任务分配
  assignTask: (taskId, data) => api.post(`/v1/tasks/${taskId}/assign`, data),
  updateTaskStatus: (taskId, data) => api.put(`/v1/tasks/${taskId}/status`, data),
  
  // 任务评论
  addTaskComment: (taskId, data) => api.post(`/v1/tasks/${taskId}/comments`, data),
  getTaskComments: (taskId) => api.get(`/v1/tasks/${taskId}/comments`)
}

// 会议服务API (8014端口)
export const meetingAPI = {
  // 会议管理
  createMeeting: (data) => api.post('/v1/meetings', data),
  getMeeting: (meetingId) => api.get(`/v1/meetings/${meetingId}`),
  updateMeeting: (meetingId, data) => api.put(`/v1/meetings/${meetingId}`, data),
  deleteMeeting: (meetingId) => api.delete(`/v1/meetings/${meetingId}`),
  getMeetings: (params) => api.get('/v1/meetings', { params }),
  
  // 会议参与
  joinMeeting: (meetingId) => api.post(`/v1/meetings/${meetingId}/join`),
  leaveMeeting: (meetingId) => api.post(`/v1/meetings/${meetingId}/leave`),
  
  // 会议录制
  startRecording: (meetingId) => api.post(`/v1/meetings/${meetingId}/recording/start`),
  stopRecording: (meetingId) => api.post(`/v1/meetings/${meetingId}/recording/stop`)
}

// 文档服务API (8017端口)
export const documentAPI = {
  // 文档管理
  createDocument: (data) => api.post('/v1/documents', data),
  getDocument: (documentId) => api.get(`/v1/documents/${documentId}`),
  updateDocument: (documentId, data) => api.put(`/v1/documents/${documentId}`, data),
  deleteDocument: (documentId) => api.delete(`/v1/documents/${documentId}`),
  getDocuments: (params) => api.get('/v1/documents', { params }),
  
  // 文档协作
  shareDocument: (documentId, data) => api.post(`/v1/documents/${documentId}/share`, data),
  getDocumentHistory: (documentId) => api.get(`/v1/documents/${documentId}/history`),
  
  // 文档评论
  addDocumentComment: (documentId, data) => api.post(`/v1/documents/${documentId}/comments`, data),
  getDocumentComments: (documentId) => api.get(`/v1/documents/${documentId}/comments`)
}

// 文件服务API (8005端口)
export const fileAPI = {
  // 文件上传下载
  uploadFile: (formData) => api.post('/v1/files/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  }),
  downloadFile: (fileId) => api.get(`/v1/files/${fileId}/download`, { responseType: 'blob' }),
  getFileInfo: (fileId) => api.get(`/v1/files/${fileId}`),
  deleteFile: (fileId) => api.delete(`/v1/files/${fileId}`),
  
  // 文件预览
  previewFile: (fileId) => api.get(`/v1/files/${fileId}/preview`),
  
  // 文件管理
  getFiles: (params) => api.get('/v1/files', { params }),
  createFolder: (data) => api.post('/v1/files/folders', data),
  moveFile: (fileId, data) => api.put(`/v1/files/${fileId}/move`, data)
}

// 搜索服务API (8007端口)
export const searchAPI = {
  // 全局搜索
  globalSearch: (params) => api.get('/v1/search/global', { params }),
  
  // 消息搜索
  searchMessages: (params) => api.get('/v1/search/messages', { params }),
  
  // 文件搜索
  searchFiles: (params) => api.get('/v1/search/files', { params }),
  
  // 联系人搜索
  searchContacts: (params) => api.get('/v1/search/contacts', { params })
}

// 通知服务API (8018端口)
export const notificationAPI = {
  // 通知管理
  getNotifications: (params) => api.get('/v1/notifications', { params }),
  markAsRead: (notificationId) => api.put(`/v1/notifications/${notificationId}/read`),
  markAllAsRead: () => api.put('/v1/notifications/read-all'),
  deleteNotification: (notificationId) => api.delete(`/v1/notifications/${notificationId}`),
  
  // 通知设置
  getNotificationSettings: () => api.get('/v1/notifications/settings'),
  updateNotificationSettings: (data) => api.put('/v1/notifications/settings', data)
}

// 群组相关API
export const groupAPI = {
  // 创建群组
  createGroup: (data) => api.post('/v1/groups', data),
  
  // 获取群组信息
  getGroup: (groupId) => api.get(`/v1/groups/${groupId}`),
  
  // 更新群组信息
  updateGroup: (groupId, data) => api.put(`/v1/groups/${groupId}`, data),
  
  // 解散群组
  dissolveGroup: (groupId) => api.delete(`/v1/groups/${groupId}`),
  
  // 添加成员
  addMembers: (groupId, data) => api.post(`/v1/groups/${groupId}/members`, data),
  
  // 移除成员
  removeMember: (groupId, userId) => api.delete(`/v1/groups/${groupId}/members/${userId}`),
  
  // 获取成员列表
  getMembers: (groupId, params) => api.get(`/v1/groups/${groupId}/members`, { params }),
  
  // 设置管理员
  setAdmin: (groupId, data) => api.post(`/v1/groups/${groupId}/admins`, data),
  
  // 用户退出群组
  leaveGroup: (groupId, userId) => api.delete(`/v1/groups/${groupId}/members/leave`, { params: { userId } }),
  
  // 搜索群成员
  searchMembers: (groupId, params) => api.get(`/v1/groups/${groupId}/members/search`, { params }),
  
  // 获取用户群组列表
  getUserGroups: (userId, params) => api.get(`/v1/groups/user/${userId}`, { params }),
  
  // 转让群主
  transferOwnership: (groupId, params) => api.post(`/v1/groups/${groupId}/transfer`, null, { params })
}

// 好友服务API
export const friendAPI = {
  // 发送好友请求
  sendFriendRequest: (data) => api.post('/v1/friends/request', data),
  
  // 接受好友请求
  acceptFriendRequest: (fromUserId, toUserId) => api.post('/v1/friends/request/accept', null, { params: { fromUserId, toUserId } }),
  
  // 拒绝好友请求
  rejectFriendRequest: (fromUserId, toUserId) => api.post('/v1/friends/request/reject', null, { params: { fromUserId, toUserId } }),
  
  // 获取收到的好友请求列表
  getReceivedRequests: (userId) => api.get(`/v1/friends/requests/received/${userId}`),
  
  // 获取好友列表
  getFriends: (userId) => api.get(`/v1/friends/${userId}`),
  
  // 删除好友
  deleteFriend: (userId, friendId) => api.delete(`/v1/friends/${userId}/friends/${friendId}`),
  
  // 搜索好友
  searchFriends: (userId, keyword, limit = 20) => api.get(`/v1/friends/${userId}/search`, { params: { keyword, limit } }),
  
  // 搜索用户（通过邮箱或手机号）- 直接调用 User-server
  searchUser: (searchType, keyword) => api.get('/v1/users/search', { params: { searchType, keyword } })
}

export default api
