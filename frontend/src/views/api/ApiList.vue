<script setup lang="ts">
/**
 * 接口列表 - M4
 * 左侧分组树 + 右侧分页表格
 */
import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import { getApis, deleteApi, batchDeleteApis, getModules } from '@/api/apidoc'

const route = useRoute()
const router = useRouter()
const projectId = computed(() => route.params.id as string)

const loading = ref(false)
const list = ref<any[]>([])
const modules = ref<any[]>([])
const selectedModuleId = ref<string>('')
const selectedRowKeys = ref<string[]>([])
const keyword = ref('')
const httpMethodFilter = ref('')
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })

const columns = [
  { title: '接口名称', dataIndex: 'name', width: 200 },
  { title: '方法', dataIndex: 'httpMethod', width: 80 },
  { title: '路径', dataIndex: 'path', ellipsis: true },
  { title: '分组', dataIndex: 'moduleName', width: 120 },
  { title: '创建时间', dataIndex: 'createdAt', width: 120 },
  { title: '操作', key: 'action', width: 180 },
]

const methodColors: Record<string, string> = { GET: 'blue', POST: 'green', PUT: 'orange', DELETE: 'red', PATCH: 'purple' }

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

function selectModule(moduleId: string) {
  selectedModuleId.value = moduleId === selectedModuleId.value ? '' : moduleId
  pagination.current = 1
  fetchList()
}

function handleSearch() { pagination.current = 1; fetchList() }

function handleDelete(record: any) {
  Modal.confirm({
    title: '确认删除', content: `确定删除接口「${record.name}」？`,
    onOk: async () => { await deleteApi(projectId.value, record.id); message.success('删除成功'); fetchList() },
  })
}

async function handleBatchDelete() {
  if (!selectedRowKeys.value.length) { message.warning('请先选择接口'); return }
  Modal.confirm({
    title: '批量删除', content: `确定删除选中的 ${selectedRowKeys.value.length} 个接口？`,
    onOk: async () => {
      await batchDeleteApis(projectId.value, selectedRowKeys.value)
      message.success('删除成功'); selectedRowKeys.value = []; fetchList()
    },
  })
}

onMounted(() => { fetchModules(); fetchList() })
</script>

<template>
  <div>
    <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:16px">
      <h2 style="margin:0">接口管理</h2>
      <div style="display:flex;gap:8px">
        <a-select v-model:value="httpMethodFilter" placeholder="HTTP 方法" allow-clear style="width:120px" @change="handleSearch">
          <a-select-option v-for="m in ['GET','POST','PUT','DELETE','PATCH']" :key="m" :value="m">{{ m }}</a-select-option>
        </a-select>
        <a-input-search v-model:value="keyword" placeholder="搜索接口" style="width:200px" allow-clear @search="handleSearch" />
        <a-button type="primary" @click="router.push(`/project/${projectId}/apis/new`)">新建接口</a-button>
        <a-button @click="router.push(`/project/${projectId}/apis/swagger-import`)">Swagger 导入</a-button>
        <a-button danger :disabled="!selectedRowKeys.length" @click="handleBatchDelete">批量删除</a-button>
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
          <span v-if="m.isSystem" style="color:#999;font-size:11px">(系统)</span>
        </div>
      </div>

      <!-- 右侧表格 -->
      <div style="flex:1">
        <a-table
          :columns="columns" :data-source="list" :loading="loading" row-key="id" size="middle"
          :row-selection="{ selectedRowKeys, onChange: (keys: string[]) => selectedRowKeys = keys }"
          :pagination="{ current: pagination.current, pageSize: pagination.pageSize, total: pagination.total, onChange: (p: number) => { pagination.current = p; fetchList() } }"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.dataIndex === 'httpMethod'">
              <a-tag :color="methodColors[record.httpMethod] || 'default'">{{ record.httpMethod }}</a-tag>
            </template>
            <template v-if="column.dataIndex === 'createdAt'">
              {{ record.createdAt?.substring(0, 10) }}
            </template>
            <template v-if="column.key === 'action'">
              <a-space>
                <a @click="router.push(`/project/${projectId}/apis/${record.id}/debug`)">调试</a>
                <a @click="router.push(`/project/${projectId}/apis/${record.id}/edit`)">编辑</a>
                <a style="color:#ff4d4f" @click="handleDelete(record)">删除</a>
              </a-space>
            </template>
          </template>
        </a-table>
      </div>
    </div>
  </div>
</template>

<style scoped>
.module-panel {
  width: 180px;
  border: 1px solid #f0f0f0;
  border-radius: 4px;
  padding: 8px;
  max-height: 500px;
  overflow-y: auto;
}
.module-title {
  font-weight: 600;
  font-size: 13px;
  padding: 4px 8px;
  margin-bottom: 4px;
  color: #666;
}
.module-item {
  padding: 6px 8px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 13px;
  transition: background 0.2s;
}
.module-item:hover { background: #f5f5f5; }
.module-item.active { background: #e6f7ff; color: #1890ff; font-weight: 500; }
</style>
