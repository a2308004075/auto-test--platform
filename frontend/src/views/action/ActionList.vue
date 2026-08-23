<!--
 @author HXN
 @date 2026-08-20 15:34
 @description Action 关键字列表视图
-->
<script setup lang="ts">
/**
 * Action 关键字列表 - M7
 * 高级搜索(name/description) + 批量操作 + 表字段调整 + 智能分页
 * 对齐原型 action-list.html
 */
import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getActions, createAction, deleteAction } from '@/api/action'
import PageHeader from '@/components/PageHeader/index.vue'
import ProSearchCard from '@/components/ProSearchCard/index.vue'
import BatchBar from '@/components/BatchBar/index.vue'
import ColumnSettings, { type ColumnItem } from '@/components/ColumnSettings/index.vue'
import ProPagination from '@/components/ProPagination/index.vue'
import { usePermission } from '@/composables/usePermission'

const route = useRoute()
const router = useRouter()
const { hasPermission } = usePermission()
const projectId = computed(() => Number(route.params.id))

// ===== 列表数据 =====
const loading = ref(false)
const list = ref<any[]>([])
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const selectedRows = ref<any[]>([])

// ===== 搜索条件 =====
const search = reactive({ name: '', desc: '' })

async function fetchList() {
  loading.value = true
  try {
    const res: any = await getActions(projectId.value, {
      keyword: search.name || undefined,
      page: pagination.current,
      pageSize: pagination.pageSize,
    })
    let items = res.data?.items || []
    // 前端二次过滤描述（后端 keyword 仅搜索 name+description，这里补充精确描述过滤）
    if (search.desc) {
      items = items.filter((item: any) =>
        (item.description || '').toLowerCase().includes(search.desc.toLowerCase()),
      )
    }
    list.value = items
    pagination.total = res.data?.total || 0
  } catch {
    list.value = []
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.current = 1
  fetchList()
}

function handleReset() {
  Object.assign(search, { name: '', desc: '' })
  handleSearch()
}

// ===== 新建 =====
async function handleCreate() {
  try {
    const res: any = await createAction(projectId.value, {
      projectId: projectId.value,
      name: `新建 Action ${Date.now() % 10000}`,
      description: '',
      nodes: [],
    })
    ElMessage.success('创建成功')
    router.push(`/project/${projectId.value}/actions/${res.data.id}/edit`)
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '创建失败')
  }
}

// ===== 删除 =====
function handleDelete(record: any) {
  ElMessageBox.confirm(
    `确定删除 Action 关键字「${record.name}」？删除后将无法恢复。`,
    '删除 Action关键字',
    { confirmButtonText: '确认删除', cancelButtonText: '取消', type: 'warning' },
  )
    .then(async () => {
      await deleteAction(projectId.value, record.id)
      ElMessage.success('已删除 1 条 Action关键字')
      fetchList()
    })
    .catch(() => {})
}

// ===== 批量操作 =====
const selectedIds = computed(() => selectedRows.value.map((r: any) => r.id))

function handleSelectionChange(rows: any[]) {
  selectedRows.value = rows
}

function clearSelection() {
  selectedRows.value = []
}

function handleBatchAction(key: string) {
  if (key === 'delete') handleBatchDelete()
}

function handleBatchDelete() {
  const count = selectedIds.value.length
  const preview = selectedRows.value
    .map((r: any) => r.name)
    .join('、')
  ElMessageBox.confirm(
    `确定删除已选中的 ${count} 条 Action关键字吗？\n${preview}\n删除后将无法恢复，请谨慎操作。`,
    '批量删除 Action关键字',
    { confirmButtonText: '确认删除', cancelButtonText: '取消', type: 'warning' },
  )
    .then(async () => {
      for (const id of selectedIds.value) {
        await deleteAction(projectId.value, id)
      }
      ElMessage.success(`已删除 ${count} 条 Action关键字`)
      clearSelection()
      fetchList()
    })
    .catch(() => {})
}

// ===== 表字段调整 =====
const defaultColumns: ColumnItem[] = [
  { key: 'name', label: '名称', locked: true, visible: true },
  { key: 'desc', label: '描述', locked: true, visible: true },
  { key: 'refs', label: '引用次数', locked: false, visible: false },
  { key: 'createTime', label: '创建时间', locked: false, visible: true },
  { key: 'updateTime', label: '更新时间', locked: false, visible: false },
  { key: 'action', label: '操作', locked: true, visible: true },
]
const columns = ref<ColumnItem[]>(defaultColumns.map((c) => ({ ...c })))

