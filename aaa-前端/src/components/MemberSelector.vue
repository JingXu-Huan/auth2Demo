<template>
  <a-modal
    v-model:open="visible"
    title="邀请好友加入群聊"
    width="800px"
    :footer="null"
    @cancel="handleCancel"
  >
    <div class="member-selector">
      <!-- 左侧选择区 -->
      <div class="selector-left">
        <!-- 全局搜索 -->
        <div class="search-section">
          <a-input
            v-model:value="searchKeyword"
            placeholder="搜索联系人、群组..."
            allow-clear
            @input="handleSearch"
          >
            <template #prefix>
              <search-outlined />
            </template>
          </a-input>
        </div>

        <!-- Tab 切换 -->
        <div class="tabs-section" v-if="!searchKeyword">
          <div
            v-for="tab in tabs"
            :key="tab.key"
            :class="['tab-item', { active: activeTab === tab.key }]"
            @click="activeTab = tab.key"
          >
            {{ tab.label }}
          </div>
        </div>

        <!-- 内容区域 -->
        <div class="content-section">
          <!-- 搜索结果 -->
          <div v-if="searchKeyword" class="search-results">
            <div v-if="searchResults.friends.length > 0" class="result-group">
              <div class="group-title">好友</div>
              <div
                v-for="friend in searchResults.friends"
                :key="friend.userId"
                class="member-item"
                @click="toggleMember(friend, 'friend')"
              >
                <a-checkbox :checked="isSelected(friend.userId)" />
                <a-avatar :src="friend.avatar" :size="32">
                  {{ friend.nickname?.charAt(0) || 'U' }}
                </a-avatar>
                <span class="member-name">{{ friend.nickname }}</span>
              </div>
            </div>

            <div v-if="searchResults.org.length > 0" class="result-group">
              <div class="group-title">组织架构</div>
              <div
                v-for="member in searchResults.org"
                :key="member.id"
                class="member-item"
                @click="toggleMember(member, 'org')"
              >
                <a-checkbox :checked="isSelected(member.id)" />
                <a-avatar :src="member.avatar" :size="32">
                  {{ member.name?.charAt(0) || 'U' }}
                </a-avatar>
                <span class="member-name">{{ member.name }}</span>
                <span class="member-dept">{{ member.department }}</span>
              </div>
            </div>
          </div>

          <!-- Tab A: 组织架构 -->
          <div v-else-if="activeTab === 'org'" class="org-view">
            <!-- 面包屑导航 -->
            <div class="breadcrumb" v-if="currentPath.length > 0">
              <span class="path-item" @click="navigateToRoot">Lantis</span>
              <span v-for="(item, index) in currentPath" :key="index">
                <right-outlined class="separator" />
                <span class="path-item" @click="navigateTo(index)">{{ item.name }}</span>
              </span>
            </div>

            <!-- 部门和成员列表 -->
            <div class="org-list">
              <!-- 子部门 -->
              <div
                v-for="dept in currentDepartments"
                :key="dept.id"
                class="dept-item"
                @click="enterDepartment(dept)"
              >
                <folder-outlined class="dept-icon" />
                <span class="dept-name">{{ dept.name }}</span>
                <right-outlined class="arrow-icon" />
              </div>

              <!-- 成员 -->
              <div
                v-for="member in currentMembers"
                :key="member.id"
                class="member-item"
                @click="toggleMember(member, 'org')"
              >
                <a-checkbox :checked="isSelected(member.id)" />
                <a-avatar :src="member.avatar" :size="32">
                  {{ member.name?.charAt(0) || 'U' }}
                </a-avatar>
                <span class="member-name">{{ member.name }}</span>
                <span class="member-role">{{ member.position }}</span>
              </div>
            </div>
          </div>

          <!-- Tab B: 我的好友 -->
          <div v-else-if="activeTab === 'friends'" class="friends-view">
            <div
              v-for="friend in friendsList"
              :key="friend.userId"
              class="member-item"
              @click="toggleMember(friend, 'friend')"
            >
              <a-checkbox :checked="isSelected(friend.userId)" />
              <a-avatar :src="friend.avatar" :size="32">
                {{ friend.nickname?.charAt(0) || 'U' }}
              </a-avatar>
              <span class="member-name">{{ friend.nickname }}</span>
            </div>
          </div>

          <!-- Tab C: 从群组导入 -->
          <div v-else-if="activeTab === 'groups'" class="groups-view">
            <!-- 群组列表 -->
            <div v-if="!selectedGroup" class="group-list">
              <div
                v-for="group in groupsList"
                :key="group.id"
                class="group-item"
                @click="enterGroup(group)"
              >
                <a-avatar :src="group.avatar" :size="40" shape="square">
                  {{ group.name?.charAt(0) || 'G' }}
                </a-avatar>
                <div class="group-info">
                  <div class="group-name">{{ group.name }}</div>
                  <div class="group-count">{{ group.memberCount }} 人</div>
                </div>
                <right-outlined class="arrow-icon" />
              </div>
            </div>

            <!-- 群成员列表 -->
            <div v-else class="group-members">
              <div class="back-btn" @click="selectedGroup = null">
                <left-outlined />
                返回群组列表
              </div>
              <div
                v-for="member in groupMembers"
                :key="member.userId"
                class="member-item"
                @click="toggleMember(member, 'group')"
              >
                <a-checkbox :checked="isSelected(member.userId)" />
                <a-avatar :src="member.avatar" :size="32">
                  {{ member.nickname?.charAt(0) || 'U' }}
                </a-avatar>
                <span class="member-name">{{ member.nickname }}</span>
                <span class="member-from">来自：{{ selectedGroup.name }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧已选区 -->
      <div class="selector-right">
        <div class="selected-header">
          已选择: {{ selectedMembers.size }} 人
        </div>
        <div class="selected-list">
          <div
            v-for="member in Array.from(selectedMembers.values())"
            :key="member.id"
            class="selected-item"
          >
            <a-avatar :src="member.avatar" :size="32">
              {{ member.name?.charAt(0) || 'U' }}
            </a-avatar>
            <span class="selected-name">{{ member.name }}</span>
            <close-outlined class="remove-btn" @click="removeMember(member.id)" />
          </div>
        </div>
      </div>
    </div>

    <!-- 底部按钮 -->
    <div class="modal-footer">
      <a-button @click="handleCancel">取消</a-button>
      <a-button
        type="primary"
        :disabled="selectedMembers.size === 0"
        @click="handleConfirm"
      >
        确定 ({{ selectedMembers.size }})
      </a-button>
    </div>
  </a-modal>
</template>

<script setup>
import { ref, reactive, computed, watch } from 'vue'
import {
  SearchOutlined,
  FolderOutlined,
  RightOutlined,
  LeftOutlined,
  CloseOutlined
} from '@ant-design/icons-vue'

const props = defineProps({
  visible: Boolean,
  friendsList: Array,
  groupsList: Array,
  orgTree: Array
})

const emit = defineEmits(['update:visible', 'confirm'])

const visible = computed({
  get: () => props.visible,
  set: (val) => emit('update:visible', val)
})

// Tab 配置
const tabs = [
  { key: 'org', label: '组织架构' },
  { key: 'friends', label: '我的好友' },
  { key: 'groups', label: '从群组导入' }
]

const activeTab = ref('org')
const searchKeyword = ref('')
const selectedMembers = reactive(new Map())

// 组织架构相关
const currentPath = ref([])
const currentDepartments = ref([])
const currentMembers = ref([])

// 群组相关
const selectedGroup = ref(null)
const groupMembers = ref([])

// 搜索结果
const searchResults = reactive({
  friends: [],
  org: []
})

// 判断是否已选中
const isSelected = (id) => {
  return selectedMembers.has(id)
}

// 切换选中状态
const toggleMember = (member, source) => {
  const id = member.userId || member.id
  if (selectedMembers.has(id)) {
    selectedMembers.delete(id)
  } else {
    selectedMembers.set(id, {
      id,
      name: member.nickname || member.name,
      avatar: member.avatar,
      source
    })
  }
}

// 移除成员
const removeMember = (id) => {
  selectedMembers.delete(id)
}

// 搜索处理
const handleSearch = () => {
  if (!searchKeyword.value) {
    searchResults.friends = []
    searchResults.org = []
    return
  }

  const keyword = searchKeyword.value.toLowerCase()

  // 搜索好友
  searchResults.friends = props.friendsList.filter(f =>
    f.nickname.toLowerCase().includes(keyword)
  )

  // 搜索组织架构（需要递归）
  searchResults.org = []
  // TODO: 实现组织架构搜索
}

// 组织架构导航
const navigateToRoot = () => {
  currentPath.value = []
  loadDepartmentContent()
}

const navigateTo = (index) => {
  currentPath.value = currentPath.value.slice(0, index + 1)
  loadDepartmentContent()
}

const enterDepartment = (dept) => {
  currentPath.value.push(dept)
  loadDepartmentContent()
}

const loadDepartmentContent = () => {
  // TODO: 根据 currentPath 加载部门内容
}

// 进入群组
const enterGroup = async (group) => {
  selectedGroup.value = group
  // TODO: 加载群成员
  groupMembers.value = []
}

// 确认选择
const handleConfirm = () => {
  const members = Array.from(selectedMembers.values()).map(m => ({
    uid: m.id,
    source: m.source
  }))
  emit('confirm', members)
  handleCancel()
}

// 取消
const handleCancel = () => {
  visible.value = false
  selectedMembers.clear()
  searchKeyword.value = ''
  activeTab.value = 'org'
}
</script>

<style scoped>
.member-selector {
  display: flex;
  height: 500px;
  gap: 1px;
  background: #f0f0f0;
}

/* 左侧选择区 */
.selector-left {
  flex: 1;
  background: #fff;
  display: flex;
  flex-direction: column;
}

.search-section {
  padding: 16px;
  border-bottom: 1px solid #f0f0f0;
}

.tabs-section {
  display: flex;
  padding: 8px 16px;
  gap: 8px;
  border-bottom: 1px solid #f0f0f0;
}

.tab-item {
  padding: 6px 12px;
  border-radius: 6px;
  font-size: 13px;
  color: #606266;
  cursor: pointer;
  transition: all 0.2s;
}

.tab-item.active {
  background: #E1EAFF;
  color: #3370FF;
  font-weight: 500;
}

.tab-item:hover:not(.active) {
  background: #F5F6F7;
}

.content-section {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

/* 成员项 */
.member-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 12px;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
}

.member-item:hover {
  background: #F5F6F7;
}

.member-name {
  flex: 1;
  font-size: 14px;
  color: #1F2329;
}

.member-dept,
.member-role,
.member-from {
  font-size: 12px;
  color: #8F959E;
}

/* 组织架构 */
.breadcrumb {
  padding: 8px 12px;
  font-size: 13px;
  color: #606266;
  border-bottom: 1px solid #f0f0f0;
}

.path-item {
  cursor: pointer;
  transition: color 0.2s;
}

.path-item:hover {
  color: #3370FF;
}

.separator {
  margin: 0 8px;
  font-size: 12px;
}

.dept-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
}

