<script setup lang="ts">
/**
 * 工具方法列表 - M6
 */
import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import { getTools, createTool, updateTool, deleteTool, testTool } from '@/api/tool'

const route = useRoute()
const projectId = computed(() => Number(route.params.id))

const loading = ref(false)
const list = ref<any[]>([])
const category = ref('')
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const modalVisible = ref(false)
const testVisible = ref(false)
const testResult = ref<any>(null)
const testLoading = ref(false)
const editingId = ref<number>(0)
const form = reactive({ name: '', category: 'CUSTOM', description: '', code: 'return "Hello"', returnType: 'String', paramDefinitions: '[]' })
const testInput = ref('{}')
const currentTestId = ref<number>(0)

const columns = [
  { title: '名称', dataIndex: 'name', width: 200 },
  { title: '分类', dataIndex: 'category', width: 100 },
  { title: '返回类型', dataIndex: 'returnType', width: 100 },
  { title: '描述', dataIndex: 'description', ellipsis: true },
  { title: '操作', key: 'action', width: 180 },
]

async function fetchList() {
  loading.value = true
  try {
    const res: any = await getTools(projectId.value, {
      category: category.value || undefined, page: pagination.current, pageSize: pagination.pageSize,
    })
    list.value = res.data?.items || []
    pagination.total = res.data?.total || 0
  } catch { list.value = [] } finally { loading.value = false }
}

function openCreate() {
  editingId.value = 0
  Object.assign(form, { name: '', category: 'CUSTOM', description: '', code: 'return "Hello"', returnType: 'String', paramDefinitions: '[]' })
  modalVisible.value = true
}

function openEdit(record: any) {
  editingId.value = record.id
  Object.assign(form, { name: record.name, category: record.category, description: record.description || '', code: record.code || '', returnType: record.returnType || '', paramDefinitions: record.paramDefinitions || '[]' })
  modalVisible.value = true
}

async function handleSubmit() {
  if (!form.name || !form.code) { message.warning('请填写必填项'); return }
  try {
    if (editingId.value) {
      await updateTool(projectId.value, editingId.value, form)
      message.success('更新成功')
    } else {
      await createTool(projectId.value, { ...form, projectId: projectId.value })
      message.success('创建成功')
    }
    modalVisible.value = false; fetchList()
  } catch (e: any) { message.error(e?.response?.data?.message || '操作失败') }
}

function openTest(record: any) {
  currentTestId.value = record.id
  testInput.value = record.testInput || '{}'
  testResult.value = null
  testVisible.value = true
}

async function handleTest() {
  testLoading.value = true
  try {
    const res: any = await testTool(projectId.value, currentTestId.value, { testInput: testInput.value })
    testResult.value = res.data
  } catch (e: any) { testResult.value = { success: false, error: e?.message } } finally { testLoading.value = false }
}

function handleDelete(record: any) {
  Modal.confirm({
    title: '确认删除', content: `确定删除工具「${record.name}」？`,
    onOk: async () => { await deleteTool(projectId.value, record.id); message.success('删除成功'); fetchList() },
  })
}

onMounted(fetchList)
</script>

<template>
  <div>
    <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:16px">
      <h2>工具方法</h2>
      <div style="display:flex;gap:8px">
        <a-radio-group v-model:value="category" button-style="solid" @change="fetchList">
          <a-radio-button value="">全部</a-radio-button>
          <a-radio-button value="BUILTIN">内置</a-radio-button>
          <a-radio-button value="CUSTOM">自定义</a-radio-button>
        </a-radio-group>
        <a-button type="primary" @click="openCreate">新建工具</a-button>
      </div>
    </div>
    <a-table :columns="columns" :data-source="list" :loading="loading" row-key="id" size="middle"
      :pagination="{ current: pagination.current, pageSize: pagination.pageSize, total: pagination.total, onChange: (p: number) => { pagination.current = p; fetchList() } }">
      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'category'">
          <a-tag :color="record.category === 'BUILTIN' ? 'blue' : 'green'">{{ record.category === 'BUILTIN' ? '内置' : '自定义' }}</a-tag>
        </template>
        <template v-if="column.key === 'action'">
          <a-space><a @click="openTest(record)">测试</a><a @click="openEdit(record)">编辑</a><a style="color:#ff4d4f" @click="handleDelete(record)">删除</a></a-space>
        </template>
      </template>
    </a-table>

    <!-- 新建/编辑弹窗 -->
    <a-modal v-model:open="modalVisible" :title="editingId ? '编辑工具' : '新建工具'" @ok="handleSubmit" :width="640">
      <a-form layout="vertical" style="margin-top:16px">
        <a-form-item label="名称" required><a-input v-model:value="form.name" /></a-form-item>
        <a-form-item label="描述"><a-input v-model:value="form.description" /></a-form-item>
        <a-form-item label="Groovy 代码" required><a-textarea v-model:value="form.code" :rows="8" style="font-family:monospace" /></a-form-item>
        <a-form-item label="返回类型"><a-input v-model:value="form.returnType" /></a-form-item>
      </a-form>
    </a-modal>

    <!-- 测试弹窗 -->
    <a-modal v-model:open="testVisible" title="在线测试" @ok="handleTest" :confirm-loading="testLoading" ok-text="执行">
      <a-form-item label="输入参数 (JSON)"><a-textarea v-model:value="testInput" :rows="4" style="font-family:monospace" /></a-form-item>
      <div v-if="testResult" style="margin-top:12px">
        <a-alert :type="testResult.success ? 'success' : 'error'" :message="testResult.success ? '执行成功' : '执行失败'" :description="testResult.output || testResult.error" show-icon />
        <div v-if="testResult.executionTimeMs" style="color:#999;margin-top:4px">耗时: {{ testResult.executionTimeMs }}ms</div>
      </div>
    </a-modal>
  </div>
</template>
