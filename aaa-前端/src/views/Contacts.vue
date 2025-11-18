<template>
  <div class="contacts-container">
    <!-- 左侧导航 -->
    <div class="contacts-sidebar">
      <div class="search-section">
        <el-input 
          v-model="searchKeyword" 
          placeholder="搜索联系人..."
          prefix-icon="Search"
          @input="handleSearch"
        />
      </div>
      
      <div class="contact-categories">
        <div 
          v-for="category in categories" 
          :key="category.key"
          :class="['category-item', { active: activeCategory === category.key }]"
          @click="selectCategory(category.key)"
        >
          <el-icon class="category-icon">
            <component :is="category.icon" />
          </el-icon>
          <span class="category-name">{{ category.name }}</span>
        </div>
      </div>
    </div>
    
    <!-- 右侧内容区域 -->
    <div class="contacts-content">
      <!-- 好友列表 -->
      <div v-if="activeCategory === 'friends'" class="friends-section">
        <div class="section-header">
          <h3>我的好友</h3>
          <el-button type="primary" @click="showAddFriendDialog = true">
            <el-icon><Plus /></el-icon>
            添加好友
          </el-button>
        </div>
        
        <div class="contacts-grid">
          <div 
            v-for="friend in filteredFriends" 
            :key="friend.id"
            class="contact-card"
            @click="viewContact(friend)"
          >
            <el-avatar :src="friend.avatar" :size="60">
              {{ friend.nickname.charAt(0) }}
            </el-avatar>
            <div class="contact-info">
              <h4>{{ friend.nickname }}</h4>
              <p>{{ friend.department || '暂无部门' }}</p>
              <div class="contact-status">
                <el-tag :type="getStatusType(friend.status)" size="small">
                  {{ getStatusText(friend.status) }}
                </el-tag>
              </div>
            </div>
            <div class="contact-actions">
              <el-button circle size="small" @click.stop="startChat(friend)">
                <el-icon><ChatDotRound /></el-icon>
              </el-button>
              <el-button circle size="small" @click.stop="makeCall(friend)">
                <el-icon><Phone /></el-icon>
              </el-button>
            </div>
          </div>
        </div>
      </div>
      
      <!-- 组织架构 -->
      <div v-else-if="activeCategory === 'organization'" class="organization-section">
        <div class="section-header">
          <h3>组织架构</h3>
          <el-button @click="refreshOrganization">
            <el-icon><Refresh /></el-icon>
            刷新
          </el-button>
        </div>
        
        <div class="organization-tree">
          <el-tree
            :data="organizationTree"
            :props="treeProps"
            node-key="id"
            :expand-on-click-node="false"
            @node-click="handleNodeClick"
          >
            <template #default="{ node, data }">
              <div class="tree-node">
                <el-icon v-if="data.type === 'department'">
                  <OfficeBuilding />
                </el-icon>
                <el-avatar v-else :src="data.avatar" :size="24">
                  {{ data.name.charAt(0) }}
                </el-avatar>
                <span class="node-label">{{ data.name }}</span>
                <span v-if="data.type === 'department'" class="member-count">
                  ({{ data.memberCount }}人)
                </span>
                <el-tag v-if="data.position" size="small" class="position-tag">
                  {{ data.position }}
                </el-tag>
              </div>
            </template>
          </el-tree>
        </div>
      </div>
      
      <!-- 群组列表 -->
      <div v-else-if="activeCategory === 'groups'" class="groups-section">
        <div class="section-header">
          <h3>我的群组</h3>
          <el-button type="primary" @click="createGroup">
            <el-icon><Plus /></el-icon>
            创建群组
          </el-button>
        </div>
        
        <!-- 加载中骨架屏 -->
        <div v-if="loadingGroups" class="groups-skeleton">
          <el-skeleton :rows="3" animated />
        </div>
        
        <!-- 空状态 -->
        <el-empty v-else-if="groups.length === 0" description="暂无群组">
          <el-button type="primary" @click="createGroup">创建第一个群组</el-button>
        </el-empty>
        
        <!-- 群组列表 -->
        <div v-else class="groups-list">
          <div 
            v-for="group in groups" 
            :key="group.id"
            class="group-item"
            @click="viewGroup(group)"
          >
            <el-avatar :src="group.avatar" :size="50" shape="square">
              {{ group.name.charAt(0) }}
            </el-avatar>
            <div class="group-info">
              <h4>{{ group.name }}</h4>
              <p>{{ group.description }}</p>
              <div class="group-meta">
                <span>{{ group.memberCount }} 人</span>
                <span>{{ formatTime(group.lastActiveTime) }}</span>
              </div>
            </div>
            <div class="group-actions">
              <el-button size="small" @click.stop="enterGroup(group)">
                进入群聊
              </el-button>
            </div>
          </div>
        </div>
      </div>
      
      <!-- 好友申请 -->
      <div v-else-if="activeCategory === 'requests'" class="requests-section">
        <div class="section-header">
          <h3>好友申请</h3>
          <el-button :icon="Refresh" circle @click="loadFriendRequests" title="刷新" />
        </div>
        
        <div class="requests-list">
          <div 
            v-for="request in friendRequests" 
            :key="request.requestId"
            class="request-item"
          >
            <el-avatar :src="request.fromAvatar" :size="40">
              {{ request.fromNickname?.charAt(0) || 'U' }}
            </el-avatar>
            <div class="request-info">
              <h4>{{ request.fromNickname || '未知用户' }}</h4>
              <p>{{ request.message || '请求添加您为好友' }}</p>
              <span class="request-time">{{ formatTime(request.createdAt) }}</span>
            </div>
            <div class="request-actions">
              <el-button 
                type="primary" 
                size="small" 
                @click="handleFriendRequest(request, 'accept')"
              >
                同意
              </el-button>
              <el-button 
                size="small" 
                @click="handleFriendRequest(request, 'reject')"
              >
                拒绝
              </el-button>
            </div>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 添加好友对话框 -->
    <el-dialog v-model="showAddFriendDialog" title="添加好友" width="500px">
      <el-form :model="addFriendForm" label-width="80px">
        <el-form-item label="邮箱">
          <el-input v-model="addFriendForm.keyword" placeholder="请输入对方邮箱地址" type="email" />
        </el-form-item>
        <el-form-item label="验证消息">
          <el-input 
            v-model="addFriendForm.message" 
            type="textarea" 
            :rows="3"
            placeholder="请输入验证消息"
          />
        </el-form-item>
      </el-form>
      
      <div v-if="searchResults.length > 0" class="search-results">
        <h4>搜索结果</h4>
        <div 
          v-for="user in searchResults" 
          :key="user.id"
          class="search-result-item"
        >
          <el-avatar :src="user.avatar" :size="40">
            {{ user.nickname.charAt(0) }}
          </el-avatar>
          <div class="user-info">
            <h5>{{ user.nickname }}</h5>
            <p>{{ user.department || '暂无部门' }}</p>
          </div>
          <el-button type="primary" size="small" @click="sendFriendRequest(user)">
            添加
          </el-button>
        </div>
      </div>
      
      <template #footer>
        <el-button @click="showAddFriendDialog = false">取消</el-button>
        <el-button type="primary" @click="searchUsers">搜索</el-button>
      </template>
    </el-dialog>
    
    <!-- 创建群组对话框 -->
    <el-dialog v-model="showCreateGroupDialog" title="创建群组" width="500px">
      <el-form :model="createGroupForm" label-width="100px" ref="createGroupFormRef">
        <el-form-item label="群组名称" required>
          <el-input 
            v-model="createGroupForm.name" 
            placeholder="请输入群组名称（2-30个字符）"
            maxlength="30"
            show-word-limit
          />
        </el-form-item>
        
        <el-form-item label="群组描述">
          <el-input 
            v-model="createGroupForm.description" 
            type="textarea"
            :rows="3"
            placeholder="请输入群组描述（可选）"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>
        
        <el-form-item label="加入方式">
          <el-radio-group v-model="createGroupForm.joinType">
            <el-radio label="FREE">自由加入</el-radio>
            <el-radio label="APPROVAL">需要审批</el-radio>
            <el-radio label="INVITE">仅邀请</el-radio>
          </el-radio-group>
        </el-form-item>
        
        <el-form-item label="最大人数">
          <el-input-number 
            v-model="createGroupForm.maxMembers" 
            :min="10" 
            :max="500" 
            :step="10"
          />
        </el-form-item>
      </el-form>
      
      <template #footer>
        <el-button @click="showCreateGroupDialog = false">取消</el-button>
        <el-button type="primary" @click="handleCreateGroup" :loading="createGroupLoading">
          创建
        </el-button>
      </template>
    </el-dialog>
    
    <!-- 群组详情对话框 -->
    <el-dialog v-model="showGroupDetailDialog" :title="currentGroup?.name" width="600px">
      <div v-if="currentGroup" class="group-detail">
        <!-- 群组基本信息 -->
        <div class="group-header">
          <el-avatar :src="currentGroup.avatar" :size="80" shape="square">
            {{ currentGroup.name.charAt(0) }}
          </el-avatar>
          <div class="group-info">
            <h3>{{ currentGroup.name }}</h3>
            <p class="description">{{ currentGroup.description || '暂无描述' }}</p>
            <div class="group-meta">
              <el-tag size="small">{{ currentGroup.memberCount }} 人</el-tag>
              <el-tag size="small" type="info">{{ getJoinTypeText(currentGroup.joinType) }}</el-tag>
            </div>
          </div>
        </div>
        
        <!-- 群公告 -->
        <div v-if="currentGroup.announcement" class="group-announcement">
          <el-divider content-position="left">
            <el-icon><Bell /></el-icon>
            群公告
          </el-divider>
          <div class="announcement-content">
            {{ currentGroup.announcement }}
          </div>
        </div>
        
        <!-- 群组成员列表 -->
        <el-divider>群成员</el-divider>
        <div class="member-list" v-loading="loadingMembers">
          <div 
            v-for="member in groupMembers" 
            :key="member.userId"
            class="member-item"
          >
            <el-avatar :src="member.avatar" :size="40">
              {{ member.nickname?.charAt(0) || member.username?.charAt(0) || 'U' }}
            </el-avatar>
            <div class="member-info">
              <span class="member-name">{{ member.nickname || member.username || `用户${member.userId}` }}</span>
              <el-tag v-if="member.role === 'OWNER'" size="small" type="danger">群主</el-tag>
              <el-tag v-else-if="member.role === 'ADMIN'" size="small" type="warning">管理员</el-tag>
            </div>
            <div class="member-actions" v-if="userStore.user && currentGroup">
              <!-- 群主权限：设置/取消管理员 + 移除成员（除自己外） -->
              <template v-if="isGroupOwner">
                <el-button
                  v-if="member.role === 'MEMBER'"
                  size="small"
                  type="primary"
                  text
                  @click="handleSetAdmin(member, true)"
                >
                  设为管理员
                </el-button>
                <el-button
                  v-else-if="member.role === 'ADMIN'"
                  size="small"
                  type="warning"
                  text
                  @click="handleSetAdmin(member, false)"
                >
                  取消管理员
                </el-button>
                <el-button
                  v-if="String(member.userId) !== String(userStore.user.id) && member.role !== 'OWNER'"
                  size="small"
                  type="danger"
                  text
                  @click="handleRemoveMember(member)"
                >
                  移除
                </el-button>
              </template>
              <!-- 管理员权限：可移除普通成员 -->
              <template v-else-if="isGroupAdmin">
                <el-button
                  v-if="member.role === 'MEMBER' && String(member.userId) !== String(userStore.user.id)"
                  size="small"
                  type="danger"
                  text
                  @click="handleRemoveMember(member)"
                >
                  移除
                </el-button>
              </template>
            </div>
          </div>
          
          <!-- 加载更多 -->
          <el-button 
            v-if="groupMembers.length < currentGroup.memberCount" 
            text 
            @click="loadMoreMembers"
          >
            加载更多成员...
          </el-button>
        </div>
        
        <!-- 操作按钮 -->
        <el-divider>操作</el-divider>
        <div class="group-actions">
          <el-button type="primary" @click="enterGroupFromDetail">
            <el-icon><ChatDotRound /></el-icon>
            进入群聊
          </el-button>
          <el-button v-if="isGroupOwner" @click="showEditGroup">
            <el-icon><Edit /></el-icon>
            编辑群组
          </el-button>
          <el-button v-if="!isGroupOwner" type="danger" @click="confirmLeaveGroup">
            退出群组
          </el-button>
          <el-button v-if="isGroupOwner" type="danger" @click="confirmDissolveGroup">
            解散群组
          </el-button>
        </div>
      </div>
      
      <template #footer>
        <el-button @click="showGroupDetailDialog = false">关闭</el-button>
      </template>
    </el-dialog>
    
    <!-- 编辑群组对话框 -->
    <el-dialog v-model="showEditGroupDialog" title="编辑群组" width="500px">
      <el-form :model="editGroupForm" label-width="100px" ref="editGroupFormRef">
        <el-form-item label="群组名称" required>
          <el-input 
            v-model="editGroupForm.name" 
            placeholder="请输入群组名称（2-30个字符）"
            maxlength="30"
            show-word-limit
          />
        </el-form-item>
        
        <el-form-item label="群组描述">
          <el-input 
            v-model="editGroupForm.description" 
            type="textarea"
            :rows="3"
            placeholder="请输入群组描述（可选）"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>
        
        <el-form-item label="群公告">
          <el-input 
            v-model="editGroupForm.announcement" 
            type="textarea"
            :rows="3"
            placeholder="请输入群公告（可选）"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
        
        <el-form-item label="加入方式">
          <el-radio-group v-model="editGroupForm.joinType">
            <el-radio label="FREE">自由加入</el-radio>
            <el-radio label="APPROVAL">需要审批</el-radio>
            <el-radio label="INVITE">仅邀请</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      
      <template #footer>
        <el-button @click="showEditGroupDialog = false">取消</el-button>
        <el-button type="primary" @click="handleUpdateGroup" :loading="updateGroupLoading">
          保存
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  Search, 
  Plus, 
  ChatDotRound, 
  Phone, 
  Refresh, 
  OfficeBuilding,
  User,
  UserFilled,
  Message,
  Edit,
  Bell
} from '@element-plus/icons-vue'
import { useUserStore } from '../stores/user'
import { userAPI, organizationAPI, groupAPI, friendAPI } from '../api'
import dayjs from 'dayjs'

