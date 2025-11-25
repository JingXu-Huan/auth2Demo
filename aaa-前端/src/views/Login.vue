<template>
  <div class="login-page">
    <!-- 左侧装饰区 -->
    <div class="login-left">
      <div class="brand">
        <div class="logo">
          <el-icon :size="48"><ChatDotRound /></el-icon>
        </div>
        <h1>AIO 协作平台</h1>
        <p>企业级即时通讯解决方案</p>
      </div>
      <div class="features">
        <div class="feature-item">
          <el-icon><ChatLineSquare /></el-icon>
          <span>即时消息</span>
        </div>
        <div class="feature-item">
          <el-icon><VideoCamera /></el-icon>
          <span>视频会议</span>
        </div>
        <div class="feature-item">
          <el-icon><Document /></el-icon>
          <span>协同文档</span>
        </div>
        <div class="feature-item">
          <el-icon><Files /></el-icon>
          <span>云端存储</span>
        </div>
      </div>
    </div>

    <!-- 右侧登录表单 -->
    <div class="login-right">
      <div class="login-card">
        <h2>欢迎回来</h2>
        <p class="subtitle">登录您的账户开始协作</p>

        <el-form ref="formRef" :model="form" :rules="rules" @submit.prevent="handleLogin">
          <el-form-item prop="email">
            <el-input
              v-model="form.email"
              placeholder="请输入邮箱"
              size="large"
              :prefix-icon="Message"
            />
          </el-form-item>

          <el-form-item prop="password">
            <el-input
              v-model="form.password"
              type="password"
              placeholder="请输入密码"
              size="large"
              :prefix-icon="Lock"
              show-password
              @keyup.enter="handleLogin"
            />
          </el-form-item>

          <div class="form-options">
            <el-checkbox v-model="rememberMe">记住我</el-checkbox>
            <el-link type="primary">忘记密码？</el-link>
          </div>

          <el-form-item>
            <el-button
              type="primary"
              size="large"
              :loading="loading"
              class="login-btn"
              @click="handleLogin"
            >
              登 录
            </el-button>
          </el-form-item>
        </el-form>

        <el-divider>其他登录方式</el-divider>

        <div class="oauth-buttons">
          <el-button class="oauth-btn gitee" @click="handleGiteeLogin">
            <svg viewBox="0 0 1024 1024" width="20" height="20">
              <path fill="#C71D23" d="M512 1024C229.222 1024 0 794.778 0 512S229.222 0 512 0s512 229.222 512 512-229.222 512-512 512z m259.149-568.883h-290.74a25.293 25.293 0 0 0-25.292 25.293l-0.026 63.206c0 13.952 11.315 25.293 25.267 25.293h177.024c13.978 0 25.293 11.315 25.293 25.267v12.646a75.853 75.853 0 0 1-75.853 75.853h-240.23a25.293 25.293 0 0 1-25.267-25.293V417.203a75.853 75.853 0 0 1 75.827-75.853h353.946a25.293 25.293 0 0 0 25.267-25.292l0.077-63.207a25.293 25.293 0 0 0-25.268-25.293H417.152a189.62 189.62 0 0 0-189.62 189.645V771.15c0 13.977 11.316 25.293 25.294 25.293h372.94a170.65 170.65 0 0 0 170.65-170.65V480.384a25.293 25.293 0 0 0-25.267-25.267z"/>
            </svg>
            Gitee 登录
          </el-button>
        </div>

        <div class="register-link">
          还没有账号？
          <router-link to="/register">立即注册</router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Message, Lock, ChatDotRound, ChatLineSquare, VideoCamera, Document, Files } from '@element-plus/icons-vue'
import { useUserStore } from '../stores/user'
import { authApi } from '../api'

const router = useRouter()
const userStore = useUserStore()

const formRef = ref()
const loading = ref(false)
const rememberMe = ref(false)

const form = reactive({
  email: '',
  password: ''
})

const rules = {
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少6位', trigger: 'blur' }
  ]
}

const handleLogin = async () => {
  if (!formRef.value) return
  
  try {
    await formRef.value.validate()
    loading.value = true

    const res = await authApi.login(form.email, form.password)
    
    if (res.code === 200 && res.data) {
      const { token, userId, username, email, displayName, avatarUrl } = res.data
      
      userStore.login({
        id: userId,
        username,
        email,
        nickname: displayName || username,
        avatar: avatarUrl
      }, token)
      
      ElMessage.success('登录成功')
      router.push('/')
    } else {
      ElMessage.error(res.message || '登录失败')
    }
  } catch (error) {
    console.error('登录失败:', error)
    ElMessage.error(error.response?.data?.message || '登录失败，请检查邮箱和密码')
  } finally {
    loading.value = false
  }
}

const handleGiteeLogin = () => {
  window.location.href = 'http://localhost:9000/oauth/gitee/login'
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
}

.login-left {
  flex: 1;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  padding: 60px;
  color: white;
}

.brand {
  text-align: center;
  margin-bottom: 60px;
}

.logo {
  width: 80px;
  height: 80px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 20px;
  backdrop-filter: blur(10px);
}

.brand h1 {
  font-size: 32px;
  font-weight: 600;
  margin: 0 0 10px;
}

.brand p {
  font-size: 16px;
  opacity: 0.9;
  margin: 0;
}

.features {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 24px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 12px;
  backdrop-filter: blur(10px);
  font-size: 15px;
}

.feature-item .el-icon {
  font-size: 24px;
}

.login-right {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f8f9fa;
  padding: 40px;
}

.login-card {
  width: 100%;
  max-width: 420px;
  background: white;
  border-radius: 16px;
  padding: 48px 40px;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.08);
}

.login-card h2 {
  font-size: 28px;
  font-weight: 600;
  color: #1f2329;
  margin: 0 0 8px;
  text-align: center;
}

.subtitle {
  color: #8f959e;
  text-align: center;
  margin: 0 0 32px;
}

.form-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.login-btn {
  width: 100%;
  height: 48px;
  font-size: 16px;
  border-radius: 8px;
}

.oauth-buttons {
  display: flex;
  justify-content: center;
  gap: 16px;
}

.oauth-btn {
  flex: 1;
  height: 44px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.oauth-btn.gitee {
  border-color: #c71d23;
  color: #c71d23;
}

.oauth-btn.gitee:hover {
  background: #fff1f0;
}

.register-link {
  text-align: center;
  margin-top: 24px;
  color: #8f959e;
}

.register-link a {
  color: #409eff;
  text-decoration: none;
  font-weight: 500;
}

@media (max-width: 900px) {
  .login-left {
    display: none;
  }
  
  .login-right {
    padding: 20px;
  }
  
  .login-card {
    padding: 32px 24px;
  }
}
</style>
