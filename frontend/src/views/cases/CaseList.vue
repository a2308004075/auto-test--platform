<script setup lang="ts">
/**
 * 测试用例列表 - M8
 */
import { ref, reactive, onMounted, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import { getCases, deleteCase, toggleCaseStatus } from '@/api/case'
import { getSuites } from '@/api/suite'

const route = useRoute()
const router = useRouter()
const projectId = computed(() => route.params.id as string)
const suiteId = computed(() => (route.query.suiteId as string) || '')

const loading = ref(false)
const list = ref<any[]>([])
const suites = ref<any[]>([])
const keyword = ref('')
const selectedSuiteId = ref<string>(suiteId.value)
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })

const columns = [
  { title: '用例名称', dataIndex: 'name', width: 220 },
  { title: '优先级', dataIndex: 'priority', width: 90 },
  { title: '超时(秒)', dataIndex: 'timeout', width: 100 },
  { title: '状态', key: 'status', width: 90 },
  { title: '创建时间', dataIndex: 'createdAt', width: 120 },
  { title: '操作', key: 'action', width: 200 },
]

const priorityColors: Record<string, string> = { P0: 'red', P1: 'orange', P2: 'blue', P3: 'default' }

async function fetchSuites() {
  try {
    const res: any = await getSuites(projectId.value, { pageSize: 200 })
    suites.value = res.data?.items || []
  } catch { suites.value = [] }
}

async function fetchList() {
  loading.value = true
  try {
    const res: any = await getCases(projectId.value, {
      suiteId: selectedSuiteId.value || undefined,
      keyword: keyword.value || undefined,
      page: pagination.current, pageSize: pagination.pageSize,
    })
    list.value = res.data?.items || []
    pagination.total = res.data?.total || 0
  } catch { list.value = [] } finally { loading.value = false }
}

function handleSearch() { pagination.current = 1; fetchList() }

function handleSuiteChange() {
  router.replace({ query: { suiteId: selectedSuiteId.value || undefined } })
  pagination.current = 1; fetchList()
}

function openCreate() {
  router.push(`/project/${projectId}/cases/new?suiteId=${selectedSuiteId.value}`)
}

function handleEdit(record: any) {
  router.push(`/project/${projectId}/cases/${record.id}/edit?suiteId=${selectedSuiteId.value}`)
}

async function handleToggleStatus(record: any) {
  try {
    await toggleCaseStatus(projectId.value, record.id)
    message.success(record.isActive ? '已禁用' : '已启用')
    fetchList()
  } catch { message.error('操作失败') }
}

function handleDelete(record: any) {
  Modal.confirm({
    title: '确认删除', content: `确定删除用例「${record.name}」？`,
    onOk: async () => { await deleteCase(projectId.value, record.id); message.success('删除成功'); fetchList() },
  })
}

watch(suiteId, (v) => { selectedSuiteId.value = v })
onMounted(() => { fetchSuites(); fetchList() })
</script>

<template>
  <div>
    <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:16px">
      <h2 style="margin:0">测试用例</h2>
      <div style="display:flex;gap:8px;align-items:center">
        <a-select v-model:value="selectedSuiteId" placeholder="全部套件" allow-clear style="width:200px" @change="handleSuiteChange">
          <a-select-option v-for="s in suites" :key="s.id" :value="s.id">{{ s.name }}</a-select-option>
        </a-select>
        <a-input-search v-model:value="keyword" placeholder="搜索用例" style="width:200px" allow-clear @search="handleSearch" />
        <a-button type="primary" @click="openCreate">新建用例</a-button>
      </div>
    </div>

    <a-table :columns="columns" :data-source="list" :loading="loading" row-key="id" size="middle"
      :pagination="{ current: pagination.current, pageSize: pagination.pageSize, total: pagination.total, onChange: (p: number) => { pagination.current = p; fetchList() } }">
      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'priority'">
          <a-tag :color="priorityColors[record.priority] || 'default'">{{ record.priority }}</a-tag>
        </template>
        <template v-if="column.key === 'status'">
          <a-tag :color="record.isActive ? 'green' : 'default'">{{ record.isActive ? '启用' : '禁用' }}</a-tag>
        </template>
        <template v-if="column.dataIndex === 'createdAt'">{{ record.createdAt?.substring(0, 10) }}</template>
        <template v-if="column.key === 'action'">
          <a-space>
            <a @click="handleEdit(record)">编辑</a>
            <a @click="handleToggleStatus(record)">{{ record.isActive ? '禁用' : '启用' }}</a>
            <a style="color:#ff4d4f" @click="handleDelete(record)">删除</a>
          </a-space>
        </template>
      </template>
    </a-table>
  </div>
</template>
