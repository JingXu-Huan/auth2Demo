<template>
  <router-view />
</template>

<script setup>
import { watch } from 'vue'
import { useUserStore } from './stores/user'
import websocket from './utils/websocket'

const userStore = useUserStore()

// 监听登录状态，自动连接/断开WebSocket
watch(() => userStore.isLoggedIn, (loggedIn) => {
  if (loggedIn && userStore.user?.id) {
    websocket.connect(userStore.user.id)
  } else {
    websocket.disconnect()
  }
}, { immediate: true })
</script>

<style>
html, body, #app {
  margin: 0;
  padding: 0;
  height: 100%;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'PingFang SC', 'Microsoft YaHei', sans-serif;
}
</style>
