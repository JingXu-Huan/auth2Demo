<template>
  <div class="messages-container">
    <!-- 左侧会话列表 -->
    <div class="conversation-list">
      <div class="search-bar">
        <el-input 
          v-model="searchKeyword" 
          placeholder="搜索会话..."
          prefix-icon="Search"
          @input="handleSearch"
        />
      </div>
      
      <div class="conversations">
        <div 
          v-for="conversation in filteredConversations" 
          :key="conversation.id"
          :class="['conversation-item', { active: selectedConversation?.id === conversation.id }]"
          @click="selectConversation(conversation)"
        >
          <el-avatar :src="conversation.avatar" :size="40">
            {{ conversation.name.charAt(0) }}
          </el-avatar>
          <div class="conversation-info">
            <div class="conversation-header">
              <span class="name">{{ conversation.name }}</span>
              <span class="time">{{ formatTime(conversation.lastMessageTime) }}</span>
            </div>
            <div class="last-message">
              <span class="content">{{ conversation.lastMessage }}</span>
              <el-badge 
                v-if="conversation.unreadCount > 0" 
                :value="conversation.unreadCount" 
                class="unread-badge"
              />
            </div>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 右侧聊天区域 -->
    <div class="chat-area" v-if="selectedConversation">
      <div class="chat-header">
        <div class="chat-info">
          <el-avatar :src="selectedConversation.avatar" :size="32">
            {{ selectedConversation.name.charAt(0) }}
          </el-avatar>
          <div class="chat-details">
            <h3>{{ selectedConversation.name }}</h3>
            <span class="status">
              <template v-if="selectedConversation.type === 'group'">
                {{ `${selectedConversation.memberCount} 人` }}
              </template>
              <template v-else>
                {{ isTyping ? '对方正在输入...' : '在线' }}
              </template>
            </span>
          </div>
        </div>
        
        <div class="chat-actions">
          <el-button circle>
            <el-icon><Phone /></el-icon>
          </el-button>
          <el-button circle>
            <el-icon><VideoCamera /></el-icon>
          </el-button>
          <el-button circle>
            <el-icon><MoreFilled /></el-icon>
          </el-button>
          <el-button
            v-if="selectedConversation.type === 'group'"
            size="small"
            @click="openInviteDialog"
          >
            邀请好友
          </el-button>
        </div>
      </div>
      
      <div class="message-list" ref="messageListRef">
        <div 
          v-for="message in messages" 
          :key="message.id"
          :class="['message-item', { 'own-message': isOwnMessage(message) }]"
        >
          <el-avatar :src="message.senderAvatar" :size="32" />
          <div class="message-content">
            <div class="message-header">
              <span class="sender-name">{{ message.senderName }}</span>
              <span class="message-time">{{ formatTime(message.timestamp) }}</span>
            </div>
            <div class="message-body">
              <div v-if="message.type === 'text'" class="text-message">
                {{ message.content || '[空消息]' }}
              </div>
              <div v-else-if="message.type === 'image'" class="image-message">
                <el-image :src="message.content" fit="cover" />
              </div>
              <div v-else-if="message.type === 'file'" class="file-message">
                <el-icon><Document /></el-icon>
                <span>{{ message.fileName }}</span>
              </div>
              <!-- 群聊邀请系统消息 -->
              <div
                v-else-if="message.type === 'system' && message.systemType === 'group_invite'"
                class="system-message invite-message"
              >
                <div class="invite-title">群聊邀请</div>
                <div class="invite-content">
                  <div class="invite-text">
                    {{ message.inviterName || `用户${message.inviterId}` }} 邀请你加入群聊「{{ message.groupName || message.content }}」
                  </div>
                  <div class="invite-actions">
                    <el-button type="primary" size="small" @click="acceptGroupInvite(message)">
                      同意加入
                    </el-button>
                    <el-button size="small" @click="rejectGroupInvite(message)">
                      拒绝
                    </el-button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
      
      <div class="input-area">
        <div class="input-toolbar">
          <el-button circle size="small">
            <el-icon><Picture /></el-icon>
          </el-button>
          <el-button circle size="small">
            <el-icon><Paperclip /></el-icon>
          </el-button>
          <el-button circle size="small">
            <el-icon><Microphone /></el-icon>
          </el-button>
        </div>
        
        <div class="input-box">
          <el-input
            v-model="messageInput"
            type="textarea"
            :rows="3"
            placeholder="输入消息..."
            @keydown.enter.prevent="handleSendMessage"
            @input="handleTyping"
          />
          <div class="input-actions">
            <el-button type="primary" @click="handleSendMessage" :disabled="!messageInput.trim()">
              发送
            </el-button>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 空状态 -->
    <div v-else class="empty-state">
      <el-empty description="选择一个会话开始聊天" />
    </div>

    <!-- 邀请好友加入群聊对话框 -->
    <el-dialog v-model="showInviteDialog" title="邀请好友加入群聊" width="480px">
      <div v-if="inviteCandidates.length === 0">
        <el-empty description="没有可邀请的好友" />
      </div>
      <div v-else class="invite-friend-list">
        <el-checkbox-group v-model="selectedInviteFriendIds">
          <div
            v-for="friend in inviteCandidates"
            :key="friend.userId"
            class="invite-friend-item"
          >
            <el-checkbox :label="friend.userId">
              <el-avatar :src="friend.avatar" :size="32">
                {{ friend.nickname?.charAt(0) || 'U' }}
              </el-avatar>
              <span class="friend-name">{{ friend.nickname || friend.username || `用户${friend.userId}` }}</span>
            </el-checkbox>
          </div>
        </el-checkbox-group>
      </div>

      <template #footer>
        <el-button @click="showInviteDialog = false">取消</el-button>
        <el-button
          type="primary"
          :loading="inviteLoading"
          :disabled="selectedInviteFriendIds.length === 0"
          @click="sendGroupInvites"
        >
          发送邀请
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../stores/user'
import { chatAPI, groupAPI, friendAPI } from '../api'
import websocketService from '../utils/websocket'
import chatDb from '../db/chatDb'
import { 
  Search, 
  Phone, 
  VideoCamera, 
  MoreFilled, 
  Document, 
  Picture, 
  Paperclip, 
  Microphone 
} from '@element-plus/icons-vue'
import dayjs from 'dayjs'

