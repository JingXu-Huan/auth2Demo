<template>
  <div class="login-container">
    <div class="login-box">
      <div class="login-left">
        <div class="welcome-graphic">
          <div
            ref="lottieContainer"
            class="welcome-lottie"
          ></div>
        </div>
      </div>

      <div class="login-right">
        <div class="logo-section">
          <h2>AIO</h2>
          <p>企业级即时通讯解决方案</p>
        </div>
        
        <!-- 登录表单 -->
        <el-form v-if="!showRegister" :model="loginForm" :rules="loginRules" ref="loginFormRef" label-width="0" @submit.prevent>
        <!-- 步骤1: 输入邮箱 -->
        <template v-if="loginStep === 1">
          <el-form-item prop="email">
            <el-input 
              v-model="loginForm.email" 
              placeholder="请输入邮箱"
              prefix-icon="Message"
              size="large"
              @keyup.enter="handleEmailCheck"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleEmailCheck" :loading="loading" size="large" style="width: 100%">
              下一步
            </el-button>
          </el-form-item>
        </template>
        
        <!-- 步骤2: 输入密码 -->
        <template v-else>
          <!-- 显示邮箱，可点击修改 -->
          <div class="email-display">
            <span>{{ loginForm.email }}</span>
            <el-button type="text" @click="resetLoginStep" size="small">修改</el-button>
          </div>
          
          <el-form-item prop="password">
            <el-input 
              v-model="loginForm.password" 
              type="password" 
              placeholder="请输入密码"
              prefix-icon="Lock"
              size="large"
              @keyup.enter="handleLogin"
              show-password
              autofocus
            />
          </el-form-item>
          
          <!-- 安全验证码（30天未登录时显示） -->
          <el-form-item v-if="showSecurityVerification" prop="securityCode">
            <div style="display: flex; gap: 8px;">
              <el-input 
                v-model="loginForm.securityCode" 
                placeholder="请输入安全验证码"
                prefix-icon="Key"
                size="large"
                style="flex: 1;"
              />
              <el-button 
                @click="sendSecurityCode" 
                :disabled="codeSending || countdown > 0"
                size="large"
                style="width: 120px;"
              >
                {{ countdown > 0 ? `${countdown}秒后重试` : '发送验证码' }}
              </el-button>
            </div>
            <div style="margin-top: 8px; font-size: 12px; color: #909399;">
              您已超过30天未登录，需要进行安全验证
            </div>
          </el-form-item>
          
          <el-form-item>
            <el-button type="primary" @click="handleLogin" :loading="loading" size="large" style="width: 100%">
              登录
            </el-button>
          </el-form-item>
        </template>
        
        <!-- 第三方登录 -->
        <div class="oauth-section">
          <div class="divider">
            <span>或</span>
          </div>
          <el-button @click="handleGiteeLogin" size="large" style="width: 100%; margin-bottom: 16px;" class="gitee-btn">
            <svg style="width: 20px; height: 20px; margin-right: 8px;" viewBox="0 0 1024 1024">
              <path fill="#C71D23" d="M512 1024C229.222 1024 0 794.778 0 512S229.222 0 512 0s512 229.222 512 512-229.222 512-512 512z m259.149-568.883h-290.74a25.293 25.293 0 0 0-25.292 25.293l-0.026 63.206c0 13.952 11.315 25.293 25.267 25.293h177.024c13.978 0 25.293 11.315 25.293 25.267v12.646a75.853 75.853 0 0 1-75.853 75.853h-240.23a25.293 25.293 0 0 1-25.267-25.293V417.203a75.853 75.853 0 0 1 75.827-75.853h353.946a25.293 25.293 0 0 0 25.267-25.292l0.077-63.207a25.293 25.293 0 0 0-25.268-25.293H417.152a189.62 189.62 0 0 0-189.62 189.645V771.15c0 13.977 11.316 25.293 25.294 25.293h372.94a170.65 170.65 0 0 0 170.65-170.65V480.384a25.293 25.293 0 0 0-25.267-25.267z"/>
            </svg>
            使用 Gitee 登录
          </el-button>
        </div>
        
        <div class="form-footer">
          <span>还没有账号？</span>
          <el-button type="text" @click="showRegister = true">立即注册</el-button>
        </div>
      </el-form>
      
      <!-- 注册表单 -->
      <el-form v-else :model="registerForm" :rules="registerRules" ref="registerFormRef" label-width="0" @submit.prevent>
        <el-form-item prop="username">
          <el-input 
            v-model="registerForm.username" 
            placeholder="请输入用户名"
            prefix-icon="User"
            size="large"
          />
        </el-form-item>
        <el-form-item prop="email">
          <el-input 
            v-model="registerForm.email" 
            placeholder="请输入邮箱"
            prefix-icon="Message"
            size="large"
          />
        </el-form-item>
        <el-form-item prop="verificationCode">
          <div style="display: flex; gap: 8px;">
            <el-input 
              v-model="registerForm.verificationCode" 
              placeholder="请输入验证码"
              prefix-icon="Key"
              size="large"
              style="flex: 1;"
            />
            <el-button 
              @click="sendVerificationCode" 
              :disabled="codeSending || countdown > 0"
              size="large"
              style="width: 120px;"
            >
              {{ countdown > 0 ? `${countdown}秒后重试` : '发送验证码' }}
            </el-button>
          </div>
        </el-form-item>
        <el-form-item prop="password">
          <el-input 
            v-model="registerForm.password" 
            type="password" 
            placeholder="请输入密码"
            prefix-icon="Lock"
            size="large"
            show-password
          />
        </el-form-item>
        <el-form-item prop="confirmPassword">
          <el-input 
            v-model="registerForm.confirmPassword" 
            type="password" 
            placeholder="确认密码"
            prefix-icon="Lock"
            size="large"
            show-password
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleRegister" :loading="registerLoading" size="large" style="width: 100%">
            注册
          </el-button>
        </el-form-item>
        <div class="form-footer">
          <span>已有账号？</span>
          <el-button type="text" @click="showRegister = false">立即登录</el-button>
        </div>
      </el-form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../stores/user'
