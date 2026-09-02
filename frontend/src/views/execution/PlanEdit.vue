<!--
 @author HXN
 @date 2026-08-20 15:34
 @description 测试计划编辑视图（三卡片布局：基础信息 + 关联自动化套件 + 执行策略）
-->
<script setup lang="ts">
/**
 * 测试计划编辑 - M9
 * 卡片一：基础信息（名称 + 环境 + 分组 + 描述）
 * 卡片二：关联自动化套件（checkbox 卡片布局）
 * 卡片三：执行策略（触发方式 + Cron 配置 + 启停 + 预览）
 */
import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getPlan, createPlan, updatePlan, getPlanGroups } from '@/api/plan'
import { getAutoSuites } from '@/api/autoSuite'
import { getEnvironments } from '@/api/environment'
import EditPageHeader from '@/components/EditPageHeader/index.vue'

const route = useRoute()
const router = useRouter()
const projectId = computed(() => Number(route.params.id))
const planId = computed(() => Number(route.params.planId))
const isEdit = computed(() => !!planId.value)

const form = reactive({
  name: '',
  description: '',
  autoSuiteIds: [] as number[],
  environmentId: null as number | null,
  scheduleCron: '',
  triggerType: 'MANUAL',
  groupId: null as number | null,
  isActive: 1,
})

const suites = ref<any[]>([])
const environments = ref<any[]>([])
const groups = ref<any[]>([])

async function loadSuites() {
  try {
    const res: any = await getAutoSuites(projectId.value, { pageSize: 200 })
    suites.value = res.data?.items || []
  } catch { suites.value = [] }
}

async function loadEnvironments() {
  try {
    const res: any = await getEnvironments(projectId.value)
    environments.value = res.data || []
  } catch { environments.value = [] }
}

async function loadGroups() {
  try {
    const res: any = await getPlanGroups(projectId.value)
    groups.value = res.data || []
  } catch { groups.value = [] }
}

async function loadPlan() {
  if (!planId.value) return
  try {
    const res: any = await getPlan(planId.value)
    const p = res.data
    Object.assign(form, {
      name: p.name || '',
      description: p.description || '',
      autoSuiteIds: p.autoSuiteIds || [],
      environmentId: p.environmentId ?? null,
      scheduleCron: p.scheduleCron || '',
      triggerType: p.triggerType || 'MANUAL',
      groupId: p.groupId ?? null,
      isActive: p.isActive ?? 1,
    })
  } catch { ElMessage.error('加载计划失败') }
}

async function handleSave() {
  if (!form.name) { ElMessage.warning('请输入计划名称'); return }
  try {
    if (isEdit.value) {
      const data: any = { ...form }
      // 编辑模式下 groupId 为 null 表示清除分组（归入"未分组"）
      if (form.groupId === null) {
        data.clearGroup = true
      }
      await updatePlan(planId.value, data)
      ElMessage.success('保存成功')
    } else {
      await createPlan(projectId.value, { ...form })
      ElMessage.success('创建成功')
      router.push(`/project/${projectId.value}/plans`)
    }
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '保存失败')
  }
}

// ===== Cron 可读性解析 =====
function parseCronReadable(cron: string): string {
  if (!cron || !cron.trim()) return ''
  const parts = cron.trim().split(/\s+/)
  if (parts.length < 5) return '表达式格式不完整'
  try {
    const [min, hour, day, month, week] = parts
    if (week !== '*' && week !== '?') {
      const weekMap: Record<string, string> = { '0': '周日', '1': '周一', '2': '周二', '3': '周三', '4': '周四', '5': '周五', '6': '周六', '7': '周日' }
      return `每${weekMap[week] || week} ${hour}:${min.padStart(2, '0')} 执行`
    }
    if (day === '*' && month === '*') return `每天 ${hour}:${min.padStart(2, '0')} 执行`
    return `Cron: ${cron}`
  } catch {
    return `Cron: ${cron}`
  }
}

const cronReadable = computed(() => parseCronReadable(form.scheduleCron))

// ===== 最近 5 次执行时间预览（简单模拟） =====
const nextExecutions = computed(() => {
  if (form.triggerType !== 'SCHEDULED' || !form.scheduleCron) return []
  const parts = form.scheduleCron.trim().split(/\s+/)
  if (parts.length < 5) return []
  try {
    const [min, hour] = parts
    const times: string[] = []
    const now = new Date()
    for (let i = 1; i <= 5; i++) {
      const d = new Date(now)
      d.setDate(d.getDate() + i)
      d.setHours(parseInt(hour, 10), parseInt(min, 10), 0, 0)
      times.push(d.toISOString().substring(0, 16).replace('T', ' '))
    }
    return times
  } catch { return [] }
})

// ===== 自动化套件切换样式 =====
function isSuiteSelected(autoSuiteId: number): boolean {
  return form.autoSuiteIds.includes(autoSuiteId)
}

function toggleSuite(autoSuiteId: number) {
  const idx = form.autoSuiteIds.indexOf(autoSuiteId)
  if (idx >= 0) {
    form.autoSuiteIds.splice(idx, 1)
  } else {
    form.autoSuiteIds.push(autoSuiteId)
  }
}

onMounted(() => {
  loadSuites()
  loadEnvironments()
  loadGroups()
  if (isEdit.value) loadPlan()
})
</script>

