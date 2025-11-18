<template>
  <div class="messages-container">
    <a-layout class="messages-layout">
      <!-- 左侧会话列表 -->
      <a-layout-sider width="320" class="conversation-sider">
        <div class="search-bar">
          <a-input 
            v-model:value="searchKeyword" 
            placeholder="搜索会话..."
            allow-clear
            @input="handleSearch"
          >
            <template #prefix>
              <search-outlined />
            </template>
          </a-input>
          <a-dropdown>
            <a-button type="text" size="small" class="more-btn">
              <template #icon>
                <more-outlined />
              </template>
            </a-button>
            <template #overlay>
              <a-menu>
                <a-menu-item key="clear" @click="clearAllConversations">
                  <delete-outlined />
                  清空会话
                </a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
        </div>
        
        <div class="conversations">
          <div 
            v-for="conversation in filteredConversations" 
            :key="conversation.id"
            :class="['conversation-item', { active: selectedConversation?.id === conversation.id }]"
            @click="selectConversation(conversation)"
            @contextmenu.prevent="showContextMenu($event, conversation)"
          >
            <a-badge :count="conversation.unreadCount" :offset="[-5, 5]">
              <a-avatar :src="conversation.avatar" :size="48" class="conversation-avatar">
                <template #icon>
                  <user-outlined v-if="conversation.type === 'private'" />
                  <team-outlined v-else />
                </template>
                {{ conversation.name?.charAt(0) || 'U' }}
              </a-avatar>
            </a-badge>
            <div class="conversation-info">
              <div class="conversation-header">
                <span class="name">{{ conversation.name }}</span>
                <span class="time">{{ formatTime(conversation.lastMessageTime) }}</span>
              </div>
              <div class="last-message">
                <span class="content">{{ conversation.lastMessage || '暂无消息' }}</span>
              </div>
            </div>
          </div>
        </div>
      </a-layout-sider>
    
      <!-- 右侧聊天区域 -->
      <a-layout-content class="chat-area" v-if="selectedConversation">
        <div class="chat-header">
          <div class="header-left">
            <a-avatar :src="selectedConversation.avatar" :size="40" class="chat-avatar">
              <template #icon>
                <user-outlined v-if="selectedConversation.type === 'private'" />
                <team-outlined v-else />
              </template>
              {{ selectedConversation.name?.charAt(0) || 'U' }}
            </a-avatar>
            
            <div class="info-column">
              <div class="title-row">
                <span class="chat-name">{{ selectedConversation.name }}</span>
                <span class="member-count" v-if="selectedConversation.type === 'group'">
                  <user-outlined />
                  {{ selectedConversation.memberCount || 2 }}
                </span>
              </div>
              <div class="tabs-row">
                <span class="tab-pill active">
                  <message-outlined />
                  消息
                </span>
                <span class="tab-pill">
                  <folder-outlined />
                  云文档
                </span>
                <span class="add-btn">
                  <plus-outlined />
                </span>
              </div>
            </div>
          </div>
          
          <div class="header-right">
            <a-tooltip title="语音通话">
              <a-button type="text" shape="circle" class="action-btn">
                <template #icon>
                  <phone-outlined />
                </template>
              </a-button>
            </a-tooltip>
            <a-tooltip title="视频通话">
              <a-button type="text" shape="circle" class="action-btn">
                <template #icon>
                  <video-camera-outlined />
                </template>
              </a-button>
            </a-tooltip>
            <a-tooltip title="邀请好友" v-if="selectedConversation.type === 'group'">
              <a-button type="text" shape="circle" class="action-btn" @click="openInviteDialog">
                <template #icon>
                  <user-add-outlined />
                </template>
              </a-button>
            </a-tooltip>
            <a-popover 
              v-model:open="showMoreMenu"
              trigger="click" 
              placement="bottomRight"
              :overlayStyle="{ padding: 0 }"
            >
              <template #content>
                <div class="feishu-more-menu">
                  <div 
                    v-if="selectedConversation.type === 'group'" 
                    class="menu-item" 
                    @click="handleAddAnnouncement"
                  >
                    <notification-outlined class="menu-icon" />
                    <span>添加群公告</span>
                  </div>
                  <div class="menu-item" @click="handleViewTasks">
                    <check-circle-outlined class="menu-icon" />
                    <span>查看任务</span>
                  </div>
                  <div class="menu-divider"></div>
                  <div class="menu-item" @click="openGroupSettings">
                    <setting-outlined class="menu-icon" />
                    <span>设置</span>
                  </div>
                </div>
              </template>
              <a-tooltip title="更多">
                <a-button type="text" shape="circle" class="action-btn">
                  <template #icon>
                    <more-outlined />
                  </template>
                </a-button>
              </a-tooltip>
            </a-popover>
          </div>
        </div>
      
        <div class="message-list" ref="messageListRef">
          <div 
            v-for="message in messages" 
            :key="message.id"
            :class="['message-item', { 'own-message': isOwnMessage(message) }]"
          >
            <a-avatar 
              :src="isOwnMessage(message) ? userStore.user?.avatar : message.senderAvatar" 
              :size="36" 
              class="message-avatar"
            >
              <template #icon>
                <user-outlined />
              </template>
              {{ isOwnMessage(message) ? (userStore.user?.nickname?.charAt(0) || 'U') : (message.senderName?.charAt(0) || 'U') }}
            </a-avatar>
            <div class="message-content">
              <div class="message-header" v-if="!isOwnMessage(message)">
                <span class="sender-name">{{ message.senderName }}</span>
                <span class="message-time">{{ formatTime(message.timestamp) }}</span>
              </div>
              <div class="message-body-wrapper">
                <div class="message-body">
              <!-- 撤回消息统一显示占位文本，双方都看不到原始内容 -->
              <div v-if="message.status === 'RECALLED'" class="recalled-message">
                <span class="recalled-text">
                  {{ isOwnMessage(message) ? '你撤回了一条消息' : `${message.senderName} 撤回了一条消息` }}
                </span>
                <span 
                  v-if="isOwnMessage(message)" 
                  class="re-edit-link"
                  @click="reEditMessage(message)"
                >
                  重新编辑
                </span>
              </div>
              <div v-else-if="message.type === 'text'" class="text-message">
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
                        <a-button type="primary" size="small" @click="acceptGroupInvite(message)">
                          同意加入
                        </a-button>
                        <a-button size="small" @click="rejectGroupInvite(message)">
                          拒绝
                        </a-button>
                      </div>
                    </div>
                  </div>
                </div>
                <!-- 消息操作菜单 -->
                <a-dropdown 
                  v-if="message.status !== 'RECALLED'"
                  :trigger="['hover']"
                  placement="bottomRight"
                  class="message-actions"
                >
                  <a-button type="text" size="small" class="message-action-btn">
                    <template #icon>
                      <more-outlined />
                    </template>
                  </a-button>
                  <template #overlay>
                    <a-menu>
                      <a-menu-item
                        v-if="isOwnMessage(message)"
                        key="recall"
                        @click="onRecallMessage(message)"
                      >
                        <undo-outlined />
                        撤回
                      </a-menu-item>
                      <a-menu-item
                        key="favorite"
                        @click="onToggleFavorite(message)"
                      >
                        <star-outlined v-if="!message.isFavorite" />
                        <star-filled v-else />
                        {{ message.isFavorite ? '取消收藏' : '收藏' }}
                      </a-menu-item>
                      <a-menu-item
                        key="delete"
                        danger
                        @click="onDeleteMessage(message)"
                      >
                        <delete-outlined />
                        删除
                      </a-menu-item>
                    </a-menu>
                  </template>
                </a-dropdown>
              </div>
            </div>
          </div>
        </div>
      
        <div class="input-area">
          <div class="input-container">
            <a-textarea
              v-model:value="messageInput"
              :rows="3"
              :placeholder="`发送给 ${selectedConversation.name}`"
              :bordered="false"
              @keydown.enter.exact.prevent="handleSendMessage"
              @input="handleTyping"
              class="input-textarea"
            />
            
            <div class="input-toolbar-bottom">
              <div class="toolbar-left">
                <a-tooltip title="文字格式">
                  <a-button type="text" size="small" class="tool-btn">
                    Aa
                  </a-button>
                </a-tooltip>
                <a-tooltip title="表情">
                  <a-button type="text" size="small" class="tool-btn">
                    <template #icon>
                      <smile-outlined />
                    </template>
                  </a-button>
                </a-tooltip>
                <a-tooltip title="@提醒">
                  <a-button type="text" size="small" class="tool-btn">
                    @
                  </a-button>
                </a-tooltip>
                <a-tooltip title="截图">
                  <a-button type="text" size="small" class="tool-btn">
                    <template #icon>
                      <picture-outlined />
                    </template>
                  </a-button>
                </a-tooltip>
                <a-tooltip title="更多">
                  <a-button type="text" size="small" class="tool-btn">
                    <template #icon>
                      <plus-outlined />
                    </template>
                  </a-button>
                </a-tooltip>
              </div>
              
              <div class="toolbar-right">
                <a-button 
                  type="text"
                  shape="circle"
                  :disabled="!messageInput.trim()"
                  @click="handleSendMessage"
                  class="send-icon-btn"
                >
                  <template #icon>
                    <send-outlined />
                  </template>
                </a-button>
              </div>
            </div>
          </div>
        </div>
      </a-layout-content>
    
      <!-- 空状态 -->
      <a-layout-content v-else class="empty-state">
        <div ref="emptyLottieContainer" class="empty-lottie"></div>
        <div class="empty-text">选择一个会话开始聊天</div>
      </a-layout-content>
    </a-layout>
  </div>

  <!-- 右键菜单 -->
  <teleport to="body">
    <div 
      v-if="contextMenuVisible" 
      class="context-menu"
      :style="{ left: contextMenuX + 'px', top: contextMenuY + 'px' }"
      @click.stop
    >
      <div class="menu-item" @click="pinConversation">
        <star-outlined />
        置顶会话
      </div>
      <div class="menu-item" @click="muteConversation">
        <bell-outlined />
        消息免打扰
      </div>
      <div class="menu-item" @click="markAsUnread">
        <mail-outlined />
        标为未读
      </div>
      <a-divider style="margin: 4px 0" />
      <div class="menu-item danger" @click="clearChatHistory">
        <delete-outlined />
        清空聊天记录
      </div>
      <div class="menu-item danger" @click="deleteConversation">
        <close-circle-outlined />
        删除会话
      </div>
    </div>
    
    <!-- 点击其他地方关闭右键菜单 -->
    <div 
      v-if="contextMenuVisible" 
      class="context-menu-mask"
      @click="hideContextMenu"
      @contextmenu.prevent="hideContextMenu"
    ></div>
  </teleport>

  <!-- 通用人员选择器 -->
  <member-selector
    v-model:visible="showInviteDialog"
    :friends-list="inviteCandidates"
    :groups-list="[]"
    :org-tree="[]"
    @confirm="handleMembersSelected"
  />

  <!-- 设置抽屉 - 飞书风格 -->
  <a-drawer
    v-model:open="showGroupSettings"
    title="设置"
    width="420"
    placement="right"
    :closable="true"
    class="feishu-group-settings"
  >
    <!-- 私聊设置 -->
    <div v-if="selectedConversation && selectedConversation.type === 'private'" class="settings-content">
      <!-- 1. 用户信息 -->
      <div class="section-info private-info">
        <a-avatar :size="64" :src="selectedConversation.avatar" class="info-avatar">
          <template #icon>
            <user-outlined />
          </template>
          {{ selectedConversation.name?.charAt(0) || 'U' }}
        </a-avatar>
        <div class="info-text">
          <div class="name">{{ selectedConversation.name }}</div>
        </div>
        <div class="share-card-btn" @click="handleShareCard">
          <share-alt-outlined />
          <span>分享个人名片</span>
        </div>
      </div>

      <!-- 2. 创建群组 -->
      <div class="action-section">
        <div class="action-item" @click="handleCreateGroupWithUser">
          <user-add-outlined class="action-icon" />
          <span>创建群组</span>
        </div>
      </div>

      <!-- 3. 详细设置 -->
      <div class="detail-settings">
        <!-- 标签 -->
        <div class="list-item clickable" @click="handleAddTag">
          <div class="item-label">标签</div>
          <div class="item-right">
            <span>添加标签</span>
            <right-outlined class="arrow-icon" />
          </div>
        </div>

        <a-divider class="thin-divider" />

        <!-- 复选框组 -->
        <div class="checkbox-group">
          <div class="checkbox-item">
            <a-checkbox v-model:checked="settingsState.isMuted">消息免打扰</a-checkbox>
          </div>
          <div class="checkbox-item">
            <a-checkbox v-model:checked="settingsState.isPinned">置顶会话</a-checkbox>
          </div>
          <div class="checkbox-item">
            <a-checkbox v-model:checked="settingsState.isMarked">添加到标记</a-checkbox>
          </div>
        </div>

        <a-divider class="thin-divider" />

        <!-- 翻译助手 -->
        <div class="list-item clickable" @click="handleTranslator">
          <div class="item-label">翻译助手</div>
          <right-outlined class="arrow-icon" />
        </div>

        <a-divider class="thin-divider" />

        <!-- 清空聊天记录 -->
        <div class="list-item clickable danger-item" @click="handleClearHistory">
          <div class="item-label-icon">
            <delete-outlined />
            <span class="ml-2">清空聊天记录</span>
          </div>
        </div>

        <a-divider class="thin-divider" />

        <!-- 举报 -->
        <div class="report-section clickable" @click="handleReport">
          <warning-outlined />
          <span>举报</span>
        </div>
      </div>
    </div>

    <!-- 群聊设置 -->
    <div v-else-if="selectedConversation && selectedConversation.type === 'group'" class="settings-content">
      <!-- 1. 群信息概览 -->
      <div class="section-info">
        <a-avatar :size="48" :src="selectedConversation.avatar" class="info-avatar">
          <template #icon>
            <team-outlined />
          </template>
          {{ selectedConversation.name?.charAt(0) || 'G' }}
        </a-avatar>
        <div class="info-text">
          <div class="name">{{ selectedConversation.name }}</div>
          <div class="edit-btn" @click="handleEditGroupInfo">编辑群信息</div>
        </div>
      </div>

      <!-- 2. 群成员管理 -->
      <div class="section-members">
        <div class="sec-header">
          <span class="sec-title">群成员</span>
          <span class="sec-right" @click="viewAllMembers">
            {{ selectedConversation.memberCount || 2 }}
            <right-outlined />
          </span>
        </div>

        <!-- 搜索框 -->
        <div class="search-wrap">
          <a-input
            v-model:value="memberSearchKey"
            placeholder="搜索"
            class="feishu-search-input"
          >
            <template #prefix>
              <search-outlined />
            </template>
          </a-input>
        </div>

        <!-- 成员头像预览 -->
        <div class="avatar-row">
          <a-avatar :size="32" class="m-avatar">X</a-avatar>
          <a-avatar :size="32" class="m-avatar">Z</a-avatar>
          
          <div class="circle-action-btn" @click="openInviteDialog">
            <plus-outlined />
          </div>
        </div>
      </div>

      <!-- 3. 功能列表 -->
      <div class="section-list">
        <div class="list-menu-item" @click="handleGroupAnnouncement">
          <span>群公告</span>
          <right-outlined />
        </div>
        <div class="list-menu-item" @click="handleGroupFiles">
          <span>群文件</span>
          <right-outlined />
        </div>
        <div class="list-menu-item" @click="handleGroupManage">
          <span>群管理</span>
          <right-outlined />
        </div>
      </div>

      <!-- 4. 详细设置区域 -->
      <div class="detail-settings">
        <!-- 群昵称 -->
        <div class="setting-section">
          <div class="section-label">群昵称</div>
          <a-input
            v-model:value="groupNickname"
            placeholder="设置你在群里的昵称"
            class="feishu-input"
          />
        </div>

        <a-divider class="thin-divider" />

        <!-- 标签 -->
        <div class="list-item clickable" @click="handleAddTag">
          <div class="item-label">标签</div>
          <div class="item-right">
            <span>添加标签</span>
            <right-outlined class="arrow-icon" />
          </div>
        </div>

        <a-divider class="thin-divider" />

        <!-- 复选框组 -->
        <div class="checkbox-group">
          <div class="checkbox-item">
            <a-checkbox v-model:checked="settingsState.isMuted">消息免打扰</a-checkbox>
          </div>
          <div class="checkbox-item">
            <a-checkbox v-model:checked="settingsState.muteAtAll">@所有人的消息不提示</a-checkbox>
          </div>
          <div class="checkbox-item">
            <a-checkbox v-model:checked="settingsState.isPinned">置顶会话</a-checkbox>
          </div>
          <div class="checkbox-item">
            <a-checkbox v-model:checked="settingsState.isMarked">添加到标记</a-checkbox>
          </div>
        </div>

        <a-divider class="thin-divider" />

        <!-- 翻译助手 -->
        <div class="list-item clickable" @click="handleTranslator">
          <div class="item-label">翻译助手</div>
          <right-outlined class="arrow-icon" />
        </div>

        <a-divider class="thin-divider" />

        <!-- 清空聊天记录 -->
        <div class="list-item clickable danger-item" @click="handleClearHistory">
          <div class="item-label-icon">
            <delete-outlined />
            <span class="ml-2">清空聊天记录</span>
          </div>
        </div>

        <a-divider class="thin-divider" />

        <!-- 举报 -->
        <div class="report-section clickable" @click="handleReport">
          <warning-outlined />
          <span>举报</span>
        </div>
      </div>

      <!-- 5. 危险操作区 -->
      <div class="footer-actions">
        <a-button class="btn-exit" block @click="handleLeaveGroup">
          退出群组
        </a-button>
        <a-button class="btn-dismiss" block @click="handleDismissGroup">
          解散群组
        </a-button>
      </div>
    </div>
  </a-drawer>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import MemberSelector from '../components/MemberSelector.vue'
