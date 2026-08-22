<!--
 @author HXN
 @date 2026-08-18 17:31
 @description 执行记录列表视图
-->
<script setup lang="ts">
/**
 * 执行记录列表 - M9
 */
import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getExecutions, cancelExecution } from '@/api/execution'
import { usePermission } from '@/composables/usePermission'

const route = useRoute()
const router = useRouter()
const { hasPermission } = usePermission()
const projectId = computed(() => Number(route.params.id))

const loading = ref(false)
const list = ref<any[]>([])
const statusFilter = ref<string>('')
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })

const statusTypeMap: Record<string, string> = {
  PENDING: 'info',
  RUNNING: '',
  COMPLETED: 'success',
  FAILED: 'danger',
  CANCELLED: 'warning',
}

const statusLabels: Record<string, string> = {
  PENDING: '等待中',
  RUNNING: '执行中',
  COMPLETED: '已完成',
  FAILED: '执行失败',
  CANCELLED: '已取消',
}

async function fetchList() {
  loading.value = true
  try {
    const res: any = await getExecutions(projectId.value, {
      status: statusFilter.value || undefined,
      page: pagination.current, pageSize: pagination.pageSize,
    })
    list.value = res.data?.items || []
    pagination.total = res.data?.total || 0
  } catch { list.value = [] } finally { loading.value = false }
}

function handleFilter() { pagination.current = 1; fetchList() }

function viewDetail(record: any) {
  router.push(`/project/${projectId}/executions/${record.id}`)
}

function handleCancel(record: any) {
  ElMessageBox.confirm(`确定取消执行「${record.planName}」？`, '取消执行', { type: 'warning' })
    .then(async () => {
      try {
        await cancelExecution(record.id)
        ElMessage.success('已取消')
        fetchList()
      } catch { ElMessage.error('操作失败') }
    })
    .catch(() => {})
}

onMounted(fetchList)
</script>

<template>
  <div>
    <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:16px">
      <h2 style="margin:0">执行记录</h2>
      <el-select v-model="statusFilter" placeholder="全部状态" clearable style="width:160px" @change="handleFilter">
        <el-option value="PENDING" label="等待中" />
        <el-option value="RUNNING" label="执行中" />
        <el-option value="COMPLETED" label="已完成" />
        <el-option value="FAILED" label="执行失败" />
        <el-option value="CANCELLED" label="已取消" />
      </el-select>
    </div>

    <el-table v-loading="loading" :data="list" row-key="id" border style="width:100%">
      <el-table-column prop="planName" label="计划" width="160" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="(statusTypeMap[row.status] || 'info') as any" size="small">{{ statusLabels[row.status] || row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="triggerType" label="触发" width="80" />
      <el-table-column prop="totalCases" label="总数" width="70" />
      <el-table-column prop="passedCases" label="通过" width="70" />
      <el-table-column prop="failedCases" label="失败" width="70" />
      <el-table-column prop="skippedCases" label="跳过" width="70" />
      <el-table-column prop="durationMs" label="耗时(ms)" width="100" />
      <el-table-column label="创建时间" width="160">
        <template #default="{ row }">{{ row.createdAt?.substring(0, 19).replace('T', ' ') }}</template>
      </el-table-column>
      <el-table-column label="操作" width="140" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link size="small" @click="viewDetail(row)">详情</el-button>
          <el-button v-if="(row.status === 'PENDING' || row.status === 'RUNNING') && hasPermission('project:execution:cancel')"
            type="danger" link size="small" @click="handleCancel(row)">取消</el-button>
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
