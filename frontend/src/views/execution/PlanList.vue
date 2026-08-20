<script setup lang="ts">
/**
 * 测试计划列表 - M9
 */
import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import { getPlans, deletePlan } from '@/api/plan'
import { startExecution } from '@/api/execution'

const route = useRoute()
const router = useRouter()
const projectId = computed(() => route.params.id as string)

const loading = ref(false)
const list = ref<any[]>([])
const keyword = ref('')
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })

const columns = [
  { title: '计划名称', dataIndex: 'name', width: 180 },
  { title: '描述', dataIndex: 'description', ellipsis: true },
  { title: '环境', dataIndex: 'environmentName', width: 120 },
  { title: '套件数', key: 'suiteCount', width: 80 },
  { title: '状态', dataIndex: 'isActive', width: 80 },
  { title: '创建时间', dataIndex: 'createdAt', width: 120 },
  { title: '操作', key: 'action', width: 260 },
]

async function fetchList() {
  loading.value = true
  try {
    const res: any = await getPlans(projectId.value, { keyword: keyword.value || undefined, page: pagination.current, pageSize: pagination.pageSize })
    list.value = res.data?.items || []
    pagination.total = res.data?.total || 0
  } catch { list.value = [] } finally { loading.value = false }
}

function handleSearch() { pagination.current = 1; fetchList() }

function handleEdit(record: any) {
  router.push(`/project/${projectId}/plans/${record.id}/edit`)
}

function handleDelete(record: any) {
  Modal.confirm({
    title: '确认删除',
    content: `确定删除计划「${record.name}」？`,
    onOk: async () => { await deletePlan(record.id); message.success('删除成功'); fetchList() },
  })
}

async function handleRun(record: any) {
  Modal.confirm({
    title: '触发执行',
    content: `确定执行计划「${record.name}」？`,
    onOk: async () => {
      try {
        const res: any = await startExecution(record.id)
        message.success('执行已触发')
        router.push(`/project/${projectId}/executions/${res.data.id}`)
      } catch { message.error('触发失败') }
    },
  })
}

onMounted(fetchList)
</script>

<template>
  <div>
    <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:16px">
      <h2 style="margin:0">测试计划</h2>
      <div style="display:flex;gap:8px">
        <a-input-search v-model:value="keyword" placeholder="搜索计划" style="width:220px" allow-clear @search="handleSearch" />
        <a-button type="primary" @click="router.push(`/project/${projectId}/plans/new`)">新建计划</a-button>
      </div>
    </div>

    <a-table :columns="columns" :data-source="list" :loading="loading" row-key="id" size="middle"
      :pagination="{ current: pagination.current, pageSize: pagination.pageSize, total: pagination.total, onChange: (p: number) => { pagination.current = p; fetchList() } }">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'suiteCount'">{{ record.suiteIds?.length || 0 }}</template>
        <template v-if="column.dataIndex === 'isActive'">
          <a-tag :color="record.isActive ? 'green' : 'default'">{{ record.isActive ? '启用' : '禁用' }}</a-tag>
        </template>
        <template v-if="column.dataIndex === 'createdAt'">{{ record.createdAt?.substring(0, 10) }}</template>
        <template v-if="column.key === 'action'">
          <a-space>
            <a-button type="link" size="small" @click="handleRun(record)">执行</a-button>
            <a @click="handleEdit(record)">编辑</a>
            <a style="color:#ff4d4f" @click="handleDelete(record)">删除</a>
          </a-space>
        </template>
      </template>
    </a-table>
  </div>
</template>
