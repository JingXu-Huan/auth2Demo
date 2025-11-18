<template>
  <div class="test-container">
    <h1>IM 系统测试页面</h1>
    <p>如果您能看到这个页面，说明前端基本配置正常。</p>
    
    <div class="test-section">
      <h2>路由测试</h2>
      <el-button @click="$router.push('/login')">跳转到登录页</el-button>
      <el-button @click="$router.push('/im')">跳转到主页</el-button>
    </div>
    
    <div class="test-section">
      <h2>组件测试</h2>
      <el-button type="primary">主要按钮</el-button>
      <el-button type="success">成功按钮</el-button>
      <el-button type="warning">警告按钮</el-button>
      <el-button type="danger">危险按钮</el-button>
    </div>
    
    <div class="test-section">
      <h2>图标测试</h2>
      <el-icon><ChatDotRound /></el-icon>
      <el-icon><User /></el-icon>
      <el-icon><UserFilled /></el-icon>
      <el-icon><Setting /></el-icon>
    </div>
  </div>
</template>

<script setup>
import { ChatDotRound, User, UserFilled, Setting } from '@element-plus/icons-vue'
</script>

<style scoped>
.test-container {
  padding: 40px;
  max-width: 800px;
  margin: 0 auto;
}

.test-section {
  margin: 30px 0;
  padding: 20px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
}

.test-section h2 {
  margin-top: 0;
  color: #333;
}

.el-button {
  margin-right: 10px;
  margin-bottom: 10px;
}

.el-icon {
  font-size: 24px;
  margin-right: 15px;
}
</style>
