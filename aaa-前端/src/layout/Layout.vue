<template>
  <div class="layout-container">
    <!-- 侧边导航栏 -->
    <div class="sidebar">
      <div class="logo">
        <img src="/file.svg" alt="Logo" class="logo-icon" />
        <span class="logo-text">IM聊天</span>
      </div>
      
      <div class="nav-menu">
        <div 
          v-for="item in menuItems" 
          :key="item.path"
          :class="['nav-item', { active: $route.path.includes(item.path) }]"
          @click="navigateTo(item.path)"
        >
          <el-icon class="nav-icon">
            <component :is="item.icon" />
          </el-icon>
          <span class="nav-text">{{ item.title }}</span>
        </div>
      </div>
      
      <div class="user-section">
        <el-dropdown @command="handleUserCommand">
          <div class="user-info">
            <el-avatar :src="userStore.user?.avatar" :size="32">
              {{ userStore.user?.nickname?.charAt(0) }}
            </el-avatar>
            <div class="user-details">
              <div class="username">{{ userStore.user?.nickname }}</div>
              <div class="status online">在线</div>
            </div>
          </div>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile">个人资料</el-dropdown-item>
              <el-dropdown-item command="settings">设置</el-dropdown-item>
              <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </div>
    
    <!-- 主内容区域 -->
    <div class="main-content">
      <div class="content-header">
        <div class="breadcrumb">
          <el-breadcrumb separator="/">
            <el-breadcrumb-item>工作台</el-breadcrumb-item>
            <el-breadcrumb-item>{{ currentPageTitle }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        
        <div class="header-actions">
          <el-button circle>
            <el-icon><Search /></el-icon>
          </el-button>
          <el-button circle>
            <el-icon><Bell /></el-icon>
          </el-button>
          <el-button circle>
            <el-icon><Setting /></el-icon>
          </el-button>
        </div>
      </div>
      
      <div class="content-body">
        <router-view />
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '../stores/user'
import { 
  ChatDotRound, 
  User, 
  UserFilled,
  Search,
  Bell,
  Setting
} from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const menuItems = [
  { path: 'messages', title: '消息', icon: 'ChatDotRound' },
  { path: 'contacts', title: '通讯录', icon: 'User' }
  // 群聊功能已整合到通讯录中
]

const currentPageTitle = computed(() => {
  const currentItem = menuItems.find(item => route.path.includes(item.path))
  return currentItem?.title || '工作台'
})

const navigateTo = (path) => {
  router.push(`/im/${path}`)
}

const handleUserCommand = (command) => {
  switch (command) {
    case 'profile':
      // 打开个人资料页面
      break
    case 'settings':
      // 打开设置页面
      break
    case 'logout':
      userStore.logout()
      router.push('/login')
      break
  }
}

// 登录状态由路由守卫处理
</script>

<style scoped>
.layout-container {
  display: flex;
  height: 100vh;
  background: #f5f5f5;
}

.sidebar {
  width: 240px;
  background: #fff;
  border-right: 1px solid #e8e8e8;
  display: flex;
  flex-direction: column;
}

.logo {
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 12px;
  border-bottom: 1px solid #f0f0f0;
}

.logo-icon {
  width: 64px;
  height: 64px;
  object-fit: contain;
}

.logo-text {
  font-size: 18px;
  font-weight: 600;
  color: #333;
}

.nav-menu {
  flex: 1;
  padding: 20px 0;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 20px;
  cursor: pointer;
  transition: all 0.3s;
  color: #666;
}

.nav-item:hover {
  background: #f5f5f5;
  color: #409eff;
}

.nav-item.active {
  background: #e6f7ff;
  color: #409eff;
  border-right: 3px solid #409eff;
}

.nav-icon {
  font-size: 18px;
}

.nav-text {
  font-size: 14px;
  font-weight: 500;
}

.user-section {
  padding: 20px;
  border-top: 1px solid #f0f0f0;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  padding: 8px;
  border-radius: 8px;
  transition: background 0.3s;
}

.user-info:hover {
  background: #f5f5f5;
}

.user-details {
  flex: 1;
}

.username {
  font-size: 14px;
  font-weight: 500;
  color: #333;
}

.status {
  font-size: 12px;
  margin-top: 2px;
}

.status.online {
  color: #67c23a;
}

.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.content-header {
  background: #fff;
  padding: 16px 24px;
  border-bottom: 1px solid #e8e8e8;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.breadcrumb {
  font-size: 14px;
}

.header-actions {
  display: flex;
  gap: 8px;
}

.content-body {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .sidebar {
    width: 60px;
  }
  
  .logo-text,
  .nav-text,
  .user-details {
    display: none;
  }
  
  .logo {
    justify-content: center;
  }
  
  .nav-item {
    justify-content: center;
    padding: 12px;
  }
  
  .user-info {
    justify-content: center;
  }
}
</style>