function isColVisible(key: string) {
  return columns.value.find((c) => c.key === key)?.visible ?? false
}

function resetColumns() {
  columns.value = defaultColumns.map((c) => ({ ...c }))
}

onMounted(fetchList)
</script>

<template>
  <div>
    <PageHeader title="Action关键字">
      <el-button
        v-if="hasPermission('project:action:add')"
        type="primary"
        @click="handleCreate"
      >
        + 新建 Action关键字
      </el-button>
    </PageHeader>

    <ProSearchCard :loading="loading" @search="handleSearch" @reset="handleReset">
      <div class="pro-search-field">
        <span class="pro-search-label">名称</span>
        <el-input
          v-model="search.name"
          placeholder="输入名称"
          clearable
          style="width: 180px"
          @keyup.enter="handleSearch"
        />
      </div>
      <div class="pro-search-field">
        <span class="pro-search-label">描述</span>
        <el-input
          v-model="search.desc"
          placeholder="输入描述"
          clearable
          style="width: 180px"
          @keyup.enter="handleSearch"
        />
      </div>
    </ProSearchCard>

    <div class="table-toolbar">
      <ColumnSettings
        :columns="columns"
        @update:columns="(v: ColumnItem[]) => (columns = v)"
        @reset="resetColumns"
      />
    </div>

    <BatchBar
      v-if="hasPermission('project:action:delete')"
      :selected-count="selectedIds.length"
      :actions="[{ key: 'delete', label: '批量删除', danger: true }]"
      @action="handleBatchAction"
      @clear="clearSelection"
    />

    <el-table
      :data="list"
      v-loading="loading"
      border
      stripe
      style="width: 100%"
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="45" />
      <el-table-column
        v-if="isColVisible('name')"
        prop="name"
        label="名称"
        width="220"
        show-overflow-tooltip
      />
      <el-table-column
        v-if="isColVisible('desc')"
        prop="description"
        label="描述"
        show-overflow-tooltip
      >
        <template #default="{ row }">
          <span v-if="row.description">{{ row.description }}</span>
          <span v-else style="color: #c0c4cc">--</span>
        </template>
      </el-table-column>
      <el-table-column
        v-if="isColVisible('refs')"
        label="引用次数"
        width="100"
        align="center"
      >
        <template #default="{ row }">{{ row.referenceCount ?? 0 }}</template>
      </el-table-column>
      <el-table-column
        v-if="isColVisible('createTime')"
        label="创建时间"
        width="160"
      >
        <template #default="{ row }">{{
          row.createdAt?.substring(0, 16)?.replace('T', ' ')
        }}</template>
      </el-table-column>
      <el-table-column
        v-if="isColVisible('updateTime')"
        label="更新时间"
        width="160"
      >
        <template #default="{ row }">{{
          row.updatedAt?.substring(0, 16)?.replace('T', ' ')
        }}</template>
      </el-table-column>
      <el-table-column
        v-if="isColVisible('action')"
        label="操作"
        width="180"
        fixed="right"
      >
        <template #default="{ row }">
          <el-button
            v-if="hasPermission('project:action:edit')"
            type="primary"
            link
            size="small"
            @click="router.push(`/project/${projectId}/actions/${row.id}/edit`)"
          >
            编辑
          </el-button>
          <el-button
            v-if="hasPermission('project:action:debug')"
            type="primary"
            link
            size="small"
            @click="router.push(`/project/${projectId}/actions/${row.id}/debug`)"
          >
            调试
          </el-button>
          <el-button
            v-if="hasPermission('project:action:delete')"
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
        <div style="padding: 48px 20px; color: #909399">
          <div style="font-size: 32px; margin-bottom: 8px; opacity: 0.4">⚡</div>
          <div>暂无匹配的 Action关键字数据</div>
        </div>
      </template>
    </el-table>

    <ProPagination
      v-model:current-page="pagination.current"
      v-model:page-size="pagination.pageSize"
      :total="pagination.total"
      @change="(p: number) => { pagination.current = p; fetchList() }"
    />
  </div>
</template>

<style scoped>
.table-toolbar {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 8px;
}
</style>
