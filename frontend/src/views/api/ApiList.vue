<script setup lang="ts">
/**
 * 接口列表 - M4
 * 左侧分组树 + 右侧分页表格
 */
import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getApis, deleteApi, batchDeleteApis, getModules } from '@/api/apidoc'

const route = useRoute()
const router = useRouter()
const projectId = computed(() => Number(route.params.id))

const loading = ref(false)
const list = ref<any[]>([])
const modules = ref<any[]>([])
const selectedModuleId = ref<number>(0)
const selectedRowKeys = ref<number[]>([])
const keyword = ref('')
const httpMethodFilter = ref('')
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })

const methodColors: Record<string, string> = { GET: '', POST: 'success', PUT: 'warning', DELETE: 'danger', PATCH: 'info' }

async function fetchModules() {
  try {
    const res: any = await getModules(projectId.value)
    modules.value = res.data || []
  } catch { /* ignore */ }
}

async function fetchList() {
  loading.value = true
  try {
    const res: any = await getApis(projectId.value, {
      moduleId: selectedModuleId.value || undefined,
      keyword: keyword.value || undefined,
      httpMethod: httpMethodFilter.value || undefined,
      page: pagination.current, pageSize: pagination.pageSize,
    })
    list.value = res.data?.items || []
    pagination.total = res.data?.total || 0
  } catch { list.value = [] } finally { loading.value = false }
}

function selectModule(moduleId: number) {
  selectedModuleId.value = moduleId === selectedModuleId.value ? 0 : moduleId
  pagination.current = 1
  fetchList()
}

function handleSearch() { pagination.current = 1; fetchList() }

function handleDelete(record: any) {
  ElMessageBox.confirm(
    `确定删除接口「${record.name}」？`,
    '确认删除',
    { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
  ).then(async () => {
    await deleteApi(projectId.value, record.id)
    ElMessage.success('删除成功')
    fetchList()
  }).catch(() => {})
}

function handleBatchDelete() {
  if (!selectedRowKeys.value.length) { ElMessage.warning('请先选择接口'); return }
  ElMessageBox.confirm(
    `确定删除选中的 ${selectedRowKeys.value.length} 个接口？`,
    '批量删除',
    { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
  ).then(async () => {
    await batchDeleteApis(projectId.value, selectedRowKeys.value)
    ElMessage.success('删除成功')
    selectedRowKeys.value = []
    fetchList()
  }).catch(() => {})
}

function handleSelectionChange(rows: any[]) {
  selectedRowKeys.value = rows.map((r: any) => r.id)
}

onMounted(() => { fetchModules(); fetchList() })
</script>

<template>
  <div>
    <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:16px">
      <h2 style="margin:0">接口管理</h2>
      <div style="display:flex;gap:8px">
        <el-select v-model="httpMethodFilter" placeholder="HTTP 方法" clearable style="width:120px" @change="handleSearch">
          <el-option v-for="m in ['GET','POST','PUT','DELETE','PATCH']" :key="m" :value="m" :label="m" />
        </el-select>
        <el-input v-model="keyword" placeholder="搜索接口" clearable style="width:200px" @keyup.enter="handleSearch" />
        <el-button type="primary" @click="router.push(`/project/${projectId}/apis/new`)">新建接口</el-button>
        <el-button @click="router.push(`/project/${projectId}/apis/swagger-import`)">Swagger 导入</el-button>
        <el-button type="danger" :disabled="!selectedRowKeys.length" @click="handleBatchDelete">批量删除</el-button>
      </div>
    </div>

    <div style="display:flex;gap:16px">
      <!-- 左侧分组 -->
      <div class="module-panel">
        <div class="module-title">分组</div>
        <div
          v-for="m in modules" :key="m.id"
          :class="['module-item', { active: selectedModuleId === m.id }]"
          @click="selectModule(m.id)"
        >
          {{ m.name }}
          <span v-if="m.isSystem" style="color:#909399;font-size:11px">(系统)</span>
        </div>
      </div>

      <!-- 右侧表格 -->
      <div style="flex:1">
        <el-table
          :data="list"
          v-loading="loading"
          @selection-change="handleSelectionChange"
          size="default"
          stripe
        >
          <el-table-column type="selection" width="50" />
          <el-table-column prop="name" label="接口名称" width="200" />
          <el-table-column label="方法" width="80">
            <template #default="{ row }">
              <el-tag :type="methodColors[row.httpMethod] || 'info'" size="small">{{ row.httpMethod }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="path" label="路径" show-overflow-tooltip />
          <el-table-column prop="moduleName" label="分组" width="120" />
          <el-table-column label="创建时间" width="120">
            <template #default="{ row }">
              {{ row.createdAt?.substring(0, 10) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="180">
            <template #default="{ row }">
              <el-button type="primary" link size="small" @click="router.push(`/project/${projectId}/apis/${row.id}/debug`)">调试</el-button>
              <el-button type="primary" link size="small" @click="router.push(`/project/${projectId}/apis/${row.id}/edit`)">编辑</el-button>
              <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div style="text-align:right;margin-top:16px" v-if="pagination.total > pagination.pageSize">
          <el-pagination
            v-model:current-page="pagination.current"
            :page-size="pagination.pageSize"
            :total="pagination.total"
            layout="prev, pager, next, total"
            @current-change="fetchList"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.module-panel {
  width: 180px;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  padding: 8px;
  max-height: 500px;
  overflow-y: auto;
  background: #fff;
}
.module-title {
  font-weight: 600;
  font-size: 13px;
  padding: 4px 8px;
  margin-bottom: 4px;
  color: #606266;
}
.module-item {
  padding: 6px 8px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 13px;
  transition: background 0.2s;
}
.module-item:hover { background: #f5f7fa; }
.module-item.active { background: #ecf5ff; color: #409eff; font-weight: 500; }
</style>