import { userAPI, authAPI } from '../api'
import lottie from 'lottie-web'
import coolAnimationData from '../asserts/酷.json'

const router = useRouter()
const userStore = useUserStore()
const loginFormRef = ref()
const registerFormRef = ref()
const loading = ref(false)
const registerLoading = ref(false)
const showRegister = ref(false)
const codeSending = ref(false)
const countdown = ref(0)
const showSecurityVerification = ref(false)
const securityEmail = ref('')
const loginStep = ref(1) // 登录步骤：1=输入邮箱，2=输入密码

const lottieContainer = ref(null)
let lottieInstance = null

onMounted(() => {
  if (lottieContainer.value) {
    lottieInstance = lottie.loadAnimation({
      container: lottieContainer.value,
      renderer: 'svg',
      loop: true,
      autoplay: true,
      animationData: coolAnimationData
    })
  }
})

onBeforeUnmount(() => {
  if (lottieInstance) {
    lottieInstance.destroy()
    lottieInstance = null
  }
})

// 登录表单
const loginForm = reactive({
  email: '',
  password: '',
  securityCode: ''
})

// 注册表单
const registerForm = reactive({
  username: '',
  email: '',
  verificationCode: '',
  password: '',
  confirmPassword: ''
})

// 登录表单验证规则
const loginRules = {
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
  ]
}

// 注册表单验证规则
const registerRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度在3到20个字符', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9_]+$/, message: '用户名只能包含字母、数字和下划线', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ],
  verificationCode: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { len: 6, message: '验证码为6位数字', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度在6到20个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { 
      validator: (rule, value, callback) => {
        if (value !== registerForm.password) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      }, 
      trigger: 'blur' 
    }
  ]
}

// Gitee登录处理
const handleGiteeLogin = () => {
  // 跳转到后端的Gitee OAuth登录接口（通过Gateway代理）
  window.location.href = 'http://localhost:9000/oauth/gitee/login'
}

// 重置登录步骤
const resetLoginStep = () => {
  loginStep.value = 1
  loginForm.password = ''
  loginForm.securityCode = ''
  showSecurityVerification.value = false
  countdown.value = 0
}

// 步骤1: 验证邮箱是否存在
const handleEmailCheck = async () => {
  if (!loginFormRef.value) return
  
  try {
    // 只验证邮箱字段
    await loginFormRef.value.validateField('email')
    loading.value = true
    
    // 调用后端接口检查邮箱是否存在
    const result = await userAPI.checkEmail(loginForm.email)
    
    if (!result.data) {
      ElMessage.error('该邮箱未注册，请先注册')
      return
    }
    
    // 邮箱存在，进入下一步
    loginStep.value = 2
    ElMessage.success('请输入密码')
    
  } catch (error) {
    if (error.message) {
      // 表单验证错误
      return
    }
    console.error('邮箱验证失败:', error)
    console.error('错误详情:', error.response?.data)
    console.error('状态码:', error.response?.status)
    
    if (error.response?.status === 401) {
      ElMessage.error('认证失败，请检查服务配置')
    } else {
      ElMessage.error('验证失败，请重试')
    }
  } finally {
    loading.value = false
  }
}

