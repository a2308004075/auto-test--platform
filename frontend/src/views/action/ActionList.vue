<script setup lang="ts">
/**
 * Action 关键字列表 - M7
 */
import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import { getActions, createAction, deleteAction } from '@/api/action'

const route = useRoute()
const router = useRouter()
const projectId = computed(() => Number(route.params.id))

const loading = ref(false)
const list = ref<any[]>([])
const keyword = ref('')
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })

const columns = [
  { title: 'Action 名称', dataIndex: 'name', width: 200 },
  { title: '描述', dataIndex: 'description', ellipsis: true },
  { title: '引用次数', dataIndex: 'referenceCount', width: 100 },
  { title: '创建时间', dataIndex: 'createdAt', width: 120 },
  { title: '操作', key: 'action', width: 200 },
]

async function fetchList() {
  loading.value = true
  try {
    const res: any = await getActions(projectId.value, {
      keyword: keyword.value, page: pagination.current, pageSize: pagination.pageSize,
    })
    list.value = res.data?.items || []
    pagination.total = res.data?.total || 0
  } catch { list.value = [] } finally { loading.value = false }
}

async function handleCreate() {
  try {
    const res: any = await createAction(projectId.value, {
      projectId: projectId.value, name: `新建 Action ${Date.now() % 10000}`, description: '', nodes: [],
    })
    message.success('创建成功')
    router.push(`/project/${projectId.value}/actions/${res.data.id}/edit`)
  } catch (e: any) { message.error(e?.response?.data?.message || '创建失败') }
}

function handleDelete(record: any) {
  Modal.confirm({
    title: '确认删除', content: `确定删除 Action「${record.name}」？`,
    onOk: async () => {
      await deleteAction(projectId.value, record.id)
      message.success('删除成功'); fetchList()
    },
  })
}

function handleSearch() { pagination.current = 1; fetchList() }
onMounted(fetchList)
</script>

<template>
  <div>
    <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:16px">
      <h2>Action 关键字</h2>
      <div style="display:flex;gap:8px">
        <a-input-search v-model:value="keyword" placeholder="搜索" style="width:200px" allow-clear @search="handleSearch" />
        <a-button type="primary" @click="handleCreate">新建 Action</a-button>
      </div>
    </div>
    <a-table :columns="columns" :data-source="list" :loading="loading" row-key="id" size="middle"
      :pagination="{ current: pagination.current, pageSize: pagination.pageSize, total: pagination.total, onChange: (p: number) => { pagination.current = p; fetchList() } }">
      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'createdAt'">{{ record.createdAt?.substring(0, 10) }}</template>
        <template v-if="column.key === 'action'">
          <a-space>
            <a @click="router.push(`/project/${projectId}/actions/${record.id}/edit`)">编辑</a>
            <a @click="router.push(`/project/${projectId}/actions/${record.id}/debug`)">调试</a>
            <a style="color:#ff4d4f" @click="handleDelete(record)">删除</a>
          </a-space>
        </template>
      </template>
    </a-table>
  </div>
</template>
