<!--
 @author HXN
 @date 2026-08-30
 @description 我的任务视图
-->
<script setup lang="ts">
/**
 * 我的任务
 * 查看当前用户被指派的所有类型任务（需求评审、用例评审、缺陷处理等）
 */
import { ref, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getMyTasks, updateTask, TASK_TYPE_MAP, TASK_STATUS_MAP, TASK_STATUS_TYPE_MAP, TASK_PRIORITY_TYPE_MAP } from '@/api/task'
import PageHeader from '@/components/PageHeader/index.vue'
import { useUserStore } from '@/stores'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const list = ref<any[]>([])

// 筛选条件
const filterTaskType = ref('')
const filterStatus = ref('')

// 任务类型选项
const taskTypeOptions = Object.entries(TASK_TYPE_MAP).map(([value, label]) => ({ value, label }))

// 任务状态选项
const statusOptions = Object.entries(TASK_STATUS_MAP).map(([value, label]) => ({ value, label }))

async function fetchList() {
  loading.value = true
  try {
    const params: Record<string, any> = { userId: userStore.userId }
    if (filterTaskType.value) params.taskType = filterTaskType.value
    if (filterStatus.value) params.status = filterStatus.value
    const res: any = await getMyTasks(params)
    list.value = res.data || []
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '加载失败')
    list.value = []
  } finally {
    loading.value = false
  }
}

/**
 * 根据任务类型构造路由并跳转
 */
function handleView(row: any) {
  const projectId = row.projectId
  const bizId = row.bizId
  switch (row.taskType) {
    case 'REQUIREMENT_REVIEW':
    case 'REQUIREMENT_MODIFY':
      router.push(`/project/${projectId}/requirements`)
      break
    case 'CASE_REVIEW':
    case 'CASE_MODIFY':
      router.push(`/project/${projectId}/cases`)
      break
    case 'CASE_EXECUTION':
      router.push(`/project/${projectId}/execution`)
      break
    case 'DEFECT_HANDLING':
      router.push(`/project/${projectId}/defects/${bizId || ''}`)
      break
    default:
      ElMessage.info('暂不支持跳转')
  }
}

/**
 * 标记任务完成
 */
async function handleComplete(row: any) {
  try {
    await updateTask(row.id, { status: 'COMPLETED' })
    ElMessage.success('已标记完成')
    fetchList()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '操作失败')
  }
}

function handleResetFilter() {
  filterTaskType.value = ''
  filterStatus.value = ''
}

// 筛选条件变化时重新加载
watch([filterTaskType, filterStatus], () => {
  fetchList()
})

onMounted(() => {
  fetchList()
})
</script>

<template>
  <div>
    <PageHeader title="我的任务" />

    <div class="tasks-card">
      <!-- 筛选区 -->
      <div class="filter-bar">
        <el-select v-model="filterTaskType" placeholder="任务类型" clearable style="width: 150px">
          <el-option v-for="opt in taskTypeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
        </el-select>
        <el-select v-model="filterStatus" placeholder="任务状态" clearable style="width: 130px">
          <el-option v-for="opt in statusOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
        </el-select>
        <el-button link type="primary" @click="handleResetFilter">重置</el-button>
      </div>

      <el-table :data="list" v-loading="loading" border stripe style="width: 100%">
        <el-table-column label="任务类型" width="110">
          <template #default="{ row }">
            <el-tag size="small">{{ TASK_TYPE_MAP[row.taskType] || row.taskType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="任务标题" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">
            <el-button type="primary" link @click="handleView(row)">{{ row.title }}</el-button>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="(TASK_STATUS_TYPE_MAP[row.status] || 'info') as any" size="small">
              {{ TASK_STATUS_MAP[row.status] || row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="优先级" width="80">
          <template #default="{ row }">
            <el-tag :type="(TASK_PRIORITY_TYPE_MAP[row.priority] || 'info') as any" size="small">
              {{ row.priority || '中' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="projectId" label="项目 ID" width="90" />
        <el-table-column prop="dueDate" label="截止日期" width="120" />
        <el-table-column prop="createdAt" label="创建时间" width="160" />
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleView(row)">查看</el-button>
            <el-button
              v-if="row.status === 'PENDING' || row.status === 'IN_PROGRESS'"
              type="success" link size="small"
              @click="handleComplete(row)"
            >完成</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<style scoped>
.tasks-card {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  padding: 20px 24px;
}
.filter-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
}
</style>