// 发送验证码
const sendVerificationCode = async () => {
  // 验证邮箱格式
  if (!registerForm.email) {
    ElMessage.error('请先输入邮箱')
    return
  }
  
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
  if (!emailRegex.test(registerForm.email)) {
    ElMessage.error('请输入正确的邮箱格式')
    return
  }
  
  try {
    codeSending.value = true
    
    // 检查邮箱是否已注册
    const emailCheck = await userAPI.checkEmail(registerForm.email)
    if (emailCheck && emailCheck.data === true) {
      ElMessage.error('该邮箱已被注册')
      return
    }
    
    // 调用发送验证码API
    await userAPI.sendVerificationCode(registerForm.email)
    ElMessage.success('验证码已发送到您的邮箱，请查收（有效期5分钟）')
    
    // 开始倒计时
    countdown.value = 60
    const timer = setInterval(() => {
      countdown.value--
      if (countdown.value <= 0) {
        clearInterval(timer)
      }
    }, 1000)
    
  } catch (error) {
    console.error('发送验证码失败:', error)
    ElMessage.error('发送验证码失败: ' + (error.message || '网络错误'))
  } finally {
    codeSending.value = false
  }
}

// 发送安全验证码
const sendSecurityCode = async () => {
  try {
    codeSending.value = true
    await userAPI.sendSecurityCode(securityEmail.value)
    ElMessage.success('安全验证码已发送到您的邮箱')
    
    // 开始倒计时
    countdown.value = 60
    const timer = setInterval(() => {
      countdown.value--
      if (countdown.value <= 0) {
        clearInterval(timer)
      }
    }, 1000)
  } catch (error) {
    console.error('发送验证码失败:', error)
    ElMessage.error('发送验证码失败')
  } finally {
    codeSending.value = false
  }
}

// 步骤2: 传统登录处理（密码验证）
const handleLogin = async () => {
  if (!loginFormRef.value) return
  
  // 确保在步骤2
  if (loginStep.value !== 2) {
    await handleEmailCheck()
    return
  }
  
  try {
    loading.value = true
    
    console.log('开始登录流程: email=', loginForm.email)
    
    // 调用 OAuth2-auth-server 的登录接口，后端验证密码并返回 JWT token
    const loginResponse = await authAPI.login(loginForm.email, loginForm.password)
    console.log('✓ 登录响应原始数据:', loginResponse)
    console.log('✓ 响应类型:', typeof loginResponse)
    console.log('✓ 响应 code:', loginResponse?.code)
    console.log('✓ 响应 message:', loginResponse?.message)
    
    // 检查响应
    if (!loginResponse || loginResponse.code !== 200) {
      console.error('❌ 登录失败检查: loginResponse=', loginResponse)
      ElMessage.error(loginResponse?.message || '登录失败')
      return
    }
    
    console.log('✓ 通过响应检查，继续处理...')
    
    const loginData = loginResponse.data
    console.log('登录数据:', loginData)
    
    if (!loginData || !loginData.token) {
      console.error('登录响应缺少 token:', loginData)
      ElMessage.error('登录失败：未获取到 token')
      return
    }
    
    // 构造用户信息
    const user = {
      id: loginData.userId,
      email: loginData.email,
      username: loginData.username,
      nickname: loginData.displayName || loginData.username,
      avatar: loginData.avatarUrl || ''
    }
    
    console.log('准备保存的用户信息:', user)
    console.log('JWT Token:', loginData.token)
    
    if (!user.id) {
      console.error('严重错误：用户ID为空！', loginData)
      ElMessage.error('登录失败：无法获取用户ID')
      return
    }
    
    // 保存真实的 JWT token 和用户信息
    userStore.setUser(user)
    userStore.setToken(loginData.token)
    
    console.log('保存后的 userStore.user:', userStore.user)
    console.log('保存后的 userStore.token:', userStore.token)
    console.log('保存后的 userStore.user.id:', userStore.user?.id)
    
    ElMessage.success('登录成功')
    
    // 重置表单
    loginForm.password = ''
    loginForm.securityCode = ''
    countdown.value = 0
    
    // 跳转到主页面
    router.push('/im')
    
  } catch (error) {
    console.error('登录失败:', error)
    ElMessage.error('登录失败: ' + (error.message || '请检查邮箱和密码'))
  } finally {
    loading.value = false
  }
}

