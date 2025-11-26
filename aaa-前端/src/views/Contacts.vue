<template>
  <div class="contacts-page">
    <!-- 左侧菜单 -->
    <div class="contacts-menu">
      <div class="menu-header">
        <h3>通讯录</h3>
      </div>
      <div class="menu-list">
        <div
          v-for="item in menuItems"
          :key="item.key"
          :class="['menu-item', { active: activeMenu === item.key }]"
          @click="activeMenu = item.key"
        >
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.label }}</span>
          <el-badge v-if="item.badge" :value="item.badge" :offset="[10, 0]" />
        </div>
      </div>
    </div>

    <!-- 右侧内容 -->
    <div class="contacts-content">
      <!-- 好友列表 -->
      <template v-if="activeMenu === 'friends'">
        <div class="content-header">
          <h2>我的好友 ({{ friends.length }})</h2>
          <el-button type="primary" :icon="Plus" @click="showAddFriend = true">
            添加好友
          </el-button>
        </div>
        
        <el-input
          v-model="searchKeyword"
          placeholder="搜索好友"
          :prefix-icon="Search"
          clearable
          class="search-bar"
        />

        <div class="friend-list">
          <div
            v-for="friend in filteredFriends"
            :key="friend.userId"
            class="friend-item"
          >
            <el-avatar :size="48" :src="friend.avatar">
              {{ friend.nickname?.charAt(0) }}
            </el-avatar>
            <div class="friend-info">
              <div class="friend-name">{{ friend.nickname }}</div>
              <div class="friend-signature">{{ friend.signature || '暂无签名' }}</div>
            </div>
            <div class="friend-actions">
              <el-button :icon="ChatLineRound" circle @click="startChat(friend)" />
              <el-button :icon="Phone" circle @click="startCall(friend)" />
            </div>
          </div>

          <el-empty v-if="friends.length === 0" description="暂无好友" />
        </div>
      </template>

      <!-- 群组列表 -->
      <template v-if="activeMenu === 'groups'">
        <div class="content-header">
          <h2>我的群组 ({{ groups.length }})</h2>
          <el-button type="primary" :icon="Plus" @click="showCreateGroup = true">
            创建群组
          </el-button>
        </div>

        <div class="group-list">
          <div
            v-for="group in groups"
            :key="group.id"
            class="group-item"
          >
            <el-avatar :size="48" :src="group.avatar" shape="square">
              {{ group.name?.charAt(0) }}
            </el-avatar>
            <div class="group-info">
              <div class="group-name">{{ group.name }}</div>
              <div class="group-meta">{{ group.memberCount }} 人</div>
            </div>
            <el-button type="primary" size="small" @click="enterGroup(group)">
              进入群聊
            </el-button>
          </div>

          <el-empty v-if="groups.length === 0" description="暂无群组" />
        </div>
      </template>

      <!-- 好友请求 -->
      <template v-if="activeMenu === 'requests'">
        <div class="content-header">
          <h2>好友请求 ({{ requests.length }})</h2>
        </div>

        <div class="request-list">
          <div
            v-for="req in requests"
            :key="req.id"
            class="request-item"
          >
            <el-avatar :size="48" :src="req.fromAvatar">
              {{ req.fromNickname?.charAt(0) }}
            </el-avatar>
            <div class="request-info">
              <div class="request-name">{{ req.fromNickname }}</div>
              <div class="request-message">{{ req.message || '请求添加您为好友' }}</div>
            </div>
            <div class="request-actions">
              <el-button type="primary" size="small" @click="acceptRequest(req)">
                接受
              </el-button>
              <el-button size="small" @click="rejectRequest(req)">
                拒绝
              </el-button>
            </div>
          </div>

          <el-empty v-if="requests.length === 0" description="暂无请求" />
        </div>
      </template>
    </div>

    <!-- 添加好友对话框 -->
    <el-dialog v-model="showAddFriend" title="添加好友" width="420px">
      <el-input
        v-model="addFriendKeyword"
        placeholder="输入邮箱或手机号搜索"
        :prefix-icon="Search"
        @keyup.enter="searchUser"
      />
      
      <div v-if="searchResult" class="search-result">
        <el-avatar :size="48" :src="searchResult.avatar">
          {{ searchResult.nickname?.charAt(0) }}
        </el-avatar>
        <div class="result-info">
          <div class="result-name">{{ searchResult.nickname }}</div>
          <div class="result-email">{{ searchResult.email }}</div>
        </div>
        <el-button type="primary" size="small" @click="sendFriendRequest">
          添加
        </el-button>
      </div>

      <template #footer>
        <el-button @click="showAddFriend = false">取消</el-button>
      </template>
    </el-dialog>

    <!-- 创建群组对话框 -->
    <el-dialog v-model="showCreateGroup" title="创建群组" width="480px">
      <el-form :model="groupForm" label-width="80px">
        <el-form-item label="群名称">
          <el-input v-model="groupForm.name" placeholder="请输入群名称" />
        </el-form-item>
        <el-form-item label="群简介">
          <el-input v-model="groupForm.description" type="textarea" :rows="3" placeholder="请输入群简介" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="showCreateGroup = false">取消</el-button>
        <el-button type="primary" @click="createGroup">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, UserFilled, Message, Plus, Search, ChatLineRound, Phone } from '@element-plus/icons-vue'
