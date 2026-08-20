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
const projectId = computed(() => route.params.id as string)

const loading = ref(false)
const dashboard = ref<any>({})

const statusColors: Record<string, string> = {
  PENDING: 'default', RUNNING: 'processing', COMPLETED: 'success',
  FAILED: 'error', CANCELLED: 'warning',
}
const statusLabels: Record<string, string> = {
  PENDING: '等待中', RUNNING: '执行中', COMPLETED: '已完成',
  FAILED: '执行失败', CANCELLED: '已取消',
}

const recentColumns = [
  { title: '计划', dataIndex: 'planName', width: 160 },
  { title: '状态', key: 'status', width: 90 },
  { title: '总数', dataIndex: 'totalCases', width: 70 },
  { title: '通过', dataIndex: 'passedCases', width: 70 },
  { title: '失败', dataIndex: 'failedCases', width: 70 },
  { title: '耗时(ms)', dataIndex: 'durationMs', width: 100 },
  { title: '时间', dataIndex: 'createdAt', width: 160 },
]

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
  <div class="dashboard">
    <h2 style="margin-bottom: 24px">{{ dashboard.projectName || '项目' }} - 仪表板</h2>
    <a-spin :spinning="loading">
      <a-row :gutter="[16, 16]">
        <a-col :span="6">
          <a-card>
            <a-statistic title="接口数" :value="dashboard.stats?.apiCount || 0" style="text-align: center" />
          </a-card>
        </a-col>
        <a-col :span="6">
          <a-card>
            <a-statistic title="关键字数" :value="dashboard.stats?.keywordCount || 0" style="text-align: center" />
          </a-card>
        </a-col>
        <a-col :span="6">
          <a-card>
            <a-statistic title="测试套件" :value="dashboard.stats?.suiteCount || 0" style="text-align: center" />
          </a-card>
        </a-col>
        <a-col :span="6">
          <a-card>
            <a-statistic title="测试用例" :value="dashboard.stats?.caseCount || 0" style="text-align: center" />
          </a-card>
        </a-col>
        <a-col :span="6">
          <a-card>
            <a-statistic title="测试计划" :value="dashboard.stats?.planCount || 0" style="text-align: center" />
          </a-card>
        </a-col>
        <a-col :span="6">
          <a-card>
            <a-statistic title="执行总数" :value="dashboard.stats?.executionCount || 0" style="text-align: center" />
          </a-card>
        </a-col>
        <a-col :span="6">
          <a-card>
            <a-statistic title="通过用例" :value="dashboard.stats?.passedCases || 0"
              :value-style="{ color: '#52c41a' }" style="text-align: center" />
          </a-card>
        </a-col>
        <a-col :span="6">
          <a-card>
            <a-statistic title="通过率" :value="dashboard.stats?.passRate || 0" suffix="%"
              :value-style="{ color: '#1890ff' }" style="text-align: center" />
          </a-card>
        </a-col>
      </a-row>

      <a-card title="最近执行" size="small" style="margin-top: 24px"
        :extra="dashboard.recentExecutions?.length ? undefined : undefined">
        <template #extra>
          <a-button type="link" size="small" @click="goExecutions">查看全部</a-button>
        </template>
        <a-table
          v-if="dashboard.recentExecutions?.length"
          :columns="recentColumns"
          :data-source="dashboard.recentExecutions"
          row-key="id"
          size="small"
          :pagination="false"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'status'">
              <a-tag :color="statusColors[record.status] || 'default'">
                {{ statusLabels[record.status] || record.status }}
              </a-tag>
            </template>
            <template v-if="column.dataIndex === 'createdAt'">
              {{ record.createdAt?.substring(0, 19).replace('T', ' ') }}
            </template>
          </template>
        </a-table>
        <a-empty v-else description="暂无执行记录" />
      </a-card>
    </a-spin>
  </div>
</template>
