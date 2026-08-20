<script setup lang="ts">
/**
 * 执行记录列表 - M9
 */
import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import { getExecutions, cancelExecution } from '@/api/execution'

const route = useRoute()
const router = useRouter()
const projectId = computed(() => route.params.id as string)

const loading = ref(false)
const list = ref<any[]>([])
const statusFilter = ref<string>('')
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })

const statusColors: Record<string, string> = {
  PENDING: 'default',
  RUNNING: 'processing',
  COMPLETED: 'success',
  FAILED: 'error',
  CANCELLED: 'warning',
}

const statusLabels: Record<string, string> = {
  PENDING: '等待中',
  RUNNING: '执行中',
  COMPLETED: '已完成',
  FAILED: '执行失败',
  CANCELLED: '已取消',
}

const columns = [
  { title: '计划', dataIndex: 'planName', width: 160 },
  { title: '状态', key: 'status', width: 100 },
  { title: '触发', dataIndex: 'triggerType', width: 80 },
  { title: '总数', dataIndex: 'totalCases', width: 70 },
  { title: '通过', dataIndex: 'passedCases', width: 70 },
  { title: '失败', dataIndex: 'failedCases', width: 70 },
  { title: '跳过', dataIndex: 'skippedCases', width: 70 },
  { title: '耗时(ms)', dataIndex: 'durationMs', width: 100 },
  { title: '创建时间', dataIndex: 'createdAt', width: 160 },
  { title: '操作', key: 'action', width: 140 },
]

async function fetchList() {
  loading.value = true
  try {
    const res: any = await getExecutions(projectId.value, {
      status: statusFilter.value || undefined,
      page: pagination.current, pageSize: pagination.pageSize,
    })
    list.value = res.data?.items || []
    pagination.total = res.data?.total || 0
  } catch { list.value = [] } finally { loading.value = false }
}

function handleFilter() { pagination.current = 1; fetchList() }

function viewDetail(record: any) {
  router.push(`/project/${projectId}/executions/${record.id}`)
}

function handleCancel(record: any) {
  Modal.confirm({
    title: '取消执行',
    content: `确定取消执行「${record.planName}」？`,
    onOk: async () => {
      try {
        await cancelExecution(record.id)
        message.success('已取消')
        fetchList()
      } catch { message.error('操作失败') }
    },
  })
}

onMounted(fetchList)
</script>

<template>
  <div>
    <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:16px">
      <h2 style="margin:0">执行记录</h2>
      <a-select v-model:value="statusFilter" placeholder="全部状态" allow-clear style="width:160px" @change="handleFilter">
        <a-select-option value="PENDING">等待中</a-select-option>
        <a-select-option value="RUNNING">执行中</a-select-option>
        <a-select-option value="COMPLETED">已完成</a-select-option>
        <a-select-option value="FAILED">执行失败</a-select-option>
        <a-select-option value="CANCELLED">已取消</a-select-option>
      </a-select>
    </div>

    <a-table :columns="columns" :data-source="list" :loading="loading" row-key="id" size="middle"
      :pagination="{ current: pagination.current, pageSize: pagination.pageSize, total: pagination.total, onChange: (p: number) => { pagination.current = p; fetchList() } }">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'status'">
          <a-tag :color="statusColors[record.status] || 'default'">{{ statusLabels[record.status] || record.status }}</a-tag>
        </template>
        <template v-if="column.dataIndex === 'createdAt'">{{ record.createdAt?.substring(0, 19).replace('T', ' ') }}</template>
        <template v-if="column.key === 'action'">
          <a-space>
            <a @click="viewDetail(record)">详情</a>
            <a v-if="record.status === 'PENDING' || record.status === 'RUNNING'"
              style="color:#ff4d4f" @click="handleCancel(record)">取消</a>
          </a-space>
        </template>
      </template>
    </a-table>
  </div>
</template>