<template>
  <div>
    <EditPageHeader :title="isEdit ? `编辑测试计划：${form.name || '加载中...'}` : '新建测试计划'">
      <el-button @click="router.back()">取消</el-button>
      <el-button type="primary" @click="handleSave">保存</el-button>
    </EditPageHeader>

    <!-- 卡片一：基础信息 -->
    <el-card style="margin-bottom:16px">
      <template #header><span>基础信息</span></template>
      <el-form label-position="top">
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="计划名称" required>
              <el-input v-model="form.name" placeholder="请输入计划名称" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="绑定环境">
              <el-select v-model="form.environmentId" placeholder="选择执行环境" clearable style="width:100%">
                <el-option v-for="env in environments" :key="env.id" :value="env.id"
                  :label="`${env.name}${env.isCurrent === 1 ? '（当前）' : ''}`" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="所属分组">
              <el-select v-model="form.groupId" placeholder="未分组" clearable style="width:100%">
                <el-option v-for="g in groups" :key="g.id" :value="g.id" :label="g.name" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="2" placeholder="可选，描述此计划的用途" />
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 卡片二：关联自动化套件 -->
    <el-card style="margin-bottom:16px">
      <template #header><span>关联自动化套件</span></template>
      <div class="suite-grid">
        <div v-for="suite in suites" :key="suite.id"
          class="suite-card"
          :class="{ selected: isSuiteSelected(suite.id) }"
          @click="toggleSuite(suite.id)">
          <el-checkbox :model-value="isSuiteSelected(suite.id)" @click.stop
            @change="toggleSuite(suite.id)" style="pointer-events:none" />
          <span class="suite-name">{{ suite.name }}</span>
          <span class="suite-count">({{ suite.caseCount || 0 }})</span>
        </div>
      </div>
      <div v-if="suites.length === 0" style="text-align:center;color:#909399;padding:20px">
        暂无可用自动化套件，请先创建自动化套件
      </div>
    </el-card>

    <!-- 卡片三：执行策略 -->
    <el-card style="margin-bottom:16px">
      <template #header><span>执行策略</span></template>

      <!-- 触发方式选择 -->
      <div class="trigger-group">
        <div class="trigger-card" :class="{ selected: form.triggerType === 'MANUAL' }"
          @click="form.triggerType = 'MANUAL'">
          <el-radio :model-value="form.triggerType" label="MANUAL" @click.stop>手动触发</el-radio>
        </div>
        <div class="trigger-card" :class="{ selected: form.triggerType === 'SCHEDULED' }"
          @click="form.triggerType = 'SCHEDULED'">
          <el-radio :model-value="form.triggerType" label="SCHEDULED" @click.stop>定时执行</el-radio>
        </div>
        <div class="trigger-card" :class="{ selected: form.triggerType === 'CI' }"
          @click="form.triggerType = 'CI'">
          <el-radio :model-value="form.triggerType" label="CI" @click.stop>CI 触发</el-radio>
        </div>
      </div>

      <!-- 定时执行配置区 -->
      <div v-if="form.triggerType === 'SCHEDULED'" class="cron-config-area">
        <el-form label-position="top">
          <el-form-item label="Cron 表达式">
            <div style="display:flex;gap:8px;align-items:center">
              <el-input v-model="form.scheduleCron" placeholder="如 0 8 * * *" style="width:200px;font-family:monospace" />
              <span v-if="cronReadable" style="font-size:12px;color:#909399">{{ cronReadable }}</span>
            </div>
          </el-form-item>
          <el-form-item label="启停">
            <el-switch v-model="form.isActive" :active-value="1" :inactive-value="0" />
          </el-form-item>
        </el-form>
        <div v-if="nextExecutions.length > 0" class="cron-preview">
          <b>最近 5 次执行时间预览：</b><br>
          {{ nextExecutions.join(' · ') }}
        </div>
      </div>

      <!-- CI 触发说明 -->
      <div v-if="form.triggerType === 'CI'" class="ci-config-area">
        <div style="font-size:13px;color:#909399;line-height:1.8">
          CI 触发模式下，计划将通过外部 CI/CD 系统（如 Jenkins、GitLab CI）的 API 调用来触发执行。<br>
          请在 CI 系统中配置 Webhook 或 API 调用以触发此计划。
        </div>
      </div>
    </el-card>
  </div>
</template>

<style scoped>
/* ===== 套件网格 ===== */
.suite-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
}
.suite-card {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  cursor: pointer;
  transition: all .15s;
  font-size: 14px;
}
.suite-card:hover {
  border-color: #409eff;
}
.suite-card.selected {
  border-color: #409eff;
  background: #ecf5ff;
}
.suite-name {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.suite-count {
  color: #909399;
  font-size: 12px;
  flex-shrink: 0;
}

/* ===== 触发方式卡片 ===== */
.trigger-group {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}
.trigger-card {
  padding: 8px 16px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  cursor: pointer;
  transition: all .15s;
}
.trigger-card:hover {
  border-color: #409eff;
}
.trigger-card.selected {
  border-color: #409eff;
  background: #ecf5ff;
}

/* ===== Cron 配置区 ===== */
.cron-config-area {
  padding: 16px;
  background: #f5f7fa;
  border-radius: 4px;
}
.cron-preview {
  margin-top: 12px;
  font-size: 12px;
  color: #909399;
  line-height: 1.8;
}

/* ===== CI 配置区 ===== */
.ci-config-area {
  padding: 16px;
  background: #f5f7fa;
  border-radius: 4px;
}
</style>
