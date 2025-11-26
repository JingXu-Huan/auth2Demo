<template>
  <div class="oauth-callback">
    <div class="loading-container" v-if="loading">
      <el-icon class="is-loading" :size="48"><Loading /></el-icon>
      <p>正在登录中，请稍候...</p>
    </div>
    <div class="error-container" v-else-if="error">
      <el-icon :size="48" color="#f56c6c"><CircleClose /></el-icon>
      <p>{{ error }}</p>
      <el-button type="primary" @click="goToLogin">返回登录</el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Loading, CircleClose } from '@element-plus/icons-vue'
import { useUserStore } from '../stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const loading = ref(true)
const error = ref('')

onMounted(() => {
  handleOAuthCallback()
})

const handleOAuthCallback = async () => {
  try {
    console.log('=== OAuth Callback Debug ===')
    console.log('Full URL:', window.location.href)
    console.log('Full query:', route.query)
    
    // 确保从 query 中正确获取参数（处理可能的数组情况）
    const getQueryParam = (param) => {
      const value = route.query[param]
      return Array.isArray(value) ? value[0] : value
    }
    
    const token = getQueryParam('token')
    const userId = getQueryParam('userId')
    const username = getQueryParam('username')
    const provider = getQueryParam('provider')
    const errorMsg = getQueryParam('error')
    
    console.log('token:', token, 'type:', typeof token)
    console.log('userId:', userId)
    console.log('username:', username)
    console.log('provider:', provider)
    
    if (errorMsg) {
      error.value = decodeURIComponent(errorMsg)
      loading.value = false
      return
    }
    
    if (!token) {
      console.error('Token is missing!')
      error.value = '未获取到登录凭证'
      loading.value = false
      return
    }
    
    // 保存登录信息
    console.log('Calling userStore.login...')
    userStore.login({
      id: userId,
      username: decodeURIComponent(username || ''),
      provider
    }, token)
    
    // 确保 token 已写入 localStorage
    console.log('After login, localStorage token:', localStorage.getItem('token'))
    
    // 验证 token 是否正确保存
    const savedToken = localStorage.getItem('token')
    if (!savedToken) {
      console.error('Token was not saved to localStorage!')
      error.value = 'Token 保存失败'
      loading.value = false
      return
    }
    
    ElMessage.success(`${provider === 'gitee' ? 'Gitee' : provider} 登录成功`)
    
    // 使用 replace 而不是 push，避免返回到回调页面
    console.log('Redirecting to /chat')
    router.replace('/chat')
  } catch (e) {
    console.error('OAuth回调处理失败:', e)
    error.value = '登录处理失败，请重试'
    loading.value = false
  }
}

const goToLogin = () => {
  router.push('/login')
}
</script>

<style scoped>
.oauth-callback {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f8f9fa;
}

.loading-container,
.error-container {
  text-align: center;
  padding: 48px;
  background: white;
  border-radius: 16px;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.08);
}

.loading-container p,
.error-container p {
  margin: 24px 0;
  color: #606266;
  font-size: 16px;
}

.is-loading {
  animation: rotating 1.5s linear infinite;
}

@keyframes rotating {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}
</style>