.dept-item:hover {
  background: #F5F6F7;
}

.dept-icon {
  font-size: 18px;
  color: #5F6368;
}

.dept-name {
  flex: 1;
  font-size: 14px;
  color: #1F2329;
}

.arrow-icon {
  font-size: 12px;
  color: #C1C4C9;
}

/* 群组 */
.group-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
}

.group-item:hover {
  background: #F5F6F7;
}

.group-info {
  flex: 1;
}

.group-name {
  font-size: 14px;
  color: #1F2329;
  margin-bottom: 4px;
}

.group-count {
  font-size: 12px;
  color: #8F959E;
}

.back-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  margin-bottom: 8px;
  color: #3370FF;
  cursor: pointer;
  font-size: 13px;
}

/* 搜索结果 */
.result-group {
  margin-bottom: 16px;
}

.group-title {
  padding: 8px 12px;
  font-size: 12px;
  color: #8F959E;
  font-weight: 500;
}

/* 右侧已选区 */
.selector-right {
  width: 240px;
  background: #FAFAFA;
  display: flex;
  flex-direction: column;
}

.selected-header {
  padding: 16px;
  font-size: 14px;
  color: #606266;
  border-bottom: 1px solid #f0f0f0;
}

.selected-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.selected-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px;
  border-radius: 6px;
  background: #fff;
  margin-bottom: 8px;
}

.selected-name {
  flex: 1;
  font-size: 13px;
  color: #1F2329;
}

.remove-btn {
  font-size: 12px;
  color: #C1C4C9;
  cursor: pointer;
  transition: color 0.2s;
}

.remove-btn:hover {
  color: #F53F3F;
}

/* 底部按钮 */
.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 16px 0 0 0;
  border-top: 1px solid #f0f0f0;
  margin-top: 16px;
}
</style>
