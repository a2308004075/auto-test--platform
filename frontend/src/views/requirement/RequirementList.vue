<!--
 @author HXN
 @date 2026-08-30
 @description 需求文档视图（左右分栏：版本列表 + 需求条目列表）
-->
<script setup lang="ts">
/**
 * 需求文档 - 版本管理与需求条目管理
 * 左侧版本列表（可增删改），右侧选中版本的需求条目列表（可增删改）
 */
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import {
  getRequirementVersions,
  createRequirementVersion,
  updateRequirementVersion,
  deleteRequirementVersion,
  getRequirementItems,
  createRequirementItem,
  updateRequirementItem,
  deleteRequirementItem,
} from '@/api/requirement'
import type { RequirementVersion, RequirementItem } from '@/api/requirement'
import { useProjectStore } from '@/stores/modules/project'
import { usePermission } from '@/composables/usePermission'

const route = useRoute()
const { hasPermission } = usePermission()
const projectStore = useProjectStore()
const projectId = computed(() => Number(route.params.id))

// ===== 字典映射 =====
const versionStatusMap: Record<string, { label: string; type: string }> = {
  PLANNING: { label: '规划中', type: 'info' },
  IN_PROGRESS: { label: '进行中', type: 'warning' },
  COMPLETED: { label: '已完成', type: 'success' },
}

const reqTypeMap: Record<string, { label: string; type: string }> = {
  FEATURE: { label: '功能', type: '' },
  IMPROVEMENT: { label: '优化', type: 'success' },
  BUG: { label: 'Bug', type: 'danger' },
}

const priorityMap: Record<string, { label: string; type: string }> = {
  HIGH: { label: '高', type: 'danger' },
  MEDIUM: { label: '中', type: 'warning' },
  LOW: { label: '低', type: 'info' },
}

const itemStatusMap: Record<string, { label: string; type: string }> = {
  PENDING: { label: '待处理', type: 'info' },
  IN_PROGRESS: { label: '进行中', type: 'warning' },
  COMPLETED: { label: '已完成', type: 'success' },
}

// ===== 版本列表 =====
const versionLoading = ref(false)
const versions = ref<RequirementVersion[]>([])
const selectedVersionId = ref<number | null>(null)

const selectedVersion = computed(() =>
  versions.value.find((v) => v.id === selectedVersionId.value) || null,
)

async function fetchVersions() {
  versionLoading.value = true
  try {
    const res: any = await getRequirementVersions(projectId.value)
    versions.value = res.data || []
    // 如果之前选中的版本已不存在，自动选中第一个
    if (selectedVersionId.value && !versions.value.find((v) => v.id === selectedVersionId.value)) {
      selectedVersionId.value = versions.value.length > 0 ? versions.value[0].id : null
    }
    if (!selectedVersionId.value && versions.value.length > 0) {
      selectedVersionId.value = versions.value[0].id
    }
  } catch {
    versions.value = []
  } finally {
    versionLoading.value = false
  }
}

// ===== 版本新建/编辑弹窗 =====
const versionModalVisible = ref(false)
const versionIsEdit = ref(false)
const versionEditingId = ref<number | null>(null)
const versionFormRef = ref<FormInstance>()
const versionForm = reactive({
  versionName: '',
  description: '',
  status: 'PLANNING',
  startDate: '',
  endDate: '',
})
const versionRules = reactive<FormRules>({
  versionName: [
    { required: true, message: '请输入版本号', trigger: 'blur' },
    { max: 100, message: '版本号长度不能超过 100 个字符', trigger: 'blur' },
  ],
})

function openCreateVersion() {
  versionIsEdit.value = false
  versionEditingId.value = null
  Object.assign(versionForm, { versionName: '', description: '', status: 'PLANNING', startDate: '', endDate: '' })
  versionModalVisible.value = true
}

