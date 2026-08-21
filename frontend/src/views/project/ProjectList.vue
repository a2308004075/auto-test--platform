<script setup lang="ts">
/**
 * 项目列表页（首页） - 对齐 UI 原型 project-list.html
 */
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getProjects, createProject, updateProject, deleteProject, toggleProjectStatus } from '@/api/project'
import { useProjectStore, useUserStore } from '@/stores'

const router = useRouter()
const projectStore = useProjectStore()
const userStore = useUserStore()

const loading = ref(false)
const list = ref<any[]>([])
const keyword = ref('')
const statusFilter = ref<string>('')
const pagination = reactive({ current: 1, pageSize: 9, total: 0 })
const modalVisible = ref(false)
const editingId = ref<number>(0)
const form = reactive({ name: '', description: '' })

// 项目名首字取色
const colorPalette = [
  '#1890ff', '#fa8c16', '#52c41a', '#f5222d', '#2f54eb',
  '#eb2f96', '#722ed1', '#13c2c2', '#a0d911', '#fa541c', '#595959',
]
const bgPalette = [
  '#e6f7ff', '#fff7e6', '#f6ffed', '#fff1f0', '#f0f5ff',
  '#fff0f6', '#f9f0ff', '#e6fffb', '#fcffe6', '#fff2e8', '#f0f0f0',
]

function getProjectColor(name: string) {
  const code = name.charCodeAt(0) % colorPalette.length
  return { color: colorPalette[code], bg: bgPalette[code] }
}

async function fetchList() {
  loading.value = true
  try {
    const params: any = {
      keyword: keyword.value,
      page: pagination.current,
      pageSize: pagination.pageSize,
    }
    if (statusFilter.value === 'active') params.status = 1
    else if (statusFilter.value === 'disabled') params.status = 0
    const res: any = await getProjects(params)
    list.value = res.data?.items || []
    pagination.total = res.data?.total || 0
  } catch {
    list.value = []
  } finally {
    loading.value = false
  }
}

function enterProject(project: any) {
  if (project.status === 0) return
  if (!userStore.isLoggedIn) {
    ElMessage.info('请先登录后再进入项目')
    return
  }
  projectStore.setCurrentProject(project.id, project.name)
  router.push(`/project/${project.id}/dashboard`)
}

function openCreate() {
  if (!userStore.isLoggedIn) {
    ElMessage.info('请先登录后再创建项目')
    return
  }
  editingId.value = 0
  form.name = ''
  form.description = ''
  modalVisible.value = true
}

function openEdit(project: any) {
  editingId.value = project.id
  form.name = project.name
  form.description = project.description || ''
  modalVisible.value = true
}

async function handleSubmit() {
  if (!form.name) { ElMessage.warning('请输入项目名称'); return }
  try {
    if (editingId.value) {
      await updateProject(editingId.value, form)
      ElMessage.success('更新成功')
    } else {
      await createProject(form)
      ElMessage.success('创建成功')
    }
    modalVisible.value = false
    fetchList()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '操作失败')
  }
}

function handleDelete(project: any) {
  ElMessageBox.confirm(
    `确定删除项目「${project.name}」？\n删除后项目数据将归档，不可恢复。项目下的接口、用例、执行记录等数据将一并删除。`,
    '确认删除',
    { confirmButtonText: '确认删除', cancelButtonText: '取消', type: 'warning', confirmButtonClass: 'el-button--danger' }
  ).then(async () => {
    await deleteProject(project.id)
    ElMessage.success('删除成功')
    fetchList()
  }).catch(() => {})
}

function handleToggleStatus(project: any) {
  const isActive = project.status === 1
  const title = isActive ? '停用项目' : '启用项目'
  const msg = isActive
    ? `确定停用项目「${project.name}」？停用后项目将无法访问，已有数据不会被删除，重新启用后可恢复使用。`
    : `确定启用项目「${project.name}」？启用后项目将恢复正常访问，所有成员可继续使用。`
  const confirmText = isActive ? '确认停用' : '确认启用'
  ElMessageBox.confirm(msg, title, {
    confirmButtonText: confirmText,
    cancelButtonText: '取消',
    type: 'warning',
  }).then(async () => {
    await toggleProjectStatus(project.id)
    ElMessage.success(isActive ? '已停用' : '已启用')
    fetchList()
  }).catch(() => {})
}

function handleSearch() {
  pagination.current = 1
  fetchList()
}

onMounted(fetchList)
</script>

