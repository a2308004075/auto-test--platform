<script setup lang="ts">
/**
 * 接口关键字列表 - M5
 * 左侧复用接口分组树 + 右侧高级搜索(category/tags) + 批量操作 + 表字段调整 + 智能分页
 * 对齐原型 keyword-list.html，分组树复用 api_module（关键字按关联接口分组归类）
 */
import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getKeywords, deleteKeyword, generateKeyword } from '@/api/keyword'
import { getModules, getApis } from '@/api/apidoc'
import PageHeader from '@/components/PageHeader/index.vue'
import ProSearchCard from '@/components/ProSearchCard/index.vue'
import BatchBar from '@/components/BatchBar/index.vue'
import ColumnSettings, { type ColumnItem } from '@/components/ColumnSettings/index.vue'
import ProPagination from '@/components/ProPagination/index.vue'

const route = useRoute()
const router = useRouter()
const projectId = computed(() => Number(route.params.id))

const methodColors: Record<string, string> = { GET: '', POST: 'success', PUT: 'warning', DELETE: 'danger', PATCH: 'info' }

// ===== 列表数据 =====
const loading = ref(false)
const list = ref<any[]>([])
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const selectedRows = ref<any[]>([])

// ===== 搜索条件 =====
const search = reactive({ keyword: '', category: '' })

// ===== 分组树（复用接口分组） =====
const modules = ref<any[]>([])
const activeModuleId = ref<number>(0) // 0 = 全部
const moduleMap = computed<Record<number, any>>(() => {
  const m: Record<number, any> = {}
  modules.value.forEach((mod) => { m[mod.id] = mod })
  return m
})
const userModules = computed(() => modules.value.filter((m: any) => m.isSystem !== 1))
const moduleTree = computed(() => {
  const userGroups = modules.value.filter((m: any) => m.isSystem !== 1)
  const buildTree = (parentId: number | null): any[] =>
    userGroups
      .filter((m: any) => (m.parentId ?? null) === parentId)
      .map((m: any) => ({ ...m, children: buildTree(m.id) }))
  return [
    { id: 0, name: '全部', isSystem: 1, apiCount: pagination.total, children: [] },
    ...buildTree(null),
  ]
})
function onModuleNodeClick(data: any) {
  activeModuleId.value = data.id
  pagination.current = 1
  fetchList()
}

async function fetchModules() {
  try {
    const res: any = await getModules(projectId.value)
    modules.value = res.data || []
  } catch { modules.value = [] }
}

async function fetchList() {
  loading.value = true
  try {
    const res: any = await getKeywords(projectId.value, {
      moduleId: activeModuleId.value || undefined,
      keyword: search.keyword || undefined,
      category: search.category || undefined,
      page: pagination.current, pageSize: pagination.pageSize,
    })
    list.value = res.data?.items || []
    pagination.total = res.data?.total || 0
  } catch { list.value = [] } finally { loading.value = false }
}

function handleSearch() { pagination.current = 1; fetchList() }
function handleReset() {
  Object.assign(search, { keyword: '', category: '' })
  activeModuleId.value = 0
  handleSearch()
}

