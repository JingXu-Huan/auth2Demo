<template>
  <div id="app">
    <router-view />
  </div>
</template>

<script setup>
import { watch, onMounted, onUnmounted } from 'vue'
import { useUserStore } from './stores/user'
import websocketService from './utils/websocket'

const userStore = useUserStore()

// 监听用户登录状态
watch(
  () => userStore.isLoggedIn,
  (isLoggedIn) => {
    if (isLoggedIn && userStore.user && userStore.user.id) {
      // 用户登录后立即建立 WebSocket 连接
      console.log('用户已登录，建立 WebSocket 连接, userId:', userStore.user.id)
      websocketService.connect(userStore.user.id)
    } else {
      // 用户登出后断开连接
      console.log('用户已登出，断开 WebSocket 连接')
      websocketService.disconnect()
    }
  },
  { immediate: true }
)

onMounted(() => {
  // 页面加载时，如果用户已登录，建立连接
  if (userStore.isLoggedIn && userStore.user && userStore.user.id) {
    console.log('页面加载，用户已登录，建立 WebSocket 连接')
    websocketService.connect(userStore.user.id)
  }
})

onUnmounted(() => {
  // 页面卸载时断开连接
  websocketService.disconnect()
})
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  background-color: #f5f5f5;
}

#app {
  height: 100vh;
  width: 100vw;
}
</style>
