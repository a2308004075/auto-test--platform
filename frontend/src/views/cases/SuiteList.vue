<script setup lang="ts">
/**
 * 测试套件列表 - M8
 */
import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import { getSuites, createSuite, updateSuite, deleteSuite } from '@/api/suite'

const route = useRoute()
const router = useRouter()
const projectId = computed(() => route.params.id as string)

const loading = ref(false)
const list = ref<any[]>([])
const keyword = ref('')
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const modalVisible = ref(false)
const editingId = ref('')
const form = reactive({ name: '', description: '', priority: 'P2' })

const columns = [
  { title: '套件名称', dataIndex: 'name', width: 200 },
  { title: '描述', dataIndex: 'description', ellipsis: true },
  { title: '优先级', dataIndex: 'priority', width: 90 },
  { title: '用例数', dataIndex: 'caseCount', width: 90 },
  { title: '创建时间', dataIndex: 'createdAt', width: 120 },
  { title: '操作', key: 'action', width: 260 },
]

const priorityColors: Record<string, string> = { P0: 'red', P1: 'orange', P2: 'blue', P3: 'default' }

async function fetchList() {
  loading.value = true
  try {
    const res: any = await getSuites(projectId.value, { keyword: keyword.value || undefined, page: pagination.current, pageSize: pagination.pageSize })
    list.value = res.data?.items || []
    pagination.total = res.data?.total || 0
  } catch { list.value = [] } finally { loading.value = false }
}

function handleSearch() { pagination.current = 1; fetchList() }

function openCreate() {
  editingId.value = ''
  Object.assign(form, { name: '', description: '', priority: 'P2' })
  modalVisible.value = true
}

function openEdit(record: any) {
  editingId.value = record.id
  Object.assign(form, { name: record.name, description: record.description || '', priority: record.priority || 'P2' })
  modalVisible.value = true
}

async function handleSubmit() {
  if (!form.name) { message.warning('请输入套件名称'); return }
  try {
    if (editingId.value) {
      await updateSuite(projectId.value, editingId.value, { ...form })
      message.success('更新成功')
    } else {
      await createSuite(projectId.value, { ...form })
      message.success('创建成功')
    }
    modalVisible.value = false; fetchList()
  } catch (e: any) { message.error(e?.response?.data?.message || '操作失败') }
}

function handleDelete(record: any) {
  Modal.confirm({
    title: '确认删除',
    content: `确定删除套件「${record.name}」？其下所有用例将一并删除。`,
    onOk: async () => { await deleteSuite(projectId.value, record.id); message.success('删除成功'); fetchList() },
  })
}

onMounted(fetchList)
</script>

<template>
  <div>
    <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:16px">
      <h2 style="margin:0">测试套件</h2>
      <div style="display:flex;gap:8px">
        <a-input-search v-model:value="keyword" placeholder="搜索套件" style="width:220px" allow-clear @search="handleSearch" />
        <a-button type="primary" @click="openCreate">新建套件</a-button>
      </div>
    </div>

    <a-table :columns="columns" :data-source="list" :loading="loading" row-key="id" size="middle"
      :pagination="{ current: pagination.current, pageSize: pagination.pageSize, total: pagination.total, onChange: (p: number) => { pagination.current = p; fetchList() } }">
      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'priority'">
          <a-tag :color="priorityColors[record.priority] || 'default'">{{ record.priority }}</a-tag>
        </template>
        <template v-if="column.dataIndex === 'createdAt'">{{ record.createdAt?.substring(0, 10) }}</template>
        <template v-if="column.key === 'action'">
          <a-space>
            <a @click="router.push(`/project/${projectId}/cases?suiteId=${record.id}`)">查看用例</a>
            <a @click="router.push(`/project/${projectId}/suites/${record.id}/edit`)">步骤配置</a>
            <a @click="openEdit(record)">基本信息</a>
            <a style="color:#ff4d4f" @click="handleDelete(record)">删除</a>
          </a-space>
        </template>
      </template>
    </a-table>

    <a-modal v-model:open="modalVisible" :title="editingId ? '编辑套件' : '新建套件'" @ok="handleSubmit">
      <a-form layout="vertical" style="margin-top:16px">
        <a-form-item label="套件名称" required><a-input v-model:value="form.name" /></a-form-item>
        <a-form-item label="描述"><a-textarea v-model:value="form.description" :rows="2" /></a-form-item>
        <a-form-item label="优先级">
          <a-select v-model:value="form.priority">
            <a-select-option value="P0">P0</a-select-option>
            <a-select-option value="P1">P1</a-select-option>
            <a-select-option value="P2">P2</a-select-option>
            <a-select-option value="P3">P3</a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>
