<script setup lang="ts">
/**
 * 测试计划列表 - M9
 */
import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getPlans, deletePlan } from '@/api/plan'
import { startExecution } from '@/api/execution'

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
  ElMessageBox.confirm(`确定删除计划「${record.name}」？`, '确认删除', { type: 'warning' })
    .then(async () => { await deletePlan(record.id); ElMessage.success('删除成功'); fetchList() })
    .catch(() => {})
}

async function handleRun(record: any) {
  ElMessageBox.confirm(`确定执行计划「${record.name}」？`, '触发执行', { type: 'info' })
    .then(async () => {
      try {
        const res: any = await startExecution(record.id)
        ElMessage.success('执行已触发')
        router.push(`/project/${projectId}/executions/${res.data.id}`)
      } catch { ElMessage.error('触发失败') }
    })
    .catch(() => {})
}

onMounted(fetchList)
</script>

<template>
  <div>
    <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:16px">
      <h2 style="margin:0">测试计划</h2>
      <div style="display:flex;gap:8px">
        <el-input v-model="keyword" placeholder="搜索计划" style="width:220px" clearable @keyup.enter="handleSearch" @clear="handleSearch">
          <template #append><el-button @click="handleSearch">搜索</el-button></template>
        </el-input>
        <el-button type="primary" @click="router.push(`/project/${projectId}/plans/new`)">新建计划</el-button>
      </div>
    </div>

    <el-table v-loading="loading" :data="list" row-key="id" border style="width:100%">
      <el-table-column prop="name" label="计划名称" width="180" />
      <el-table-column prop="description" label="描述" show-overflow-tooltip />
      <el-table-column prop="environmentName" label="环境" width="120" />
      <el-table-column label="套件数" width="80">
        <template #default="{ row }">{{ row.suiteIds?.length || 0 }}</template>
      </el-table-column>
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.isActive === 1 ? 'success' : 'info'" size="small">{{ row.isActive === 1 ? '启用' : '禁用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" width="120">
        <template #default="{ row }">{{ row.createdAt?.substring(0, 10) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="260">
        <template #default="{ row }">
          <el-button type="success" link size="small" @click="handleRun(row)">执行</el-button>
          <el-button type="primary" link size="small" @click="handleEdit(row)">编辑</el-button>
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
