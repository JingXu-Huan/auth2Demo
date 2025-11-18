import Dexie from 'dexie'

// 全局聊天本地数据库：用于会话列表和消息历史本地缓存
const chatDb = new Dexie('IMChatDB')

chatDb.version(1).stores({
  // 会话表
  // conversationId: 会话 ID（单聊：userA-userB，群聊：GROUP:groupId）
  // lastMessageTime: 最后一条消息时间戳（用于排序）
  // isPinned: 是否置顶
  conversations: '&conversationId, lastMessageTime, isPinned',

  // 消息表
  // messageId: 服务器生成的消息 ID（作为主键）
  // conversationId + createdAt: 用于按会话和时间维度查询
  messages: '&messageId, conversationId, createdAt, [conversationId+createdAt]',

  // 系统通知表
  // id: 自动生成的通知 ID
  // timestamp: 通知时间戳
  // isRead: 是否已读
  // eventType: 通知类型
  // conversationId: 关联的会话 ID
  notifications: '++id, timestamp, isRead, eventType, conversationId'
})

export default chatDb