const router = useRouter()
const userStore = useUserStore()

const searchKeyword = ref('')
const activeCategory = ref('friends')
const showAddFriendDialog = ref(false)
const showCreateGroupDialog = ref(false)
const createGroupLoading = ref(false)
const searchResults = ref([])

const addFriendForm = reactive({
  keyword: '',
  message: '您好，我想添加您为好友'
})

const createGroupForm = reactive({
  name: '',
  description: '',
  joinType: 'FREE',
  maxMembers: 100
})

const createGroupFormRef = ref(null)

// 群组详情相关
const showGroupDetailDialog = ref(false)
const currentGroup = ref(null)
const groupMembers = ref([])
const loadingMembers = ref(false)
const memberPage = ref(1)
const memberPageSize = ref(20)

// 编辑群组相关
const showEditGroupDialog = ref(false)
const updateGroupLoading = ref(false)
const editGroupForm = reactive({
  name: '',
  description: '',
  announcement: '',
  joinType: 'FREE'
})
const editGroupFormRef = ref(null)

// 是否是群主
const isGroupOwner = computed(() => {
  if (!currentGroup.value || !userStore.user) return false
  return currentGroup.value.ownerId === userStore.user.id
})

// 是否是管理员（包括群主），用于控制部分操作权限
const isGroupAdmin = computed(() => {
  if (!currentGroup.value || !userStore.user) return false
  if (currentGroup.value.ownerId === userStore.user.id) return true
  const me = groupMembers.value.find(m => String(m.userId) === String(userStore.user.id))
  return !!me && me.role === 'ADMIN'
})

