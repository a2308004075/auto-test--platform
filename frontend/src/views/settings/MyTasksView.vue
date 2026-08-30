<!--
 @author HXN
 @date 2026-08-30
 @description 我的任务视图
-->
<script setup lang="ts">
/**
 * 我的任务
 * 查看当前用户被指派的缺陷（待处理状态）
 */
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getMyDefectTasks } from '@/api/defect'
import PageHeader from '@/components/PageHeader/index.vue'
import { useUserStore } from '@/stores'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const list = ref<any[]>([])

const statusLabelMap: Record<string, string> = {
  NEW: '新建', PENDING: '待验证', COMPLETED: '已完成', REOPENED: '重新打开', CLOSED: '已关闭'
}
const statusTypeMap: Record<string, string> = {
  NEW: 'info', PENDING: 'warning', COMPLETED: 'success', REOPENED: 'danger', CLOSED: ''
}
const severityTypeMap: Record<string, string> = { '致命': 'danger', '严重': 'warning', '一般': 'info', '提示': '' }

async function fetchList() {
  loading.value = true
  try {
    const res: any = await getMyDefectTasks(userStore.userId)
    list.value = res.data || []
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '加载失败')
    list.value = []
  } finally {
    loading.value = false
  }
}

function handleView(row: any) {
  router.push(`/project/${row.projectId}/defects/${row.id}`)
}

onMounted(() => {
  fetchList()
})
</script>

<template>
  <div>
    <PageHeader title="我的任务" />

    <div class="tasks-card">
      <el-table :data="list" v-loading="loading" border stripe style="width: 100%">
        <el-table-column prop="defectNo" label="缺陷编号" width="160" show-overflow-tooltip />
        <el-table-column prop="title" label="缺陷标题" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">
            <el-button type="primary" link @click="handleView(row)">{{ row.title }}</el-button>
          </template>
        </el-table-column>
        <el-table-column prop="projectId" label="项目 ID" width="100" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="(statusTypeMap[row.status] || 'info') as any" size="small">{{ statusLabelMap[row.status] || row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="严重级别" width="90">
          <template #default="{ row }">
            <el-tag :type="(severityTypeMap[row.severity] || 'info') as any" size="small">{{ row.severity }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="moduleName" label="所属模块" width="120" show-overflow-tooltip />
        <el-table-column prop="dueDate" label="计划完成时间" width="130" />
        <el-table-column prop="createdAt" label="创建时间" width="160" />
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleView(row)">查看</el-button>
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
</style>
