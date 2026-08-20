<script setup lang="ts">
/**
 * 接口关键字列表 - M5
 */
import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import { getKeywords, createKeyword, updateKeyword, deleteKeyword, generateKeyword } from '@/api/keyword'
import { getApis } from '@/api/apidoc'

const route = useRoute()
const projectId = computed(() => Number(route.params.id))

const loading = ref(false)
const list = ref<any[]>([])
const keyword = ref('')
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const modalVisible = ref(false)
const editingId = ref<number>(0)
const generateVisible = ref(false)
const generateLoading = ref(false)
const apis = ref<any[]>([])
const selectedApiId = ref<number>(0)
const form = reactive({ name: '', httpMethod: 'GET', path: '', description: '', requestParams: '[]', requestBody: '{}', responseBody: '{}' })

const columns = [
  { title: '关键字名称', dataIndex: 'name', width: 200 },
  { title: 'HTTP 方法', dataIndex: 'httpMethod', width: 100 },
  { title: '路径', dataIndex: 'path', ellipsis: true },
  { title: '描述', dataIndex: 'description', ellipsis: true },
  { title: '创建时间', dataIndex: 'createdAt', width: 120 },
  { title: '操作', key: 'action', width: 180 },
]

async function fetchList() {
  loading.value = true
  try {
    const res: any = await getKeywords(projectId.value, {
      keyword: keyword.value, page: pagination.current, pageSize: pagination.pageSize,
    })
    list.value = res.data?.items || []
    pagination.total = res.data?.total || 0
  } catch { list.value = [] } finally { loading.value = false }
}

function openCreate() {
  editingId.value = 0
  Object.assign(form, { name: '', httpMethod: 'GET', path: '', description: '', requestParams: '[]', requestBody: '{}', responseBody: '{}' })
  modalVisible.value = true
}

function openEdit(record: any) {
  editingId.value = record.id
  Object.assign(form, {
    name: record.name, httpMethod: record.httpMethod, path: record.path,
    description: record.description || '', requestParams: record.requestParams || '[]',
    requestBody: record.requestBody || '{}', responseBody: record.responseBody || '{}',
  })
  modalVisible.value = true
}

async function handleSubmit() {
  if (!form.name || !form.path) { message.warning('请填写必填项'); return }
  try {
    if (editingId.value) {
      await updateKeyword(projectId.value, editingId.value, form)
      message.success('更新成功')
    } else {
      await createKeyword(projectId.value, { ...form, projectId: projectId.value })
      message.success('创建成功')
    }
    modalVisible.value = false; fetchList()
  } catch (e: any) { message.error(e?.response?.data?.message || '操作失败') }
}

async function openGenerate() {
  try {
    const res: any = await getApis(projectId.value, { page: 1, pageSize: 100 })
    apis.value = res.data?.items || []
  } catch { apis.value = [] }
  selectedApiId.value = 0
  generateVisible.value = true
}

async function handleGenerate() {
  if (!selectedApiId.value) { message.warning('请选择接口'); return }
  generateLoading.value = true
  try {
    await generateKeyword(projectId.value, selectedApiId.value)
    message.success('生成成功')
    generateVisible.value = false
    fetchList()
  } catch (e: any) { message.error(e?.response?.data?.message || '生成失败') } finally { generateLoading.value = false }
}

function handleDelete(record: any) {
  Modal.confirm({
    title: '确认删除', content: `确定删除关键字「${record.name}」？`,
    onOk: async () => { await deleteKeyword(projectId.value, record.id); message.success('删除成功'); fetchList() },
  })
}

function handleSearch() { pagination.current = 1; fetchList() }
onMounted(fetchList)
</script>

<template>
  <div>
    <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:16px">
      <h2>接口关键字</h2>
      <div style="display:flex;gap:8px">
        <a-input-search v-model:value="keyword" placeholder="搜索" style="width:200px" allow-clear @search="handleSearch" />
        <a-button @click="openGenerate">从接口生成</a-button>
        <a-button type="primary" @click="openCreate">新建关键字</a-button>
      </div>
    </div>
    <a-table :columns="columns" :data-source="list" :loading="loading" row-key="id" size="middle"
      :pagination="{ current: pagination.current, pageSize: pagination.pageSize, total: pagination.total, onChange: (p: number) => { pagination.current = p; fetchList() } }">
      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'httpMethod'">
          <a-tag :color="record.httpMethod === 'GET' ? 'blue' : record.httpMethod === 'POST' ? 'green' : 'orange'">{{ record.httpMethod }}</a-tag>
        </template>
        <template v-if="column.dataIndex === 'createdAt'">{{ record.createdAt?.substring(0, 10) }}</template>
        <template v-if="column.key === 'action'">
          <a-space>
            <a @click="openEdit(record)">编辑</a>
            <a style="color:#ff4d4f" @click="handleDelete(record)">删除</a>
          </a-space>
        </template>
      </template>
    </a-table>

    <!-- 新建/编辑弹窗 -->
    <a-modal v-model:open="modalVisible" :title="editingId ? '编辑关键字' : '新建关键字'" @ok="handleSubmit" :width="640">
      <a-form layout="vertical" style="margin-top:16px">
        <a-form-item label="名称" required><a-input v-model:value="form.name" /></a-form-item>
        <a-row :gutter="12">
          <a-col :span="6">
            <a-form-item label="HTTP 方法">
              <a-select v-model:value="form.httpMethod">
                <a-select-option v-for="m in ['GET','POST','PUT','DELETE','PATCH']" :key="m" :value="m">{{ m }}</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="18"><a-form-item label="路径" required><a-input v-model:value="form.path" /></a-form-item></a-col>
        </a-row>
        <a-form-item label="描述"><a-textarea v-model:value="form.description" :rows="2" /></a-form-item>
      </a-form>
    </a-modal>

    <!-- 从接口生成弹窗 -->
    <a-modal v-model:open="generateVisible" title="从接口快速生成关键字" @ok="handleGenerate" :confirm-loading="generateLoading">
      <a-form-item label="选择接口" style="margin-top:16px">
        <a-select v-model:value="selectedApiId" placeholder="选择要生成关键字的接口" show-search option-filter-prop="label">
          <a-select-option v-for="api in apis" :key="api.id" :value="api.id" :label="api.name">
            <a-tag :color="api.httpMethod === 'GET' ? 'blue' : 'green'" size="small">{{ api.httpMethod }}</a-tag>
            {{ api.name }}
          </a-select-option>
        </a-select>
      </a-form-item>
    </a-modal>
  </div>
</template>