const userStore = useUserStore()
const route = useRoute()
const router = useRouter()
const messageListRef = ref()
const searchKeyword = ref('')
const selectedConversation = ref(null)
const messageInput = ref('')
const messages = ref([])
const isTyping = ref(false)

let typingTimer = null

// 群聊邀请相关状态
const showInviteDialog = ref(false)
const inviteCandidates = ref([])            // 可邀请的好友列表
const selectedInviteFriendIds = ref([])     // 选中的好友 userId 列表
const inviteLoading = ref(false)

// 会话数据（从接口加载）
const conversations = ref([])

const loadMessagesFromLocal = async (conversationId) => {
  try {
    return await chatDb.messages
      .where('conversationId')
      .equals(conversationId)
      .sortBy('createdAt')
  } catch (error) {
    console.error('从本地加载消息失败:', error)
    return []
  }
}

const saveMessagesToLocal = async (messageList) => {
  try {
    if (!messageList || !messageList.length) return
    await chatDb.messages.bulkPut(messageList)
  } catch (error) {
    console.error('保存消息到本地失败:', error)
  }
}

// 加载会话列表
const loadConversations = async () => {
  try {
    if (!userStore.user || !userStore.user.id) return

    // 1. 优先从本地 IndexedDB 加载会话列表
    try {
      const localConversations = await chatDb.conversations.toArray()
      if (localConversations && localConversations.length > 0) {
        conversations.value = localConversations.sort(
          (a, b) => (b.lastMessageTime || 0) - (a.lastMessageTime || 0)
        )
        return
      }
    } catch (e) {
      console.error('从本地加载会话列表失败:', e)
    }

    // 2. 本地没有记录时，从后端加载群聊列表作为初始化数据
    const groupResponse = await groupAPI.getUserGroups(userStore.user.id, { page: 1, size: 50 })

    if (groupResponse.data && groupResponse.data.groups) {
      const groupConversations = groupResponse.data.groups.map(group => ({
        id: group.groupId,
        name: group.name,
        avatar: group.avatar || '',
        type: 'group',
        memberCount: group.memberCount,
        lastMessage: '',
        lastMessageTime: new Date(group.createdAt).getTime(),
        unreadCount: 0,
        conversationId: `GROUP:${group.groupId}`,
        isPinned: false
      }))

      conversations.value = groupConversations

      // 同步到本地会话表
      try {
        await chatDb.conversations.bulkPut(groupConversations)
      } catch (e) {
        console.error('保存群聊会话到本地失败:', e)
      }
    }
  } catch (error) {
    console.error('加载会话列表失败:', error)
  }
}

// 过滤会话
const filteredConversations = computed(() => {
  let filtered = conversations.value

  // 按搜索关键词过滤
  if (searchKeyword.value) {
    filtered = filtered.filter(conv => 
      conv.name.toLowerCase().includes(searchKeyword.value.toLowerCase()) ||
      conv.lastMessage.toLowerCase().includes(searchKeyword.value.toLowerCase())
    )
  }

  return filtered.sort((a, b) => b.lastMessageTime - a.lastMessageTime)
})

