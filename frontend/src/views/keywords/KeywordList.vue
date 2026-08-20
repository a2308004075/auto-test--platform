<script setup lang="ts">
/**
 * 接口关键字列表 - M5
 */
import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
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
  if (!form.name || !form.path) { ElMessage.warning('请填写必填项'); return }
  try {
    if (editingId.value) {
      await updateKeyword(projectId.value, editingId.value, form)
      ElMessage.success('更新成功')
    } else {
      await createKeyword(projectId.value, { ...form, projectId: projectId.value })
      ElMessage.success('创建成功')
    }
    modalVisible.value = false; fetchList()
  } catch (e: any) { ElMessage.error(e?.response?.data?.message || '操作失败') }
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
  if (!selectedApiId.value) { ElMessage.warning('请选择接口'); return }
  generateLoading.value = true
  try {
    await generateKeyword(projectId.value, selectedApiId.value)
    ElMessage.success('生成成功')
    generateVisible.value = false
    fetchList()
  } catch (e: any) { ElMessage.error(e?.response?.data?.message || '生成失败') } finally { generateLoading.value = false }
}

function handleDelete(record: any) {
  ElMessageBox.confirm(`确定删除关键字「${record.name}」？`, '确认删除', { type: 'warning' })
    .then(async () => { await deleteKeyword(projectId.value, record.id); ElMessage.success('删除成功'); fetchList() })
    .catch(() => {})
}

function handleSearch() { pagination.current = 1; fetchList() }
onMounted(fetchList)
</script>

<template>
  <div>
    <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:16px">
      <h2>接口关键字</h2>
      <div style="display:flex;gap:8px">
        <el-input v-model="keyword" placeholder="搜索" style="width:200px" clearable @keyup.enter="handleSearch" @clear="handleSearch">
          <template #append><el-button @click="handleSearch">搜索</el-button></template>
        </el-input>
        <el-button @click="openGenerate">从接口生成</el-button>
        <el-button type="primary" @click="openCreate">新建关键字</el-button>
      </div>
    </div>
    <el-table v-loading="loading" :data="list" row-key="id" border style="width:100%">
      <el-table-column prop="name" label="关键字名称" width="200" />
      <el-table-column label="HTTP 方法" width="100">
        <template #default="{ row }">
          <el-tag :type="row.httpMethod === 'GET' ? '' : row.httpMethod === 'POST' ? 'success' : 'warning'" size="small">{{ row.httpMethod }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="path" label="路径" show-overflow-tooltip />
      <el-table-column prop="description" label="描述" show-overflow-tooltip />
      <el-table-column label="创建时间" width="120">
        <template #default="{ row }">{{ row.createdAt?.substring(0, 10) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="180">
        <template #default="{ row }">
          <el-button type="primary" link size="small" @click="openEdit(row)">编辑</el-button>
          <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <div style="display:flex;justify-content:flex-end;margin-top:16px">
      <el-pagination background layout="total, prev, pager, next" :total="pagination.total"
        :page-size="pagination.pageSize" :current-page="pagination.current"
        @current-change="(p: number) => { pagination.current = p; fetchList() }" />
    </div>

    <!-- 新建/编辑弹窗 -->
    <el-dialog v-model="modalVisible" :title="editingId ? '编辑关键字' : '新建关键字'" width="640px">
      <el-form label-position="top">
        <el-form-item label="名称" required>
          <el-input v-model="form.name" />
        </el-form-item>
        <el-row :gutter="12">
          <el-col :span="6">
            <el-form-item label="HTTP 方法">
              <el-select v-model="form.httpMethod" style="width:100%">
                <el-option v-for="m in ['GET','POST','PUT','DELETE','PATCH']" :key="m" :value="m" :label="m" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="18">
            <el-form-item label="路径" required>
              <el-input v-model="form.path" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="modalVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 从接口生成弹窗 -->
    <el-dialog v-model="generateVisible" title="从接口快速生成关键字" width="500px">
      <el-form-item label="选择接口">
        <el-select v-model="selectedApiId" placeholder="选择要生成关键字的接口" filterable style="width:100%">
          <el-option v-for="api in apis" :key="api.id" :value="api.id" :label="`[${api.httpMethod}] ${api.name}`" />
        </el-select>
      </el-form-item>
      <template #footer>
        <el-button @click="generateVisible = false">取消</el-button>
        <el-button type="primary" :loading="generateLoading" @click="handleGenerate">生成</el-button>
      </template>
    </el-dialog>
  </div>
</template>
