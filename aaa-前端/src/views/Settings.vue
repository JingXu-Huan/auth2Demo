<template>
  <div class="settings-page">
    <div class="settings-header">
      <h2>设置</h2>
    </div>

    <div class="settings-content">
      <!-- 个人信息 -->
      <div class="settings-section">
        <h3>个人信息</h3>
        <div class="profile-card">
          <div class="avatar-section">
            <el-avatar :size="80" :src="userStore.userAvatar">
              {{ userStore.userName?.charAt(0) }}
            </el-avatar>
            <el-button size="small">更换头像</el-button>
          </div>
          <div class="info-form">
            <div class="form-item">
              <label>昵称</label>
              <el-input v-model="profile.nickname" placeholder="请输入昵称" />
            </div>
            <div class="form-item">
              <label>个性签名</label>
              <el-input v-model="profile.signature" placeholder="请输入个性签名" />
            </div>
            <el-button type="primary" @click="saveProfile">保存修改</el-button>
          </div>
        </div>
      </div>

      <!-- 账号安全 -->
      <div class="settings-section">
        <h3>账号安全</h3>
        <div class="setting-item" @click="changePassword">
          <div class="item-info">
            <el-icon><Lock /></el-icon>
            <span>修改密码</span>
          </div>
          <el-icon><ArrowRight /></el-icon>
        </div>
        <div class="setting-item">
          <div class="item-info">
            <el-icon><Iphone /></el-icon>
            <span>绑定手机</span>
          </div>
          <span class="item-value">{{ userStore.user?.phone || '未绑定' }}</span>
          <el-icon><ArrowRight /></el-icon>
        </div>
        <div class="setting-item">
          <div class="item-info">
            <el-icon><Message /></el-icon>
            <span>绑定邮箱</span>
          </div>
          <span class="item-value">{{ userStore.user?.email }}</span>
          <el-icon><ArrowRight /></el-icon>
        </div>
      </div>

      <!-- 通知设置 -->
      <div class="settings-section">
        <h3>通知设置</h3>
        <div class="setting-item">
          <div class="item-info">
            <el-icon><Bell /></el-icon>
            <span>消息通知</span>
          </div>
          <el-switch v-model="settings.messageNotify" />
        </div>
        <div class="setting-item">
          <div class="item-info">
            <el-icon><ChatDotRound /></el-icon>
            <span>声音提醒</span>
          </div>
          <el-switch v-model="settings.soundNotify" />
        </div>
      </div>

      <!-- 关于 -->
      <div class="settings-section">
        <h3>关于</h3>
        <div class="setting-item">
          <div class="item-info">
            <el-icon><InfoFilled /></el-icon>
            <span>版本信息</span>
          </div>
          <span class="item-value">v1.0.0</span>
        </div>
      </div>
    </div>

    <!-- 修改密码对话框 -->
    <el-dialog v-model="showPasswordDialog" title="修改密码" width="400px">
      <el-form :model="passwordForm" label-width="80px">
        <el-form-item label="旧密码">
          <el-input v-model="passwordForm.oldPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="passwordForm.newPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="确认密码">
          <el-input v-model="passwordForm.confirmPassword" type="password" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showPasswordDialog = false">取消</el-button>
        <el-button type="primary" @click="confirmChangePassword">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Lock, Iphone, Message, Bell, ChatDotRound, InfoFilled, ArrowRight } from '@element-plus/icons-vue'
import { useUserStore } from '../stores/user'
import { userApi } from '../api'

const userStore = useUserStore()
const showPasswordDialog = ref(false)

const profile = reactive({
  nickname: userStore.user?.nickname || '',
  signature: userStore.user?.signature || ''
})

const settings = reactive({
  messageNotify: true,
  soundNotify: true
})

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const saveProfile = async () => {
  try {
    await userApi.updateUser(userStore.userId, {
      nickname: profile.nickname,
      signature: profile.signature
    })
    userStore.updateUser(profile)
    ElMessage.success('保存成功')
  } catch (error) {
    ElMessage.error('保存失败')
  }
}

const changePassword = () => {
  passwordForm.oldPassword = ''
  passwordForm.newPassword = ''
  passwordForm.confirmPassword = ''
  showPasswordDialog.value = true
}

const confirmChangePassword = async () => {
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    ElMessage.error('两次密码不一致')
    return
  }
  
  try {
    await userApi.changePassword(userStore.userId, {
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword
    })
    ElMessage.success('密码修改成功')
    showPasswordDialog.value = false
  } catch (error) {
    ElMessage.error('修改失败')
  }
}

onMounted(() => {
  profile.nickname = userStore.user?.nickname || ''
  profile.signature = userStore.user?.signature || ''
})
</script>

<style scoped>
.settings-page {
  height: 100%;
  background: #f5f6f7;
  overflow-y: auto;
}

.settings-header {
  padding: 24px;
  background: #fff;
  border-bottom: 1px solid #e5e6eb;
}

.settings-header h2 {
  margin: 0;
  font-size: 22px;
}

.settings-content {
  padding: 24px;
  max-width: 800px;
}

.settings-section {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 16px;
}

.settings-section h3 {
  margin: 0 0 16px;
  font-size: 16px;
  color: #1f2329;
}

.profile-card {
  display: flex;
  gap: 32px;
}

.avatar-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.info-form {
  flex: 1;
}

.form-item {
  margin-bottom: 16px;
}

.form-item label {
  display: block;
  margin-bottom: 8px;
  font-size: 14px;
  color: #646a73;
}

.setting-item {
  display: flex;
  align-items: center;
  padding: 16px 0;
  border-bottom: 1px solid #f0f1f2;
  cursor: pointer;
}

.setting-item:last-child {
  border-bottom: none;
}

.item-info {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
}

.item-value {
  color: #8f959e;
  margin-right: 8px;
}
</style>
