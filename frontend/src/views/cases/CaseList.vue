<!--
 @author HXN
 @date 2026-08-18 17:31
 @description 测试用例列表视图
-->
<script setup lang="ts">
/**
 * 测试用例列表 - M8
 */
import { ref, reactive, onMounted, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getCases, deleteCase, toggleCaseStatus } from '@/api/case'
import { getSuites } from '@/api/suite'

const route = useRoute()
const router = useRouter()
const projectId = computed(() => Number(route.params.id))
const suiteId = computed(() => Number(route.query.suiteId) || 0)

const loading = ref(false)
const list = ref<any[]>([])
const suites = ref<any[]>([])
const keyword = ref('')
const selectedSuiteId = ref<number>(suiteId.value)
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })

const priorityTypeMap: Record<string, string> = { P0: 'danger', P1: 'warning', P2: '', P3: 'info' }

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
    ElMessage.success(record.isActive === 1 ? '已禁用' : '已启用')
    fetchList()
  } catch { ElMessage.error('操作失败') }
}

function handleDelete(record: any) {
  ElMessageBox.confirm(`确定删除用例「${record.name}」？`, '确认删除', { type: 'warning' })
    .then(async () => { await deleteCase(projectId.value, record.id); ElMessage.success('删除成功'); fetchList() })
    .catch(() => {})
}

watch(suiteId, (v) => { selectedSuiteId.value = v })
onMounted(() => { fetchSuites(); fetchList() })
</script>

<template>
  <div>
    <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:16px">
      <h2 style="margin:0">测试用例</h2>
      <div style="display:flex;gap:8px;align-items:center">
        <el-select v-model="selectedSuiteId" placeholder="全部套件" clearable style="width:200px" @change="handleSuiteChange">
          <el-option v-for="s in suites" :key="s.id" :value="s.id" :label="s.name" />
        </el-select>
        <el-input v-model="keyword" placeholder="搜索用例" style="width:200px" clearable @keyup.enter="handleSearch" @clear="handleSearch">
          <template #append><el-button @click="handleSearch">搜索</el-button></template>
        </el-input>
        <el-button type="primary" @click="openCreate">新建用例</el-button>
      </div>
    </div>

    <el-table v-loading="loading" :data="list" row-key="id" border style="width:100%">
      <el-table-column prop="name" label="用例名称" width="220" />
      <el-table-column label="优先级" width="90">
        <template #default="{ row }">
          <el-tag :type="(priorityTypeMap[row.priority] || 'info') as any" size="small">{{ row.priority }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="timeout" label="超时(秒)" width="100" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.isActive === 1 ? 'success' : 'info'" size="small">{{ row.isActive === 1 ? '启用' : '禁用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" width="120">
        <template #default="{ row }">{{ row.createdAt?.substring(0, 10) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link size="small" @click="handleEdit(row)">编辑</el-button>
          <el-button type="primary" link size="small" @click="handleToggleStatus(row)">{{ row.isActive === 1 ? '禁用' : '启用' }}</el-button>
          <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <div v-if="pagination.total > 0" class="pagination">
      <el-pagination
        v-model:current-page="pagination.current"
        v-model:page-size="pagination.pageSize"
        :total="pagination.total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        background
        @current-change="(p: number) => { pagination.current = p; fetchList() }"
        @size-change="(s: number) => { pagination.pageSize = s; pagination.current = 1; fetchList() }"
      />
    </div>
  </div>
</template>

<style scoped>
.pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