import { 
  SearchOutlined,
  UserOutlined,
  TeamOutlined,
  MoreOutlined,
  DeleteOutlined,
  RightOutlined,
  PlusOutlined,
  MinusOutlined,
  PhoneOutlined,
  VideoCameraOutlined,
  UserAddOutlined,
  NotificationOutlined,
  CheckCircleOutlined,
  SettingOutlined,
  WarningOutlined,
  EditOutlined,
  ShareAltOutlined,
  FolderOutlined,
  PictureOutlined,
  PaperClipOutlined,
  AudioOutlined,
  SmileOutlined,
  SendOutlined,
  UndoOutlined,
  StarOutlined,
  StarFilled,
  BellOutlined,
  MailOutlined,
  CloseCircleOutlined
} from '@ant-design/icons-vue'
import { useUserStore } from '../stores/user'
import { chatAPI, chatMessageAPI, groupAPI, friendAPI } from '../api'
import websocketService from '../utils/websocket'
import chatDb from '../db/chatDb'
import { Document } from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import lottie from 'lottie-web'
import emptyAnimationData from '../asserts/让我看看.json'

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

const emptyLottieContainer = ref(null)
let emptyLottieInstance = null

// 右键菜单相关
const contextMenuVisible = ref(false)
const contextMenuX = ref(0)
const contextMenuY = ref(0)
const contextMenuTarget = ref(null)

