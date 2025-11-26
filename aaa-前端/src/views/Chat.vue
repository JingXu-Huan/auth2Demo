<template>
  <div class="chat-page">
    <!-- 左侧会话列表 -->
    <div class="conversation-panel">
      <div class="panel-header">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索"
          :prefix-icon="Search"
          clearable
          class="search-input"
        />
      </div>

      <div class="conversation-list">
        <div
          v-for="conv in filteredConversations"
          :key="conv.id"
          :class="['conversation-item', { active: currentConversation?.id === conv.id }]"
          @click="selectConversation(conv)"
        >
          <el-badge :value="conv.unreadCount" :hidden="!conv.unreadCount" :offset="[-5, 5]">
            <el-avatar :size="48" :src="conv.avatar">
              {{ conv.name?.charAt(0) }}
            </el-avatar>
          </el-badge>
          <div class="conv-info">
            <div class="conv-header">
              <span class="conv-name">{{ conv.name }}</span>
              <span class="conv-time">{{ formatTime(conv.lastMessageTime) }}</span>
            </div>
            <div class="conv-message">{{ conv.lastMessage || '暂无消息' }}</div>
          </div>
        </div>

        <el-empty v-if="conversations.length === 0" description="暂无会话" />
      </div>
    </div>

    <!-- 右侧聊天区域 -->
    <div class="chat-panel" v-if="currentConversation">
      <!-- 聊天头部 -->
      <div class="chat-header">
        <div class="header-info">
          <h3>{{ currentConversation.name }}</h3>
          <span v-if="currentConversation.type === 'group'" class="member-count">
            {{ currentConversation.memberCount }} 人
          </span>
        </div>
        <div class="header-actions">
          <el-button :icon="Phone" circle />
          <el-button :icon="VideoCamera" circle />
          <el-button :icon="More" circle />
        </div>
      </div>

      <!-- 消息列表 -->
      <div class="message-list" ref="messageListRef">
        <div
          v-for="msg in messages"
          :key="msg.id"
          :class="['message-item', { 'own': String(msg.senderId) === String(userStore.userId) }]"
        >
          <el-avatar :size="36" :src="msg.senderAvatar" v-if="String(msg.senderId) !== String(userStore.userId)">
            {{ msg.senderName?.charAt(0) }}
          </el-avatar>
          
          <div class="message-content">
            <div class="message-sender" v-if="String(msg.senderId) !== String(userStore.userId) && currentConversation.type === 'group'">
              {{ msg.senderName }}
            </div>
            <div class="message-bubble">
              <template v-if="msg.status === 'recalled'">
                <span class="recalled-text">消息已撤回</span>
              </template>
              <template v-else>
                {{ msg.content }}
              </template>
            </div>
            <div class="message-time">{{ formatTime(msg.createdAt) }}</div>
          </div>
          
          <el-avatar :size="36" :src="userStore.userAvatar" v-if="String(msg.senderId) === String(userStore.userId)">
            {{ userStore.userName?.charAt(0) }}
          </el-avatar>
        </div>
      </div>

      <!-- 输入区域 -->
      <div class="input-area">
        <div class="input-toolbar">
          <el-button :icon="Picture" text />
          <el-button :icon="Folder" text />
          <el-button :icon="Mic" text />
        </div>
        <div class="input-box">
          <el-input
            v-model="inputMessage"
            type="textarea"
            :rows="3"
            placeholder="输入消息..."
            resize="none"
            @keydown.enter.exact.prevent="sendMessage"
          />
          <el-button
            type="primary"
            :icon="Promotion"
            :disabled="!inputMessage || !inputMessage.trim()"
            @click="sendMessage"
          >
            发送
          </el-button>
        </div>
      </div>
    </div>

    <!-- 空状态 -->
    <div class="empty-panel" v-else>
      <el-empty description="选择一个会话开始聊天">
        <template #image>
          <el-icon :size="80" color="#c0c4cc"><ChatLineRound /></el-icon>
        </template>
      </el-empty>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Phone, VideoCamera, More, Picture, Folder, Mic, Promotion, ChatLineRound } from '@element-plus/icons-vue'
