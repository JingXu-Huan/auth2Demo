<template>
  <div class="documents-page">
    <div class="page-header">
      <h2>云文档</h2>
      <el-button type="primary" :icon="Plus" @click="createDocument">
        新建文档
      </el-button>
    </div>

    <div class="doc-list">
      <div
        v-for="doc in documents"
        :key="doc.docId"
        class="doc-item"
        @click="openDocument(doc)"
      >
        <div class="doc-icon">
          <el-icon :size="32" color="#3370ff"><Document /></el-icon>
        </div>
        <div class="doc-info">
          <div class="doc-title">{{ doc.title }}</div>
          <div class="doc-meta">
            <span>{{ formatTime(doc.updatedAt) }} 更新</span>
          </div>
        </div>
        <el-dropdown trigger="click" @command="handleCommand($event, doc)">
          <el-button :icon="More" circle />
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="rename">
                <el-icon><Edit /></el-icon>
                重命名
              </el-dropdown-item>
              <el-dropdown-item command="share">
                <el-icon><Share /></el-icon>
                分享
              </el-dropdown-item>
              <el-dropdown-item command="delete" divided>
                <el-icon><Delete /></el-icon>
                删除
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>

      <el-empty v-if="documents.length === 0" description="暂无文档">
        <el-button type="primary" @click="createDocument">创建第一个文档</el-button>
      </el-empty>
    </div>

    <!-- 新建文档对话框 -->
    <el-dialog v-model="showCreateDialog" title="新建文档" width="400px">
      <el-input v-model="newDocTitle" placeholder="请输入文档标题" />
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" @click="confirmCreate">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Document, More, Edit, Share, Delete } from '@element-plus/icons-vue'
import { docApi } from '../api'
import dayjs from 'dayjs'
import relativeTime from 'dayjs/plugin/relativeTime'
import 'dayjs/locale/zh-cn'

dayjs.extend(relativeTime)
dayjs.locale('zh-cn')

const documents = ref([])
const showCreateDialog = ref(false)
const newDocTitle = ref('')

const loadDocuments = async () => {
  try {
    const res = await docApi.getMyDocs()
    if (res.code === 200 && res.data) {
      documents.value = res.data || []
    } else if (res.success && res.documents) {
      documents.value = res.documents || []
    }
  } catch (error) {
    // 文档服务可能未启动，静默处理
    console.warn('文档服务暂不可用:', error.message)
    documents.value = []
  }
}

const createDocument = () => {
  newDocTitle.value = ''
  showCreateDialog.value = true
}

const confirmCreate = async () => {
  if (!newDocTitle.value.trim()) {
    ElMessage.warning('请输入文档标题')
    return
  }

  try {
    const res = await docApi.create(newDocTitle.value.trim())
    if (res.success) {
      ElMessage.success('创建成功')
      showCreateDialog.value = false
      loadDocuments()
    }
  } catch (error) {
    ElMessage.error('创建失败')
  }
}

const openDocument = (doc) => {
  ElMessage.info('文档编辑功能开发中')
}

const handleCommand = async (command, doc) => {
  switch (command) {
    case 'rename':
      const newName = await ElMessageBox.prompt('请输入新名称', '重命名', {
        inputValue: doc.title,
        confirmButtonText: '确定',
        cancelButtonText: '取消'
      })
      if (newName.value) {
        ElMessage.info('重命名功能开发中')
      }
      break
    case 'share':
      ElMessage.info('分享功能开发中')
      break
    case 'delete':
      await ElMessageBox.confirm('确定删除该文档吗？', '删除确认', {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning'
      })
      try {
        await docApi.remove(doc.docId)
        ElMessage.success('删除成功')
        loadDocuments()
      } catch (error) {
        ElMessage.error('删除失败')
      }
      break
  }
}

const formatTime = (time) => {
  return dayjs(time).fromNow()
}

onMounted(() => {
  loadDocuments()
})
</script>

<style scoped>
.documents-page {
  height: 100%;
  padding: 24px;
  background: #f5f6f7;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.page-header h2 {
  margin: 0;
  font-size: 22px;
}

.doc-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
}

.doc-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
  background: #fff;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.doc-item:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  transform: translateY(-2px);
}

.doc-icon {
  width: 56px;
  height: 56px;
  background: #e8f3ff;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.doc-info {
  flex: 1;
}

.doc-title {
  font-weight: 500;
  margin-bottom: 4px;
  color: #1f2329;
}

.doc-meta {
  font-size: 13px;
  color: #8f959e;
}
</style>
