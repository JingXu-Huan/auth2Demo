import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useChatStore = defineStore('chat', () => {
  // 会话列表
  const conversations = ref([])
  // 当前选中的会话
  const currentConversation = ref(null)
  // 消息列表 (按会话ID索引)
  const messagesMap = ref({})
  // 未读消息数
  const unreadCount = ref(0)

  // 计算属性
  const currentMessages = computed(() => {
    if (!currentConversation.value) return []
    return messagesMap.value[currentConversation.value.id] || []
  })

  const totalUnread = computed(() => {
    return conversations.value.reduce((sum, conv) => sum + (conv.unreadCount || 0), 0)
  })

  // 设置会话列表
  const setConversations = (list) => {
    conversations.value = list
  }

  // 添加会话
  const addConversation = (conversation) => {
    const index = conversations.value.findIndex(c => c.id === conversation.id)
    if (index === -1) {
      conversations.value.unshift(conversation)
    } else {
      conversations.value[index] = conversation
    }
  }

  // 选择会话
  const selectConversation = (conversation) => {
    currentConversation.value = conversation
    // 清除未读数
    if (conversation) {
      const conv = conversations.value.find(c => c.id === conversation.id)
      if (conv) {
        conv.unreadCount = 0
      }
    }
  }

  // 设置消息列表
  const setMessages = (conversationId, messages) => {
    messagesMap.value[conversationId] = messages
  }

  // 添加消息
  const addMessage = (conversationId, message) => {
    if (!messagesMap.value[conversationId]) {
      messagesMap.value[conversationId] = []
    }
    
    // 去重检查：根据消息ID判断是否已存在
    const exists = messagesMap.value[conversationId].some(m => 
      String(m.id) === String(message.id)
    )
    if (exists) {
      console.log('[ChatStore] 消息已存在，跳过:', message.id)
      return
    }
    
    messagesMap.value[conversationId].push(message)
    
    // 更新会话的最后一条消息
    const conv = conversations.value.find(c => c.id === conversationId)
    if (conv) {
      conv.lastMessage = message.content
      conv.lastMessageTime = message.createdAt || new Date().toISOString()
      
      // 如果不是当前会话，增加未读数
      if (currentConversation.value?.id !== conversationId) {
        conv.unreadCount = (conv.unreadCount || 0) + 1
      }
      
      // 将会话移到顶部
      const index = conversations.value.indexOf(conv)
      if (index > 0) {
        conversations.value.splice(index, 1)
        conversations.value.unshift(conv)
      }
    }
  }

  // 更新消息状态
  const updateMessage = (conversationId, messageId, updates) => {
    const messages = messagesMap.value[conversationId]
    if (messages) {
      const msg = messages.find(m => m.id === messageId || m.tempId === messageId)
      if (msg) {
        Object.assign(msg, updates)
      }
    }
  }

  // 删除消息
  const deleteMessage = (conversationId, messageId) => {
    const messages = messagesMap.value[conversationId]
    if (messages) {
      const index = messages.findIndex(m => m.id === messageId)
      if (index > -1) {
        messages.splice(index, 1)
      }
    }
  }

  // 清空数据
  const clear = () => {
    conversations.value = []
    currentConversation.value = null
    messagesMap.value = {}
    unreadCount.value = 0
  }

  return {
    conversations,
    currentConversation,
    messagesMap,
    unreadCount,
    currentMessages,
    totalUnread,
    setConversations,
    addConversation,
    selectConversation,
    setMessages,
    addMessage,
    updateMessage,
    deleteMessage,
    clear
  }
})