// 注册处理
const handleRegister = async () => {
  if (!registerFormRef.value) return
  
  try {
    await registerFormRef.value.validate()
    registerLoading.value = true
    
    // 步骤1: 验证邮箱验证码
    const verifyResult = await userAPI.verifyEmailCode(registerForm.email, registerForm.verificationCode)
    
    if (!verifyResult.data) {
      ElMessage.error('验证码错误或已过期')
      return
    }
    
    // 步骤2: 调用注册API
    const result = await userAPI.register({
      username: registerForm.username,
      email: registerForm.email,
      password: registerForm.password,
      verificationCode: registerForm.verificationCode
    })
    
    if (result.code === 200 || result.success) {
      ElMessage.success('注册成功！请登录')
      showRegister.value = false
      // 清空注册表单
      Object.assign(registerForm, {
        username: '',
        email: '',
        verificationCode: '',
        password: '',
        confirmPassword: ''
      })
      countdown.value = 0
    } else {
      ElMessage.error(result.message || '注册失败')
    }
    
  } catch (error) {
    console.error('注册失败:', error)
    ElMessage.error('注册失败: ' + (error.message || '网络错误'))
  } finally {
    registerLoading.value = false
  }
}
</script>

<style scoped>
.login-container {
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: radial-gradient(circle at 0 0, #67b5ff 0%, #3b82f6 35%, #1d4ed8 80%);
  padding: 20px;
}

.login-box {
  width: 100%;
  max-width: 960px;
  min-height: 520px;
  display: flex;
  background: #ffffff;
  border-radius: 24px;
  box-shadow: 0 24px 60px rgba(15, 23, 42, 0.35);
  overflow: hidden;
  backdrop-filter: blur(10px);
}

.login-left {
  flex: 1.1;
  padding: 40px 32px;
  background: linear-gradient(145deg, #2563eb 0%, #4f46e5 45%, #7c3aed 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  color: #ffffff;
}

.login-left::before,
.login-left::after {
  content: '';
  position: absolute;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.16);
  filter: blur(0.5px);
}

.login-left::before {
  width: 220px;
  height: 220px;
  top: 8%;
  left: -40px;
}

.login-left::after {
  width: 280px;
  height: 280px;
  bottom: -60px;
  right: -40px;
}

.welcome-graphic {
  position: relative;
  z-index: 1;
  max-width: 320px;
  text-align: center;
}

.welcome-lottie {
  width: 360px;
  height: 360px;
  margin: 0 auto;
}

.welcome-image {
  width: 140px;
  height: 140px;
  object-fit: contain;
  margin: 0 auto 20px;
  display: block;
}

.welcome-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 10px 24px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.92);
  color: #1d4ed8;
  font-weight: 600;
  margin-bottom: 20px;
  box-shadow: 0 10px 30px rgba(15, 23, 42, 0.25);
}

.welcome-text {
  margin: 0;
  font-size: 18px;
  line-height: 1.6;
  color: rgba(241, 245, 249, 0.96);
}

.login-right {
  flex: 1;
  padding: 40px 48px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  background: #ffffff;
}

.logo-section {
  text-align: center;
  margin-bottom: 40px;
}

.logo {
  font-size: 48px;
  margin-bottom: 16px;
  display: inline-block;
  padding: 16px;
  background: linear-gradient(135deg, #409eff, #67c23a);
  border-radius: 50%;
  box-shadow: 0 8px 16px rgba(64, 158, 255, 0.3);
}

.logo-section h2 {
  margin: 0 0 8px 0;
  color: #333;
  font-size: 24px;
  font-weight: 600;
}

.logo-section p {
  margin: 0;
  color: #666;
  font-size: 14px;
}

.el-form-item {
  margin-bottom: 24px;
}

.form-footer {
  text-align: center;
  margin-top: 20px;
  color: #666;
  font-size: 14px;
}

.form-footer .el-button {
  color: #409eff;
  font-weight: 500;
}

.form-footer .el-button:hover {
  color: #66b1ff;
}

/* 邮箱显示区域 */
.email-display {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: #f5f7fa;
  border-radius: 8px;
  margin-bottom: 20px;
}

.email-display span {
  color: #303133;
  font-size: 14px;
  font-weight: 500;
}

.email-display .el-button {
  color: #409eff;
  padding: 0;
}

/* OAuth登录样式 */
.oauth-section {
  margin: 24px 0;
}

.divider {
  position: relative;
  text-align: center;
  margin: 20px 0;
}

.divider::before {
  content: '';
  position: absolute;
  top: 50%;
  left: 0;
  right: 0;
  height: 1px;
  background: #dcdfe6;
}

.divider span {
  background: white;
  padding: 0 16px;
  color: #909399;
  font-size: 14px;
}

.gitee-btn {
  background: #fff !important;
  border: 1px solid #dcdfe6 !important;
  color: #606266 !important;
}

.gitee-btn:hover {
  border-color: #C71D23 !important;
  color: #C71D23 !important;
}

/* 响应式设计 */
@media (max-width: 480px) {
  .login-box {
    flex-direction: column;
    max-width: 420px;
    min-height: auto;
  }

  .login-left {
    padding: 24px 20px 12px;
  }

  .login-right {
    padding: 32px 20px 24px;
  }
}
</style>