const buildConversationId = (conversation) => {
  if (!conversation || !userStore.user || !userStore.user.id) {
    return null
  }
  if (conversation.type === 'private') {
    const userIds = [String(userStore.user.id), String(conversation.id)].sort()
    return userIds.join('-')
  }
  if (conversation.type === 'group') {
    return `GROUP:${conversation.id}`
  }
  return null
}

// 保存会话到本地 IndexedDB
const saveConversationToLocal = async (conversation) => {
  try {
    const conversationId = buildConversationId(conversation)
    if (!conversationId) return

    await chatDb.conversations.put({
      ...conversation,
      conversationId,
      lastMessageTime: conversation.lastMessageTime || Date.now(),
      isPinned: conversation.isPinned || false
    })
  } catch (error) {
    console.error('保存会话到本地失败:', error)
  }
}

// 选择会话
const selectConversation = async (conversation) => {
  selectedConversation.value = conversation
  
  // 清除未读数
  conversation.unreadCount = 0
  saveConversationToLocal(conversation).catch(err => {
    console.error('更新本地会话未读数失败:', err)
  })
  
  // 加载消息历史
  await loadMessages(conversation)
}

// 加载消息
const loadMessages = async (conversation) => {
  try {
    const conversationId = buildConversationId(conversation)
    if (!conversationId) {
      messages.value = []
      return
    }

    const localList = await loadMessagesFromLocal(conversationId)
    if (localList.length > 0) {
      messages.value = localList
      nextTick(() => {
        scrollToBottom()
      })
      return
    }

    const res = await chatAPI.getChatHistory({
      conversationId: conversationId,
      size: 50
    })

    const rawList = Array.isArray(res?.data) ? res.data : []

    const normalized = rawList
      .map(msg => {
        const contentType = (msg.contentType || 'TEXT').toUpperCase()
        let content = ''
        let systemType = null
        let groupId = null
        let groupName = null
        let inviterId = null
        let inviterName = null
        
        // 从 payload 中提取内容
        if (msg.payload) {
          if (typeof msg.payload === 'string') {
            try {
              const parsed = JSON.parse(msg.payload)
              content = parsed.text || parsed.content || msg.payload
            } catch (e) {
              content = msg.payload
            }
          } else if (typeof msg.payload === 'object') {
            // 群聊邀请系统消息
            if (contentType === 'SYSTEM' && msg.payload.type === 'GROUP_INVITE') {
              systemType = 'group_invite'
              groupId = msg.payload.groupId
              groupName = msg.payload.groupName
              inviterId = msg.payload.inviterId
              inviterName = msg.payload.inviterName
              content = msg.payload.text || msg.payload.content || groupName || ''
            } else if (contentType === 'TEXT') {
              content = msg.payload.text || msg.payload.content || ''
            } else {
              content = msg.payload.url || msg.payload.imageUrl || msg.payload.videoUrl || msg.payload.audioUrl || msg.payload.fileUrl || ''
            }
          }
        }
        
        if (!content && msg.content) {
          content = msg.content
        }
        
        return {
          id: msg.messageId,
          messageId: msg.messageId,
          conversationId,
          senderId: msg.senderId,
          senderName: msg.senderName || `用户${msg.senderId}`,
          senderAvatar: '',
          type: contentType.toLowerCase(),
          content: content,
          timestamp: msg.createdAt,
          createdAt: msg.createdAt,
          systemType,
          groupId,
          groupName,
          inviterId,
          inviterName
        }
      })
      .sort((a, b) => a.timestamp - b.timestamp)

    messages.value = normalized
    await saveMessagesToLocal(normalized)

    nextTick(() => {
      scrollToBottom()
    })
  } catch (error) {
    console.error('加载消息失败:', error)
    // 使用模拟数据作为后备
    messages.value = []
  }
}