// 分类数据
const categories = ref([
  { key: 'friends', name: '我的好友', icon: 'UserFilled', count: 0 },
  { key: 'organization', name: '组织架构', icon: 'OfficeBuilding', count: 0 },
  { key: 'groups', name: '我的群组', icon: 'User', count: 0 },
  { key: 'requests', name: '好友申请', icon: 'Message', count: 0 }
])

// 好友列表
const friends = ref([])
const groups = ref([])
const friendRequests = ref([])
const organizationTree = ref([])
const loadingGroups = ref(false)

const treeProps = {
  children: 'children',
  label: 'name'
}

// 过滤后的好友列表
const filteredFriends = computed(() => {
  if (!searchKeyword.value) return friends.value
  
  return friends.value.filter(friend => 
    friend.nickname.toLowerCase().includes(searchKeyword.value.toLowerCase()) ||
    friend.department?.toLowerCase().includes(searchKeyword.value.toLowerCase())
  )
})

// 选择分类
const selectCategory = (key) => {
  activeCategory.value = key
  
  if (key === 'friends') {
    loadFriends()
  } else if (key === 'organization') {
    loadOrganization()
  } else if (key === 'groups') {
    loadGroups()
  } else if (key === 'requests') {
    loadFriendRequests()
  }
}

// 设置/取消管理员
const handleSetAdmin = async (member, makeAdmin) => {
  if (!currentGroup.value) return

  const action = makeAdmin ? 'ADD' : 'REMOVE'

  try {
    const res = await groupAPI.setAdmin(currentGroup.value.id, {
      userId: member.userId,
      action
    })

    if (res.code === 200) {
      ElMessage.success(makeAdmin ? '已设为管理员' : '已取消管理员')
      await loadGroupMembers(currentGroup.value.id)
    } else {
      ElMessage.error(res.message || '操作失败')
    }
  } catch (error) {
    console.error('设置管理员失败:', error)
    ElMessage.error('操作失败')
  }
}

