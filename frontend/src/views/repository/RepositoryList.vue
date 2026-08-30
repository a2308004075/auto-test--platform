<!--
 @author HXN
 @date 2026-08-30
 @description 项目源代码列表视图
-->
<script setup lang="ts">
/**
 * 项目源代码 - Git 仓库登记与代码拉取
 */
import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import {
  getRepositories,
  createRepository,
  updateRepository,
  deleteRepository,
  pullRepository,
  getPullLogs,
} from '@/api/repository'
import { useProjectStore } from '@/stores/modules/project'
import { usePermission } from '@/composables/usePermission'
import { useDict } from '@/composables/useDict'

const route = useRoute()
const { hasPermission } = usePermission()
const projectStore = useProjectStore()
const projectId = computed(() => Number(route.params.id))

// 拉取状态字典（禁止前端硬编码状态文案）
const { options: pullStatusOptions } = useDict('repository_pull_status')
const pullStatusLabel = computed(() => {
  const map: Record<string, string> = {}
  pullStatusOptions.value.forEach((o) => {
    map[o.value] = o.label
  })
  return (val: string | null | undefined) => (val ? map[val] || val : '-')
})

const loading = ref(false)
const list = ref<any[]>([])

// 新建/编辑弹窗
const modalVisible = ref(false)
const isEdit = ref(false)
const editingId = ref<number | null>(null)
const formRef = ref<FormInstance>()
const form = reactive({
  name: '',
  gitUrl: '',
  branch: '',
  authUsername: '',
  authPassword: '',
  description: '',
})

const rules = reactive<FormRules>({
  name: [
    { required: true, message: '请输入仓库名称', trigger: 'blur' },
    { max: 50, message: '仓库名称长度不能超过 50 个字符', trigger: 'blur' },
  ],
  gitUrl: [
    { required: true, message: '请输入 Git 地址', trigger: 'blur' },
    { max: 500, message: 'Git 地址长度不能超过 500 个字符', trigger: 'blur' },
  ],
  branch: [{ max: 100, message: '分支长度不能超过 100 个字符', trigger: 'blur' }],
})

// 行级拉取 loading（防止同一仓库重复点击拉取）
const pullingIds = ref<number[]>([])

// 拉取记录抽屉
const logsDrawerVisible = ref(false)
const logsLoading = ref(false)
const logsList = ref<any[]>([])
const currentRepoName = ref('')

async function fetchList() {
  loading.value = true
  try {
    const res: any = await getRepositories(projectId.value)
    list.value = res.data || []
  } catch {
    list.value = []
  } finally {
    loading.value = false
  }
}

function openCreate() {
  isEdit.value = false
  editingId.value = null
  Object.assign(form, { name: '', gitUrl: '', branch: '', authUsername: '', authPassword: '', description: '' })
  modalVisible.value = true
}

function openEdit(record: any) {
  isEdit.value = true
  editingId.value = record.id
  Object.assign(form, {
    name: record.name,
    gitUrl: record.gitUrl,
    branch: record.branch || '',
    authUsername: record.authUsername || '',
    authPassword: '',
    description: record.description || '',
  })
  modalVisible.value = true
}

function handleSubmit() {
  formRef.value?.validate(async (valid) => {
    if (!valid) return
    try {
      if (isEdit.value && editingId.value) {
        await updateRepository(projectId.value, editingId.value, { ...form })
        ElMessage.success('保存成功')
      } else {
        await createRepository(projectId.value, { ...form, projectId: projectId.value })
        ElMessage.success('创建成功')
      }
      modalVisible.value = false
      fetchList()
    } catch (e: any) {
      ElMessage.error(e?.response?.data?.message || '保存失败')
    }
  })
}

function handleDialogClosed() {
  formRef.value?.resetFields()
}

async function handlePull(record: any) {
  pullingIds.value.push(record.id)
  try {
    const res: any = await pullRepository(projectId.value, record.id)
    if (res.data?.success) {
      const typeText = res.data.pullType === 'CLONE' ? '克隆成功' : '拉取成功'
      const commitText = res.data.commitId ? `（${res.data.commitId.substring(0, 8)}）` : ''
      ElMessage.success(`${typeText}${commitText}`)
    } else {
      ElMessage.error(res.data?.message || '拉取失败')
    }
    fetchList()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '拉取失败')
  } finally {
    pullingIds.value = pullingIds.value.filter((id) => id !== record.id)
  }
}