// 发送消息
const handleSendMessage = async () => {
  if (!messageInput.value.trim() || !selectedConversation.value) return

  if (!userStore.user || !userStore.user.id) {
    ElMessage.error('用户信息缺失，请重新登录后再试')
    return
  }

  const conversationId = buildConversationId(selectedConversation.value)
  if (!conversationId) {
    ElMessage.error('会话ID生成失败')
    return
  }

  const now = Date.now()
  const newMessage = {
    id: now,
    messageId: null,
    conversationId,
    senderId: userStore.user.id,
    senderName: userStore.user.nickname,
    senderAvatar: userStore.user.avatar,
    type: 'text',
    content: messageInput.value.trim(),
    timestamp: now,
    createdAt: now,
    status: 'sending'
  }

  
  messages.value.push(newMessage)

  // 更新会话最后消息
  selectedConversation.value.lastMessage = messageInput.value.trim()
  selectedConversation.value.lastMessageTime = Date.now()

   // 同步会话摘要到本地
  saveConversationToLocal(selectedConversation.value).catch(error => {
    console.error('更新本地会话失败:', error)
  })

  messageInput.value = ''

  nextTick(() => {
    scrollToBottom()
  })

  try {
    const channelType = selectedConversation.value.type === 'private' ? 'PRIVATE' : 'GROUP'

    const response = await chatAPI.sendMessage({
      senderId: String(userStore.user.id),
      receiverId: channelType === 'PRIVATE' ? String(selectedConversation.value.id) : null,
      groupId: channelType === 'GROUP' ? String(selectedConversation.value.id) : null,
      conversationId,
      channelType,
      contentType: 'TEXT',
      payload: { text: newMessage.content }
    })

    if (response.success && response.data) {
      const serverMsg = response.data
      const createdAt = serverMsg.createdAt || newMessage.createdAt
      newMessage.id = serverMsg.messageId
      newMessage.messageId = serverMsg.messageId
      newMessage.timestamp = createdAt
      newMessage.createdAt = createdAt
      newMessage.status = 'sent'

      await chatDb.messages.put({
        ...newMessage
      })

      console.log('消息发送成功')
    } else {
      newMessage.status = 'failed'
    }
  } catch (error) {
    console.error('发送消息失败:', error)
    newMessage.status = 'failed'
    ElMessage.error('发送消息失败')
  }
}

// 处理输入
const handleTyping = () => {
  // 仅在私聊会话下发送 typing 信令
  if (!selectedConversation.value || !userStore.user || !userStore.user.id) return
  if (selectedConversation.value.type !== 'private') return

  const conversationId = buildConversationId(selectedConversation.value)
  if (!conversationId) return

  chatAPI.typing({
    fromUserId: String(userStore.user.id),
    toUserId: String(selectedConversation.value.id),
    conversationId,
    typing: true
  }).catch(console.error)
}

// 搜索处理
const handleSearch = () => {
  // 搜索逻辑已在computed中处理
}

// 开始聊天
const startChat = (friend) => {
  router.push('/im/messages')
}

// 滚动到底部
const scrollToBottom = () => {
  if (messageListRef.value) {
    messageListRef.value.scrollTop = messageListRef.value.scrollHeight
  }
}

// 打开邀请好友对话框
const openInviteDialog = async () => {
  if (!selectedConversation.value || selectedConversation.value.type !== 'group') {
    ElMessage.warning('请先选择一个群聊')
    return
  }

  if (!userStore.user || !userStore.user.id) {
    ElMessage.error('用户信息缺失，请重新登录')
    return
  }

  try {
    inviteLoading.value = true

    // 加载好友列表
    const friendRes = await friendAPI.getFriends(userStore.user.id)
    const friends = friendRes.code === 200 && Array.isArray(friendRes.data)
      ? friendRes.data
      : []

    // 加载当前群成员，过滤掉已在群中的好友
    const groupId = selectedConversation.value.id
    let memberIds = []
    try {
      const memberRes = await groupAPI.getMembers(groupId, { page: 1, size: 500 })
      const members = memberRes.code === 200 && memberRes.data
        ? (memberRes.data.members || memberRes.data.records || [])
        : []
      memberIds = members.map(m => m.userId)
    } catch (e) {
      console.error('加载群成员失败:', e)
    }

    const memberIdSet = new Set(memberIds.map(id => String(id)))
    inviteCandidates.value = friends.filter(f => !memberIdSet.has(String(f.userId)))
    selectedInviteFriendIds.value = []
    showInviteDialog.value = true
  } catch (error) {
    console.error('加载可邀请好友失败:', error)
    ElMessage.error('加载可邀请好友失败')
  } finally {
    inviteLoading.value = false
  }
}

