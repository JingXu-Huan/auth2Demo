<template>
  <div class="chat-container">
    <!-- 侧边栏 -->
    <div class="sidebar">
      <div class="user-info">
        <el-avatar :src="userStore.user?.avatar" :size="40" />
        <div class="user-details">
          <div class="username">{{ userStore.user?.nickname }}</div>
          <div class="status">在线</div>
        </div>
        <el-dropdown @command="handleCommand">
          <el-icon class="more-icon"><MoreFilled /></el-icon>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="groups">群组管理</el-dropdown-item>
              <el-dropdown-item command="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>

      <div class="online-count">
        <el-icon><User /></el-icon>
        <span>在线用户: {{ onlineCount }}</span>
      </div>

      <div class="chat-list">
        <div class="chat-item active">
          <el-avatar :size="40">群</el-avatar>
          <div class="chat-info">
            <div class="chat-name">全局聊天室</div>
            <div class="last-message">欢迎来到聊天室</div>
          </div>
        </div>
      </div>
    </div>

    <!-- 聊天区域 -->
    <div class="chat-area">
      <div class="chat-header">
        <h3>全局聊天室</h3>
        <div class="header-actions">
          <el-button @click="clearMessages" size="small">清空消息</el-button>
        </div>
      </div>

      <div class="message-list" ref="messageListRef">
        <div 
          v-for="message in messages" 
          :key="message.id"
          :class="['message-item', { 'own-message': message.senderId === userStore.user?.id }]"
        >
          <el-avatar :src="message.avatar" :size="36" />
          <div class="message-content">
            <div class="message-header">
              <span class="sender-name">{{ message.senderName }}</span>
              <span class="message-time">{{ formatTime(message.timestamp) }}</span>
            </div>
            <div class="message-text">{{ message.content }}</div>
          </div>
        </div>
      </div>

      <div class="input-area">
        <div class="typing-indicator" v-if="typingUsers.length > 0">
          {{ typingUsers.join(', ') }} 正在输入...
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
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { MoreFilled, User } from '@element-plus/icons-vue'
import { useUserStore } from '../stores/user'
import { chatAPI } from '../api'
import dayjs from 'dayjs'

const router = useRouter()
const userStore = useUserStore()
const messageListRef = ref()
const messageInput = ref('')
const onlineCount = ref(0)
const typingUsers = ref([])
const messages = ref([])
let websocket = null
let typingTimer = null

// 检查登录状态
if (!userStore.isLoggedIn) {
  router.push('/login')
}

// WebSocket连接
const connectWebSocket = () => {
  const wsUrl = `ws://localhost:8002/ws/${userStore.user.id}`
  websocket = new WebSocket(wsUrl)
  
  websocket.onopen = () => {
    console.log('WebSocket连接成功')
    ElMessage.success('连接成功')
    updateOnlineCount()
  }
  
  websocket.onmessage = (event) => {
    try {
      const data = JSON.parse(event.data)
      handleWebSocketMessage(data)
    } catch (error) {
      console.error('解析WebSocket消息失败:', error)
    }
  }
  
  websocket.onclose = () => {
    console.log('WebSocket连接关闭')
    ElMessage.warning('连接已断开')
  }
  
  websocket.onerror = (error) => {
    console.error('WebSocket错误:', error)
    ElMessage.error('连接错误')
  }
}

// 处理WebSocket消息
const handleWebSocketMessage = (data) => {
  if (data.type === 'message') {
    addMessage({
      id: data.messageId || Date.now(),
      senderId: data.senderId,
      senderName: data.senderName || `用户${data.senderId}`,
      avatar: data.avatar || 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png',
      content: data.payload?.text || data.content || '消息内容',
      timestamp: data.createdAt || Date.now()
    })
  } else if (data.type === 'typing') {
    handleTypingIndicator(data)
  }
}

// 添加消息
const addMessage = (message) => {
  messages.value.push(message)
  nextTick(() => {
    scrollToBottom()
  })
}

// 滚动到底部
const scrollToBottom = () => {
  if (messageListRef.value) {
    messageListRef.value.scrollTop = messageListRef.value.scrollHeight
  }
}

// 发送消息
const handleSendMessage = () => {
  if (!messageInput.value.trim()) return
  
  const message = {
    senderId: userStore.user.id,
    receiverId: null, // 群聊
    channelType: 'GROUP',
    contentType: 'TEXT',
    payload: {
      text: messageInput.value.trim()
    },
    createdAt: Date.now()
  }
  
  if (websocket && websocket.readyState === WebSocket.OPEN) {
    websocket.send(JSON.stringify(message))
    
    // 添加到本地消息列表
    addMessage({
      id: Date.now(),
      senderId: userStore.user.id,
      senderName: userStore.user.nickname,
      avatar: userStore.user.avatar,
      content: messageInput.value.trim(),
      timestamp: Date.now()
    })
    
    messageInput.value = ''
  } else {
    ElMessage.error('连接已断开，请刷新页面重试')
  }
}

