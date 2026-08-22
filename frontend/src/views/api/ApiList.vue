<!--
 @author HXN
 @date 2026-08-18 17:31
 @description API 接口列表视图
-->
<script setup lang="ts">
/**
 * 接口列表 - M4
 * 左侧分组面板 + 右侧高级搜索 + 批量操作 + 表字段调整 + 分页表格
 * 对齐原型 api-list.html（分组树形多层暂未做，后端 ApiModule 缺 parentId）
 */
import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getApis, deleteApi, batchDeleteApis, batchMoveApis, getModules, createModule, updateModule, deleteModule,
} from '@/api/apidoc'
import PageHeader from '@/components/PageHeader/index.vue'
import ProSearchCard from '@/components/ProSearchCard/index.vue'
import BatchBar from '@/components/BatchBar/index.vue'
import ColumnSettings, { type ColumnItem } from '@/components/ColumnSettings/index.vue'
import ProPagination from '@/components/ProPagination/index.vue'
import ApiDebugModal from '@/components/ApiDebugModal/index.vue'
import { useDict } from '@/composables/useDict'
import { usePermission } from '@/composables/usePermission'

const route = useRoute()
const router = useRouter()
const projectId = computed(() => Number(route.params.id))
const { hasPermission } = usePermission()

const methodColors: Record<string, string> = { GET: '', POST: 'success', PUT: 'warning', DELETE: 'danger', PATCH: 'info' }
const { options: httpMethodOptions } = useDict('http_method')
const { options: sourceTypeOptions } = useDict('source_type')

// ===== 列表数据 =====
const loading = ref(false)
const list = ref<any[]>([])
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const selectedRows = ref<any[]>([])

// ===== 搜索条件 =====
const search = reactive({ name: '', path: '', method: '', source: '' })

// ===== 分组 =====
const modules = ref<any[]>([])
const activeModuleId = ref<number>(0) // 0 = 全部
const moduleMap = computed<Record<number, any>>(() => {
  const m: Record<number, any> = {}
  modules.value.forEach((mod) => { m[mod.id] = mod })
  return m
})
// 用户分组（非系统），用于父分组下拉
const userModules = computed(() => modules.value.filter((m) => m.isSystem !== 1))
// 批量移动可选分组（用户分组 + 未分类系统分组）
const moveTargetModules = computed(() =>
  modules.value.filter((m) => m.isSystem !== 1 || m.name === '未分类')
)
// 分组树：全部(虚拟) + 系统分组(未分类等，排除全部) + 用户分组按 parentId 建树
const moduleTree = computed(() => {
  const userGroups = modules.value.filter((m) => m.isSystem !== 1)
  const buildTree = (parentId: number | null): any[] =>
    userGroups
      .filter((m) => (m.parentId ?? null) === parentId)
      .map((m) => ({ ...m, children: buildTree(m.id) }))
  // 系统分组（排除"全部"，用虚拟节点代替）
  const systemGroups = modules.value
    .filter((m) => m.isSystem === 1 && m.name !== '全部')
    .map((m) => ({ ...m, children: [] }))
  return [
    { id: 0, name: '全部', isSystem: 1, apiCount: pagination.total, children: [] },
    ...systemGroups,
    ...buildTree(null),
  ]
})
function onModuleNodeClick(data: any) {
  selectModule(data.id)
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
    const res: any = await getApis(projectId.value, {
      moduleId: activeModuleId.value || undefined,
      keyword: search.name || undefined,
      path: search.path || undefined,
      httpMethod: search.method || undefined,
      source: search.source || undefined,
      page: pagination.current, pageSize: pagination.pageSize,
    })
    list.value = res.data?.items || []
    pagination.total = res.data?.total || 0
  } catch { list.value = [] } finally { loading.value = false }
}

function selectModule(id: number) {
  activeModuleId.value = id === activeModuleId.value ? 0 : id
  pagination.current = 1
  fetchList()
}

function handleSearch() { pagination.current = 1; fetchList() }
function handleReset() {
  Object.assign(search, { name: '', path: '', method: '', source: '' })
  handleSearch()
}

// ===== 批量操作 =====
const selectedIds = computed(() => selectedRows.value.map((r: any) => r.id))
function handleSelectionChange(rows: any[]) { selectedRows.value = rows }