// 发送群聊邀请（通过 SYSTEM/GROUP_INVITE 系统消息）
const sendGroupInvites = async () => {
  if (!selectedConversation.value || selectedConversation.value.type !== 'group') {
    ElMessage.error('当前不是群聊，无法发送邀请')
    return
  }

  if (selectedInviteFriendIds.value.length === 0) {
    ElMessage.warning('请选择要邀请的好友')
    return
  }

  if (!userStore.user || !userStore.user.id) {
    ElMessage.error('用户信息缺失，请重新登录')
    return
  }

  const groupId = selectedConversation.value.id
  const groupName = selectedConversation.value.name
  const inviterId = userStore.user.id
  const inviterName = userStore.user.nickname

  try {
    inviteLoading.value = true

    const tasks = selectedInviteFriendIds.value.map(friendId => {
      return chatAPI.sendMessage({
        senderId: String(inviterId),
        receiverId: String(friendId),
        channelType: 'PRIVATE',
        contentType: 'SYSTEM',
        payload: {
          type: 'GROUP_INVITE',
          groupId,
          groupName,
          inviterId,
          inviterName
        }
      })
    })

    const results = await Promise.allSettled(tasks)
    const successCount = results.filter(r => r.status === 'fulfilled' && r.value && r.value.success).length

    if (successCount > 0) {
      ElMessage.success(`已向 ${successCount} 位好友发送邀请`)
      showInviteDialog.value = false
    } else {
      ElMessage.error('发送邀请失败')
    }
  } catch (error) {
    console.error('发送群聊邀请失败:', error)
    ElMessage.error('发送邀请失败')
  } finally {
    inviteLoading.value = false
  }
}

// 接受群聊邀请
const acceptGroupInvite = async (message) => {
  if (!userStore.user || !userStore.user.id) {
    ElMessage.error('用户信息缺失，请重新登录')
    return
  }

  const groupId = message.groupId
  const inviterId = message.inviterId

  if (!groupId || !inviterId) {
    ElMessage.error('邀请信息不完整，无法加入群聊')
    return
  }

  try {
    const res = await groupAPI.addMembers(groupId, {
      userIds: [userStore.user.id],
      inviterId
    })

    if (res.code === 200) {
      ElMessage.success('已加入群聊')
      router.push({
        path: '/im/messages',
        query: { type: 'group', groupId }
      })
    } else {
      ElMessage.error(res.message || '加入群聊失败')
    }
  } catch (error) {
    console.error('接受群聊邀请失败:', error)
    ElMessage.error('加入群聊失败')
  }
}

// 拒绝群聊邀请
const rejectGroupInvite = () => {
  ElMessage.info('已拒绝该群聊邀请')
}

// 判断是否是自己发送的消息
const isOwnMessage = (message) => {
  if (!userStore.user || !userStore.user.id) return false
  // 转换为字符串进行比较，避免类型不匹配
  return String(message.senderId) === String(userStore.user.id)
}

// 格式化时间
const formatTime = (timestamp) => {
  const now = dayjs()
  const time = dayjs(timestamp)
  
  if (now.diff(time, 'day') === 0) {
    return time.format('HH:mm')
  } else if (now.diff(time, 'day') === 1) {
    return '昨天'
  } else if (now.diff(time, 'week') === 0) {
    return time.format('dddd')
  } else {
    return time.format('MM-DD')
  }
}

// 从路由参数初始化（支持从通讯录点击好友/群组跳转）
const initFromRoute = async () => {
  const { conversationId, friendId, friendName, type, groupId } = route.query
  
  console.log('initFromRoute - 路由参数:', { conversationId, friendId, friendName, type, groupId })
  
  // 如果是好友聊天
  if (conversationId && friendId && friendName) {
    console.log('创建好友会话:', friendName)
    // 创建好友会话对象
    const friendConversation = {
      id: friendId,
      name: friendName,
      avatar: '',
      type: 'private',
      lastMessage: '',
      lastMessageTime: Date.now(),
      unreadCount: 0
    }
    
    // 添加到会话列表（如果不存在）
    const existingIndex = conversations.value.findIndex(
      conv => conv.type === 'private' && conv.id === friendId
    )
    
    if (existingIndex === -1) {
      conversations.value.unshift(friendConversation)
    }

    // 持久化好友会话到本地
    saveConversationToLocal(friendConversation).catch(error => {
      console.error('保存好友会话到本地失败:', error)
    })
    
    // 选中该会话
    selectedConversation.value = friendConversation
    console.log('已选中好友会话:', selectedConversation.value)
    
    // 加载历史消息
    try {
      await loadMessages(friendConversation)
      console.log('历史消息加载完成，消息数量:', messages.value.length)
    } catch (error) {
      console.error('加载历史消息失败:', error)
      // 即使加载失败，也显示聊天界面
      messages.value = []
    }
    
    return
  }
  
  // 如果是群组类型且有groupId
  if (type === 'group' && groupId) {
    try {
      // 获取群组信息
      const groupResponse = await groupAPI.getGroup(groupId)
      
      if (groupResponse.code === 200 && groupResponse.data) {
        const groupData = groupResponse.data
        
        // 创建或找到群组会话
        let groupConversation = conversations.value.find(
          conv => conv.type === 'group' && conv.id === groupId
        )
        
        if (!groupConversation) {
          // 如果会话列表中没有，创建新的会话对象
          groupConversation = {
            id: groupData.groupId,
            name: groupData.name,
            avatar: groupData.avatar || '',
            type: 'group',
            memberCount: groupData.memberCount,
            lastMessage: '',
            lastMessageTime: new Date(),
            unreadCount: 0
          }
          
          // 添加到会话列表顶部
          conversations.value.unshift(groupConversation)

          // 持久化群聊会话到本地
          saveConversationToLocal(groupConversation).catch(error => {
            console.error('保存群聊会话到本地失败:', error)
          })
        }
        
        // 选中该会话
        await selectConversation(groupConversation)
        
        // 清除URL参数
        router.replace({ path: route.path })
      }
    } catch (error) {
      console.error('加载群组失败:', error)
      ElMessage.error('无法打开群聊')
    }
  }
}