// 处理输入
const handleTyping = () => {
  if (typingTimer) {
    clearTimeout(typingTimer)
  }
  
  // 发送正在输入状态
  if (websocket && websocket.readyState === WebSocket.OPEN) {
    websocket.send(JSON.stringify({
      type: 'typing',
      senderId: userStore.user.id,
      senderName: userStore.user.nickname
    }))
  }
  
  // 3秒后停止输入状态
  typingTimer = setTimeout(() => {
    if (websocket && websocket.readyState === WebSocket.OPEN) {
      websocket.send(JSON.stringify({
        type: 'stop_typing',
        senderId: userStore.user.id
      }))
    }
  }, 3000)
}

// 处理输入指示器
const handleTypingIndicator = (data) => {
  if (data.senderId === userStore.user.id) return
  
  if (data.type === 'typing') {
    if (!typingUsers.value.includes(data.senderName)) {
      typingUsers.value.push(data.senderName)
    }
  } else if (data.type === 'stop_typing') {
    const index = typingUsers.value.indexOf(data.senderName)
    if (index > -1) {
      typingUsers.value.splice(index, 1)
    }
  }
}

// 更新在线用户数
const updateOnlineCount = async () => {
  try {
    const response = await chatAPI.getOnlineUsers()
    onlineCount.value = response.data?.count || 0
  } catch (error) {
    console.error('获取在线用户数失败:', error)
  }
}

// 清空消息
const clearMessages = () => {
  messages.value = []
  ElMessage.success('消息已清空')
}

// 格式化时间
const formatTime = (timestamp) => {
  return dayjs(timestamp).format('HH:mm:ss')
}

// 处理下拉菜单命令
const handleCommand = (command) => {
  if (command === 'groups') {
    router.push('/groups')
  } else if (command === 'logout') {
    userStore.logout()
    router.push('/login')
  }
}

onMounted(() => {
  connectWebSocket()
  // 定期更新在线用户数
  setInterval(updateOnlineCount, 5000)
})

onUnmounted(() => {
  if (websocket) {
    websocket.close()
  }
  if (typingTimer) {
    clearTimeout(typingTimer)
  }
})
</script>

<style scoped>
.chat-container {
  display: flex;
  height: 100vh;
  background: #f5f5f5;
}

.sidebar {
  width: 300px;
  background: white;
  border-right: 1px solid #e0e0e0;
  display: flex;
  flex-direction: column;
}

.user-info {
  padding: 20px;
  border-bottom: 1px solid #e0e0e0;
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-details {
  flex: 1;
}

.username {
  font-weight: 500;
  color: #333;
}

.status {
  font-size: 12px;
  color: #67c23a;
}

.more-icon {
  cursor: pointer;
  color: #666;
}

.online-count {
  padding: 15px 20px;
  display: flex;
  align-items: center;
  gap: 8px;
  color: #666;
  font-size: 14px;
  border-bottom: 1px solid #e0e0e0;
}

.chat-list {
  flex: 1;
  overflow-y: auto;
}

.chat-item {
  padding: 15px 20px;
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  border-bottom: 1px solid #f0f0f0;
}

.chat-item:hover {
  background: #f5f5f5;
}

.chat-item.active {
  background: #e6f7ff;
}

.chat-info {
  flex: 1;
}

.chat-name {
  font-weight: 500;
  color: #333;
}

.last-message {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}

.chat-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: white;
}

.chat-header {
  padding: 20px;
  border-bottom: 1px solid #e0e0e0;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.chat-header h3 {
  margin: 0;
  color: #333;
}

.message-list {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.message-item {
  display: flex;
  gap: 12px;
}

.message-item.own-message {
  flex-direction: row-reverse;
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

.message-text {
  background: #f0f0f0;
  padding: 8px 12px;
  border-radius: 8px;
  word-wrap: break-word;
}

.own-message .message-text {
  background: #409eff;
  color: white;
}

.input-area {
  border-top: 1px solid #e0e0e0;
  padding: 20px;
}

.typing-indicator {
  font-size: 12px;
  color: #999;
  margin-bottom: 10px;
  font-style: italic;
}

.input-box {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.input-actions {
  display: flex;
  justify-content: flex-end;
}
</style>