// 显示右键菜单
const showContextMenu = (event, conversation) => {
  contextMenuTarget.value = conversation
  contextMenuX.value = event.clientX
  contextMenuY.value = event.clientY
  contextMenuVisible.value = true
}

// 隐藏右键菜单
const hideContextMenu = () => {
  contextMenuVisible.value = false
  contextMenuTarget.value = null
}

// 置顶会话
const pinConversation = () => {
  if (contextMenuTarget.value) {
    ElMessage.info('置顶功能开发中...')
    hideContextMenu()
  }
}

// 消息免打扰
const muteConversation = () => {
  if (contextMenuTarget.value) {
    ElMessage.info('免打扰功能开发中...')
    hideContextMenu()
  }
}

// 标为未读
const markAsUnread = () => {
  if (contextMenuTarget.value) {
    ElMessage.info('标为未读功能开发中...')
    hideContextMenu()
  }
}

// 清空聊天记录
const clearChatHistory = async () => {
  if (!contextMenuTarget.value) return
  
  try {
    await ElMessageBox.confirm(
      '确定要清空该会话的聊天记录吗？此操作不可恢复。',
      '清空聊天记录',
      {
        confirmButtonText: '清空',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    const conversationId = buildConversationId(contextMenuTarget.value)
    if (conversationId) {
      await chatDb.messages
        .where('conversationId')
        .equals(conversationId)
        .delete()
      
      if (selectedConversation.value?.id === contextMenuTarget.value.id) {
        messages.value = []
      }
      
      ElMessage.success('聊天记录已清空')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('清空聊天记录失败:', error)
      ElMessage.error('清空失败')
    }
  } finally {
    hideContextMenu()
  }
}

// 删除会话
const deleteConversation = async () => {
  if (!contextMenuTarget.value) return
  
  try {
    await ElMessageBox.confirm(
      '确定要删除该会话吗？聊天记录也将被删除。',
      '删除会话',
      {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'error'
      }
    )
    
    const conversationId = buildConversationId(contextMenuTarget.value)
    if (conversationId) {
      // 删除本地消息
      await chatDb.messages
        .where('conversationId')
        .equals(conversationId)
        .delete()
      
      // 删除会话
      await chatDb.conversations
        .where('conversationId')
        .equals(conversationId)
        .delete()
      
      // 从列表中移除
      conversations.value = conversations.value.filter(
        c => buildConversationId(c) !== conversationId
      )
      
      if (selectedConversation.value?.id === contextMenuTarget.value.id) {
        selectedConversation.value = null
        messages.value = []
      }
      
      ElMessage.success('会话已删除')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除会话失败:', error)
      ElMessage.error('删除失败')
    }
  } finally {
    hideContextMenu()
  }
}

// 重新编辑撤回的消息
const reEditMessage = (message) => {
  if (message.originalContent) {
    // 将原始内容恢复到输入框
    messageInput.value = message.originalContent
    ElMessage.success('消息内容已恢复到输入框')
    
    // 聚焦到输入框
    nextTick(() => {
      // 查找输入框元素并聚焦
      const textarea = document.querySelector('.input-box textarea')
      if (textarea) {
        textarea.focus()
        // 将光标移到末尾
        textarea.setSelectionRange(textarea.value.length, textarea.value.length)
      }
    })
  } else {
    ElMessage.warning('无法恢复消息内容，原始内容已丢失')
  }
}

// 群聊邀请相关状态
const showInviteDialog = ref(false)
const inviteCandidates = ref([])            // 可邀请的好友列表
const selectedInviteFriendIds = ref([])     // 选中的好友 userId 列表
const inviteLoading = ref(false)

// 群组设置相关状态
const showMoreMenu = ref(false)
const showGroupSettings = ref(false)
const memberSearchKey = ref('')
const groupNickname = ref('')
const settingsState = reactive({
  isMuted: false,
  muteAtAll: false,
  isPinned: false,
  isMarked: false
})

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

// 撤回消息
const onRecallMessage = async (message) => {
  try {
    if (!isOwnMessage(message)) {
      ElMessage.warning('只能撤回自己发送的消息')
      return
    }
    if (!message.messageId) {
      ElMessage.warning('消息尚未发送成功，无法撤回')
      return
    }

    const res = await chatMessageAPI.recallMessage(message.messageId)
    if (res && res.success) {
      // 保存原始内容到 originalContent 字段，以便重新编辑
      const originalContent = message.content
      message.status = 'RECALLED'
      message.content = '[已撤回]'
      message.originalContent = originalContent // 保存原始内容
      await chatDb.messages.put({ ...message })
      ElMessage.success('消息已撤回')
    } else {
      ElMessage.error(res?.message || '撤回消息失败')
    }
  } catch (error) {
    console.error('撤回消息失败:', error)
    ElMessage.error('撤回消息失败')
  }
}

// 收藏 / 取消收藏
const onToggleFavorite = async (message) => {
  try {
    if (!userStore.user || !userStore.user.id) {
      ElMessage.error('用户信息缺失，请重新登录')
      return
    }
    if (!message.messageId) {
      ElMessage.warning('消息尚未发送成功，无法收藏')
      return
    }

    const action = message.isFavorite ? 'REMOVE' : 'ADD'
    const req = {
      userId: String(userStore.user.id),
      messageId: message.messageId,
      action
    }

    const res = await chatMessageAPI.favoriteMessage(req)
    if (res && res.success) {
      message.isFavorite = !message.isFavorite
      await chatDb.messages.put({ ...message })
      ElMessage.success(message.isFavorite ? '已收藏消息' : '已取消收藏')
    } else {
      ElMessage.error(res?.message || '操作失败')
    }
  } catch (error) {
    console.error('收藏消息失败:', error)
    ElMessage.error('收藏消息失败')
  }
}

// 删除消息（本地删除）
const onDeleteMessage = async (message) => {
  try {
    if (!userStore.user || !userStore.user.id) {
      ElMessage.error('用户信息缺失，请重新登录')
      return
    }
    if (!message.messageId) {
      ElMessage.warning('消息尚未发送成功，无法删除')
      return
    }

    await ElMessageBox.confirm(
      '确定要删除该消息吗？此操作仅会从你的聊天记录中移除，不影响对方。',
      '删除消息',
      {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    const req = {
      userId: String(userStore.user.id),
      messageIds: [message.messageId]
    }

    const res = await chatMessageAPI.deleteMessages(req)
    if (res && res.success) {
      messages.value = messages.value.filter(m => m.id !== message.id)
      try {
        await chatDb.messages.delete(message.id)
      } catch (e) {
        console.error('从本地删除消息失败:', e)
      }
      ElMessage.success('消息已删除')
    } else {
      ElMessage.error(res?.message || '删除消息失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除消息失败:', error)
      ElMessage.error('删除消息失败')
    }
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

// 清空本地所有会话（仅删除会话摘要，保留聊天记录）
const clearAllConversations = async () => {
  try {
    await chatDb.conversations.clear()
    conversations.value = []
    selectedConversation.value = null
    messages.value = []
  } catch (error) {
    console.error('清空会话失败:', error)
    ElMessage.error('清空会话失败')
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

// 过滤会话（同时按会话唯一键去重）
const filteredConversations = computed(() => {
  let filtered = conversations.value

  // 按搜索关键词过滤
  if (searchKeyword.value) {
    const keyword = searchKeyword.value.toLowerCase()
    filtered = filtered.filter(conv => 
      (conv.name || '').toLowerCase().includes(keyword) ||
      (conv.lastMessage || '').toLowerCase().includes(keyword)
    )
  }

  // 使用会话唯一键去重：优先使用 canonical conversationId，其次用 type+id 兜底
  const uniqueMap = new Map()
  for (const conv of filtered) {
    const convId = buildConversationId(conv)
    const key = convId || `${conv.type || 'unknown'}:${String(conv.id)}`
    const existing = uniqueMap.get(key)

    // 保留最后一条消息时间更新的会话
    if (!existing || (conv.lastMessageTime || 0) > (existing.lastMessageTime || 0)) {
      uniqueMap.set(key, conv)
    }
  }

  return Array.from(uniqueMap.values()).sort(
    (a, b) => (b.lastMessageTime || 0) - (a.lastMessageTime || 0)
  )
})

const buildConversationId = (conversation) => {
  if (!conversation || !userStore.user || !userStore.user.id) {
    return null
  }
  // 优先使用后端返回的 canonical conversationId
  if (conversation.conversationId) {
    return conversation.conversationId
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
  console.log('选择会话:', conversation)
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
          status: msg.status || 'SENT',
          isFavorite: !!msg.isFavorite,
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

  // 同步会话摘要到本地（仅在已有 canonical 会话ID 时持久化）
  if (selectedConversation.value.conversationId) {
    saveConversationToLocal(selectedConversation.value).catch(error => {
      console.error('更新本地会话失败:', error)
    })
  }

  messageInput.value = ''

  nextTick(() => {
    scrollToBottom()
  })

  try {
    const channelType = selectedConversation.value.type === 'private' ? 'PRIVATE' : 'GROUP'

    const payload = {
      senderId: String(userStore.user.id),
      receiverId: channelType === 'PRIVATE' ? String(selectedConversation.value.id) : null,
      groupId: channelType === 'GROUP' ? String(selectedConversation.value.id) : null,
      channelType,
      contentType: 'TEXT',
      payload: { text: newMessage.content }
    }

    // 仅在已有 canonical 会话ID 时传给后端；
    // 新建单聊会话时由后端统一生成 conversationId
    if (selectedConversation.value.conversationId) {
      payload.conversationId = selectedConversation.value.conversationId
    } else if (selectedConversation.value.type === 'group') {
      // 群聊的会话ID是确定规则，可直接使用
      payload.conversationId = conversationId
    }

    const response = await chatAPI.sendMessage(payload)

    if (response.success && response.data) {
      const serverMsg = response.data
      const createdAt = serverMsg.createdAt || newMessage.createdAt
      newMessage.id = serverMsg.messageId
      newMessage.messageId = serverMsg.messageId
      newMessage.timestamp = createdAt
      newMessage.createdAt = createdAt
      newMessage.status = 'sent'

      // 使用服务端返回的 canonical conversationId 回写本地消息和会话
      if (serverMsg.conversationId) {
        newMessage.conversationId = serverMsg.conversationId
        if (!selectedConversation.value.conversationId) {
          selectedConversation.value.conversationId = serverMsg.conversationId
        }
      }

      await chatDb.messages.put({
        ...newMessage
      })

      // 确保会话摘要中的会话ID也与后端保持一致
      saveConversationToLocal(selectedConversation.value).catch(error => {
        console.error('更新本地会话失败(发送成功后):', error)
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

// 处理人员选择器确认
const handleMembersSelected = async (members) => {
  if (!selectedConversation.value || selectedConversation.value.type !== 'group') {
    ElMessage.error('当前不是群聊，无法发送邀请')
    return
  }

  if (members.length === 0) {
    ElMessage.warning('请选择要邀请的成员')
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

    const tasks = members.map(member => {
      return chatAPI.sendMessage({
        senderId: String(inviterId),
        receiverId: String(member.uid),
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
      ElMessage.success(`已向 ${successCount} 位成员发送邀请`)
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

// 群组设置相关函数
const handleAddAnnouncement = () => {
  showMoreMenu.value = false
  ElMessage.info('添加群公告功能开发中')
}

const handleViewTasks = () => {
  showMoreMenu.value = false
  ElMessage.info('查看任务功能开发中')
}

const openGroupSettings = () => {
  showMoreMenu.value = false
  showGroupSettings.value = true
}

const handleEditGroupInfo = () => {
  ElMessage.info('编辑群信息功能开发中')
}

const viewAllMembers = () => {
  ElMessage.info('查看全部成员功能开发中')
}

const handleRemoveMembers = () => {
  ElMessage.info('移除成员功能开发中')
}

const handleGroupAnnouncement = () => {
  ElMessage.info('群公告功能开发中')
}

const handleGroupFiles = () => {
  ElMessage.info('群文件功能开发中')
}

const handleGroupManage = () => {
  ElMessage.info('群管理功能开发中')
}

const handleLeaveGroup = () => {
  ElMessageBox.confirm('确定要退出该群组吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    ElMessage.success('已退出群组')
    showGroupSettings.value = false
  }).catch(() => {})
}

const handleDismissGroup = () => {
  ElMessageBox.confirm('解散后，所有成员将被移出群组，且无法恢复。确定要解散该群组吗？', '警告', {
    confirmButtonText: '确定解散',
    cancelButtonText: '取消',
    type: 'error'
  }).then(() => {
    ElMessage.success('群组已解散')
    showGroupSettings.value = false
  }).catch(() => {})
}

const handleAddTag = () => {
  ElMessage.info('添加标签功能开发中')
}

const handleTranslator = () => {
  ElMessage.info('翻译助手功能开发中')
}

const handleClearHistory = () => {
  ElMessageBox.confirm('确定要清空聊天记录吗？此操作不可恢复。', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    ElMessage.success('聊天记录已清空')
  }).catch(() => {})
}

const handleReport = () => {
  ElMessage.info('举报功能开发中')
}

const handleEditUserInfo = () => {
  ElMessage.info('编辑用户信息功能开发中')
}

const handleCreateGroupWithUser = () => {
  ElMessage.info('创建群组功能开发中')
}

const handleShareCard = () => {
  ElMessage.info('分享个人名片功能开发中')
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

    const canonicalConversationId = String(conversationId)
    const friendIdStr = String(friendId)

    // 1. 优先根据 conversationId 查找已有会话，避免重复
    let targetConversation = conversations.value.find(conv => {
      const convId = buildConversationId(conv)
      return convId && convId === canonicalConversationId
    })

    // 2. 兼容旧数据：根据 type + id 兜底查找（id 统一使用字符串比较）
    if (!targetConversation) {
      targetConversation = conversations.value.find(conv =>
        conv.type === 'private' && String(conv.id) === friendIdStr
      )
    }

    if (targetConversation) {
      // 更新展示信息
      targetConversation.name = friendName
      targetConversation.avatar = targetConversation.avatar || ''
      targetConversation.conversationId = targetConversation.conversationId || canonicalConversationId

      // 持久化最新会话信息
      saveConversationToLocal(targetConversation).catch(error => {
        console.error('保存好友会话到本地失败:', error)
      })
    } else {
      // 创建新的好友会话对象
      targetConversation = {
        id: friendIdStr,
        name: friendName,
        avatar: '',
        type: 'private',
        lastMessage: '',
        lastMessageTime: Date.now(),
        unreadCount: 0,
        conversationId: canonicalConversationId
      }

      conversations.value.unshift(targetConversation)

      // 持久化好友会话到本地
      saveConversationToLocal(targetConversation).catch(error => {
        console.error('保存好友会话到本地失败:', error)
      })
    }
    
    // 选中该会话
    selectedConversation.value = targetConversation
    console.log('已选中好友会话:', selectedConversation.value)
    
    // 加载历史消息
    try {
      await loadMessages(targetConversation)
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
        
        // 创建或找到群组会话（id 统一按字符串比较，避免类型不一致导致重复）
        let groupConversation = conversations.value.find(
          conv => conv.type === 'group' && String(conv.id) === String(groupId)
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
      {
        const targetNames = (eventData.targetUsers || [])
          .map(u => u.name)
          .join('、') || '新成员'
        notification.message = `${eventData.operatorName || '管理员'} 邀请 ${targetNames} 加入了群聊`
      }
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
          {
            const targetNames = (eventData.targetUsers || [])
              .map(u => u.name)
              .join('、') || '成员'
            notification.message = `${targetNames} 退出了群聊`
          }
        } else {
          notification.title = '成员被移出'
          {
            const targetNames = (eventData.targetUsers || [])
              .map(u => u.name)
              .join('、') || '成员'
            notification.message = `${targetNames} 被移出群聊`
          }
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

  if (contentType === 'SYSTEM' && data.payload && data.payload.type === 'RECALL') {
    const targetMessageId = data.payload.messageId
    if (targetMessageId) {
      const idx = messages.value.findIndex(m => m.messageId === targetMessageId)
      if (idx !== -1) {
        const target = messages.value[idx]
        target.status = 'RECALLED'
        target.content = '[已撤回]'
      }

      try {
        const existing = await chatDb.messages.get(targetMessageId)
        if (existing) {
          existing.status = 'RECALLED'
          existing.content = '[已撤回]'
          await chatDb.messages.put(existing)
        }
      } catch (error) {
        console.error('更新本地撤回消息失败:', error)
      }

      const conv = conversations.value.find(c => {
        const convId = buildConversationId(c)
        return convId === data.conversationId
      })
      if (conv) {
        conv.lastMessage = '[已撤回]'
        conv.lastMessageTime = Date.now()
        saveConversationToLocal(conv).catch(err => {
          console.error('更新本地会话失败(撤回):', err)
        })
      }
    }
    return
  }

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
  // 未登录直接跳转登录页
  if (!userStore.user || !userStore.user.id) {
    router.push('/login')
    return
  }

  // 注册 WebSocket 消息监听
  websocketService.onMessage(handleWebSocketMessage)

  // 初始化空状态 Lottie 动画
  if (emptyLottieContainer.value) {
    emptyLottieInstance = lottie.loadAnimation({
      container: emptyLottieContainer.value,
      renderer: 'svg',
      loop: false,
      autoplay: true,
      animationData: emptyAnimationData
    })
  }

  // 先加载会话列表，再根据路由参数初始化
  await loadConversations()
  await initFromRoute()
})

onUnmounted(() => {
  // 取消 WebSocket 监听
  websocketService.offMessage(handleWebSocketMessage)

  if (typingTimer) {
    clearTimeout(typingTimer)
  }

  if (emptyLottieInstance) {
    emptyLottieInstance.destroy()
    emptyLottieInstance = null
  }
})

 </script>

 <style scoped>
.messages-container {
  height: 100%;
  background: #f5f6f7;
}

.messages-layout {
  height: 100%;
  background: #f5f6f7;
}

/* 左侧会话列表 */
.conversation-sider {
  background: #fff !important;
  border-right: 1px solid #e5e7eb;
}

.search-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  border-bottom: 1px solid #f0f0f0;
  height: 64px;
  box-sizing: border-box;
}

.search-bar .ant-input-affix-wrapper {
  flex: 1;
}

.search-bar .more-btn {
  color: #8f959e;
  font-size: 16px;
}

.search-bar .more-btn:hover {
  color: #4E59CC;
  background: #f5f6f7;
}

.conversations {
  flex: 1;
  overflow-y: auto;
  padding: 4px 0;
}

.conversation-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  margin: 4px 8px;
  cursor: pointer;
  transition: all 0.2s ease;
  border-radius: 8px;
}

.conversation-item:hover {
  background: #f5f6f7;
}

.conversation-item.active {
  background: #e8efff;
  box-shadow: 0 1px 4px rgba(78, 89, 204, 0.1);
}


.conversation-avatar {
  flex-shrink: 0;
  background: linear-gradient(135deg, #5B7ADB 0%, #4E59CC 100%);
}

.conversation-info {
  flex: 1;
  min-width: 0;
}

.conversation-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}

.name {
  font-weight: 600;
  color: #1f2329;
  font-size: 15px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.time {
  font-size: 12px;
  color: #8f959e;
  flex-shrink: 0;
  margin-left: 8px;
}

.last-message {
  display: flex;
  align-items: center;
}

.content {
  font-size: 13px;
  color: #8f959e;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
}

/* 右侧聊天区域 */
.chat-area {
  display: flex;
  flex-direction: column;
  background: #fff;
}

/* 聊天头部 - 飞书精致风格 */
.chat-header {
  padding: 12px 16px;
  border-bottom: 1px solid #DEE0E3;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #fff;
  height: 64px;
  box-sizing: border-box;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.chat-avatar {
  background: linear-gradient(135deg, #5B7ADB 0%, #4E59CC 100%);
  flex-shrink: 0;
}

/* 信息列 - 紧凑布局 */
.info-column {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 4px;
}

/* 标题行 - 层级分明 */
.title-row {
  display: flex;
  align-items: center;
  gap: 6px;
  line-height: 1.2;
}

.chat-name {
  font-size: 16px;
  font-weight: 600;
  color: #1F2329;
}

.member-count {
  display: flex;
  align-items: center;
  gap: 3px;
  font-size: 12px;
  color: #8F959E;
  font-weight: normal;
}

.member-count .anticon {
  font-size: 13px;
}

/* 标签页胶囊 - 精致化 */
.tabs-row {
  display: flex;
  align-items: center;
  gap: 6px;
}

.tab-pill {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  border-radius: 8px;
  font-size: 12px;
  font-weight: 500;
  color: #646A73;
  cursor: pointer;
  transition: all 0.2s;
}

.tab-pill .anticon {
  font-size: 13px;
}

.tab-pill.active {
  background-color: #E1EAFF;
  color: #3370FF;
  font-weight: 500;
}

.tab-pill:hover:not(.active) {
  background-color: #F5F6F7;
  color: #1F2329;
}

.add-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 2px 6px;
  border-radius: 4px;
  color: #8F959E;
  cursor: pointer;
  transition: all 0.2s;
  font-size: 16px;
}

.add-btn:hover {
  background: #F5F6F7;
  color: #1F2329;
}

/* 右侧操作按钮 */
.header-right {
  display: flex;
  align-items: center;
  gap: 4px;
}

.action-btn {
  color: #5F6368;
  font-size: 18px;
  width: 32px;
  height: 32px;
  transition: all 0.2s;
  border-radius: 50%;
}

.action-btn:hover {
  background: rgba(0, 0, 0, 0.05);
  color: #1F2329;
}

/* 标签页 */
.chat-tabs {
  display: flex;
  align-items: center;
  padding: 0 24px;
  background: #fff;
  border-bottom: 1px solid #f0f0f0;
  height: 48px;
  gap: 24px;
}

.tab-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 12px 0;
  font-size: 14px;
  color: #8f959e;
  cursor: pointer;
  border-bottom: 2px solid transparent;
  transition: all 0.2s;
}

.tab-item:hover {
  color: #4E59CC;
}

.tab-item.active {
  color: #4E59CC;
  border-bottom-color: #4E59CC;
  font-weight: 500;
}

.tab-add {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  color: #8f959e;
  cursor: pointer;
  border-radius: 4px;
  transition: all 0.2s;
}

.tab-add:hover {
  background: #f5f6f7;
  color: #4E59CC;
}

.chat-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.chat-avatar {
  background: linear-gradient(135deg, #5B7ADB 0%, #4E59CC 100%);
}

.chat-details h3 {
  margin: 0 0 4px 0;
  font-size: 18px;
  font-weight: 600;
  color: #1f2329;
}

.status {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #8f959e;
}

.status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #52c41a;
}

.status-dot.typing {
  background: #faad14;
  animation: pulse 1.5s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

.chat-actions {
  display: flex;
  gap: 8px;
  align-items: center;
}

/* 消息列表 */
.message-list {
  flex: 1;
  padding: 16px 24px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 16px;
  background: #F5F6F7;
}

.message-item {
  display: flex;
  gap: 10px;
  align-items: flex-start;
}

.message-item.own-message {
  flex-direction: row-reverse;
  justify-content: flex-start;
}

.message-avatar {
  flex-shrink: 0;
  background: linear-gradient(135deg, #5B7ADB 0%, #4E59CC 100%);
}

.message-content {
  max-width: 60%;
  display: flex;
  flex-direction: column;
}

.message-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.sender-name {
  font-size: 13px;
  color: #8f959e;
  font-weight: 500;
}

.message-time {
  font-size: 12px;
  color: #8f959e;
}

.message-body-wrapper {
  display: flex;
  align-items: center;
  gap: 8px;
}

.own-message .message-body-wrapper {
  flex-direction: row-reverse;
}

.message-body {
  background: #FFFFFF;
  padding: 8px 12px;
  border-radius: 12px;
  word-wrap: break-word;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.08);
  border: none;
}

.own-message .message-body {
  background: #e6e8fa;
  color: #1f2329;
  border: none;
  box-shadow: 0 2px 4px rgba(78, 89, 204, 0.12);
}

.message-actions {
  opacity: 0;
  transition: opacity 0.2s;
}

.message-item:hover .message-actions {
  opacity: 1;
}

.message-action-btn {
  padding: 4px;
}

.text-message {
  line-height: 1.5;
  font-size: 14px;
  color: #1f2329;
}

/* 系统消息样式（撤回、加群等） */
.recalled-message {
  text-align: center;
  margin: 16px auto;
  width: 100%;
  font-size: 12px;
  color: #8f959e;
  background: transparent !important;
  padding: 0;
  border: none !important;
  box-shadow: none !important;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.recalled-text {
  color: #8F959E;
}

.re-edit-link {
  color: #4E59CC;
  cursor: pointer;
  transition: all 0.2s;
  font-size: 12px;
  font-weight: 500;
}

.re-edit-link:hover {
  color: #3d47a3;
  text-decoration: underline;
}

.message-item:has(.recalled-message) {
  justify-content: center;
}

.message-item:has(.recalled-message) .message-avatar {
  display: none;
}

.message-item:has(.recalled-message) .message-content {
  max-width: 100%;
  align-items: center;
}

.message-item:has(.recalled-message) .message-body {
  background: transparent !important;
  border: none !important;
  box-shadow: none !important;
  padding: 0;
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

/* 输入区域 - 飞书风格 */
.input-area {
  padding: 16px 24px 20px 24px;
  background: #F5F6F7;
}

.input-container {
  background: #fff;
  border: 1px solid #DEE0E3;
  border-radius: 8px;
  overflow: hidden;
  transition: all 0.2s;
}

.input-container:focus-within {
  border-color: #4E59CC;
  box-shadow: 0 0 0 2px rgba(78, 89, 204, 0.1);
}

.input-textarea {
  padding: 12px 16px;
  font-size: 14px;
  line-height: 1.5;
  min-height: 80px;
}

.input-textarea :deep(.ant-input) {
  resize: none;
  border: none;
  padding: 0;
}

.input-textarea :deep(.ant-input:focus) {
  box-shadow: none;
}

.input-textarea :deep(.ant-input::placeholder) {
  color: #C1C4C9;
}

/* 底部工具栏 */
.input-toolbar-bottom {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  border-top: 1px solid #F0F0F0;
  background: #FAFAFA;
}

.toolbar-left {
  display: flex;
  gap: 4px;
}

.tool-btn {
  color: #5F6368;
  font-size: 16px;
  padding: 4px 8px;
  transition: all 0.2s;
  border-radius: 4px;
}

.tool-btn:hover {
  color: #4E59CC;
  background: rgba(78, 89, 204, 0.08);
}

.toolbar-right {
  display: flex;
  align-items: center;
}

.send-icon-btn {
  color: #5F6368;
  font-size: 18px;
  transition: all 0.2s;
}

.send-icon-btn:not(:disabled) {
  color: #4E59CC;
}

.send-icon-btn:not(:disabled):hover {
  background: rgba(78, 89, 204, 0.1);
  transform: scale(1.1);
}

.send-icon-btn:disabled {
  color: #C1C4C9;
  cursor: not-allowed;
}

/* 空状态 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: #fff;
}

.empty-lottie {
  width: 220px;
  height: 220px;
}

.empty-text {
  margin-top: 16px;
  font-size: 14px;
  color: #8f959e;
}

/* 右键菜单 */
.context-menu-mask {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 999;
}

.context-menu {
  position: fixed;
  background: white;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  padding: 6px 0;
  z-index: 1000;
  min-width: 160px;
}

.context-menu .menu-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 16px;
  font-size: 14px;
  cursor: pointer;
  color: #1f2329;
  transition: background 0.2s;
}

.context-menu .menu-item:hover {
  background: #f5f6f7;
}

.context-menu .menu-item.danger {
  color: #f5222d;
}

.context-menu .menu-item.danger:hover {
  background: #fff1f0;
}

.context-menu .menu-item .anticon {
  font-size: 14px;
}

/* 飞书风格 - 更多菜单 Popover */
.feishu-more-menu {
  width: 180px;
  padding: 4px 0;
}

.feishu-more-menu .menu-item {
  padding: 10px 16px;
  font-size: 14px;
  color: #1F2329;
  cursor: pointer;
  display: flex;
  align-items: center;
  transition: background 0.2s;
}

.feishu-more-menu .menu-item:hover {
  background-color: #F5F6F7;
}

.feishu-more-menu .menu-icon {
  font-size: 18px;
  color: #5F6368;
  margin-right: 12px;
  flex-shrink: 0;
}

.feishu-more-menu .menu-divider {
  height: 1px;
  background: #F0F0F0;
  margin: 4px 0;
}

/* 飞书风格 - 群组设置抽屉 */
.feishu-group-settings :deep(.ant-drawer-header) {
  background-color: #FFFFFF !important;
  border-bottom: 1px solid #F0F0F0;
  padding: 16px 20px;
}

.feishu-group-settings :deep(.ant-drawer-body) {
  background-color: #FFFFFF !important;
  padding: 0;
  overflow-y: auto;
}

.feishu-group-settings :deep(.ant-drawer-content-wrapper) {
  box-shadow: -2px 0 8px rgba(0, 0, 0, 0.15);
}

.feishu-group-settings :deep(.ant-drawer-content) {
  background-color: #FFFFFF !important;
}

.settings-content {
  width: 100%;
}

/* 1. 群信息概览 */
.section-info {
  display: flex;
  align-items: flex-start;
  padding: 24px;
  border-bottom: 1px solid #F5F6F7;
}

.info-avatar {
  flex-shrink: 0;
}

.info-text {
  margin-left: 12px;
  flex: 1;
}

.info-text .name {
  font-size: 16px;
  font-weight: 600;
  color: #1F2329;
  line-height: 1.4;
  margin-bottom: 2px;
}

.info-text .edit-btn {
  font-size: 12px;
  color: #3370FF;
  cursor: pointer;
  transition: opacity 0.2s;
}

.info-text .edit-btn:hover {
  opacity: 0.8;
}

/* 2. 群成员管理 */
.section-members {
  padding: 24px;
  border-bottom: 1px solid #F5F6F7;
}

.sec-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.sec-title {
  font-size: 14px;
  font-weight: 600;
  color: #1F2329;
}

.sec-right {
  font-size: 12px;
  color: #999999;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 6px;
  transition: color 0.2s;
}

.sec-right:hover {
  color: #666666;
}

.sec-right .anticon {
  color: #BFBFBF;
}

.search-wrap {
  margin-bottom: 16px;
}

/* 飞书搜索框 - 核心样式 */
.feishu-search-input :deep(.ant-input-affix-wrapper) {
  background-color: #F5F5F5;
  border: 1px solid #E8E8E8;
  box-shadow: none !important;
  border-radius: 4px;
  padding-left: 8px;
  transition: all 0.2s;
}

.feishu-search-input :deep(.ant-input) {
  background-color: #F5F5F5;
  height: 30px;
  font-size: 13px;
  color: #1F2329;
}

.feishu-search-input :deep(.ant-input)::placeholder {
  color: #AAAAAA;
}

.feishu-search-input :deep(.ant-input-prefix) {
  color: #AAAAAA;
}

/* 聚焦时的效果 - 弱化 */
.feishu-search-input :deep(.ant-input-affix-wrapper:focus-within) {
  background-color: #FFFFFF !important;
  border-color: #D9D9D9 !important;
  box-shadow: none !important;
}

/* 头像流与加减号 */
.avatar-row {
  display: flex;
  gap: 10px;
  align-items: center;
  margin-top: 12px;
}

.m-avatar {
  width: 32px;
  height: 32px;
  flex-shrink: 0;
  font-size: 14px;
  font-weight: 600;
}

.m-avatar:nth-child(1) {
  background-color: #E1EAFF;
  color: #4E59CC;
}

.m-avatar:nth-child(2) {
  background-color: #FFE7D9;
  color: #D46B08;
}

/* 圆形加减号按钮 - 核心样式 */
.circle-action-btn {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: none;
  background-color: #F5F5F5;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #8F959E;
  cursor: pointer;
  transition: all 0.2s;
  flex-shrink: 0;
  font-size: 16px;
  font-weight: 300;
}

.circle-action-btn:hover {
  background-color: #E8E8E8;
  color: #595959;
}

/* 3. 功能列表 */
.section-list {
  padding: 0 24px;
  border-bottom: 1px solid #F5F6F7;
}

.list-menu-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 48px;
  font-size: 14px;
  color: #1F2329;
  border-bottom: 1px solid #F5F6F7;
  cursor: pointer;
  transition: all 0.2s;
}

.list-menu-item:last-child {
  border-bottom: none;
}

.list-menu-item:hover {
  background-color: #F8F9FA;
  margin: 0 -24px;
  padding: 0 24px;
}

.list-menu-item .anticon {
  font-size: 12px;
  color: #BFBFBF;
}

/* 4. 群昵称设置 */
.section-nickname {
  padding: 24px;
  border-bottom: 1px solid #F5F6F7;
}

.nickname-label {
  font-size: 13px;
  color: #606266;
  margin-bottom: 8px;
}

.nickname-input {
  border-radius: 4px;
}

.nickname-input :deep(.ant-input) {
  border-color: #DCDFE6;
}

/* 5. 危险操作区 */
.footer-actions {
  margin-top: 40px;
  display: flex;
  gap: 12px;
  padding: 0 24px 30px 24px;
}

.btn-exit,
.btn-dismiss {
  flex: 1;
  height: 36px;
  font-size: 14px;
  border-radius: 4px;
  font-weight: 500;
}

.btn-exit {
  color: #F54A45 !important;
  border-color: #F54A45 !important;
  background: #FFFFFF !important;
}

.btn-exit:hover {
  background: #FFF5F5 !important;
}

.btn-dismiss {
  background-color: #F54A45 !important;
  border-color: #F54A45 !important;
  color: #FFFFFF !important;
}

.btn-dismiss:hover {
  background-color: #E03E3E !important;
}

/* 详细设置区域 */
.detail-settings {
  padding: 0 24px;
  color: #1F2329;
}

.setting-section {
  margin-bottom: 16px;
}

.section-label {
  font-size: 14px;
  font-weight: 500;
  margin-bottom: 8px;
  color: #1F2329;
}

/* 飞书风格输入框 */
.feishu-input {
  background-color: #F5F5F5;
  border: 1px solid #E8E8E8;
  border-radius: 4px;
  color: #1F2329;
  height: 36px;
  transition: all 0.2s;
}

.feishu-input:focus,
.feishu-input:hover {
  background-color: #fff !important;
  border-color: #D9D9D9 !important;
  box-shadow: none !important;
}

.feishu-input :deep(.ant-input) {
  background-color: transparent;
}

.feishu-input:focus :deep(.ant-input),
.feishu-input:hover :deep(.ant-input) {
  background-color: #fff;
}

/* 极细分割线 */
.thin-divider {
  margin: 16px 0;
  border-top: 1px solid #F5F6F7;
}

.thin-divider :deep(.ant-divider) {
  margin: 16px 0;
  border-color: #F5F6F7;
}

/* 列表项通用布局 */
.list-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
  font-size: 14px;
  color: #1F2329;
}

.clickable {
  cursor: pointer;
}

.clickable:hover {
  opacity: 0.8;
}

.item-label {
  font-size: 14px;
  color: #1F2329;
}

.item-right {
  display: flex;
  align-items: center;
  color: #8F959E;
  font-size: 13px;
}

.arrow-icon {
  font-size: 12px;
  margin-left: 4px;
  color: #C0C4CC;
}

.item-label-icon {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #1F2329;
}

.ml-2 {
  margin-left: 8px;
}

/* 复选框组 - 卡片样式 */
.checkbox-group {
  display: flex;
  flex-direction: column;
  gap: 16px;
  background-color: #FAFAFA;
  padding: 16px;
  border-radius: 6px;
  margin: 8px 0;
}

.checkbox-item {
  font-size: 14px;
}

.checkbox-item :deep(.ant-checkbox-wrapper) {
  font-size: 14px;
  color: #1F2329;
}

/* 危险项样式 */
.danger-item {
  color: #F5222D !important;
}

.danger-item .item-label-icon {
  color: #F5222D !important;
}

/* 举报区域 */
.report-section {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 4px;
  color: #8F959E;
  font-size: 13px;
  margin: 24px 0;
}

/* 私聊设置样式 */
.section-info {
  position: relative;
}

.private-info {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 32px 24px 24px 24px;
}

.private-info .info-avatar {
  margin-bottom: 12px;
}

.private-info .info-text {
  margin-left: 0;
  text-align: center;
  margin-bottom: 16px;
}

.share-card-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  font-size: 13px;
  color: #3370FF;
  cursor: pointer;
  transition: all 0.2s;
  border-radius: 4px;
}

.share-card-btn:hover {
  background-color: #F0F5FF;
}

.share-card-btn .anticon {
  font-size: 14px;
}

.edit-icon {
  position: absolute;
  top: 24px;
  right: 24px;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: #8F959E;
  transition: all 0.2s;
}

.edit-icon:hover {
  color: #1F2329;
  background-color: #F5F5F5;
  border-radius: 50%;
}

.action-section {
  padding: 16px 24px;
  border-bottom: 1px solid #F5F6F7;
}

.action-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 0;
  font-size: 14px;
  color: #1F2329;
  cursor: pointer;
  transition: opacity 0.2s;
}

.action-item:hover {
  opacity: 0.8;
}

.action-icon {
  font-size: 16px;
  color: #8F959E;
}
</style>
