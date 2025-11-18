import api from './index'

/**
 * 聊天消息服务 API
 * 对应后端: IM-message-server (8002端口)
 * 路径前缀: /api/v1/chat
 */
export const chatMessageAPI = {
  // ==================== 消息发送 ====================
  
  /**
   * 发送消息
   * @param {Object} message - 消息对象
   * @param {string} message.senderId - 发送者ID
   * @param {string} message.receiverId - 接收者ID (单聊)
   * @param {string} message.groupId - 群组ID (群聊)
   * @param {string} message.channelType - 频道类型: PRIVATE/GROUP
   * @param {string} message.contentType - 内容类型: TEXT/IMAGE/VIDEO/AUDIO/FILE
   * @param {Object} message.payload - 消息内容
   */
  sendMessage: (message) => api.post('/v1/chat/send', message),

  // ==================== 消息历史 ====================
  
  /**
   * 获取聊天历史
   * @param {string} conversationId - 会话ID
   * @param {number} cursor - 游标（时间戳）
   * @param {number} size - 每页大小，默认50
   */
  getHistory: (conversationId, cursor = null, size = 50) => 
    api.get('/v1/chat/history', { 
      params: { conversationId, cursor, size } 
    }),

  /**
   * 获取过滤后的历史消息（排除用户已删除）
   * @param {string} conversationId - 会话ID
   * @param {string} userId - 用户ID
   * @param {number} cursor - 游标
   * @param {number} size - 每页大小
   */
  getHistoryFiltered: (conversationId, userId, cursor = null, size = 50) =>
    api.get('/v1/chat/history/filtered', {
      params: { conversationId, userId, cursor, size }
    }),

  // ==================== 消息状态 ====================
  
  /**
   * 标记消息已送达
   * @param {string} messageId - 消息ID
   */
  ackDelivered: (messageId) => 
    api.post('/v1/chat/ack-delivered', null, { params: { messageId } }),

  /**
   * 标记消息已读
   * @param {string} messageId - 消息ID
   * @param {string} userId - 用户ID（群聊已读回执）
   */
  ackRead: (messageId, userId = null) => 
    api.post('/v1/chat/ack-read', null, { params: { messageId, userId } }),

  /**
   * 批量标记已读
   * @param {Array<string>} messageIds - 消息ID数组
   */
  readBatch: (messageIds) => 
    api.post('/v1/chat/read-batch', { messageIds }),

  /**
   * 获取已读详情
   * @param {string} messageId - 消息ID
   */
  getReadDetail: (messageId) => 
    api.get('/v1/chat/read/detail', { params: { messageId } }),

  // ==================== 消息操作 ====================
  
  /**
   * 撤回消息
   * @param {string} messageId - 消息ID
   */
  recallMessage: (messageId) => 
    api.post('/v1/chat/recall', null, { params: { messageId } }),

  /**
   * 转发消息
   * @param {Object} request - 转发请求
   * @param {string} request.messageId - 原消息ID
   * @param {Array<string>} request.targetUserIds - 目标用户ID列表
   * @param {Array<string>} request.targetGroupIds - 目标群组ID列表
   */
  forwardMessage: (request) => 
    api.post('/v1/chat/forward', request),

  /**
   * 删除消息（用户本地删除）
   * @param {Object} request - 删除请求
   * @param {string} request.userId - 用户ID
   * @param {Array<string>} request.messageIds - 消息ID列表
   */
  deleteMessages: (request) => 
    api.post('/v1/chat/delete', request),

  // ==================== 消息收藏 ====================
  
  /**
   * 收藏/取消收藏消息
   * @param {Object} request - 收藏请求
   * @param {string} request.userId - 用户ID
   * @param {string} request.messageId - 消息ID
   * @param {string} request.action - 操作: ADD/REMOVE
   */
  favoriteMessage: (request) => 
    api.post('/v1/chat/favorite', request),

  /**
   * 获取收藏列表
   * @param {string} userId - 用户ID
   * @param {string} conversationId - 会话ID（可选）
   * @param {number} page - 页码
   * @param {number} size - 每页大小
   */
  getFavorites: (userId, conversationId = null, page = 1, size = 20) =>
    api.get('/v1/chat/favorites', {
      params: { userId, conversationId, page, size }
    }),

  // ==================== 消息置顶 ====================
  
  /**
   * 置顶/取消置顶消息
   * @param {Object} request - 置顶请求
   * @param {string} request.userId - 用户ID
   * @param {string} request.conversationId - 会话ID
   * @param {string} request.messageId - 消息ID
   * @param {string} request.action - 操作: ADD/REMOVE
   */
  pinMessage: (request) => 
    api.post('/v1/chat/pin', request),

  /**
   * 获取置顶消息列表
   * @param {string} userId - 用户ID
   * @param {string} conversationId - 会话ID
   */
  getPinnedMessages: (userId, conversationId) =>
    api.get('/v1/chat/pins', { params: { userId, conversationId } }),

  // ==================== 未读消息 ====================
  
  /**
   * 获取未读消息摘要
   * @param {string} userId - 用户ID
   */
  getUnreadSummary: (userId) => 
    api.get('/v1/chat/unread/summary', { params: { userId } }),

  // ==================== 消息搜索 ====================
  
  /**
   * 在会话内搜索消息
   * @param {string} conversationId - 会话ID
   * @param {string} senderId - 发送者ID（可选）
   * @param {string} contentType - 内容类型（可选）
   * @param {string} keyword - 关键字（可选）
   * @param {number} page - 页码
   * @param {number} size - 每页大小
   */
  searchMessages: (conversationId, senderId = null, contentType = null, keyword = null, page = 1, size = 20) =>
    api.get('/v1/chat/search', {
      params: { conversationId, senderId, contentType, keyword, page, size }
    }),

  /**
   * 全局搜索消息
   * @param {string} userId - 用户ID
   * @param {string} keyword - 关键字（可选）
   * @param {number} page - 页码
   * @param {number} size - 每页大小
   */
  searchGlobal: (userId, keyword = null, page = 1, size = 20) =>
    api.get('/v1/chat/search/global', {
      params: { userId, keyword, page, size }
    }),

  // ==================== 实时状态 ====================
  
  /**
   * 发送正在输入状态
   * @param {Object} request - 输入状态请求
   * @param {string} request.conversationId - 会话ID
   * @param {string} request.userId - 用户ID
   * @param {boolean} request.typing - 是否正在输入
   */
  sendTyping: (request) => 
    api.post('/v1/chat/typing', request),

  /**
   * 获取在线用户数
   */
  getOnlineUsers: () => 
    api.get('/v1/chat/online-users')
}

export default chatMessageAPI