// 移除成员
const handleRemoveMember = async (member) => {
  if (!currentGroup.value) return

  try {
    const res = await groupAPI.removeMember(currentGroup.value.id, member.userId)
    if (res.code === 200) {
      ElMessage.success('已移除成员')
      await loadGroupMembers(currentGroup.value.id)
    } else {
      ElMessage.error(res.message || '移除失败')
    }
  } catch (error) {
    console.error('移除成员失败:', error)
    ElMessage.error('移除失败')
  }
}

// 加载好友列表
const loadFriends = async () => {
  try {
    const response = await friendAPI.getFriends(userStore.user.id)
    if (response.code === 200 && response.data) {
      friends.value = response.data
    } else {
      friends.value = []
    }
    categories.value[0].count = friends.value.length
  } catch (error) {
    console.error('加载好友列表失败:', error)
  }
}

// 加载组织架构
const loadOrganization = async () => {
  try {
    // TODO: 从后端加载组织架构
    organizationTree.value = []
  } catch (error) {
    console.error('加载组织架构失败:', error)
  }
}

// 加载群组列表
const loadGroups = async () => {
  // 防止重复加载
  if (loadingGroups.value) return
  
  try {
    if (!userStore.user || !userStore.user.id) {
      console.error('用户信息缺失')
      groups.value = []
      categories.value[2].count = 0
      return
    }

    loadingGroups.value = true
    const response = await groupAPI.getUserGroups(userStore.user.id, { page: 1, size: 50 })
    const groupList = response.data?.groups || []

    groups.value = groupList.map(group => ({
      id: group.groupId,
      name: group.name,
      avatar: group.avatar || '',
      description: group.description || '暂无描述',
      announcement: group.announcement || '',
      memberCount: group.memberCount || 0,
      maxMembers: group.maxMembers || 500,
      joinType: group.joinType || 'FREE',
      ownerId: group.ownerId,
      createdAt: group.createdAt,
      updatedAt: group.updatedAt,
      lastActiveTime: dayjs(group.updatedAt || group.createdAt || Date.now()).valueOf()
    }))

    categories.value[2].count = groups.value.length
  } catch (error) {
    console.error('加载群组列表失败:', error)
    groups.value = []
    categories.value[2].count = 0
  } finally {
    loadingGroups.value = false
  }
}