import { useUserStore } from '../stores/user'
import { useChatStore } from '../stores/chat'
import { userApi, friendApi, groupApi, channelApi } from '../api'

const router = useRouter()
const userStore = useUserStore()
const chatStore = useChatStore()

const activeMenu = ref('friends')
const searchKeyword = ref('')
const showAddFriend = ref(false)
const showCreateGroup = ref(false)
const addFriendKeyword = ref('')
const searchResult = ref(null)

const friends = ref([])
const groups = ref([])
const requests = ref([])

const groupForm = reactive({
  name: '',
  description: ''
})

const menuItems = computed(() => [
  { key: 'friends', label: '我的好友', icon: 'User', badge: null },
  { key: 'groups', label: '我的群组', icon: 'UserFilled', badge: null },
  { key: 'requests', label: '好友请求', icon: 'Message', badge: requests.value.length || null }
])

const filteredFriends = computed(() => {
  if (!searchKeyword.value) return friends.value
  const keyword = searchKeyword.value.toLowerCase()
  return friends.value.filter(f => f.nickname?.toLowerCase().includes(keyword))
})

// 加载好友列表
const loadFriends = async () => {
  try {
    const res = await friendApi.getFriends()
    if (res.code === 200) {
      friends.value = res.data || []
    }
  } catch (error) {
    console.error('加载好友失败:', error)
  }
}

// 加载群组列表（暂时使用好友分组 API）
const loadGroups = async () => {
  try {
    const res = await groupApi.getGroups()
    if (res.code === 200) {
      groups.value = res.data || []
    }
  } catch (error) {
    console.error('加载群组失败:', error)
  }
}

// 加载好友请求
const loadRequests = async () => {
  try {
    const res = await friendApi.getPendingRequests()
    if (res.code === 200) {
      requests.value = res.data || []
    }
  } catch (error) {
    console.error('加载请求失败:', error)
  }
}

// 搜索用户
const searchUser = async () => {
  if (!addFriendKeyword.value) return
  
  try {
    const res = await userApi.searchUser(addFriendKeyword.value)
    if (res.code === 200 && res.data) {
      searchResult.value = res.data
    } else {
      ElMessage.warning('未找到用户')
      searchResult.value = null
    }
  } catch (error) {
    ElMessage.error('搜索失败')
  }
}

// 发送好友请求
const sendFriendRequest = async () => {
  if (!searchResult.value) return
  
  try {
    // 后端需要 targetUserId, message, source 等字段
    await friendApi.sendRequest({
      targetUserId: searchResult.value.id,
      message: '请求添加您为好友',
      source: 'search'
    })
    ElMessage.success('请求已发送')
    showAddFriend.value = false
    searchResult.value = null
    addFriendKeyword.value = ''
  } catch (error) {
    ElMessage.error('发送失败')
  }
}

