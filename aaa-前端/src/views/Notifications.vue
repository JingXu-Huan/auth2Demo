<template>
  <div class="notifications-container">
    <div class="notifications-header">
      <h2>系统通知</h2>
      <el-button v-if="unreadCount > 0" text type="primary" @click="markAllAsRead">
        全部标记为已读
      </el-button>
    </div>

    <div class="notifications-list">
      <div 
        v-for="notification in notifications" 
        :key="notification.id"
        :class="['notification-item', { unread: !notification.isRead }]"
        @click="handleNotificationClick(notification)"
      >
        <div class="notification-icon">
          <el-icon :size="24" :color="getIconColor(notification.eventType)">
            <component :is="getIcon(notification.eventType)" />
          </el-icon>
        </div>
        
        <div class="notification-content">
          <div class="notification-title">{{ notification.title }}</div>
          <div class="notification-message">{{ notification.message }}</div>
          <div class="notification-time">{{ formatTime(notification.timestamp) }}</div>
        </div>

        <div v-if="!notification.isRead" class="unread-dot"></div>
      </div>

      <el-empty v-if="notifications.length === 0" description="暂无通知" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { 
  UserFilled, 
  RemoveFilled, 
  DeleteFilled,
  WarningFilled,
  InfoFilled
} from '@element-plus/icons-vue'
import chatDb from '../db/chatDb'
import dayjs from 'dayjs'
import relativeTime from 'dayjs/plugin/relativeTime'
import 'dayjs/locale/zh-cn'

dayjs.extend(relativeTime)
dayjs.locale('zh-cn')

const router = useRouter()
const notifications = ref([])

const unreadCount = computed(() => {
  return notifications.value.filter(n => !n.isRead).length
})

// 加载通知列表
const loadNotifications = async () => {
  try {
    const allNotifications = await chatDb.notifications
      .orderBy('timestamp')
      .reverse()
      .toArray()
    notifications.value = allNotifications
  } catch (error) {
    console.error('加载通知失败:', error)
  }
}

// 标记单个通知为已读
const markAsRead = async (notificationId) => {
  try {
    await chatDb.notifications.update(notificationId, { isRead: true })
    const notification = notifications.value.find(n => n.id === notificationId)
    if (notification) {
      notification.isRead = true
    }
  } catch (error) {
    console.error('标记通知已读失败:', error)
  }
}

// 标记所有通知为已读
const markAllAsRead = async () => {
  try {
    const unreadIds = notifications.value
      .filter(n => !n.isRead)
      .map(n => n.id)
    
    for (const id of unreadIds) {
      await chatDb.notifications.update(id, { isRead: true })
    }
    
    notifications.value.forEach(n => {
      n.isRead = true
    })
  } catch (error) {
    console.error('标记所有通知已读失败:', error)
  }
}

// 处理通知点击
const handleNotificationClick = async (notification) => {
  // 标记为已读
  if (!notification.isRead) {
    await markAsRead(notification.id)
  }

  // 根据通知类型跳转
  if (notification.conversationId) {
    // 跳转到对应的会话
    router.push({
      path: '/im/messages',
      query: {
        type: notification.conversationId.startsWith('GROUP:') ? 'group' : 'private',
        id: notification.conversationId.replace('GROUP:', '')
      }
    })
  }
}

// 获取图标
const getIcon = (eventType) => {
  const iconMap = {
    'member_added': UserFilled,
    'member_removed': RemoveFilled,
    'group_disbanded': DeleteFilled,
    'kicked_out': WarningFilled
  }
  return iconMap[eventType] || InfoFilled
}

// 获取图标颜色
const getIconColor = (eventType) => {
  const colorMap = {
    'member_added': '#67c23a',
    'member_removed': '#e6a23c',
    'group_disbanded': '#f56c6c',
    'kicked_out': '#f56c6c'
  }
  return colorMap[eventType] || '#909399'
}

// 格式化时间
const formatTime = (timestamp) => {
  return dayjs(timestamp).fromNow()
}

onMounted(() => {
  loadNotifications()
})
</script>

<style scoped>
.notifications-container {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #fff;
  border-radius: 8px;
}

.notifications-header {
  padding: 20px;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.notifications-header h2 {
  font-size: 18px;
  font-weight: 600;
  color: #333;
  margin: 0;
}

.notifications-list {
  flex: 1;
  overflow-y: auto;
}

.notification-item {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  padding: 16px 20px;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  transition: background 0.3s;
  position: relative;
}

.notification-item:hover {
  background: #f5f5f5;
}

.notification-item.unread {
  background: #f0f9ff;
}

.notification-icon {
  flex-shrink: 0;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: #f5f5f5;
  display: flex;
  align-items: center;
  justify-content: center;
}

.notification-content {
  flex: 1;
  min-width: 0;
}

.notification-title {
  font-size: 14px;
  font-weight: 600;
  color: #333;
  margin-bottom: 4px;
}

.notification-message {
  font-size: 13px;
  color: #666;
  margin-bottom: 4px;
  line-height: 1.5;
}

.notification-time {
  font-size: 12px;
  color: #999;
}

.unread-dot {
  position: absolute;
  top: 20px;
  right: 20px;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #409eff;
}
</style>
