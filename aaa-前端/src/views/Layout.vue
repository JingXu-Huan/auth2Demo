<template>
  <div class="app-layout">
    <!-- 侧边导航 -->
    <aside class="sidebar">
      <div class="sidebar-top">
        <div class="logo">
          <el-icon :size="28"><ChatDotRound /></el-icon>
        </div>
        
        <nav class="nav-menu">
          <div
            v-for="item in menuItems"
            :key="item.path"
            :class="['nav-item', { active: $route.path === item.path }]"
            @click="$router.push(item.path)"
          >
            <el-badge :value="item.badge" :hidden="!item.badge" :offset="[2, -2]">
              <el-icon :size="22"><component :is="item.icon" /></el-icon>
            </el-badge>
            <span class="nav-label">{{ item.label }}</span>
          </div>
        </nav>
      </div>
      
      <div class="sidebar-bottom">
        <el-dropdown trigger="click" placement="right-start">
          <div class="user-avatar-wrapper">
            <el-avatar :size="40" :src="userStore.userAvatar">
              {{ userStore.userName?.charAt(0) }}
            </el-avatar>
          </div>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item disabled>
                <div class="user-info-dropdown">
                  <strong>{{ userStore.userName }}</strong>
                  <span>{{ userStore.user?.email }}</span>
                </div>
              </el-dropdown-item>
              <el-dropdown-item divided @click="$router.push('/settings')">
                <el-icon><Setting /></el-icon>
                设置
              </el-dropdown-item>
              <el-dropdown-item divided @click="handleLogout">
                <el-icon><SwitchButton /></el-icon>
                退出登录
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </aside>

    <!-- 主内容区 -->
    <main class="main-content">
      <router-view v-slot="{ Component }">
        <transition name="fade" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </main>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { ChatDotRound, ChatLineRound, User, Document, Setting, SwitchButton } from '@element-plus/icons-vue'
import { useUserStore } from '../stores/user'
import { useChatStore } from '../stores/chat'

const router = useRouter()
const userStore = useUserStore()
const chatStore = useChatStore()

const menuItems = computed(() => [
  { path: '/chat', label: '消息', icon: 'ChatLineRound', badge: chatStore.totalUnread || null },
  { path: '/contacts', label: '通讯录', icon: 'User', badge: null },
  { path: '/documents', label: '文档', icon: 'Document', badge: null }
])

const handleLogout = () => {
  ElMessageBox.confirm('确定要退出登录吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    userStore.logout()
    chatStore.clear()
    router.push('/login')
  }).catch(() => {})
}
</script>

<style scoped>
.app-layout {
  display: flex;
  height: 100vh;
  background: #f5f6f7;
}

.sidebar {
  width: 72px;
  background: #2b2f36;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 16px 0;
}

.sidebar-top {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.logo {
  width: 44px;
  height: 44px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  margin-bottom: 24px;
}

.nav-menu {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.nav-item {
  width: 52px;
  height: 52px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  border-radius: 12px;
  cursor: pointer;
  color: #8f959e;
  transition: all 0.2s;
}

.nav-item:hover {
  background: rgba(255, 255, 255, 0.08);
  color: #fff;
}

.nav-item.active {
  background: rgba(51, 112, 255, 0.15);
  color: #3370ff;
}

.nav-label {
  font-size: 10px;
  margin-top: 2px;
}

.sidebar-bottom {
  display: flex;
  justify-content: center;
}

.user-avatar-wrapper {
  cursor: pointer;
  transition: transform 0.2s;
}

.user-avatar-wrapper:hover {
  transform: scale(1.05);
}

.user-info-dropdown {
  display: flex;
  flex-direction: column;
  padding: 8px 0;
}

.user-info-dropdown strong {
  color: #1f2329;
}

.user-info-dropdown span {
  font-size: 12px;
  color: #8f959e;
  margin-top: 4px;
}

.main-content {
  flex: 1;
  overflow: hidden;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.15s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