// 监听路由变化（支持在Messages页面内切换群组）
watch(
  () => route.query,
  async (newQuery) => {
    if (newQuery.type === 'group' && newQuery.groupId) {
      await initFromRoute()
    }
  },
  { deep: true }
)

// 处理系统事件（群成员变更、被踢出等）
const handleSystemEvent = async (data) => {
  console.log('handleSystemEvent 被调用，完整数据:', JSON.stringify(data, null, 2))
  
  const eventType = data.payload?.eventType
  const eventData = data.payload?.data || {}
  
  console.log('收到系统事件:', eventType, eventData)
  
  if (!eventType) {
    console.error('系统事件缺少 eventType:', data)
    return
  }

  // 创建通知
  const notification = {
    timestamp: data.createdAt || Date.now(),
    isRead: 0,
    eventType,
    conversationId: data.conversationId,
    title: '',
    message: '',
    data: eventData
  }

  // 根据事件类型设置通知内容
  switch (eventType) {
    case 'member_added':
      notification.title = '新成员加入'
      notification.message = `${eventData.operatorName || '管理员'} 邀请 ${eventData.targetUsers?.map(u => u.name).join('、') || '新成员'} 加入了群聊`
      break
    
    case 'member_removed':
      // 检查是否是自己被移出或主动退出
      const isMyself = eventData.targetUsers?.some(u => String(u.user_id) === String(userStore.user?.id))
      
      if (isMyself) {
        // 自己退出或被踢出群聊
        if (eventData.isVoluntary) {
          // 主动退出
          notification.title = '您已退出群聊'
          notification.message = '您已成功退出该群聊'
        } else {
          // 被踢出
          notification.title = '您已被移出群聊'
          notification.message = `您已被 ${eventData.operatorName || '管理员'} 移出群聊`
        }
        
        // 从会话列表中移除该群聊
        const index = conversations.value.findIndex(c => 
          buildConversationId(c) === data.conversationId
        )
        if (index !== -1) {
          conversations.value.splice(index, 1)
          console.log('已从会话列表移除群聊:', data.conversationId)
        }
        
        // 如果当前正在查看这个群聊，清空消息
        if (selectedConversation.value?.conversationId === data.conversationId) {
          selectedConversation.value = null
          messages.value = []
        }
        
        // 如果是被踢出，弹窗提示
        if (!eventData.isVoluntary) {
          ElMessageBox.alert('您已被移出该群聊', '提示', {
            confirmButtonText: '确定'
          })
        }
      } else {
        // 其他成员被移出或主动退出
        if (eventData.isVoluntary) {
          notification.title = '成员退出群聊'
          notification.message = `${eventData.targetUsers?.map(u => u.name).join('、') || '成员'} 退出了群聊`
        } else {
          notification.title = '成员被移出'
          notification.message = `${eventData.targetUsers?.map(u => u.name).join('、') || '成员'} 被移出群聊`
        }
      }
      break
    
    case 'kicked_out':
      // 保留兼容性，但现在统一使用 member_removed
      notification.title = '成员变更'
      notification.message = `群成员发生变更`
      break
    
    case 'group_disbanded':
      notification.title = '群聊已解散'
      notification.message = `群聊已被解散`
      
      // 弹窗提示
      ElMessageBox.alert('该群聊已被解散', '提示', {
        confirmButtonText: '确定',
        callback: () => {
          // 跳转回消息列表
          if (selectedConversation.value?.conversationId === data.conversationId) {
            selectedConversation.value = null
            messages.value = []
          }
          // 从会话列表中移除
          const index = conversations.value.findIndex(c => 
            buildConversationId(c) === data.conversationId
          )
          if (index !== -1) {
            conversations.value.splice(index, 1)
          }
        }
      })
      break
  }

  // 保存通知到本地
  try {
    await chatDb.notifications.add(notification)
    console.log('系统事件通知已保存')
  } catch (error) {
    console.error('保存系统事件通知失败:', error)
  }

  // 在聊天记录中插入灰条提示
  if (data.conversationId) {
    const systemMessage = {
      id: `system_${Date.now()}`,
      messageId: data.messageId || `system_${Date.now()}`,
      conversationId: data.conversationId,
      senderId: 'system',
      senderName: '系统',
      type: 'system',
      content: notification.message,
      timestamp: data.createdAt || Date.now(),
      createdAt: data.createdAt || Date.now(),
      systemType: 'event',
      eventType
    }

    // 保存到本地消息表
    await chatDb.messages.put(systemMessage).catch(err => {
      console.error('保存系统消息失败:', err)
    })

    // 如果是当前会话，显示在聊天记录中
    if (selectedConversation.value) {
      const currentConversationId = buildConversationId(selectedConversation.value)
      if (currentConversationId === data.conversationId) {
        messages.value.push(systemMessage)
        nextTick(() => {
          scrollToBottom()
        })
      }
    }

    // 更新会话的最后一条消息
    const conversation = conversations.value.find(c => {
      const convId = buildConversationId(c)
      return convId === data.conversationId
    })
    if (conversation) {
      conversation.lastMessage = notification.message
      conversation.lastMessageTime = systemMessage.timestamp
      await saveConversationToLocal(conversation).catch(err => {
        console.error('更新会话失败:', err)
      })
    }
  }
}

