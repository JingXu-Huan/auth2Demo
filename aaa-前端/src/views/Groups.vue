<template>
  <div class="groups-container">
    <div class="header">
      <h2>我的群聊</h2>
      <div class="header-actions">
        <el-button type="primary" @click="showCreateDialog = true">
          <el-icon><Plus /></el-icon>
          创建群组
        </el-button>
      </div>
    </div>

    <!-- 群组列表 -->
    <div class="groups-grid">
      <div 
        v-for="group in groups" 
        :key="group.groupId"
        class="group-card"
        @click="selectGroup(group)"
      >
        <div class="group-avatar">
          <el-avatar :size="60" :src="group.avatar">
            {{ group.name.charAt(0) }}
          </el-avatar>
        </div>
        <div class="group-info">
          <h3>{{ group.name }}</h3>
          <p>{{ group.description || '暂无描述' }}</p>
          <div class="group-stats">
            <span>{{ group.memberCount }}/{{ group.maxMembers }} 成员</span>
            <span class="join-type">{{ getJoinTypeText(group.joinType) }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 创建群组对话框 -->
    <el-dialog v-model="showCreateDialog" title="创建群组" width="500px">
      <el-form :model="createForm" :rules="createRules" ref="createFormRef" label-width="80px">
        <el-form-item label="群组名称" prop="name">
          <el-input v-model="createForm.name" placeholder="请输入群组名称" />
        </el-form-item>
        <el-form-item label="群组描述" prop="description">
          <el-input 
            v-model="createForm.description" 
            type="textarea" 
            :rows="3"
            placeholder="请输入群组描述"
          />
        </el-form-item>
        <el-form-item label="最大成员">
          <el-input-number v-model="createForm.maxMembers" :min="2" :max="500" />
        </el-form-item>
        <el-form-item label="加入方式">
          <el-select v-model="createForm.joinType" placeholder="请选择加入方式">
            <el-option label="自由加入" value="FREE" />
            <el-option label="需要审批" value="APPROVAL" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" @click="handleCreateGroup" :loading="creating">
          创建
        </el-button>
      </template>
    </el-dialog>

    <!-- 群组详情对话框 -->
    <el-dialog v-model="showDetailDialog" :title="selectedGroup?.name" width="800px">
      <div v-if="selectedGroup" class="group-detail">
        <el-tabs v-model="activeTab">
          <el-tab-pane label="群组信息" name="info">
            <div class="group-detail-info">
              <div class="info-item">
                <label>群组ID:</label>
                <span>{{ selectedGroup.groupId }}</span>
              </div>
              <div class="info-item">
                <label>群组名称:</label>
                <span>{{ selectedGroup.name }}</span>
              </div>
              <div class="info-item">
                <label>群组描述:</label>
                <span>{{ selectedGroup.description || '暂无描述' }}</span>
              </div>
              <div class="info-item">
                <label>成员数量:</label>
                <span>{{ selectedGroup.memberCount }}/{{ selectedGroup.maxMembers }}</span>
              </div>
              <div class="info-item">
                <label>加入方式:</label>
                <span>{{ getJoinTypeText(selectedGroup.joinType) }}</span>
              </div>
              <div class="info-item">
                <label>创建时间:</label>
                <span>{{ formatTime(selectedGroup.createdAt) }}</span>
              </div>
            </div>
          </el-tab-pane>
          
          <el-tab-pane label="成员管理" name="members">
            <div class="members-section">
              <div class="members-header">
                <el-input 
                  v-model="memberSearch"
                  placeholder="搜索成员"
                  style="width: 200px"
                  @input="searchMembers"
                >
                  <template #prefix>
                    <el-icon><Search /></el-icon>
                  </template>
                </el-input>
                <el-button type="primary" @click="showAddMemberDialog = true">
                  添加成员
                </el-button>
              </div>
              
              <el-table :data="members" style="width: 100%">
                <el-table-column prop="userId" label="用户ID" width="100" />
                <el-table-column prop="nickname" label="昵称" />
                <el-table-column prop="role" label="角色">
                  <template #default="{ row }">
                    <el-tag :type="getRoleTagType(row.role)">
                      {{ getRoleText(row.role) }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="joinedAt" label="加入时间">
                  <template #default="{ row }">
                    {{ formatTime(row.joinedAt) }}
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="200">
                  <template #default="{ row }">
                    <el-button 
                      v-if="row.role === 'MEMBER'" 
                      size="small" 
                      @click="setAdmin(row.userId, 'ADD')"
                    >
                      设为管理员
                    </el-button>
                    <el-button 
                      v-if="row.role === 'ADMIN'" 
                      size="small" 
                      @click="setAdmin(row.userId, 'REMOVE')"
                    >
                      取消管理员
                    </el-button>
                    <el-button 
                      v-if="row.role !== 'OWNER'" 
                      size="small" 
                      type="danger" 
                      @click="removeMember(row.userId)"
                    >
                      移除
                    </el-button>
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>
    </el-dialog>

    <!-- 添加成员对话框 -->
    <el-dialog v-model="showAddMemberDialog" title="添加成员" width="400px">
      <el-form :model="addMemberForm" ref="addMemberFormRef">
        <el-form-item label="用户ID">
          <el-input 
            v-model="addMemberForm.userIds" 
            placeholder="请输入用户ID，多个用逗号分隔"
            type="textarea"
            :rows="3"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddMemberDialog = false">取消</el-button>
        <el-button type="primary" @click="handleAddMembers" :loading="addingMember">
          添加
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, ChatDotRound, Search } from '@element-plus/icons-vue'
import { useUserStore } from '../stores/user'
import { groupAPI } from '../api'
import dayjs from 'dayjs'

const router = useRouter()
const userStore = useUserStore()

const groups = ref([])
const showCreateDialog = ref(false)
const showDetailDialog = ref(false)
const showAddMemberDialog = ref(false)
const selectedGroup = ref(null)
const members = ref([])
const memberSearch = ref('')
const activeTab = ref('info')
const creating = ref(false)
const addingMember = ref(false)

const createForm = reactive({
  name: '',
  description: '',
  maxMembers: 100,
  joinType: 'FREE'
})

const addMemberForm = reactive({
  userIds: ''
})

const createRules = {
  name: [
    { required: true, message: '请输入群组名称', trigger: 'blur' }
  ]
}

// 检查登录状态
if (!userStore.isLoggedIn) {
  router.push('/login')
}

// 获取用户群组列表
const loadUserGroups = async () => {
  try {
    const response = await groupAPI.getUserGroups(userStore.user.id, { page: 1, size: 50 })
    groups.value = response.data?.groups || []
  } catch (error) {
    console.error('获取群组列表失败:', error)
    ElMessage.error('加载群组列表失败')
    groups.value = []
  }
}

// 创建群组
const handleCreateGroup = async () => {
  try {
    creating.value = true
    const formData = {
      ...createForm,
      ownerId: userStore.user.id
    }
    const response = await groupAPI.createGroup(formData)

    if (response && response.code === 200 && response.data) {
      groups.value.unshift(response.data)
      showCreateDialog.value = false
      ElMessage.success(response.message || '群组创建成功')
    } else {
      throw new Error(response?.message || '创建群组失败')
    }
    
    // 重置表单
    Object.assign(createForm, {
      name: '',
      description: '',
      maxMembers: 100,
      joinType: 'FREE'
    })
    
  } catch (error) {
    console.error('创建群组失败:', error)
    ElMessage.error('创建群组失败')
  } finally {
    creating.value = false
  }
}

// 选择群组 - 打开详情对话框
const selectGroup = async (group) => {
  selectedGroup.value = group
  showDetailDialog.value = true
  activeTab.value = 'members'
  await loadMembers(group.groupId)
}

// 加载成员列表
const loadMembers = async (groupId) => {
  try {
    const response = await groupAPI.getMembers(groupId, { page: 1, size: 100 })
    members.value = response.data?.members || []
  } catch (error) {
    console.error('获取成员列表失败:', error)
    ElMessage.error('加载成员列表失败')
    members.value = []
  }
}

// 搜索成员
const searchMembers = async () => {
  if (!memberSearch.value.trim()) {
    await loadMembers(selectedGroup.value.groupId)
    return
  }
  
  try {
    const response = await groupAPI.searchMembers(selectedGroup.value.groupId, {
      keyword: memberSearch.value,
      limit: 20
    })
    members.value = response.data || []
  } catch (error) {
    console.error('搜索成员失败:', error)
  }
}

// 添加成员
const handleAddMembers = async () => {
  if (!addMemberForm.userIds.trim()) {
    ElMessage.warning('请输入用户ID')
    return
  }
  
  try {
    addingMember.value = true
    const userIds = addMemberForm.userIds.split(',').map(id => parseInt(id.trim())).filter(id => !isNaN(id))
    
    const response = await groupAPI.addMembers(selectedGroup.value.groupId, {
      userIds,
      inviterId: userStore.user.id
    })
    
    ElMessage.success(`成功添加 ${response.data?.addedCount || 0} 个成员`)
    showAddMemberDialog.value = false
    addMemberForm.userIds = ''
    await loadMembers(selectedGroup.value.groupId)
    
  } catch (error) {
    console.error('添加成员失败:', error)
    ElMessage.error('添加成员失败')
  } finally {
    addingMember.value = false
  }
}

// 设置管理员
const setAdmin = async (userId, action) => {
  try {
    await groupAPI.setAdmin(selectedGroup.value.groupId, { userId, action })
    ElMessage.success(action === 'ADD' ? '设置管理员成功' : '取消管理员成功')
    await loadMembers(selectedGroup.value.groupId)
  } catch (error) {
    console.error('设置管理员失败:', error)
    ElMessage.error('操作失败')
  }
}

// 移除成员
const removeMember = async (userId) => {
  try {
    await ElMessageBox.confirm('确定要移除该成员吗？', '确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    await groupAPI.removeMember(selectedGroup.value.groupId, userId)
    ElMessage.success('移除成员成功')
    await loadMembers(selectedGroup.value.groupId)
    
  } catch (error) {
    if (error !== 'cancel') {
      console.error('移除成员失败:', error)
      ElMessage.error('移除成员失败')
    }
  }
}

// 工具函数
const getJoinTypeText = (joinType) => {
  const map = {
    'FREE': '自由加入',
    'APPROVAL': '需要审批'
  }
  return map[joinType] || joinType
}

const getRoleText = (role) => {
  const map = {
    'OWNER': '群主',
    'ADMIN': '管理员',
    'MEMBER': '成员'
  }
  return map[role] || role
}

const getRoleTagType = (role) => {
  const map = {
    'OWNER': 'danger',
    'ADMIN': 'warning',
    'MEMBER': 'info'
  }
  return map[role] || 'info'
}

const formatTime = (timestamp) => {
  return dayjs(timestamp).format('YYYY-MM-DD HH:mm:ss')
}

onMounted(() => {
  loadUserGroups()
})
</script>

<style scoped>
.groups-container {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
}

.header h2 {
  margin: 0;
  color: #333;
}

.header-actions {
  display: flex;
  gap: 10px;
}

.groups-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
}

.group-card {
  background: white;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  cursor: pointer;
  transition: all 0.3s;
}

.group-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
}

.group-avatar {
  text-align: center;
  margin-bottom: 15px;
}

.group-info h3 {
  margin: 0 0 8px 0;
  color: #333;
  text-align: center;
}

.group-info p {
  margin: 0 0 15px 0;
  color: #666;
  font-size: 14px;
  text-align: center;
  min-height: 20px;
}

.group-stats {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #999;
}

.join-type {
  background: #f0f0f0;
  padding: 2px 6px;
  border-radius: 4px;
}

.group-detail-info {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.info-item {
  display: flex;
  align-items: center;
}

.info-item label {
  width: 100px;
  font-weight: 500;
  color: #666;
}

.info-item span {
  color: #333;
}

.members-section {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.members-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