function handleBatchAction(key: string) {
  if (key === 'delete') handleBatchDelete()
  else if (key === 'move') batchMoveVisible.value = true
}
function clearSelection() { selectedRows.value = [] }

function handleBatchDelete() {
  ElMessageBox.confirm(
    `确定删除选中的 ${selectedIds.value.length} 个接口？`,
    '批量删除',
    { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' },
  ).then(async () => {
    await batchDeleteApis(projectId.value, selectedIds.value)
    ElMessage.success('删除成功')
    clearSelection()
    fetchModules(); fetchList()
  }).catch(() => {})
}

// 批量改组
const batchMoveVisible = ref(false)
const batchMoveTarget = ref<number | null>(null)
async function handleBatchMove() {
  if (!batchMoveTarget.value) { ElMessage.warning('请选择目标分组'); return }
  try {
    await batchMoveApis(projectId.value, batchMoveTarget.value, selectedIds.value)
    ElMessage.success('移动成功')
    batchMoveVisible.value = false
    batchMoveTarget.value = null
    clearSelection()
    fetchModules(); fetchList()
  } catch (e: any) { ElMessage.error(e?.response?.data?.message || '操作失败') }
}

function handleDelete(record: any) {
  ElMessageBox.confirm(`确定删除接口「${record.name}」？`, '确认删除', { type: 'warning' })
    .then(async () => { await deleteApi(projectId.value, record.id); ElMessage.success('删除成功'); fetchModules(); fetchList() })
    .catch(() => {})
}

// ===== 分组 CRUD =====
const groupModalVisible = ref(false)
const editingGroupId = ref<number>(0)
const groupForm = reactive({ name: '', servicePrefix: '', description: '', parentId: null as number | null })

function openCreateGroup(parentId?: number | null) {
  editingGroupId.value = 0
  Object.assign(groupForm, { name: '', servicePrefix: '', description: '', parentId: parentId ?? null })
  groupModalVisible.value = true
}
function openEditGroup(m: any) {
  if (m.isSystem === 1) { ElMessage.info('系统分组不可编辑'); return }
  editingGroupId.value = m.id
  Object.assign(groupForm, { name: m.name, servicePrefix: m.servicePrefix || '', description: m.description || '', parentId: m.parentId ?? null })
  groupModalVisible.value = true
}
async function handleGroupSubmit() {
  if (!groupForm.name) { ElMessage.warning('请输入分组名称'); return }
  try {
    if (editingGroupId.value) {
      await updateModule(projectId.value, editingGroupId.value, groupForm)
      ElMessage.success('更新成功')
    } else {
      await createModule(projectId.value, groupForm)
      ElMessage.success('创建成功')
    }
    groupModalVisible.value = false
    fetchModules()
  } catch (e: any) { ElMessage.error(e?.response?.data?.message || '操作失败') }
}
function handleDeleteGroup(m: any) {
  if (m.isSystem === 1) { ElMessage.info('系统分组不可删除'); return }
  ElMessageBox.confirm(`确定删除分组「${m.name}」？`, '确认删除', { type: 'warning' })
    .then(async () => { await deleteModule(projectId.value, m.id); ElMessage.success('删除成功'); fetchModules() })
    .catch(() => {})
}

// ===== 表字段调整 =====
const defaultColumns: ColumnItem[] = [
  { key: 'id', label: 'ID', locked: true, visible: true },
  { key: 'name', label: '接口名称', locked: true, visible: true },
  { key: 'path', label: '路径', locked: true, visible: true },
  { key: 'method', label: '方法', locked: true, visible: true },
  { key: 'params', label: '参数', locked: false, visible: false },
  { key: 'returnType', label: '返回', locked: false, visible: false },
  { key: 'group', label: '分组', locked: false, visible: true },
  { key: 'prefix', label: '服务前缀', locked: false, visible: false },
  { key: 'desc', label: '描述', locked: true, visible: true },
  { key: 'source', label: '来源', locked: true, visible: true },
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
function sourceLabel(t?: string) {
  if (!t) return '--'
  return sourceTypeOptions.value.find((s) => s.value === t)?.label || t
}
function formatParams(raw?: string) {
  if (!raw) return '--'
  try {
    const arr = JSON.parse(raw)
    if (Array.isArray(arr) && arr.length) return arr.map((p: any) => p.name || p.key).filter(Boolean).join(', ')
    return '--'
  } catch { return '--' }
}

// ===== 调试弹窗 =====
const debugVisible = ref(false)
const debugApiId = ref(0)
function openDebug(id: number) {
  debugApiId.value = id
  debugVisible.value = true
}

onMounted(() => { fetchModules(); fetchList() })
</script>

<template>
  <div>
    <PageHeader title="接口文档">
      <el-button v-if="hasPermission('project:api:swagger')" @click="router.push(`/project/${projectId}/apis/swagger-import`)">导入 Swagger</el-button>
      <el-button v-if="hasPermission('project:api:add')" type="primary" @click="router.push(`/project/${projectId}/apis/new`)">+ 新建接口</el-button>
    </PageHeader>

    <div class="api-layout">
      <!-- 左侧分组 -->
      <div class="module-panel">
        <div class="module-head">
          <span class="module-title">分组</span>
          <el-button v-if="hasPermission('project:api:group')" size="small" type="primary" link @click="openCreateGroup()">+ 新建</el-button>
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
                <span class="module-name">
                  {{ data.name }}
                  <span v-if="data.servicePrefix" class="module-prefix">{{ data.servicePrefix }}</span>
                </span>
                <span class="module-count">{{ data.apiCount ?? 0 }}</span>
                <span v-if="data.isSystem !== 1 && hasPermission('project:api:group')" class="module-ops" @click.stop>
                  <el-button link size="small" @click="openCreateGroup(data.id)">+ 子级</el-button>
                  <el-button link size="small" @click="openEditGroup(data)">编辑</el-button>
                  <el-button link size="small" type="danger" @click="handleDeleteGroup(data)">删除</el-button>
                </span>
              </div>
            </template>
          </el-tree>
        </div>
      </div>

      <!-- 右侧内容 -->
      <div class="api-content">
        <ProSearchCard :loading="loading" @search="handleSearch" @reset="handleReset">
          <div class="pro-search-field">
            <span class="pro-search-label">接口名称</span>
            <el-input v-model="search.name" placeholder="输入接口名称" clearable style="width: 180px" @keyup.enter="handleSearch" />
          </div>
          <div class="pro-search-field">
            <span class="pro-search-label">接口路径</span>
            <el-input v-model="search.path" placeholder="输入接口路径" clearable style="width: 180px" @keyup.enter="handleSearch" />
          </div>
          <div class="pro-search-field">
            <span class="pro-search-label">请求方法</span>
            <el-select v-model="search.method" placeholder="全部方法" clearable style="width: 140px">
              <el-option v-for="m in httpMethodOptions" :key="m.value" :value="m.value" :label="m.label" />
            </el-select>
          </div>
          <template #collapse>
            <div class="pro-search-field">
              <span class="pro-search-label">接口来源</span>
              <el-select v-model="search.source" placeholder="全部来源" clearable style="width: 160px">
                <el-option v-for="s in sourceTypeOptions" :key="s.value" :value="s.value" :label="s.label" />
              </el-select>
            </div>
          </template>
        </ProSearchCard>

        <div class="table-toolbar">
          <ColumnSettings
            :columns="columns"
            @update:columns="(v: ColumnItem[]) => (columns = v)"
            @reset="resetColumns"
          />
        </div>

        <BatchBar
          v-if="hasPermission('project:api:batch')"
          :selected-count="selectedIds.length"
          :actions="[{ key: 'move', label: '批量修改分组' }, { key: 'delete', label: '批量删除', danger: true }]"
          @action="handleBatchAction"
          @clear="clearSelection"
        />

        <el-table :data="list" v-loading="loading" border stripe style="width: 100%" @selection-change="handleSelectionChange">
          <el-table-column type="selection" width="45" />
          <el-table-column v-if="isColVisible('id')" prop="id" label="ID" width="70" />
          <el-table-column v-if="isColVisible('name')" prop="name" label="接口名称" width="180" show-overflow-tooltip />
          <el-table-column v-if="isColVisible('path')" prop="path" label="路径" width="220" show-overflow-tooltip />
          <el-table-column v-if="isColVisible('method')" label="方法" width="80">
            <template #default="{ row }">
              <el-tag :type="methodColors[row.httpMethod] || 'info'" size="small">{{ row.httpMethod }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column v-if="isColVisible('params')" label="参数" width="180" show-overflow-tooltip>
            <template #default="{ row }">{{ formatParams(row.requestParams) }}</template>
          </el-table-column>
          <el-table-column v-if="isColVisible('returnType')" label="返回" width="100">
            <template #default="{ row }">{{ row.responseBody ? 'JSON' : '--' }}</template>
          </el-table-column>
          <el-table-column v-if="isColVisible('group')" label="分组" width="120">
            <template #default="{ row }">{{ row.moduleName || moduleMap[row.moduleId]?.name || '--' }}</template>
          </el-table-column>
          <el-table-column v-if="isColVisible('prefix')" label="服务前缀" width="120">
            <template #default="{ row }">{{ moduleMap[row.moduleId]?.servicePrefix || '--' }}</template>
          </el-table-column>
          <el-table-column v-if="isColVisible('desc')" prop="description" label="描述" show-overflow-tooltip />
          <el-table-column v-if="isColVisible('source')" label="来源" width="90">
            <template #default="{ row }">{{ sourceLabel(row.sourceType) }}</template>
          </el-table-column>
          <el-table-column v-if="isColVisible('createTime')" label="创建时间" width="120">
            <template #default="{ row }">{{ row.createdAt?.substring(0, 10) }}</template>
          </el-table-column>
          <el-table-column v-if="isColVisible('action')" label="操作" width="170" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" link size="small" @click="openDebug(row.id)">调试</el-button>
              <el-button v-if="hasPermission('project:api:edit')" type="primary" link size="small" @click="router.push(`/project/${projectId}/apis/${row.id}/edit`)">编辑</el-button>
              <el-button v-if="hasPermission('project:api:delete')" type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
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

    <!-- 分组新建/编辑弹窗 -->
    <el-dialog v-model="groupModalVisible" :title="editingGroupId ? '编辑分组' : '新建分组'" width="460px">
      <el-form label-position="top">
        <el-form-item label="分组名称" required>
          <el-input v-model="groupForm.name" placeholder="如：用户管理服务" />
        </el-form-item>
        <el-form-item label="父分组">
          <el-select v-model="groupForm.parentId" placeholder="无（根分组）" clearable style="width: 100%">
            <el-option
              v-for="m in userModules.filter((x: any) => x.id !== editingGroupId)"
              :key="m.id" :value="m.id" :label="m.name"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="服务前缀">
          <el-input v-model="groupForm.servicePrefix" placeholder="可选，如 /api/users" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="groupForm.description" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="groupModalVisible = false">取消</el-button>
        <el-button type="primary" @click="handleGroupSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 批量修改分组弹窗 -->
    <el-dialog v-model="batchMoveVisible" title="批量修改分组" width="420px">
      <p style="margin: 0 0 12px; color: #606266; font-size: 13px;">
        将选中的 <b style="color: #409eff">{{ selectedIds.length }}</b> 条接口移动到：
      </p>
      <el-select v-model="batchMoveTarget" placeholder="选择目标分组" filterable style="width: 100%">
        <el-option v-for="m in moveTargetModules" :key="m.id" :value="m.id" :label="m.name" />
      </el-select>
      <template #footer>
        <el-button @click="batchMoveVisible = false">取消</el-button>
        <el-button type="primary" @click="handleBatchMove">确认移动</el-button>
      </template>
    </el-dialog>

    <!-- 调试弹窗 -->
    <ApiDebugModal v-model="debugVisible" :project-id="projectId" :api-id="debugApiId" />
  </div>
</template>

<style scoped>
.api-layout {
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
.module-prefix {
  font-size: 11px;
  color: #c0c4cc;
  margin-left: 4px;
}
.module-count {
  font-size: 12px;
  color: #909399;
  flex-shrink: 0;
}
.module-ops {
  display: none;
  gap: 0;
}
.module-tree-node:hover .module-ops {
  display: flex;
}
.api-content {
  flex: 1;
  min-width: 0;
}
.table-toolbar {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 8px;
}
</style>
