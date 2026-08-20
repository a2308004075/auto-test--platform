<script setup lang="ts">
/**
 * Action 关键字列表 - M7
 */
import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getActions, createAction, deleteAction } from '@/api/action'

const route = useRoute()
const router = useRouter()
const projectId = computed(() => Number(route.params.id))

const loading = ref(false)
const list = ref<any[]>([])
const keyword = ref('')
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })

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
    ElMessage.success('创建成功')
    router.push(`/project/${projectId.value}/actions/${res.data.id}/edit`)
  } catch (e: any) { ElMessage.error(e?.response?.data?.message || '创建失败') }
}

function handleDelete(record: any) {
  ElMessageBox.confirm(`确定删除 Action「${record.name}」？`, '确认删除', { type: 'warning' })
    .then(async () => {
      await deleteAction(projectId.value, record.id)
      ElMessage.success('删除成功'); fetchList()
    })
    .catch(() => {})
}

function handleSearch() { pagination.current = 1; fetchList() }
onMounted(fetchList)
</script>

<template>
  <div>
    <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:16px">
      <h2>Action 关键字</h2>
      <div style="display:flex;gap:8px">
        <el-input v-model="keyword" placeholder="搜索" style="width:200px" clearable @keyup.enter="handleSearch" @clear="handleSearch">
          <template #append><el-button @click="handleSearch">搜索</el-button></template>
        </el-input>
        <el-button type="primary" @click="handleCreate">新建 Action</el-button>
      </div>
    </div>
    <el-table v-loading="loading" :data="list" row-key="id" border style="width:100%">
      <el-table-column prop="name" label="Action 名称" width="200" />
      <el-table-column prop="description" label="描述" show-overflow-tooltip />
      <el-table-column prop="referenceCount" label="引用次数" width="100" />
      <el-table-column label="创建时间" width="120">
        <template #default="{ row }">{{ row.createdAt?.substring(0, 10) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="200">
        <template #default="{ row }">
          <el-button type="primary" link size="small" @click="router.push(`/project/${projectId}/actions/${row.id}/edit`)">编辑</el-button>
          <el-button type="primary" link size="small" @click="router.push(`/project/${projectId}/actions/${row.id}/debug`)">调试</el-button>
          <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <div style="display:flex;justify-content:flex-end;margin-top:16px">
      <el-pagination background layout="total, prev, pager, next" :total="pagination.total"
        :page-size="pagination.pageSize" :current-page="pagination.current"
        @current-change="(p: number) => { pagination.current = p; fetchList() }" />
    </div>
  </div>
</template>
