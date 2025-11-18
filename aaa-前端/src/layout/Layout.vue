<template>
  <div class="layout-container">
    <!-- 侧边导航栏 -->
    <div class="sidebar">
      <div class="logo">
        <img src="/file.svg" alt="Logo" class="logo-icon" />
      </div>
      
      <div class="nav-menu">
        <el-tooltip 
          v-for="item in menuItems" 
          :key="item.path"
          :content="item.title"
          placement="right"
        >
          <div 
            :class="['nav-item', { active: $route.path.includes(item.path) }]"
            @click="navigateTo(item.path)"
          >
            <el-badge 
              v-if="item.showBadge && unreadNotificationCount > 0" 
              :value="unreadNotificationCount" 
              :max="99"
              :offset="[5, 5]"
            >
              <el-icon class="nav-icon">
                <component :is="item.icon" />
              </el-icon>
            </el-badge>
            <el-icon v-else class="nav-icon">
              <component :is="item.icon" />
            </el-icon>
          </div>
        </el-tooltip>
      </div>
      
      <div class="user-section">
        <el-tooltip content="个人中心" placement="right">
          <el-avatar 
            :src="userStore.user?.avatar" 
            :size="40" 
            class="user-avatar"
            @click="showUserCenter = true"
          >
            {{ userStore.user?.nickname?.charAt(0) }}
          </el-avatar>
        </el-tooltip>
      </div>
    </div>
    
    <!-- 主内容区域 -->
    <div class="main-content">
      <router-view />
    </div>

    <!-- 个人中心抽屉 - 飞书风格 -->
    <a-drawer
      v-model:open="showUserCenter"
      title="个人中心"
      width="420"
      placement="right"
      :closable="true"
      class="feishu-user-center"
    >
      <div class="user-center-content">
        <!-- 1. 个人核心信息区 -->
        <div class="user-info-section">
          <a-avatar :size="80" :src="userStore.user?.avatar" class="user-avatar-large">
            {{ userStore.user?.nickname?.charAt(0) }}
          </a-avatar>
          <div class="user-name">{{ userStore.user?.nickname || '未设置昵称' }}</div>
          <div class="user-id">ID: {{ userStore.user?.id }}</div>
          <div class="user-signature">{{ userStore.user?.signature || '这个人很懒，什么都没写' }}</div>
          <div class="edit-profile-btn" @click="handleEditProfile">
            <edit-outlined />
          </div>
        </div>

        <!-- 2. 账户与安全 -->
        <div class="settings-group">
          <div class="group-title">账户与安全</div>
          <div class="settings-list">
            <div class="setting-item" @click="handleAccountSecurity">
              <safety-outlined class="setting-icon" />
              <span class="setting-label">账号与安全</span>
              <right-outlined class="setting-arrow" />
            </div>
            <div class="setting-item" @click="handleDeviceManagement">
              <laptop-outlined class="setting-icon" />
              <span class="setting-label">登录设备管理</span>
              <span class="setting-value">3台设备</span>
              <right-outlined class="setting-arrow" />
            </div>
            <div class="setting-item" @click="handlePrivacy">
              <lock-outlined class="setting-icon" />
              <span class="setting-label">隐私设置</span>
              <right-outlined class="setting-arrow" />
            </div>
          </div>
        </div>

        <!-- 3. 应用与通知 -->
        <div class="settings-group">
          <div class="group-title">应用与通知</div>
          <div class="settings-list">
            <div class="setting-item" @click="handleGeneralSettings">
              <setting-outlined class="setting-icon" />
              <span class="setting-label">通用设置</span>
              <span class="setting-value">简体中文</span>
              <right-outlined class="setting-arrow" />
            </div>
            <div class="setting-item" @click="handleNotifications">
              <bell-outlined class="setting-icon" />
              <span class="setting-label">通知与提醒</span>
              <right-outlined class="setting-arrow" />
            </div>
          </div>
        </div>

        <!-- 4. 帮助与反馈 -->
        <div class="settings-group">
          <div class="group-title">帮助与反馈</div>
          <div class="settings-list">
            <div class="setting-item" @click="handleHelp">
              <question-circle-outlined class="setting-icon" />
              <span class="setting-label">帮助中心</span>
              <right-outlined class="setting-arrow" />
            </div>
            <div class="setting-item" @click="handleFeedback">
              <message-outlined class="setting-icon" />
              <span class="setting-label">意见反馈</span>
              <right-outlined class="setting-arrow" />
            </div>
            <div class="setting-item" @click="handleAbout">
              <info-circle-outlined class="setting-icon" />
              <span class="setting-label">关于</span>
              <span class="setting-value">v1.0.0</span>
              <right-outlined class="setting-arrow" />
            </div>
          </div>
        </div>

        <!-- 5. 退出登录 -->
        <div class="logout-section">
          <a-button class="logout-btn" block @click="handleLogout">
            退出登录
          </a-button>
          <div class="version-info">版本 1.0.0</div>
        </div>
      </div>
    </a-drawer>

    <!-- 编辑个人资料抽屉 - 飞书风格 -->
    <a-drawer
      v-model:open="showEditProfile"
      width="420"
      placement="right"
      :closable="true"
      class="feishu-edit-profile"
    >
      <template #title>
        <div class="drawer-header">
          <span class="drawer-title">修改资料</span>
          <a-button
            type="primary"
            :disabled="!formChanged"
            :loading="saving"
            @click="handleSaveProfile"
            class="header-save-btn"
          >
            保存
          </a-button>
        </div>
      </template>
      <div class="edit-profile-content">
        <!-- 头像上传 -->
        <div class="avatar-upload-section">
          <div class="avatar-upload" @click="handleAvatarClick">
            <a-avatar :size="100" :src="profileForm.avatar" class="upload-avatar">
              {{ profileForm.nickname?.charAt(0) || 'U' }}
            </a-avatar>
            <div class="upload-overlay">
              <camera-outlined class="upload-icon" />
              <div class="upload-text">更换头像</div>
            </div>
            <div class="avatar-edit-icon">
              <camera-outlined />
            </div>
          </div>
          <div class="avatar-hint">点击修改头像</div>
        </div>

        <!-- 表单区域 -->
        <div class="profile-form">
          <!-- 昵称 -->
          <div class="form-item">
            <div class="form-label">昵称</div>
            <a-input
              v-model:value="profileForm.nickname"
              placeholder="请输入昵称"
              allow-clear
              class="form-input"
              @input="handleFormChange"
            />
          </div>

          <!-- Lantis ID -->
          <div class="form-item">
            <div class="form-label">Lantis ID</div>
            <div class="id-display">
              <span class="id-value">{{ profileForm.id }}</span>
              <a-button type="text" size="small" @click="handleCopyId">
                <copy-outlined />
                复制
              </a-button>
            </div>
            <div class="form-hint">ID 不可修改</div>
          </div>

          <!-- 性别 -->
          <div class="form-item">
            <div class="form-label">性别</div>
            <a-radio-group v-model:value="profileForm.gender" @change="handleFormChange">
              <a-radio value="male">男</a-radio>
              <a-radio value="female">女</a-radio>
              <a-radio value="other">其他</a-radio>
            </a-radio-group>
          </div>

          <!-- 个性签名 -->
          <div class="form-item">
            <div class="form-label">
              <span>个性签名</span>
              <span class="char-count">{{ signatureLength }}/30</span>
            </div>
            <a-textarea
              v-model:value="profileForm.signature"
              placeholder="这个人很懒，什么都没写"
              :rows="3"
              :maxlength="30"
              show-count
              class="form-textarea"
              @input="handleFormChange"
            />
          </div>
        </div>
      </div>
    </a-drawer>
  </div>