async function handleShowLogs(record: any) {
  currentRepoName.value = record.name
  logsDrawerVisible.value = true
  logsLoading.value = true
  try {
    const res: any = await getPullLogs(projectId.value, record.id)
    logsList.value = res.data || []
  } catch {
    logsList.value = []
  } finally {
    logsLoading.value = false
  }
}

function handleDelete(record: any) {
  ElMessageBox.confirm(
    `确定删除仓库「${record.name}」？删除后将同时删除服务器上已拉取的本地代码目录，且不可恢复。`,
    '确认删除',
    { type: 'warning', confirmButtonText: '确认删除', cancelButtonText: '取消' }
  )
    .then(async () => {
      await deleteRepository(projectId.value, record.id)
      ElMessage.success('删除成功')
      fetchList()
    })
    .catch(() => {})
}

// ───────────────────── 展示格式化 ─────────────────────

function formatTime(value: string | null | undefined) {
  return value ? value.substring(0, 19).replace('T', ' ') : '-'
}

function formatCommit(value: string | null | undefined) {
  return value ? value.substring(0, 8) : '-'
}

function formatDuration(ms: number | null | undefined) {
  if (ms == null) return '-'
  if (ms < 1000) return `${ms}ms`
  const seconds = Math.floor(ms / 1000)
  if (seconds < 60) return `${seconds}s`
  const minutes = Math.floor(seconds / 60)
  return `${minutes}m${seconds % 60}s`
}

function formatPullType(value: string) {
  return value === 'CLONE' ? '克隆' : '更新'
}

function statusTagType(status: string | null | undefined) {
  if (status === 'SUCCESS') return 'success'
  if (status === 'FAILED') return 'danger'
  if (status === 'RUNNING') return 'warning'
  return 'info'
}

onMounted(fetchList)
</script>