function openEditVersion(version: RequirementVersion) {
  versionIsEdit.value = true
  versionEditingId.value = version.id
  Object.assign(versionForm, {
    versionName: version.versionName,
    description: version.description || '',
    status: version.status,
    startDate: version.startDate || '',
    endDate: version.endDate || '',
  })
  versionModalVisible.value = true
}

function handleVersionSubmit() {
  versionFormRef.value?.validate(async (valid) => {
    if (!valid) return
    try {
      const data = {
        versionName: versionForm.versionName,
        description: versionForm.description || undefined,
        status: versionForm.status,
        startDate: versionForm.startDate || undefined,
        endDate: versionForm.endDate || undefined,
      }
      if (versionIsEdit.value && versionEditingId.value) {
        await updateRequirementVersion(versionEditingId.value, data)
        ElMessage.success('保存成功')
      } else {
        await createRequirementVersion(projectId.value, data)
        ElMessage.success('创建成功')
      }
      versionModalVisible.value = false
      await fetchVersions()
    } catch (e: any) {
      ElMessage.error(e?.response?.data?.message || '保存失败')
    }
  })
}

function handleVersionDialogClosed() {
  versionFormRef.value?.resetFields()
}

function handleDeleteVersion(version: RequirementVersion) {
  const itemCount = version.itemCount || 0
  const msg = itemCount > 0
    ? `确定删除版本「${version.versionName}」？该版本下有 ${itemCount} 个需求条目，将一并删除且不可恢复。`
    : `确定删除版本「${version.versionName}」？此操作不可恢复。`
  ElMessageBox.confirm(msg, '确认删除', {
    type: 'warning',
    confirmButtonText: '确认删除',
    cancelButtonText: '取消',
  })
    .then(async () => {
      await deleteRequirementVersion(version.id)
      ElMessage.success('删除成功')
      if (selectedVersionId.value === version.id) {
        selectedVersionId.value = null
      }
      await fetchVersions()
      items.value = []
    })
    .catch(() => {})
}

// ===== 需求条目列表 =====
const itemsLoading = ref(false)
const items = ref<RequirementItem[]>([])

async function fetchItems() {
  if (!selectedVersionId.value) {
    items.value = []
    return
  }
  itemsLoading.value = true
  try {
    const res: any = await getRequirementItems(selectedVersionId.value)
    items.value = res.data || []
  } catch {
    items.value = []
  } finally {
    itemsLoading.value = false
  }
}

watch(selectedVersionId, () => {
  fetchItems()
})

// ===== 需求条目新建/编辑弹窗 =====
const itemModalVisible = ref(false)
const itemIsEdit = ref(false)
const itemEditingId = ref<number | null>(null)
const itemFormRef = ref<FormInstance>()
const itemForm = reactive({
  title: '',
  description: '',
  reqType: 'FEATURE',
  priority: 'MEDIUM',
  status: 'PENDING',
  assignee: '',
  deadline: '',
})
const itemRules = reactive<FormRules>({
  title: [
    { required: true, message: '请输入需求标题', trigger: 'blur' },
    { max: 200, message: '需求标题长度不能超过 200 个字符', trigger: 'blur' },
  ],
})

function openCreateItem() {
  if (!selectedVersionId.value) return
  itemIsEdit.value = false
  itemEditingId.value = null
  Object.assign(itemForm, {
    title: '',
    description: '',
    reqType: 'FEATURE',
    priority: 'MEDIUM',
    status: 'PENDING',
    assignee: '',
    deadline: '',
  })
  itemModalVisible.value = true
}

function openEditItem(item: RequirementItem) {
  itemIsEdit.value = true
  itemEditingId.value = item.id
  Object.assign(itemForm, {
    title: item.title,
    description: item.description || '',
    reqType: item.reqType,
    priority: item.priority,
    status: item.status,
    assignee: item.assignee || '',
    deadline: item.deadline || '',
  })
  itemModalVisible.value = true
}