// 加载好友申请
const loadFriendRequests = async () => {
  try {
    const response = await friendAPI.getReceivedRequests(userStore.user.id)
    if (response.code === 200 && response.data) {
      friendRequests.value = response.data
    } else {
      friendRequests.value = []
    }
    categories.value[3].count = friendRequests.value.length
  } catch (error) {
    console.error('加载好友申请失败:', error)
    friendRequests.value = []
  }
}

// 搜索用户
const searchUsers = async () => {
  if (!addFriendForm.keyword.trim()) {
    ElMessage.warning('请输入邮箱地址')
    return
  }
  
  try {
    const response = await friendAPI.searchUser('email', addFriendForm.keyword)
    
    if (response.code === 200 && response.data) {
      // 将单个用户包装成数组
      searchResults.value = [{
        id: response.data.id,
        nickname: response.data.displayName || response.data.username,
        avatar: response.data.avatarUrl,
        email: response.data.email
      }]
    } else {
      searchResults.value = []
      ElMessage.info('未找到匹配的用户')
    }
  } catch (error) {
    console.error('搜索用户失败:', error)
    searchResults.value = []
    ElMessage.error('搜索失败')
  }
}

// 发送好友申请
const sendFriendRequest = async (user) => {
  try {
    const response = await friendAPI.sendFriendRequest({
      fromUserId: userStore.user.id,
      toUserId: user.id,
      message: addFriendForm.message
    })
    
    if (response.code === 200) {
      ElMessage.success('好友请求已发送，等待对方接受')
      showAddFriendDialog.value = false
      searchResults.value = []
      addFriendForm.keyword = ''
      addFriendForm.message = ''
    } else {
      ElMessage.error(response.message || '发送好友请求失败')
    }
  } catch (error) {
    console.error('发送好友请求失败:', error)
    ElMessage.error('发送好友请求失败')
  }
}

// 处理好友申请
const handleFriendRequest = async (request, action) => {
  try {
    if (action === 'accept') {
      await friendAPI.acceptFriendRequest(request.fromUserId, userStore.user.id)
      ElMessage.success('已同意好友申请')
      // 刷新好友列表
      await loadFriends()
    } else {
      await friendAPI.rejectFriendRequest(request.fromUserId, userStore.user.id)
      ElMessage.success('已拒绝好友申请')
    }
    
    // 刷新好友请求列表
    await loadFriendRequests()
  } catch (error) {
    console.error('处理好友申请失败:', error)
    ElMessage.error('操作失败')
  }
}