import { useUserStore } from '../stores/user'
import { useChatStore } from '../stores/chat'
import { channelApi, messageApi } from '../api'
import websocket from '../utils/websocket'
import dayjs from 'dayjs'
import relativeTime from 'dayjs/plugin/relativeTime'
import isToday from 'dayjs/plugin/isToday'
import isYesterday from 'dayjs/plugin/isYesterday'
import 'dayjs/locale/zh-cn'

dayjs.extend(relativeTime)
dayjs.extend(isToday)
dayjs.extend(isYesterday)
dayjs.locale('zh-cn')

const userStore = useUserStore()
const chatStore = useChatStore()

const searchKeyword = ref('')
const inputMessage = ref('')
const messageListRef = ref()

const conversations = computed(() => chatStore.conversations)
const currentConversation = computed(() => chatStore.currentConversation)
const messages = computed(() => chatStore.currentMessages)

const filteredConversations = computed(() => {
  if (!searchKeyword.value) return conversations.value
  const keyword = searchKeyword.value.toLowerCase()
  return conversations.value.filter(c => c.name?.toLowerCase().includes(keyword))
})

// 加载会话列表
const loadConversations = async () => {
  try {
    const res = await channelApi.getChannels()
    if (res.code === 200) {
      const list = (res.data || []).map(channel => ({
        id: channel.id,
        name: channel.name || '未命名会话',
        avatar: channel.avatar,
        type: channel.type,
        memberCount: channel.memberCount,
        lastMessage: channel.lastMessage,
        lastMessageTime: channel.lastActiveAt,
        unreadCount: channel.unreadCount || 0
      }))
      chatStore.setConversations(list)
    }
  } catch (error) {
    console.error('加载会话失败:', error)
  }
}

// 选择会话
const selectConversation = async (conv) => {
  chatStore.selectConversation(conv)
  await loadMessages(conv.id)
}

// 加载消息
const loadMessages = async (channelId) => {
  try {
    const res = await messageApi.getHistory({ channelId, limit: 50 })
    if (res.code === 200) {
      chatStore.setMessages(channelId, res.data || [])
      scrollToBottom()
    }
  } catch (error) {
    console.error('加载消息失败:', error)
  }
}

// 发送消息
const sendMessage = async () => {
  if (!inputMessage.value.trim() || !currentConversation.value) return

  const content = inputMessage.value.trim()
  const tempId = `temp_${Date.now()}`
  
  // 先添加到本地
  const tempMessage = {
    id: tempId,
    tempId,
    channelId: currentConversation.value.id,
    senderId: userStore.userId,
    senderName: userStore.userName,
    senderAvatar: userStore.userAvatar,
    content,
    type: 'text',
    status: 'sending',
    createdAt: new Date().toISOString()
  }
  
  chatStore.addMessage(currentConversation.value.id, tempMessage)
  inputMessage.value = ''
  scrollToBottom()

  try {
    const res = await messageApi.send({
      channelId: currentConversation.value.id,
      content,
      msgType: 1  // 1-文本, 2-图片, 3-文件, 4-语音, 5-视频
    })

    if (res.code === 200) {
      // 后端返回的是 messageId (Long)，不是对象
      chatStore.updateMessage(currentConversation.value.id, tempId, {
        id: res.data,
        status: 'sent'
      })
    } else {
      chatStore.updateMessage(currentConversation.value.id, tempId, { status: 'failed' })
      ElMessage.error('发送失败')
    }
  } catch (error) {
    chatStore.updateMessage(currentConversation.value.id, tempId, { status: 'failed' })
    ElMessage.error('发送失败')
  }
}

// 滚动到底部
const scrollToBottom = () => {
  nextTick(() => {
    if (messageListRef.value) {
      messageListRef.value.scrollTop = messageListRef.value.scrollHeight
    }
  })
}

// 格式化时间
const formatTime = (time) => {
  if (!time) return ''
  const date = dayjs(time)
  if (date.isToday()) {
    return date.format('HH:mm')
  }
  if (date.isYesterday()) {
    return '昨天'
  }
  return date.format('MM-DD')
}

