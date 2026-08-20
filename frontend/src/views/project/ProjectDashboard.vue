<script setup lang="ts">
/**
 * 项目仪表板 - M2/M10
 * 统计卡片 + 最近执行记录
 */
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getProjectDashboard } from '@/api/project'

const route = useRoute()
const router = useRouter()
const projectId = computed(() => Number(route.params.id))

const loading = ref(false)
const dashboard = ref<any>({})

const statusTypes: Record<string, string> = {
  PENDING: 'info', RUNNING: '', COMPLETED: 'success',
  FAILED: 'danger', CANCELLED: 'warning',
}
const statusLabels: Record<string, string> = {
  PENDING: '等待中', RUNNING: '执行中', COMPLETED: '已完成',
  FAILED: '执行失败', CANCELLED: '已取消',
}

async function fetchDashboard() {
  loading.value = true
  try {
    const res: any = await getProjectDashboard(projectId.value)
    dashboard.value = res.data || {}
  } catch {
    dashboard.value = {}
  } finally {
    loading.value = false
  }
}

function goExecutions() {
  router.push(`/project/${projectId}/executions`)
}

onMounted(fetchDashboard)
</script>

<template>
  <div class="dashboard" v-loading="loading">
    <h2 style="margin-bottom: 20px">{{ dashboard.projectName || '项目' }} - 仪表板</h2>

    <el-row :gutter="16" style="margin-bottom: 16px">
      <el-col :span="6" v-for="stat in [
        { label: '接口数', value: dashboard.stats?.apiCount },
        { label: '关键字数', value: dashboard.stats?.keywordCount },
        { label: '测试套件', value: dashboard.stats?.suiteCount },
        { label: '测试用例', value: dashboard.stats?.caseCount },
      ]" :key="stat.label">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value">{{ stat.value || 0 }}</div>
          <div class="stat-label">{{ stat.label }}</div>
        </el-card>
      </el-col>
    </el-row>
    <el-row :gutter="16" style="margin-bottom: 24px">
      <el-col :span="6" v-for="stat in ([
        { label: '测试计划', value: dashboard.stats?.planCount, color: '', suffix: '' },
        { label: '执行总数', value: dashboard.stats?.executionCount, color: '', suffix: '' },
        { label: '通过用例', value: dashboard.stats?.passedCases, color: '#67c23a', suffix: '' },
        { label: '通过率', value: dashboard.stats?.passRate, color: '#409eff', suffix: '%' },
      ] as Array<{ label: string; value: any; color: string; suffix: string }>)" :key="stat.label">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value" :style="{ color: stat.color || '#303133' }">
            {{ stat.value || 0 }}{{ stat.suffix || '' }}
          </div>
          <div class="stat-label">{{ stat.label }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card>
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <span>最近执行</span>
          <el-button type="primary" link size="small" @click="goExecutions">查看全部</el-button>
        </div>
      </template>
      <el-table
        v-if="dashboard.recentExecutions?.length"
        :data="dashboard.recentExecutions"
        size="small"
        stripe
      >
        <el-table-column prop="planName" label="计划" width="160" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTypes[row.status] || 'info'" size="small">
              {{ statusLabels[row.status] || row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="totalCases" label="总数" width="70" />
        <el-table-column prop="passedCases" label="通过" width="70" />
        <el-table-column prop="failedCases" label="失败" width="70" />
        <el-table-column prop="durationMs" label="耗时(ms)" width="100" />
        <el-table-column label="时间" width="160">
          <template #default="{ row }">
            {{ row.createdAt?.substring(0, 19).replace('T', ' ') }}
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-else description="暂无执行记录" />
    </el-card>
  </div>
</template>

<style scoped>
.stat-card {
  text-align: center;
  margin-bottom: 0;
}
.stat-value {
  font-size: 28px;
  font-weight: 600;
  color: #303133;
  line-height: 1.2;
}
.stat-label {
  font-size: 13px;
  color: #909399;
  margin-top: 4px;
}
</style>