// 开始聊天
const startChat = (friend) => {
  // 构建会话ID（单聊格式：较小ID-较大ID）
  const userId1 = Math.min(userStore.user.id, friend.userId)
  const userId2 = Math.max(userStore.user.id, friend.userId)
  const conversationId = `${userId1}-${userId2}`
  
  // 跳转到消息页面，并传递会话信息
  router.push({
    path: '/im/messages',
    query: {
      conversationId: conversationId,
      friendId: friend.userId,
      friendName: friend.nickname || friend.username
    }
  })
}

// 拨打电话
const makeCall = (friend) => {
  ElMessage.info('语音通话功能开发中...')
}

// 查看联系人详情
const viewContact = (contact) => {
  ElMessage.info('联系人详情功能开发中...')
}

// 查看群组详情
const viewGroup = async (group) => {
  try {
    currentGroup.value = group
    showGroupDetailDialog.value = true
    
    // 加载群成员
    await loadGroupMembers(group.id)
  } catch (error) {
    console.error('加载群组详情失败:', error)
    ElMessage.error('无法加载群组详情')
  }
}

// 加载群成员
const loadGroupMembers = async (groupId) => {
  try {
    loadingMembers.value = true
    memberPage.value = 1
    
    const response = await groupAPI.getMembers(groupId, {
      page: memberPage.value,
      size: memberPageSize.value
    })
    
    if (response.code === 200 && response.data) {
      groupMembers.value = response.data.members || response.data.records || []
    }
  } catch (error) {
    console.error('加载群成员失败:', error)
  } finally {
    loadingMembers.value = false
  }
}

// 加载更多成员
const loadMoreMembers = async () => {
  if (!currentGroup.value) return
  
  try {
    loadingMembers.value = true
    memberPage.value++
    
    const response = await groupAPI.getMembers(currentGroup.value.id, {
      page: memberPage.value,
      size: memberPageSize.value
    })
    
    if (response.code === 200 && response.data) {
      const newMembers = response.data.members || response.data.records || []
      groupMembers.value = [...groupMembers.value, ...newMembers]
    }
  } catch (error) {
    console.error('加载更多成员失败:', error)
  } finally {
    loadingMembers.value = false
  }
}

// 从详情页进入群聊
const enterGroupFromDetail = () => {
  if (!currentGroup.value) return
  showGroupDetailDialog.value = false
  enterGroup(currentGroup.value)
}

// 获取加入方式文本
const getJoinTypeText = (joinType) => {
  const typeMap = {
    'FREE': '自由加入',
    'APPROVAL': '需要审批',
    'INVITE': '仅邀请'
  }
  return typeMap[joinType] || joinType
}

// 编辑群组（仅群主）- 打开编辑对话框
const showEditGroup = () => {
  if (!currentGroup.value) return
  
  // 填充表单数据
  editGroupForm.name = currentGroup.value.name || ''
  editGroupForm.description = currentGroup.value.description || ''
  editGroupForm.announcement = currentGroup.value.announcement || ''
  editGroupForm.joinType = currentGroup.value.joinType || 'FREE'
  
  showEditGroupDialog.value = true
}

// 处理更新群组
const handleUpdateGroup = async () => {
  // 验证
  if (!editGroupForm.name || editGroupForm.name.trim().length < 2) {
    ElMessage.warning('请输入群组名称（至少2个字符）')
    return
  }
  
  if (!currentGroup.value) {
    ElMessage.error('群组信息缺失')
    return
  }
  
  try {
    updateGroupLoading.value = true
    
    const requestData = {
      name: editGroupForm.name.trim(),
      description: editGroupForm.description.trim() || null,
      announcement: editGroupForm.announcement.trim() || null,
      joinType: editGroupForm.joinType
    }
    
    const response = await groupAPI.updateGroup(currentGroup.value.id, requestData)
    
    if (response.code === 200) {
      ElMessage.success('群组信息已更新！')
      showEditGroupDialog.value = false
      
      // 更新当前群组信息
      if (response.data) {
        currentGroup.value = {
          ...currentGroup.value,
          ...response.data
        }
      }
      
      // 刷新群组列表
      await loadGroups()
    } else {
      ElMessage.error(response.message || '更新失败')
    }
  } catch (error) {
    console.error('更新群组失败:', error)
    ElMessage.error('更新失败: ' + (error.message || '网络错误'))
  } finally {
    updateGroupLoading.value = false
  }
}

