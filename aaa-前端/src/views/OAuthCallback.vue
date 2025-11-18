<template>
  <div class="oauth-callback">
    <div class="callback-container">
      <div v-if="loading" class="loading-section">
        <el-icon class="loading-icon"><Loading /></el-icon>
        <h3>正在处理登录...</h3>
        <p>请稍候，我们正在验证您的身份</p>
      </div>
      
      <div v-else-if="success" class="success-section">
        <el-icon class="success-icon"><SuccessFilled /></el-icon>
        <h3>登录成功！</h3>
        <p>欢迎回来，{{ userInfo.username }}</p>
        <el-button type="primary" @click="goToApp">进入应用</el-button>
      </div>
      
      <div v-else class="error-section">
        <el-icon class="error-icon"><CircleCloseFilled /></el-icon>
        <h3>登录失败</h3>
        <p>{{ errorMessage }}</p>
        <el-button @click="goToLogin">返回登录</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Loading, SuccessFilled, CircleCloseFilled } from '@element-plus/icons-vue'
import { useUserStore } from '../stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const loading = ref(true)
const success = ref(false)
const errorMessage = ref('')
const userInfo = ref({})

onMounted(async () => {
  try {
    // 检查URL参数
    const { success: successParam, error: errorParam, username, email, id } = route.query
    
    console.log('OAuth回调参数:', { successParam, errorParam, username, email, id })
    
    if (successParam === 'false' || errorParam) {
      throw new Error(errorParam || 'OAuth授权失败')
    }
    
    if (successParam !== 'true') {
      throw new Error('未知的回调状态')
    }
    
    // 从URL参数获取用户信息（避免Session跨域问题）
    if (!username || !id) {
      console.error('用户信息不完整:', { username, id })
      throw new Error('用户信息不完整，请重新登录')
    }
    
    const user = {
      id: parseInt(id),
      username: username,
      email: email || '',
      nickname: username,
      avatar: ''
    }
    
    userInfo.value = user
    
    // 保存用户信息到store
    userStore.setUser(user)
    userStore.setToken('gitee-oauth-token-' + Date.now())
    
    success.value = true
    ElMessage.success('Gitee登录成功！')
    
    // 2秒后自动跳转
    setTimeout(() => {
      goToApp()
    }, 2000)
    
  } catch (error) {
    console.error('OAuth回调处理失败:', error)
    errorMessage.value = error.message || '登录处理失败'
    success.value = false
  } finally {
    loading.value = false
  }
})

const goToApp = () => {
  router.push('/im')
}

const goToLogin = () => {
  router.push('/login')
}
</script>

<style scoped>
.oauth-callback {
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.callback-container {
  text-align: center;
  padding: 40px;
  background: white;
  border-radius: 16px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.1);
  max-width: 400px;
  width: 100%;
}

.loading-section,
.success-section,
.error-section {
  padding: 20px 0;
}

.loading-icon {
  font-size: 48px;
  color: #409eff;
  animation: spin 1s linear infinite;
}

.success-icon {
  font-size: 48px;
  color: #67c23a;
}

.error-icon {
  font-size: 48px;
  color: #f56c6c;
}

h3 {
  margin: 20px 0 10px 0;
  color: #333;
  font-size: 24px;
}

p {
  margin: 0 0 30px 0;
  color: #666;
  font-size: 16px;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}
</style>