function handleItemSubmit() {
  itemFormRef.value?.validate(async (valid) => {
    if (!valid) return
    try {
      const data = {
        title: itemForm.title,
        description: itemForm.description || undefined,
        reqType: itemForm.reqType,
        priority: itemForm.priority,
        status: itemForm.status,
        assignee: itemForm.assignee || undefined,
        deadline: itemForm.deadline || undefined,
      }
      if (itemIsEdit.value && itemEditingId.value) {
        await updateRequirementItem(itemEditingId.value, data)
        ElMessage.success('保存成功')
      } else if (selectedVersionId.value) {
        await createRequirementItem(selectedVersionId.value, data)
        ElMessage.success('创建成功')
      }
      itemModalVisible.value = false
      await fetchItems()
      // 刷新版本列表以更新条目计数
      fetchVersions()
    } catch (e: any) {
      ElMessage.error(e?.response?.data?.message || '保存失败')
    }
  })
}

function handleItemDialogClosed() {
  itemFormRef.value?.resetFields()
}

function handleDeleteItem(item: RequirementItem) {
  ElMessageBox.confirm(
    `确定删除需求「${item.title}」？此操作不可恢复。`,
    '确认删除',
    { type: 'warning', confirmButtonText: '确认删除', cancelButtonText: '取消' },
  )
    .then(async () => {
      await deleteRequirementItem(item.id)
      ElMessage.success('删除成功')
      await fetchItems()
      fetchVersions()
    })
    .catch(() => {})
}

// ===== 格式化 =====
function formatDate(value: string | null | undefined) {
  return value ? value.substring(0, 10) : '-'
}

onMounted(fetchVersions)
</script>