// ===== 批量操作 =====
const selectedIds = computed(() => selectedRows.value.map((r: any) => r.id))
function handleSelectionChange(rows: any[]) { selectedRows.value = rows }
function clearSelection() { selectedRows.value = [] }
function handleBatchAction(key: string) {
  if (key === 'delete') handleBatchDelete()
}
function handleBatchDelete() {
  ElMessageBox.confirm(
    `确定删除选中的 ${selectedIds.value.length} 个关键字？`,
    '批量删除',
    { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' },
  ).then(async () => {
    for (const id of selectedIds.value) {
      await deleteKeyword(projectId.value, id)
    }
    ElMessage.success('删除成功')
    clearSelection()
    fetchList()
  }).catch(() => {})
}

function handleDelete(record: any) {
  ElMessageBox.confirm(`确定删除关键字「${record.name}」？`, '确认删除', { type: 'warning' })
    .then(async () => { await deleteKeyword(projectId.value, record.id); ElMessage.success('删除成功'); fetchList() })
    .catch(() => {})
}

// ===== 从接口生成 =====
const generateVisible = ref(false)
const generateLoading = ref(false)
const apis = ref<any[]>([])
const selectedApiId = ref<number>(0)
async function openGenerate() {
  try {
    const res: any = await getApis(projectId.value, { page: 1, pageSize: 100 })
    apis.value = res.data?.items || []
  } catch { apis.value = [] }
  selectedApiId.value = 0
  generateVisible.value = true
}
async function handleGenerate() {
  if (!selectedApiId.value) { ElMessage.warning('请选择接口'); return }
  generateLoading.value = true
  try {
    await generateKeyword(projectId.value, selectedApiId.value)
    ElMessage.success('生成成功')
    generateVisible.value = false
    fetchList()
  } catch (e: any) { ElMessage.error(e?.response?.data?.message || '生成失败') } finally { generateLoading.value = false }
}

// ===== 表字段调整 =====
const colSettingsVisible = ref(false)
const defaultColumns: ColumnItem[] = [
  { key: 'name', label: '关键字名称', locked: true, visible: true },
  { key: 'method', label: '方法', locked: false, visible: true },
  { key: 'api', label: '关联接口', locked: false, visible: true },
  { key: 'module', label: '分组', locked: false, visible: false },
  { key: 'category', label: '分类', locked: false, visible: true },
  { key: 'tags', label: '标签', locked: false, visible: true },
  { key: 'refs', label: '引用', locked: false, visible: false },
  { key: 'desc', label: '描述', locked: true, visible: true },
  { key: 'createTime', label: '创建时间', locked: false, visible: false },
  { key: 'action', label: '操作', locked: true, visible: true },
]
const columns = ref<ColumnItem[]>(defaultColumns.map((c) => ({ ...c })))
function isColVisible(key: string) {
  return columns.value.find((c) => c.key === key)?.visible ?? false
}
function resetColumns() {
  columns.value = defaultColumns.map((c) => ({ ...c }))
}

// ===== 格式化 =====
function parseTags(raw?: string): string[] {
  if (!raw) return []
  try { const a = JSON.parse(raw); return Array.isArray(a) ? a : [] } catch { return [] }
}

onMounted(() => { fetchModules(); fetchList() })
</script>

<template>
  <div>
    <PageHeader title="接口关键字">
      <el-button @click="openGenerate">从接口生成</el-button>
      <el-button type="primary" @click="router.push(`/project/${projectId}/keywords/new`)">+ 新建关键字</el-button>
    </PageHeader>

    <div class="kw-layout">
      <!-- 左侧分组树（复用接口分组） -->
      <div class="module-panel">
        <div class="module-head">
          <span class="module-title">接口分组</span>
        </div>
        <div class="module-tree">
          <el-tree
            :data="moduleTree"
            node-key="id"
            :props="{ label: 'name', children: 'children' }"
            :default-expand-all="true"
            :expand-on-click-node="false"
            @node-click="onModuleNodeClick"
          >
            <template #default="{ data }">
              <div :class="['module-tree-node', { active: activeModuleId === data.id }]">
                <span class="module-name">{{ data.name }}</span>
                <span class="module-count">{{ data.apiCount ?? 0 }}</span>
              </div>
            </template>
          </el-tree>
        </div>
      </div>

      <!-- 右侧内容 -->
      <div class="kw-content">
        <ProSearchCard :loading="loading" @search="handleSearch" @reset="handleReset">
          <div class="pro-search-field">
            <span class="pro-search-label">关键字名称</span>
            <el-input v-model="search.keyword" placeholder="输入关键字名称" clearable style="width: 180px" @keyup.enter="handleSearch" />
          </div>
          <div class="pro-search-field">
            <span class="pro-search-label">分类</span>
            <el-input v-model="search.category" placeholder="输入分类" clearable style="width: 160px" @keyup.enter="handleSearch" />
          </div>
        </ProSearchCard>

        <div class="table-toolbar">
          <el-button @click="colSettingsVisible = true">表字段调整</el-button>
        </div>

        <BatchBar
          :selected-count="selectedIds.length"
          :actions="[{ key: 'delete', label: '批量删除', danger: true }]"
          @action="handleBatchAction"
          @clear="clearSelection"
        />

        <el-table :data="list" v-loading="loading" border stripe style="width: 100%" @selection-change="handleSelectionChange">
          <el-table-column type="selection" width="45" />
          <el-table-column v-if="isColVisible('name')" prop="name" label="关键字名称" width="180" show-overflow-tooltip />
          <el-table-column v-if="isColVisible('method')" label="方法" width="80">
            <template #default="{ row }">
              <el-tag :type="methodColors[row.httpMethod] || 'info'" size="small">{{ row.httpMethod || '--' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column v-if="isColVisible('api')" label="关联接口" width="260" show-overflow-tooltip>
            <template #default="{ row }">
              <span v-if="row.apiName">{{ row.apiName }}</span>
              <span v-else style="color: #c0c4cc">未关联</span>
              <code v-if="row.apiPath" style="font-size: 12px; color: #909399; margin-left: 6px">{{ row.apiPath }}</code>
            </template>
          </el-table-column>
          <el-table-column v-if="isColVisible('module')" label="分组" width="120">
            <template #default="{ row }">{{ row.moduleName || moduleMap[row.moduleId]?.name || '--' }}</template>
          </el-table-column>
          <el-table-column v-if="isColVisible('category')" label="分类" width="120">
            <template #default="{ row }">
              <el-tag v-if="row.category" size="small">{{ row.category }}</el-tag>
              <span v-else style="color: #c0c4cc">--</span>
            </template>
          </el-table-column>
          <el-table-column v-if="isColVisible('tags')" label="标签" width="200">
            <template #default="{ row }">
              <el-tag v-for="t in parseTags(row.tags)" :key="t" size="small" type="info" style="margin-right: 4px">{{ t }}</el-tag>
              <span v-if="!parseTags(row.tags).length" style="color: #c0c4cc">--</span>
            </template>
          </el-table-column>
          <el-table-column v-if="isColVisible('refs')" label="引用" width="70">
            <template #default="{ row }">{{ row.referenceCount ?? 0 }}</template>
          </el-table-column>
          <el-table-column v-if="isColVisible('desc')" prop="description" label="描述" show-overflow-tooltip />
          <el-table-column v-if="isColVisible('createTime')" label="创建时间" width="120">
            <template #default="{ row }">{{ row.createdAt?.substring(0, 10) }}</template>
          </el-table-column>
          <el-table-column v-if="isColVisible('action')" label="操作" width="140" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" link size="small" @click="router.push(`/project/${projectId}/keywords/${row.id}/edit`)">编辑</el-button>
              <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <ProPagination
          v-model:current-page="pagination.current"
          v-model:page-size="pagination.pageSize"
          :total="pagination.total"
          @change="(p: number) => { pagination.current = p; fetchList() }"
        />
      </div>
    </div>

    <!-- 从接口生成弹窗 -->
    <el-dialog v-model="generateVisible" title="从接口快速生成关键字" width="500px">
      <el-form-item label="选择接口">
        <el-select v-model="selectedApiId" placeholder="选择要生成关键字的接口" filterable style="width: 100%">
          <el-option v-for="api in apis" :key="api.id" :value="api.id" :label="`[${api.httpMethod}] ${api.name}`" />
        </el-select>
      </el-form-item>
      <template #footer>
        <el-button @click="generateVisible = false">取消</el-button>
        <el-button type="primary" :loading="generateLoading" @click="handleGenerate">生成</el-button>
      </template>
    </el-dialog>

    <!-- 表字段调整 -->
    <ColumnSettings
      v-model="colSettingsVisible"
      :columns="columns"
      @update:columns="(v: ColumnItem[]) => (columns = v)"
      @reset="resetColumns"
    />
  </div>
</template>

<style scoped>
.kw-layout {
  display: flex;
  gap: 16px;
  align-items: flex-start;
}
.module-panel {
  width: 220px;
  flex-shrink: 0;
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  padding: 12px;
}
.module-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.module-title {
  font-weight: 600;
  font-size: 14px;
  color: #303133;
}
.module-tree {
  max-height: 560px;
  overflow-y: auto;
  margin-top: 8px;
}
.module-tree :deep(.el-tree-node__content) {
  height: auto;
  padding: 2px 0;
}
.module-tree-node {
  display: flex;
  align-items: center;
  flex: 1;
  padding: 2px 4px;
  border-radius: 4px;
  font-size: 13px;
  gap: 6px;
  width: 100%;
}
.module-tree-node:hover {
  background: #f5f7fa;
}
.module-tree-node.active {
  background: #ecf5ff;
  color: #409eff;
  font-weight: 500;
}
.module-name {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.module-count {
  font-size: 12px;
  color: #909399;
  flex-shrink: 0;
}
.kw-content {
  flex: 1;
  min-width: 0;
}
.table-toolbar {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 8px;
}
</style>
