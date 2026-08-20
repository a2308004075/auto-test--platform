<script setup lang="ts">
/**
 * 测试套件列表 - M8
 */
import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getSuites, createSuite, updateSuite, deleteSuite } from '@/api/suite'

const route = useRoute()
const router = useRouter()
const projectId = computed(() => Number(route.params.id))

const loading = ref(false)
const list = ref<any[]>([])
const keyword = ref('')
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const modalVisible = ref(false)
const editingId = ref<number>(0)
const form = reactive({ name: '', description: '', priority: 'P2' })

const priorityTypeMap: Record<string, string> = { P0: 'danger', P1: 'warning', P2: '', P3: 'info' }

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
  editingId.value = 0
  Object.assign(form, { name: '', description: '', priority: 'P2' })
  modalVisible.value = true
}

function openEdit(record: any) {
  editingId.value = record.id
  Object.assign(form, { name: record.name, description: record.description || '', priority: record.priority || 'P2' })
  modalVisible.value = true
}

async function handleSubmit() {
  if (!form.name) { ElMessage.warning('请输入套件名称'); return }
  try {
    if (editingId.value) {
      await updateSuite(projectId.value, editingId.value, { ...form })
      ElMessage.success('更新成功')
    } else {
      await createSuite(projectId.value, { ...form })
      ElMessage.success('创建成功')
    }
    modalVisible.value = false; fetchList()
  } catch (e: any) { ElMessage.error(e?.response?.data?.message || '操作失败') }
}

function handleDelete(record: any) {
  ElMessageBox.confirm(`确定删除套件「${record.name}」？其下所有用例将一并删除。`, '确认删除', { type: 'warning' })
    .then(async () => { await deleteSuite(projectId.value, record.id); ElMessage.success('删除成功'); fetchList() })
    .catch(() => {})
}

onMounted(fetchList)
</script>

<template>
  <div>
    <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:16px">
      <h2 style="margin:0">测试套件</h2>
      <div style="display:flex;gap:8px">
        <el-input v-model="keyword" placeholder="搜索套件" style="width:220px" clearable @keyup.enter="handleSearch" @clear="handleSearch">
          <template #append><el-button @click="handleSearch">搜索</el-button></template>
        </el-input>
        <el-button type="primary" @click="openCreate">新建套件</el-button>
      </div>
    </div>

    <el-table v-loading="loading" :data="list" row-key="id" border style="width:100%">
      <el-table-column prop="name" label="套件名称" width="200" />
      <el-table-column prop="description" label="描述" show-overflow-tooltip />
      <el-table-column label="优先级" width="90">
        <template #default="{ row }">
          <el-tag :type="(priorityTypeMap[row.priority] || 'info') as any" size="small">{{ row.priority }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="caseCount" label="用例数" width="90" />
      <el-table-column label="创建时间" width="120">
        <template #default="{ row }">{{ row.createdAt?.substring(0, 10) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="260">
        <template #default="{ row }">
          <el-button type="primary" link size="small" @click="router.push(`/project/${projectId}/cases?suiteId=${row.id}`)">查看用例</el-button>
          <el-button type="primary" link size="small" @click="router.push(`/project/${projectId}/suites/${row.id}/edit`)">步骤配置</el-button>
          <el-button type="primary" link size="small" @click="openEdit(row)">基本信息</el-button>
          <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <div style="display:flex;justify-content:flex-end;margin-top:16px">
      <el-pagination background layout="total, prev, pager, next" :total="pagination.total"
        :page-size="pagination.pageSize" :current-page="pagination.current"
        @current-change="(p: number) => { pagination.current = p; fetchList() }" />
    </div>

    <el-dialog v-model="modalVisible" :title="editingId ? '编辑套件' : '新建套件'" width="500px">
      <el-form label-position="top">
        <el-form-item label="套件名称" required>
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="优先级">
          <el-select v-model="form.priority" style="width:100%">
            <el-option value="P0" label="P0" />
            <el-option value="P1" label="P1" />
            <el-option value="P2" label="P2" />
            <el-option value="P3" label="P3" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="modalVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>