// WebSocket 消息处理器
const handleWebSocketMessage = async (data) => {
  console.log('收到新消息:', data)
  console.log('消息详情 - contentType:', data.contentType, 'payload:', data.payload)
  const contentType = (data.contentType || 'TEXT').toUpperCase()

  // 处理系统事件通知（群成员变更、被踢出等）
  if (contentType === 'SYSTEM' && data.payload && data.payload.type === 'EVENT') {
    console.log('识别为系统事件，准备处理')
    await handleSystemEvent(data)
    return
  } else if (contentType === 'SYSTEM') {
    console.log('SYSTEM 消息但不是 EVENT 类型:', data.payload)
  }

  // 处理 typing 信令（SYSTEM 类型，payload.type = TYPING），不当作普通消息渲染
  if (contentType === 'SYSTEM' && data.payload && data.payload.type === 'TYPING') {
    const currentConversationId = selectedConversation.value
      ? buildConversationId(selectedConversation.value)
      : null

    if (
      currentConversationId &&
      currentConversationId === data.conversationId &&
      String(data.senderId) !== String(userStore.user?.id)
    ) {
      const typingFlag = !!data.payload.typing
      isTyping.value = typingFlag

      if (typingFlag) {
        if (typingTimer) clearTimeout(typingTimer)
        typingTimer = setTimeout(() => {
          isTyping.value = false
          typingTimer = null
        }, 4000)
      }
    }

    return
  }

  // 如果是聊天 / 系统消息
  if (data.messageId && data.conversationId) {
    let systemType = null
    let groupId = null
    let groupName = null
    let inviterId = null
    let inviterName = null
    let content = data.payload?.text || data.payload?.content || ''

    // 群聊邀请系统消息
    if (contentType === 'SYSTEM' && data.payload && data.payload.type === 'GROUP_INVITE') {
      systemType = 'group_invite'
      groupId = data.payload.groupId
      groupName = data.payload.groupName
      inviterId = data.payload.inviterId
      inviterName = data.payload.inviterName
      if (!content) {
        content = groupName || ''
      }
    }

    const createdAt = data.createdAt || Date.now()
    const newMessage = {
      id: data.messageId,
      messageId: data.messageId,
      conversationId: data.conversationId,
      senderId: data.senderId,
      senderName: data.senderName || `用户${data.senderId}`,
      senderAvatar: '',
      type: contentType.toLowerCase(),
      content,
      timestamp: createdAt,
      createdAt,
      systemType,
      groupId,
      groupName,
      inviterId,
      inviterName
    }

    chatDb.messages.put(newMessage).catch(error => {
      console.error('保存消息到本地失败:', error)
    })

    // 如果是当前会话的消息，添加到消息列表
    if (selectedConversation.value) {
      const currentConversationId = buildConversationId(selectedConversation.value)
      if (currentConversationId === data.conversationId) {
        messages.value.push(newMessage)
        nextTick(() => {
          scrollToBottom()
        })
      }
    }

    // 更新会话列表的最后一条消息
    const conversation = conversations.value.find(c => {
      const convId = buildConversationId(c)
      return convId === data.conversationId
    })

    if (conversation) {
      conversation.lastMessage = newMessage.content
      conversation.lastMessageTime = newMessage.timestamp
      if (selectedConversation.value?.id !== conversation.id) {
        conversation.unreadCount = (conversation.unreadCount || 0) + 1
      }

      // 同步会话到本地
      saveConversationToLocal(conversation).catch(error => {
        console.error('更新本地会话失败:', error)
      })
    }
  }
}