// 接受请求
const acceptRequest = async (req) => {
  try {
    const res = await friendApi.auditRequest({
      requestId: req.id,
      action: 'ACCEPT'
    })
    if (res.code === 200) {
      ElMessage.success('已添加为好友')
      loadRequests()
      loadFriends()
    } else {
      ElMessage.error(res.message || '操作失败')
    }
  } catch (error) {
    console.error('接受请求失败:', error)
    ElMessage.error('操作失败')
  }
}

// 拒绝请求
const rejectRequest = async (req) => {
  try {
    const res = await friendApi.auditRequest({
      requestId: req.id,
      action: 'REJECT'
    })
    if (res.code === 200) {
      ElMessage.success('已拒绝')
      loadRequests()
    } else {
      ElMessage.error(res.message || '操作失败')
    }
  } catch (error) {
    console.error('拒绝请求失败:', error)
    ElMessage.error('操作失败')
  }
}

// 开始聊天
const startChat = async (friend) => {
  try {
    const res = await channelApi.getPrivateChannel(friend.userId)
    if (res.code === 200) {
      chatStore.selectConversation({
        id: res.data.id,
        name: friend.nickname,
        avatar: friend.avatar,
        type: 'private'
      })
      router.push('/chat')
    }
  } catch (error) {
    ElMessage.error('创建会话失败')
  }
}

// 语音通话
const startCall = (friend) => {
  ElMessage.info('语音通话功能开发中')
}

// 进入群聊
const enterGroup = (group) => {
  chatStore.selectConversation({
    id: group.id,
    name: group.name,
    avatar: group.avatar,
    type: 'group',
    memberCount: group.memberCount
  })
  router.push('/chat')
}

// 创建好友分组（注意：这是好友分组，不是群聊）
const createGroup = async () => {
  if (!groupForm.name) {
    ElMessage.warning('请输入分组名称')
    return
  }
  
  try {
    // groupApi.create 只需要 name 参数
    const res = await groupApi.create(groupForm.name)
    
    if (res.code === 200) {
      ElMessage.success('创建成功')
      showCreateGroup.value = false
      groupForm.name = ''
      groupForm.description = ''
      loadGroups()
    } else {
      ElMessage.error(res.message || '创建失败')
    }
  } catch (error) {
    console.error('创建分组失败:', error)
    ElMessage.error('创建失败')
  }
}

onMounted(() => {
  loadFriends()
  loadGroups()
  loadRequests()
})
</script>

<style scoped>
.contacts-page {
  display: flex;
  height: 100%;
  background: #fff;
}

.contacts-menu {
  width: 240px;
  border-right: 1px solid #e5e6eb;
  display: flex;
  flex-direction: column;
}

.menu-header {
  padding: 20px;
  border-bottom: 1px solid #e5e6eb;
}

.menu-header h3 {
  margin: 0;
  font-size: 18px;
}

.menu-list {
  padding: 12px;
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.menu-item:hover {
  background: #f5f6f7;
}

.menu-item.active {
  background: #e8f3ff;
  color: #3370ff;
}

.contacts-content {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
}

.content-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.content-header h2 {
  margin: 0;
  font-size: 18px;
}

.search-bar {
  margin-bottom: 16px;
}

.friend-list,
.group-list,
.request-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.friend-item,
.group-item,
.request-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  background: #f9fafb;
  border-radius: 12px;
}

.friend-info,
.group-info,
.request-info {
  flex: 1;
}

.friend-name,
.group-name,
.request-name {
  font-weight: 500;
  margin-bottom: 4px;
}

.friend-signature,
.group-meta,
.request-message {
  font-size: 13px;
  color: #8f959e;
}

.friend-actions,
.request-actions {
  display: flex;
  gap: 8px;
}

.search-result {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  margin-top: 16px;
  background: #f9fafb;
  border-radius: 12px;
}

.result-info {
  flex: 1;
}

.result-name {
  font-weight: 500;
}

.result-email {
  font-size: 13px;
  color: #8f959e;
}
</style>