<template>
  <div class="project-home">
    <!-- 页头 -->
    <div class="page-header">
      <div>
        <h1 class="page-title">首页</h1>
        <p class="page-desc">选择项目快速进入工作台</p>
      </div>
      <el-button v-if="userStore.isLoggedIn" type="primary" @click="openCreate">+ 新建项目</el-button>
    </div>

    <!-- 搜索筛选区 -->
    <div class="toolbar">
      <el-input
        v-model="keyword"
        placeholder="搜索项目名称"
        style="width: 220px"
        clearable
        @keyup.enter="handleSearch"
        @clear="handleSearch"
      />
      <el-select v-model="statusFilter" placeholder="全部状态" style="width: 120px" clearable @change="handleSearch">
        <el-option label="全部状态" value="" />
        <el-option label="启用" value="active" />
        <el-option label="停用" value="disabled" />
      </el-select>
    </div>

    <!-- 项目卡片网格 -->
    <div v-loading="loading" class="project-grid">
      <div
        v-for="project in list"
        :key="project.id"
        class="project-card"
        :class="{ disabled: project.status === 0 }"
        @click="enterProject(project)"
      >
        <div class="card-body">
          <!-- 标题行 -->
          <div class="card-header-row">
            <div class="card-title-area">
              <div class="project-icon" :style="{ background: getProjectColor(project.name).bg, color: getProjectColor(project.name).color }">
                {{ project.name?.charAt(0) || '?' }}
              </div>
              <span class="project-name">{{ project.name }}</span>
            </div>
            <div class="card-actions" @click.stop>
              <span
                class="status-tag"
                :class="project.status === 1 ? 'tag-active' : 'tag-disabled'"
                @click="userStore.isLoggedIn && handleToggleStatus(project)"
              >
                {{ project.status === 1 ? '启用' : '停用' }}
              </span>
              <template v-if="userStore.isLoggedIn">
                <button class="btn-edit" @click="openEdit(project)">编辑</button>
                <button class="btn-delete" @click="handleDelete(project)">删除</button>
              </template>
            </div>
          </div>
          <!-- 描述 -->
          <p class="card-desc">{{ project.description || '暂无描述' }}</p>
          <!-- 统计 -->
          <div class="card-stats">
            <span>接口：<b>{{ project.apiCount || 0 }}</b></span>
            <span>关键字：<b>{{ project.keywordCount || 0 }}</b></span>
            <span>Action：<b>{{ project.actionCount || 0 }}</b></span>
            <span>用例：<b>{{ project.caseCount || 0 }}</b></span>
            <span>套件：<b>{{ project.suiteCount || 0 }}</b></span>
            <span>计划：<b>{{ project.planCount || 0 }}</b></span>
          </div>
        </div>
        <div class="card-footer">
          <span>创建于 {{ project.createdAt?.substring(0, 16).replace('T', ' ') }}</span>
        </div>
      </div>
    </div>

    <el-empty v-if="!list.length && !loading" :description="`暂无项目${userStore.isLoggedIn ? '，点击「新建项目」开始' : ''}`" />

    <!-- 分页 -->
    <div v-if="pagination.total > pagination.pageSize" class="pagination-wrap">
      <el-pagination
        v-model:current-page="pagination.current"
        :page-size="pagination.pageSize"
        :total="pagination.total"
        layout="total, prev, pager, next"
        @current-change="fetchList"
      />
    </div>

    <!-- 新建/编辑弹窗 -->
    <el-dialog v-model="modalVisible" :title="editingId ? '编辑项目信息' : '新建项目'" width="500px">
      <el-form label-position="top" style="margin-top: 8px">
        <el-form-item label="项目名称">
          <el-input v-model="form.name" placeholder="请输入项目名称" maxlength="50" show-word-limit />
          <div class="form-hint" v-if="editingId">必填，最长 50 字符</div>
        </el-form-item>
        <el-form-item label="项目描述">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入项目描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="modalVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">{{ editingId ? '保存' : '确定' }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.project-home {
  max-width: 1400px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
.page-title {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #303133;
}
.page-desc {
  margin: 4px 0 0;
  font-size: 14px;
  color: #909399;
}

.toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.project-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14px;
}

.project-card {
  background: #fff;
  border-radius: 6px;
  border: 1px solid #f0f0f0;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.03), 0 1px 6px -1px rgba(0, 0, 0, 0.02), 0 2px 4px rgba(0, 0, 0, 0.02);
  cursor: pointer;
  transition: box-shadow 0.2s, opacity 0.4s ease, transform 0.2s;
}
.project-card:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  transform: translateY(-1px);
}
.project-card.disabled {
  opacity: 0.7;
  cursor: default;
}
.project-card.disabled:hover {
  transform: none;
}

.card-body {
  padding: 16px 18px 12px;
}

.card-header-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.card-title-area {
  display: flex;
  align-items: center;
  gap: 8px;
}

.project-icon {
  width: 34px;
  height: 34px;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 15px;
  font-weight: 700;
  flex-shrink: 0;
}

.project-name {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.card-actions {
  display: flex;
  align-items: center;
  gap: 6px;
}

.status-tag {
  font-size: 12px;
  padding: 1px 8px;
  border-radius: 3px;
  cursor: pointer;
  user-select: none;
  transition: opacity 0.2s;
  font-weight: 500;
}
.status-tag:hover { opacity: 0.7; }
.tag-active {
  background: #f6ffed;
  color: #52c41a;
  border: 1px solid #b7eb8f;
}
.tag-disabled {
  background: #f5f5f5;
  color: #909399;
  border: 1px solid #d9d9d9;
}

.btn-edit, .btn-delete {
  background: none;
  border: 1px solid;
  cursor: pointer;
  font-size: 12px;
  padding: 1px 8px;
  border-radius: 3px;
  transition: color 0.2s, background 0.2s;
  line-height: 1.6;
  font-weight: 500;
}
.btn-edit {
  color: #409eff;
  border-color: #409eff;
}
.btn-edit:hover {
  color: #fff;
  background: #409eff;
}
.btn-delete {
  color: #f56c6c;
  border-color: #f56c6c;
}
.btn-delete:hover {
  color: #fff;
  background: #f56c6c;
}

.card-desc {
  font-size: 12px;
  color: #909399;
  margin: 0 0 12px;
  min-height: 18px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.card-stats {
  display: flex;
  gap: 12px;
  font-size: 12px;
  color: #606266;
  flex-wrap: wrap;
}
.card-stats b {
  font-weight: 600;
  color: #303133;
}

.card-footer {
  padding: 10px 18px;
  border-top: 1px solid #f0f0f0;
  font-size: 11px;
  color: #c0c4cc;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}

.form-hint {
  font-size: 11px;
  color: #909399;
  margin-top: 4px;
}

@media (max-width: 1200px) {
  .project-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