</template>

<script setup>
import { computed, ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '../stores/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import chatDb from '../db/chatDb'
import { 
  ChatDotRound, 
  User, 
  UserFilled,
  Search,
  Bell,
  Setting
} from '@element-plus/icons-vue'
import {
  EditOutlined,
  RightOutlined,
  SafetyOutlined,
  LockOutlined,
  SettingOutlined,
  BellOutlined,
  DatabaseOutlined,
  QuestionCircleOutlined,
  MessageOutlined,
  InfoCircleOutlined,
  LaptopOutlined,
  CameraOutlined,
  CopyOutlined
} from '@ant-design/icons-vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const unreadNotificationCount = ref(0)
const showUserCenter = ref(false)
const showEditProfile = ref(false)
const formChanged = ref(false)
const saving = ref(false)

// 个人资料表单
const profileForm = ref({
  avatar: '',
  nickname: '',
  id: '',
  gender: 'male',
  signature: '',
  phone: '',
  email: ''
})

// 原始数据备份
const originalForm = ref({})

// 计算属性
const signatureLength = computed(() => profileForm.value.signature?.length || 0)

// 加载未读通知数
const loadUnreadCount = async () => {
  try {
    const count = await chatDb.notifications.where('isRead').equals(0).count()
    unreadNotificationCount.value = count
  } catch (error) {
    console.error('加载未读通知数失败:', error)
  }
}

onMounted(() => {
  loadUnreadCount()
  // 每30秒刷新一次未读数
  setInterval(loadUnreadCount, 30000)
})

const menuItems = [
  { path: 'messages', title: '消息', icon: 'ChatDotRound' },
  { path: 'contacts', title: '通讯录', icon: 'User' },
  { path: 'notifications', title: '通知', icon: 'Bell', showBadge: true }
]

const currentPageTitle = computed(() => {
  const currentItem = menuItems.find(item => route.path.includes(item.path))
  return currentItem?.title || '工作台'
})

const navigateTo = (path) => {
  router.push(`/im/${path}`)
}

// 个人中心处理函数
const handleEditProfile = () => {
  // 初始化表单数据
  profileForm.value = {
    avatar: userStore.user?.avatar || '',
    nickname: userStore.user?.nickname || '',
    id: userStore.user?.id || '',
    gender: userStore.user?.gender || 'male',
    signature: userStore.user?.signature || '',
    phone: userStore.user?.phone || '',
    email: userStore.user?.email || ''
  }
  // 备份原始数据
  originalForm.value = JSON.parse(JSON.stringify(profileForm.value))
  formChanged.value = false
  showEditProfile.value = true
}

const handleAccountSecurity = () => {
  ElMessage.info('账号与安全功能开发中')
}

const handleDeviceManagement = () => {
  ElMessage.info('登录设备管理功能开发中')
}

const handlePrivacy = () => {
  ElMessage.info('隐私设置功能开发中')
}

const handleGeneralSettings = () => {
  ElMessage.info('通用设置功能开发中')
}

const handleNotifications = () => {
  ElMessage.info('通知与提醒功能开发中')
}

// 编辑资料处理函数
const handleFormChange = () => {
  // 检查表单是否有变化
  formChanged.value = JSON.stringify(profileForm.value) !== JSON.stringify(originalForm.value)
}

const handleAvatarClick = () => {
  ElMessage.info('头像上传功能开发中')
}

const handleCopyId = () => {
  navigator.clipboard.writeText(profileForm.value.id).then(() => {
    ElMessage.success('ID已复制到剪贴板')
  }).catch(() => {
    ElMessage.error('复制失败')
  })
}

const handleSaveProfile = async () => {
  if (!formChanged.value) return
  
  saving.value = true
  try {
    // 模拟保存
    await new Promise(resolve => setTimeout(resolve, 1000))
    
    // 更新用户信息
    Object.assign(userStore.user, {
      nickname: profileForm.value.nickname,
      gender: profileForm.value.gender,
      signature: profileForm.value.signature
    })
    
    ElMessage.success('保存成功')
    showEditProfile.value = false
    formChanged.value = false
  } catch (error) {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

const handleHelp = () => {
  ElMessage.info('帮助中心功能开发中')
}

const handleFeedback = () => {
  ElMessage.info('意见反馈功能开发中')
}

const handleAbout = () => {
  ElMessage.info('关于功能开发中')
}

const handleLogout = () => {
  ElMessageBox.confirm('确定要退出登录吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    userStore.logout()
    router.push('/login')
    showUserCenter.value = false
  }).catch(() => {})
}

// 登录状态由路由守卫处理
</script>

<style scoped>
.layout-container {
  display: flex;
  height: 100vh;
  background: #f5f6f7;
  overflow: hidden;
}

/* 侧边栏 - 飞书风格窄条 */
.sidebar {
  width: 68px;
  background: #2c2d30;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 16px 0;
  box-shadow: 2px 0 8px rgba(0, 0, 0, 0.1);
}

.logo {
  display: flex;
  justify-content: center;
  align-items: center;
  margin-bottom: 20px;
}

.logo-icon {
  width: 40px;
  height: 40px;
  object-fit: contain;
  border-radius: 8px;
}

.nav-menu {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: 100%;
  padding: 0 10px;
}

.nav-item {
  display: flex;
  justify-content: center;
  align-items: center;
  width: 48px;
  height: 48px;
  cursor: pointer;
  transition: all 0.2s;
  color: #a0a0a3;
  border-radius: 8px;
  position: relative;
}

.nav-item:hover {
  background: rgba(255, 255, 255, 0.1);
  color: #fff;
}

.nav-item.active {
  background: rgba(91, 122, 219, 0.2);
  color: #5B7ADB;
}

.nav-item.active::before {
  content: '';
  position: absolute;
  left: -10px;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 24px;
  background: #5B7ADB;
  border-radius: 0 2px 2px 0;
}

.nav-icon {
  font-size: 22px;
}

.user-section {
  padding-top: 16px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  display: flex;
  justify-content: center;
}

.user-avatar {
  cursor: pointer;
  transition: transform 0.2s;
  background: linear-gradient(135deg, #5B7ADB 0%, #4E59CC 100%);
}

.user-avatar:hover {
  transform: scale(1.05);
}

.main-content {
  flex: 1;
  height: 100vh;
  overflow: hidden;
  background: #f5f6f7;
}

/* 美化滚动条 */
:deep(::-webkit-scrollbar) {
  width: 6px;
  height: 6px;
}

:deep(::-webkit-scrollbar-thumb) {
  background: #dee0e3;
  border-radius: 3px;
}

:deep(::-webkit-scrollbar-thumb:hover) {
  background: #c1c4c9;
}

:deep(::-webkit-scrollbar-track) {
  background: transparent;
}

/* 个人中心抽屉样式 */
.feishu-user-center :deep(.ant-drawer-header) {
  background-color: #FFFFFF;
  border-bottom: 1px solid #F0F0F0;
  padding: 16px 20px;
}

.feishu-user-center :deep(.ant-drawer-body) {
  background-color: #FFFFFF;
  padding: 0;
  overflow-y: auto;
}

.user-center-content {
  width: 100%;
}

/* 1. 个人核心信息区 */
.user-info-section {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 40px 24px 32px 24px;
  background: linear-gradient(180deg, #F8F9FA 0%, #FFFFFF 100%);
  border-bottom: 1px solid #F0F0F0;
  margin-bottom: 8px;
}

.user-avatar-large {
  margin-bottom: 16px;
  background: linear-gradient(135deg, #5B7ADB 0%, #4E59CC 100%);
}

.user-name {
  font-size: 20px;
  font-weight: 600;
  color: #1F2329;
  margin-bottom: 8px;
}

.user-id {
  font-size: 13px;
  color: #8F959E;
  margin-bottom: 8px;
}

.user-signature {
  font-size: 13px;
  color: #8F959E;
  text-align: center;
  max-width: 300px;
}

.edit-profile-btn {
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
  border-radius: 50%;
}

.edit-profile-btn:hover {
  color: #1F2329;
  background-color: #F5F5F5;
}

/* 2. 设置分组 */
.settings-group {
  padding: 20px 0;
  border-bottom: 1px solid #F5F6F7;
}

.settings-group:last-of-type {
  border-bottom: none;
}

.group-title {
  font-size: 11px;
  font-weight: 500;
  color: #BFBFBF;
  padding: 0 24px 12px 24px;
  text-transform: uppercase;
  letter-spacing: 0.8px;
}

.settings-list {
  padding: 0;
}

.setting-item {
  display: flex;
  align-items: center;
  padding: 14px 24px;
  cursor: pointer;
  transition: background 0.2s;
}

.setting-item:hover {
  background-color: #F8F9FA;
}

.setting-icon {
  font-size: 18px;
  color: #666666;
  margin-right: 12px;
  flex-shrink: 0;
}

.setting-label {
  flex: 1;
  font-size: 14px;
  color: #1F2329;
}

.setting-value {
  font-size: 13px;
  color: #8F959E;
  margin-right: 8px;
}

.setting-arrow {
  font-size: 12px;
  color: #BFBFBF;
  flex-shrink: 0;
}

/* 3. 退出登录区域 */
.logout-section {
  padding: 24px;
  text-align: center;
}

.logout-btn {
  height: 40px;
  font-size: 14px;
  font-weight: 500;
  color: #F5222D !important;
  border-color: #F5222D !important;
  background: #FFFFFF !important;
  border-radius: 6px;
}

.logout-btn:hover {
  background: #FFF1F0 !important;
}

.version-info {
  margin-top: 16px;
  font-size: 12px;
  color: #BFBFBF;
}

/* 编辑个人资料抽屉样式 */
.feishu-edit-profile :deep(.ant-drawer-header) {
  background-color: #FFFFFF;
  border-bottom: 1px solid #F0F0F0;
  padding: 16px 20px;
}

.feishu-edit-profile :deep(.ant-drawer-body) {
  background-color: #FFFFFF;
  padding: 0;
  overflow-y: auto;
}

.edit-profile-content {
  width: 100%;
}

/* 头部标题和保存按钮 */
.drawer-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
}

.drawer-title {
  font-size: 16px;
  font-weight: 600;
  color: #1F2329;
}

.header-save-btn {
  height: 32px;
  padding: 0 16px;
  font-size: 14px;
  border-radius: 4px;
}

.header-save-btn:disabled {
  background: #F5F6F7 !important;
  border-color: #F5F6F7 !important;
  color: #BFBFBF !important;
}

/* 头像上传区域 */
.avatar-upload-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 40px 24px 32px 24px;
  background: linear-gradient(180deg, #F8F9FA 0%, #FFFFFF 100%);
  border-bottom: 1px solid #F0F0F0;
}

.avatar-upload {
  position: relative;
  cursor: pointer;
  margin-bottom: 12px;
}

.avatar-edit-icon {
  position: absolute;
  bottom: 0;
  right: 0;
  width: 32px;
  height: 32px;
  background: #FFFFFF;
  border: 2px solid #F0F0F0;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #8F959E;
  font-size: 14px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.avatar-hint {
  font-size: 13px;
  color: #8F959E;
}

.upload-avatar {
  background: linear-gradient(135deg, #5B7ADB 0%, #4E59CC 100%);
}

.upload-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.4);
  border-radius: 50%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.2s;
}

.avatar-upload:hover .upload-overlay {
  opacity: 1;
}

.upload-icon {
  font-size: 24px;
  color: #FFFFFF;
  margin-bottom: 4px;
}

.upload-text {
  font-size: 12px;
  color: #FFFFFF;
}

/* 表单区域 */
.profile-form {
  padding: 24px;
}

.form-item {
  margin-bottom: 24px;
}

.form-label {
  font-size: 14px;
  font-weight: 500;
  color: #1F2329;
  margin-bottom: 8px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.char-count {
  font-size: 12px;
  font-weight: 400;
  color: #8F959E;
}

.form-input,
.form-textarea {
  width: 100%;
}

.form-input :deep(.ant-input-affix-wrapper) {
  border: none;
  border-bottom: 1px solid #E8E8E8;
  border-radius: 0;
  padding-left: 0;
  padding-right: 0;
  box-shadow: none;
  transition: border-color 0.2s;
}

.form-input :deep(.ant-input-affix-wrapper:focus-within) {
  border-bottom: 2px solid #3370FF;
  box-shadow: none;
}

.form-input :deep(.ant-input-affix-wrapper:hover) {
  border-bottom-color: #3370FF;
}

.form-input :deep(.ant-input) {
  border: none;
  padding-left: 0;
  padding-right: 0;
  font-size: 14px;
  box-shadow: none;
}

.form-input :deep(.ant-input:focus) {
  border: none;
  box-shadow: none;
}

.form-textarea :deep(.ant-input) {
  border: 1px solid #E8E8E8;
  border-radius: 4px;
  font-size: 14px;
  resize: none;
}

.form-textarea :deep(.ant-input:focus) {
  border-color: #3370FF;
  box-shadow: 0 0 0 2px rgba(51, 112, 255, 0.1);
}

.id-display {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 0;
  border-bottom: 1px solid #E8E8E8;
}

.id-value {
  font-size: 14px;
  color: #1F2329;
}

.id-display :deep(.ant-btn) {
  color: #3370FF;
  padding: 0;
}

.secure-field {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 12px;
  background: #F5F6F7;
  border-radius: 4px;
}

.secure-value {
  font-size: 14px;
  color: #1F2329;
}

.form-hint {
  font-size: 12px;
  color: #8F959E;
  margin-top: 4px;
}
</style>