onMounted(async () => {
  console.log('Messages.vue - onMounted 开始')
  console.log('当前用户:', userStore.user)
  console.log('路由参数:', route.query)
  
  // 注册 WebSocket 消息处理器
  websocketService.onMessage(handleWebSocketMessage)
  
  // 先加载会话列表
  await loadConversations()
  console.log('会话列表加载完成，数量:', conversations.value.length)
  
  // 然后处理路由参数
  await initFromRoute()
  console.log('路由参数处理完成')
  console.log('选中的会话:', selectedConversation.value)
  
  // 默认选择第一个会话
  if (!selectedConversation.value && conversations.value.length > 0) {
    await selectConversation(conversations.value[0])
  }
  
  console.log('Messages.vue - onMounted 完成')
})

onUnmounted(() => {
  // 移除消息处理器
  websocketService.offMessage(handleWebSocketMessage)

  if (typingTimer) {
    clearTimeout(typingTimer)
    typingTimer = null
  }
})
</script>

<style scoped>
.messages-container {
  display: flex;
  height: 100%;
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
}

.conversation-list {
  width: 320px;
  border-right: 1px solid #e8e8e8;
  display: flex;
  flex-direction: column;
}

.search-bar {
  padding: 16px;
  border-bottom: 1px solid #f0f0f0;
}

.conversations {
  flex: 1;
  overflow-y: auto;
}

.conversation-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  cursor: pointer;
  transition: background 0.3s;
  border-bottom: 1px solid #f8f8f8;
}

.conversation-item:hover {
  background: #f5f5f5;
}

.conversation-item.active {
  background: #e6f7ff;
}

.conversation-info {
  flex: 1;
  min-width: 0;
}

.conversation-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}

.name {
  font-weight: 500;
  color: #333;
  font-size: 14px;
}

.time {
  font-size: 12px;
  color: #999;
}

.last-message {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.content {
  font-size: 13px;
  color: #666;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
}

.unread-badge {
  margin-left: 8px;
}

.chat-area {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.chat-header {
  padding: 16px 20px;
  border-bottom: 1px solid #e8e8e8;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.chat-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.chat-details h3 {
  margin: 0;
  font-size: 16px;
  color: #333;
}

.status {
  font-size: 12px;
  color: #666;
}

.chat-actions {
  display: flex;
  gap: 8px;
}

.message-list {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.message-item {
  display: flex;
  gap: 12px;
}

.message-item.own-message {
  flex-direction: row-reverse;
  justify-content: flex-start; /* 确保右对齐 */
}

.message-content {
  max-width: 60%;
}

.message-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.own-message .message-header {
  flex-direction: row-reverse;
}

.sender-name {
  font-size: 12px;
  color: #666;
  font-weight: 500;
}

.message-time {
  font-size: 11px;
  color: #999;
}

.message-body {
  background: #f0f0f0;
  padding: 8px 12px;
  border-radius: 4px 12px 12px 12px; /* 别人的消息：左上角小圆角 */
  word-wrap: break-word;
  min-height: 24px; /* 确保最小高度 */
  display: inline-block;
}

.own-message .message-body {
  background: #409eff;
  color: white;
  border-radius: 12px 4px 12px 12px; /* 自己的消息：右上角小圆角 */
}

.own-message .message-content {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}

.own-message .sender-name {
  display: none; /* 隐藏自己的名字 */
}

.text-message {
  line-height: 1.4;
  font-size: 14px;
  color: #333;
}

.own-message .text-message {
  color: white; /* 自己的消息文字为白色 */
}

.image-message .el-image {
  max-width: 200px;
  border-radius: 4px;
}

.file-message {
  display: flex;
  align-items: center;
  gap: 8px;
}

.input-area {
  border-top: 1px solid #e8e8e8;
  padding: 16px 20px;
}

.input-toolbar {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}

.input-box {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.input-actions {
  display: flex;
  justify-content: flex-end;
}

.empty-state {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