// 确认退出群组
const confirmLeaveGroup = () => {
  ElMessageBox.confirm(
    '确定要退出该群组吗？',
    '提示',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    await handleLeaveGroup()
  }).catch(() => {})
}

// 处理退出群组
const handleLeaveGroup = async () => {
  if (!currentGroup.value || !userStore.user) return
  
  try {
    const response = await groupAPI.leaveGroup(
      currentGroup.value.id,
      userStore.user.id
    )
    
    if (response.code === 200) {
      ElMessage.success('已退出群组')
      showGroupDetailDialog.value = false
      await loadGroups()
    } else {
      ElMessage.error(response.message || '退出失败')
    }
  } catch (error) {
    console.error('退出群组失败:', error)
    ElMessage.error('退出失败')
  }
}

// 确认解散群组（仅群主）
const confirmDissolveGroup = () => {
  ElMessageBox.confirm(
    '解散群组后将无法恢复，确定要解散吗？',
    '警告',
    {
      confirmButtonText: '确定解散',
      cancelButtonText: '取消',
      type: 'error',
      confirmButtonClass: 'el-button--danger'
    }
  ).then(async () => {
    await handleDissolveGroup()
  }).catch(() => {})
}

// 处理解散群组
const handleDissolveGroup = async () => {
  if (!currentGroup.value) return
  
  try {
    const response = await groupAPI.dissolveGroup(currentGroup.value.id)
    
    if (response.code === 200) {
      ElMessage.success('群组已解散')
      showGroupDetailDialog.value = false
      await loadGroups()
    } else {
      ElMessage.error(response.message || '解散失败')
    }
  } catch (error) {
    console.error('解散群组失败:', error)
    ElMessage.error('解散失败')
  }
}

// 进入群聊
const enterGroup = (group) => {
  router.push({
    path: '/im/messages',
    query: { type: 'group', groupId: group.id }
  })
}

// 创建群组 - 打开弹窗
const createGroup = () => {
  // 重置表单
  createGroupForm.name = ''
  createGroupForm.description = ''
  createGroupForm.joinType = 'FREE'
  createGroupForm.maxMembers = 100
  showCreateGroupDialog.value = true
}

// 处理创建群组
const handleCreateGroup = async () => {
  // 验证
  if (!createGroupForm.name || createGroupForm.name.trim().length < 2) {
    ElMessage.warning('请输入群组名称（至少2个字符）')
    return
  }
  
  if (!userStore.user || !userStore.user.id) {
    ElMessage.error('用户信息缺失，请重新登录')
    return
  }
  
  try {
    createGroupLoading.value = true
    
    const requestData = {
      name: createGroupForm.name.trim(),
      description: createGroupForm.description.trim() || null,
      joinType: createGroupForm.joinType,
      maxMembers: createGroupForm.maxMembers,
      ownerId: userStore.user.id,
      memberIds: [] // 初始只有群主
    }
    
    const response = await groupAPI.createGroup(requestData)
    
    if (response.code === 200) {
      ElMessage.success('群组创建成功！')
      showCreateGroupDialog.value = false
      
      // 刷新群组列表
      await loadGroups()
      
      // 切换到群组标签
      activeCategory.value = 'groups'
    } else {
      ElMessage.error(response.message || '创建失败')
    }
  } catch (error) {
    console.error('创建群组失败:', error)
    ElMessage.error('创建失败: ' + (error.message || '网络错误'))
  } finally {
    createGroupLoading.value = false
  }
}

// 刷新组织架构
const refreshOrganization = () => {
  loadOrganization()
  ElMessage.success('已刷新')
}

// 处理树节点点击
const handleNodeClick = (data) => {
  if (data.type === 'user') {
    viewContact(data)
  }
}

// 搜索处理
const handleSearch = () => {
  // 搜索逻辑已在computed中处理
}

// 获取状态类型
const getStatusType = (status) => {
  const map = {
    'online': 'success',
    'busy': 'warning',
    'away': 'info',
    'offline': 'info'
  }
  return map[status] || 'info'
}

// 获取状态文本
const getStatusText = (status) => {
  const map = {
    'online': '在线',
    'busy': '忙碌',
    'away': '离开',
    'offline': '离线'
  }
  return map[status] || '未知'
}

// 格式化时间
const formatTime = (timestamp) => {
  return dayjs(timestamp).format('MM-DD HH:mm')
}

// 定时器ID
let pollingTimer = null