// 消息处理函数
const handleWebSocketMessage = (data) => {
  console.log('[Chat] 收到WebSocket消息:', data)
  
  // 判断是否是聊天消息（有 messageId 和 channelId 字段）
  if (data.messageId && data.channelId) {
    // 构建消息对象
    const message = {
      id: data.messageId,
      content: data.content,
      senderId: data.senderId,
      type: data.msgType === 1 ? 'text' : 'other',
      createdAt: data.createdAt,
      status: 'sent'
    }
    
    // 不添加自己发送的消息（已经在发送时添加了）
    if (String(data.senderId) !== String(userStore.userId)) {
      chatStore.addMessage(data.channelId, message)
      console.log('[Chat] 添加新消息到会话:', data.channelId, message)
    }
    
    if (currentConversation.value?.id === data.channelId) {
      scrollToBottom()
    }
  }
}

onMounted(() => {
  loadConversations()
  
  // 先清除所有 message 事件处理器，再注册新的（防止重复）
  websocket.offAll('message')
  websocket.on('message', handleWebSocketMessage)
  
  // 连接 WebSocket
  if (userStore.userId) {
    websocket.connect(userStore.userId)
    console.log('[Chat] WebSocket 连接已发起, userId:', userStore.userId)
  }
})

// 组件卸载时移除监听器
onUnmounted(() => {
  websocket.offAll('message')
  console.log('[Chat] WebSocket 消息监听器已移除')
})

// 监听消息变化，自动滚动
watch(messages, () => {
  scrollToBottom()
})
</script>

<style scoped>
.chat-page {
  display: flex;
  height: 100%;
  background: #fff;
}

.conversation-panel {
  width: 300px;
  border-right: 1px solid #e5e6eb;
  display: flex;
  flex-direction: column;
}

.panel-header {
  padding: 16px;
  border-bottom: 1px solid #e5e6eb;
}

.search-input :deep(.el-input__wrapper) {
  border-radius: 8px;
  background: #f5f6f7;
}

.conversation-list {
  flex: 1;
  overflow-y: auto;
}

.conversation-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  cursor: pointer;
  transition: background 0.2s;
}

.conversation-item:hover {
  background: #f5f6f7;
}

.conversation-item.active {
  background: #e8f3ff;
}

.conv-info {
  flex: 1;
  min-width: 0;
}

.conv-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}

.conv-name {
  font-weight: 500;
  color: #1f2329;
}

.conv-time {
  font-size: 12px;
  color: #8f959e;
}

.conv-message {
  font-size: 13px;
  color: #8f959e;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.chat-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid #e5e6eb;
}

.header-info h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
}

.member-count {
  font-size: 12px;
  color: #8f959e;
  margin-left: 8px;
}

.header-actions {
  display: flex;
  gap: 8px;
}

.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  background: #f5f6f7;
}

.message-item {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
}

.message-item.own {
  flex-direction: row-reverse;
}

.message-content {
  max-width: 60%;
}

.message-sender {
  font-size: 12px;
  color: #8f959e;
  margin-bottom: 4px;
}

.message-bubble {
  padding: 12px 16px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
  word-break: break-word;
}

.message-item.own .message-bubble {
  background: #3370ff;
  color: #fff;
}

.recalled-text {
  color: #8f959e;
  font-style: italic;
}

.message-time {
  font-size: 11px;
  color: #8f959e;
  margin-top: 4px;
}

.message-item.own .message-time {
  text-align: right;
}

.input-area {
  border-top: 1px solid #e5e6eb;
  background: #fff;
}

.input-toolbar {
  padding: 8px 16px;
  border-bottom: 1px solid #f0f1f2;
}

.input-box {
  display: flex;
  gap: 12px;
  padding: 12px 16px;
  align-items: flex-end;
}

.input-box .el-textarea {
  flex: 1;
}

.input-box :deep(.el-textarea__inner) {
  border-radius: 8px;
  resize: none;
  color: #333 !important;
  background: #fff !important;
  caret-color: #333 !important;
}

.empty-panel {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f6f7;
}
</style>