<template>
  <div>
    <!-- 页面头部 -->
    <div class="page-header">
      <h2>项目源代码</h2>
      <el-button v-if="hasPermission('project:repo:add')" type="primary" @click="openCreate">
        + 新建仓库
      </el-button>
    </div>

    <!-- 项目上下文栏 -->
    <div class="repo-project-bar">
      <span>&#x1F4CC;</span>
      <span>当前项目：<span class="project-name">{{ projectStore.currentProjectName }}</span></span>
      <span class="bar-sep">|</span>
      <span>登记 Git 仓库并拉取代码到服务器，供测试执行使用</span>
    </div>

    <!-- 仓库列表表格 -->
    <div class="repo-table-section">
      <el-table v-loading="loading" :data="list" row-key="id" style="width: 100%">
        <el-table-column prop="name" label="仓库名称" min-width="140">
          <template #default="{ row }">
            <strong>{{ row.name }}</strong>
          </template>
        </el-table-column>
        <el-table-column prop="gitUrl" label="Git 地址" min-width="260" show-overflow-tooltip />
        <el-table-column prop="branch" label="分支" width="110">
          <template #default="{ row }">
            {{ row.branch || '默认' }}
          </template>
        </el-table-column>
        <el-table-column label="认证" width="70" align="center">
          <template #default="{ row }">
            {{ row.hasAuth ? '有' : '无' }}
          </template>
        </el-table-column>
        <el-table-column label="最近拉取状态" width="120" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.lastPullStatus" :type="statusTagType(row.lastPullStatus)" size="small">
              {{ pullStatusLabel(row.lastPullStatus) }}
            </el-tag>
            <span v-else class="empty-text-inline">-</span>
          </template>
        </el-table-column>
        <el-table-column label="最近拉取时间" width="170">
          <template #default="{ row }">
            {{ formatTime(row.lastPullAt) }}
          </template>
        </el-table-column>
        <el-table-column label="最近 Commit" width="110">
          <template #default="{ row }">
            {{ formatCommit(row.lastCommitId) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="230" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="hasPermission('project:repo:pull')"
              type="primary"
              link
              size="small"
              :loading="pullingIds.includes(row.id)"
              @click="handlePull(row)"
            >
              拉取
            </el-button>
            <el-button
              v-if="hasPermission('project:repo:logs')"
              type="primary"
              link
              size="small"
              @click="handleShowLogs(row)"
            >
              记录
            </el-button>
            <el-button
              v-if="hasPermission('project:repo:edit')"
              type="primary"
              link
              size="small"
              @click="openEdit(row)"
            >
              编辑
            </el-button>
            <el-button
              v-if="hasPermission('project:repo:delete')"
              type="danger"
              link
              size="small"
              @click="handleDelete(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
        <template #empty>
          <div class="empty-text">暂无仓库，点击「新建仓库」开始登记 Git 仓库</div>
        </template>
      </el-table>
    </div>

    <!-- 新建/编辑仓库弹窗 -->
    <el-dialog
      v-model="modalVisible"
      :title="isEdit ? '编辑仓库' : '新建仓库'"
      width="560px"
      @closed="handleDialogClosed"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="仓库名称" prop="name">
          <el-input v-model="form.name" placeholder="如 auto-test-platform" maxlength="50" show-word-limit />
        </el-form-item>
        <el-form-item label="Git 地址" prop="gitUrl">
          <el-input v-model="form.gitUrl" placeholder="https://github.com/user/repo.git" maxlength="500" />
        </el-form-item>
        <el-form-item label="分支" prop="branch">
          <el-input v-model="form.branch" placeholder="留空使用仓库默认分支" maxlength="100" />
        </el-form-item>
        <el-form-item label="认证用户名" prop="authUsername">
          <el-input v-model="form.authUsername" placeholder="私有仓库填写用户名（Token 场景填 Token 用户名）" maxlength="200" />
        </el-form-item>
        <el-form-item label="认证密码 / Token" prop="authPassword">
          <el-input
            v-model="form.authPassword"
            type="password"
            show-password
            :placeholder="isEdit ? '留空保持不变' : '私有仓库填写密码或 Access Token'"
            maxlength="500"
          />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="2"
            placeholder="仓库描述"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="modalVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 拉取记录抽屉 -->
    <el-drawer v-model="logsDrawerVisible" :title="`拉取记录 - ${currentRepoName}`" size="640px">
      <el-table v-loading="logsLoading" :data="logsList" row-key="id" style="width: 100%">
        <el-table-column label="时间" width="165">
          <template #default="{ row }">
            {{ formatTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="类型" width="70" align="center">
          <template #default="{ row }">
            {{ formatPullType(row.pullType) }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">
              {{ pullStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="分支" width="100">
          <template #default="{ row }">
            {{ row.branch || '默认' }}
          </template>
        </el-table-column>
        <el-table-column label="Commit" width="90">
          <template #default="{ row }">
            {{ formatCommit(row.commitId) }}
          </template>
        </el-table-column>
        <el-table-column label="耗时" width="80">
          <template #default="{ row }">
            {{ formatDuration(row.durationMs) }}
          </template>
        </el-table-column>
        <el-table-column label="信息" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.message || '-' }}
          </template>
        </el-table-column>
        <template #empty>
          <div class="empty-text">暂无拉取记录</div>
        </template>
      </el-table>
    </el-drawer>
  </div>
</template>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.page-header h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
}

.repo-project-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 16px;
  background: #ecf5ff;
  border: 1px solid #c6e2ff;
  border-radius: 6px;
  margin-bottom: 16px;
  font-size: 13px;
  color: rgba(0, 0, 0, 0.65);
}

.repo-project-bar .project-name {
  font-weight: 600;
  color: #409eff;
}

.repo-project-bar .bar-sep {
  color: rgba(0, 0, 0, 0.25);
}

.repo-table-section {
  background: #fff;
  border-radius: 8px;
  border: 1px solid #f0f0f0;
}

.empty-text {
  padding: 32px 0;
  color: rgba(0, 0, 0, 0.25);
  font-size: 13px;
}

.empty-text-inline {
  color: rgba(0, 0, 0, 0.25);
}
</style>