onMounted(() => {
  // 检查登录状态
  if (!userStore.isLoggedIn || !userStore.user) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  
  // 初始加载
  loadFriends()
  loadFriendRequests()
  
  // 每30秒轮询一次好友请求
  pollingTimer = setInterval(() => {
    loadFriendRequests()
  }, 30000)
})

onUnmounted(() => {
  // 清除定时器
  if (pollingTimer) {
    clearInterval(pollingTimer)
  }
})
</script>

<style scoped>
.contacts-container {
  display: flex;
  height: 100%;
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
}

.contacts-sidebar {
  width: 280px;
  border-right: 1px solid #e8e8e8;
  display: flex;
  flex-direction: column;
}

.search-section {
  padding: 16px;
  border-bottom: 1px solid #f0f0f0;
}

.contact-categories {
  flex: 1;
  padding: 16px 0;
}

.category-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  cursor: pointer;
  transition: background 0.3s;
  color: #666;
}

.category-item:hover {
  background: #f5f5f5;
}

.category-item.active {
  background: #e6f7ff;
  color: #409eff;
}

.category-icon {
  font-size: 18px;
}

.category-name {
  flex: 1;
  font-size: 14px;
}

.category-count {
  font-size: 12px;
  color: #999;
}

.contacts-content {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.section-header h3 {
  margin: 0;
  color: #333;
}

.contacts-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
}

.contact-card {
  background: #f8f9fa;
  border-radius: 8px;
  padding: 16px;
  cursor: pointer;
  transition: all 0.3s;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
}

.contact-card:hover {
  background: #e6f7ff;
  transform: translateY(-2px);
}

.contact-info {
  margin: 12px 0;
}

.contact-info h4 {
  margin: 0 0 4px 0;
  color: #333;
}

.contact-info p {
  margin: 0 0 8px 0;
  color: #666;
  font-size: 13px;
}

.contact-actions {
  display: flex;
  gap: 8px;
}

.organization-tree {
  background: #f8f9fa;
  border-radius: 8px;
  padding: 16px;
}

.tree-node {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
}

.node-label {
  font-size: 14px;
  color: #333;
}

.member-count {
  font-size: 12px;
  color: #999;
}

.position-tag {
  margin-left: auto;
}

.groups-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.group-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  background: #f8f9fa;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.3s;
}

.group-item:hover {
  background: #e6f7ff;
}

.group-info {
  flex: 1;
}

.group-info h4 {
  margin: 0 0 4px 0;
  color: #333;
}

.group-info p {
  margin: 0 0 8px 0;
  color: #666;
  font-size: 13px;
}

.group-meta {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: #999;
}

.requests-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.request-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  background: #f8f9fa;
  border-radius: 8px;
}

.request-info {
  flex: 1;
}

.request-info h4 {
  margin: 0 0 4px 0;
  color: #333;
}

.request-info p {
  margin: 0 0 4px 0;
  color: #666;
  font-size: 13px;
}

.request-time {
  font-size: 12px;
  color: #999;
}

.request-actions {
  display: flex;
  gap: 8px;
}

.search-results {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
}

.search-results h4 {
  margin: 0 0 12px 0;
  color: #333;
}

.search-result-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 0;
}

.user-info {
  flex: 1;
}

.user-info h5 {
  margin: 0 0 2px 0;
  color: #333;
}

.user-info p {
  margin: 0;
  color: #666;
  font-size: 12px;
}

/* 群组详情样式 */
.group-detail {
  padding: 12px 0;
}

.group-header {
  display: flex;
  gap: 20px;
  align-items: flex-start;
  padding-bottom: 20px;
}

.group-header .group-info {
  flex: 1;
}

.group-header h3 {
  margin: 0 0 8px 0;
  font-size: 20px;
  color: #333;
}

.group-header .description {
  margin: 0 0 12px 0;
  color: #666;
  font-size: 14px;
  line-height: 1.6;
}

.group-header .group-meta {
  display: flex;
  gap: 8px;
}

.member-list {
  max-height: 300px;
  overflow-y: auto;
}

.member-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border-radius: 6px;
  transition: background 0.2s;
}

.member-item:hover {
  background: #f5f5f5;
}

.member-info {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 8px;
}

.member-name {
  color: #333;
  font-size: 14px;
}

.group-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
  flex-wrap: wrap;
}

/* 群公告样式 */
.group-announcement {
  margin-bottom: 20px;
}

.announcement-content {
  padding: 12px;
  background: #fff9e6;
  border-left: 3px solid #faad14;
  border-radius: 4px;
  color: #666;
  font-size: 14px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}

/* 群组骨架屏样式 */
.groups-skeleton {
  padding: 24px;
}
</style>