<template>
  <div>
    <!-- 页面头部 -->
    <div class="page-header">
      <h2>需求文档</h2>
    </div>

    <!-- 项目上下文栏 -->
    <div class="req-project-bar">
      <span>&#x1F4CC;</span>
      <span>当前项目：<span class="project-name">{{ projectStore.currentProjectName }}</span></span>
      <span class="bar-sep">|</span>
      <span>管理需求版本与需求条目，跟踪需求进度</span>
    </div>

    <!-- 左右分栏主体 -->
    <div class="req-main">
      <!-- 左侧：版本列表 -->
      <div class="req-left-panel">
        <div class="panel-header">
          <span class="panel-title">版本列表</span>
          <el-button v-if="hasPermission('project:req:version:create')" type="primary" size="small" @click="openCreateVersion">
            + 新建版本
          </el-button>
        </div>

        <div v-loading="versionLoading" class="version-list">
          <div
            v-for="version in versions"
            :key="version.id"
            class="version-card"
            :class="{ active: selectedVersionId === version.id }"
            @click="selectedVersionId = version.id"
          >
            <div class="version-card-top">
              <span class="version-name">{{ version.versionName }}</span>
              <el-tag :type="versionStatusMap[version.status]?.type || 'info'" size="small">
                {{ versionStatusMap[version.status]?.label || version.status }}
              </el-tag>
            </div>
            <div class="version-card-meta">
              <span>{{ version.itemCount || 0 }} 个需求</span>
              <span v-if="version.startDate || version.endDate">
                {{ formatDate(version.startDate) }} ~ {{ formatDate(version.endDate) }}
              </span>
            </div>
            <div v-if="version.description" class="version-card-desc">
              {{ version.description }}
            </div>
            <div class="version-card-actions" @click.stop>
              <el-button
                v-if="hasPermission('project:req:version:edit')"
                type="primary"
                link
                size="small"
                @click="openEditVersion(version)"
              >
                编辑
              </el-button>
              <el-button
                v-if="hasPermission('project:req:version:delete')"
                type="danger"
                link
                size="small"
                @click="handleDeleteVersion(version)"
              >
                删除
              </el-button>
            </div>
          </div>

          <div v-if="!versionLoading && versions.length === 0" class="empty-text">
            暂无版本，点击「新建版本」开始
          </div>
        </div>
      </div>

      <!-- 右侧：需求条目列表 -->
      <div class="req-right-panel">
        <template v-if="selectedVersion">
          <div class="panel-header">
            <div class="panel-header-left">
              <span class="panel-title">{{ selectedVersion.versionName }} - 需求条目</span>
              <el-tag :type="versionStatusMap[selectedVersion.status]?.type || 'info'" size="small">
                {{ versionStatusMap[selectedVersion.status]?.label || selectedVersion.status }}
              </el-tag>
            </div>
            <el-button
              v-if="hasPermission('project:req:item:create')"
              type="primary"
              size="small"
              @click="openCreateItem"
            >
              + 新建需求
            </el-button>
          </div>

          <el-table v-loading="itemsLoading" :data="items" row-key="id" style="width: 100%">
            <el-table-column prop="title" label="标题" min-width="180">
              <template #default="{ row }">
                <strong>{{ row.title }}</strong>
              </template>
            </el-table-column>
            <el-table-column label="类型" width="80" align="center">
              <template #default="{ row }">
                <el-tag :type="reqTypeMap[row.reqType]?.type || ''" size="small">
                  {{ reqTypeMap[row.reqType]?.label || row.reqType }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="优先级" width="70" align="center">
              <template #default="{ row }">
                <el-tag :type="priorityMap[row.priority]?.type || 'info'" size="small" effect="plain">
                  {{ priorityMap[row.priority]?.label || row.priority }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="80" align="center">
              <template #default="{ row }">
                <el-tag :type="itemStatusMap[row.status]?.type || 'info'" size="small">
                  {{ itemStatusMap[row.status]?.label || row.status }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="负责人" width="90">
              <template #default="{ row }">
                {{ row.assignee || '-' }}
              </template>
            </el-table-column>
            <el-table-column label="截止日期" width="110">
              <template #default="{ row }">
                {{ formatDate(row.deadline) }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="130" fixed="right">
              <template #default="{ row }">
                <el-button
                  v-if="hasPermission('project:req:item:edit')"
                  type="primary"
                  link
                  size="small"
                  @click="openEditItem(row)"
                >
                  编辑
                </el-button>
                <el-button
                  v-if="hasPermission('project:req:item:delete')"
                  type="danger"
                  link
                  size="small"
                  @click="handleDeleteItem(row)"
                >
                  删除
                </el-button>
              </template>
            </el-table-column>
            <template #empty>
              <div class="empty-text">暂无需求条目，点击「新建需求」开始</div>
            </template>
          </el-table>
        </template>

        <template v-else>
          <div class="empty-state">
            <p>请从左侧选择一个版本，查看和管理需求条目</p>
          </div>
        </template>
      </div>
    </div>

    <!-- 新建/编辑版本弹窗 -->
    <el-dialog
      v-model="versionModalVisible"
      :title="versionIsEdit ? '编辑版本' : '新建版本'"
      width="520px"
      @closed="handleVersionDialogClosed"
    >
      <el-form ref="versionFormRef" :model="versionForm" :rules="versionRules" label-position="top">
        <el-form-item label="版本号" prop="versionName">
          <el-input v-model="versionForm.versionName" placeholder="如 V1.0、Release 2.0" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="versionForm.description" type="textarea" :rows="2" placeholder="版本描述" maxlength="500" show-word-limit />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="versionForm.status" placeholder="选择状态" style="width: 100%">
            <el-option label="规划中" value="PLANNING" />
            <el-option label="进行中" value="IN_PROGRESS" />
            <el-option label="已完成" value="COMPLETED" />
          </el-select>
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="计划开始日期" prop="startDate">
              <el-date-picker
                v-model="versionForm.startDate"
                type="date"
                placeholder="开始日期"
                format="YYYY-MM-DD"
                value-format="YYYY-MM-DD"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="计划结束日期" prop="endDate">
              <el-date-picker
                v-model="versionForm.endDate"
                type="date"
                placeholder="结束日期"
                format="YYYY-MM-DD"
                value-format="YYYY-MM-DD"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="versionModalVisible = false">取消</el-button>
        <el-button type="primary" @click="handleVersionSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 新建/编辑需求条目弹窗 -->
    <el-dialog
      v-model="itemModalVisible"
      :title="itemIsEdit ? '编辑需求' : '新建需求'"
      width="600px"
      @closed="handleItemDialogClosed"
    >
      <el-form ref="itemFormRef" :model="itemForm" :rules="itemRules" label-position="top">
        <el-form-item label="标题" prop="title">
          <el-input v-model="itemForm.title" placeholder="需求标题" maxlength="200" show-word-limit />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="itemForm.description" type="textarea" :rows="3" placeholder="需求详细描述" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="需求类型" prop="reqType">
              <el-select v-model="itemForm.reqType" placeholder="类型" style="width: 100%">
                <el-option label="功能" value="FEATURE" />
                <el-option label="优化" value="IMPROVEMENT" />
                <el-option label="Bug" value="BUG" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="优先级" prop="priority">
              <el-select v-model="itemForm.priority" placeholder="优先级" style="width: 100%">
                <el-option label="高" value="HIGH" />
                <el-option label="中" value="MEDIUM" />
                <el-option label="低" value="LOW" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="状态" prop="status">
              <el-select v-model="itemForm.status" placeholder="状态" style="width: 100%">
                <el-option label="待处理" value="PENDING" />
                <el-option label="进行中" value="IN_PROGRESS" />
                <el-option label="已完成" value="COMPLETED" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="负责人" prop="assignee">
              <el-input v-model="itemForm.assignee" placeholder="负责人" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="截止日期" prop="deadline">
              <el-date-picker
                v-model="itemForm.deadline"
                type="date"
                placeholder="截止日期"
                format="YYYY-MM-DD"
                value-format="YYYY-MM-DD"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="itemModalVisible = false">取消</el-button>
        <el-button type="primary" @click="handleItemSubmit">确定</el-button>
      </template>
    </el-dialog>
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

.req-project-bar {
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

.req-project-bar .project-name {
  font-weight: 600;
  color: #409eff;
}

.req-project-bar .bar-sep {
  color: rgba(0, 0, 0, 0.25);
}

/* ===== 左右分栏布局 ===== */
.req-main {
  display: flex;
  gap: 16px;
  min-height: calc(100vh - 220px);
}

.req-left-panel {
  width: 300px;
  flex-shrink: 0;
  background: #fff;
  border-radius: 8px;
  border: 1px solid #f0f0f0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.req-right-panel {
  flex: 1;
  background: #fff;
  border-radius: 8px;
  border: 1px solid #f0f0f0;
  overflow: hidden;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #f0f0f0;
}

.panel-header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.panel-title {
  font-size: 14px;
  font-weight: 600;
}

/* ===== 版本列表 ===== */
.version-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.version-card {
  padding: 12px;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  margin-bottom: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.version-card:hover {
  border-color: #c6e2ff;
  background: #f5f9ff;
}

.version-card.active {
  border-color: #409eff;
  background: #ecf5ff;
}

.version-card-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}

.version-name {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.version-card-meta {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #909399;
  margin-bottom: 4px;
}

.version-card-desc {
  font-size: 12px;
  color: #909399;
  margin-bottom: 6px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.version-card-actions {
  display: flex;
  gap: 8px;
  margin-top: 4px;
}

/* ===== 空状态 ===== */
.empty-text {
  padding: 32px 0;
  color: rgba(0, 0, 0, 0.25);
  font-size: 13px;
  text-align: center;
}

.empty-state {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  min-height: 300px;
  color: rgba(0, 0, 0, 0.25);
  font-size: 14px;
}
</style>
