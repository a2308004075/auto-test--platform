<script setup lang="ts">
/**
 * 项目概览页 - 对齐 UI 原型 project-dashboard.html
 * 5 层布局：项目头部 → 健康度 → KPI卡片 → 趋势分析 → 覆盖率与风险
 */
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import * as echarts from 'echarts'
import { getProjectDashboard } from '@/api/project'

const route = useRoute()
const router = useRouter()
const projectId = computed(() => Number(route.params.id))

const loading = ref(false)
const dash = ref<any>({})
const stats = computed(() => dash.value?.stats || {})
const trend = computed(() => dash.value?.trend || {})

// 健康度评级
function healthGrade(score: number) {
  if (score >= 90) return '优秀'
  if (score >= 75) return '良好'
  if (score >= 60) return '一般'
  return '较差'
}
function healthColor(score: number) {
  if (score >= 90) return '#52c41a'
  if (score >= 75) return '#52c41a'
  if (score >= 60) return '#faad14'
  return '#f5222d'
}

// SVG 圆环参数
const circleR = 50
const circleC = 2 * Math.PI * circleR // ≈ 314.16
function circleDash(score: number) {
  const filled = (score / 100) * circleC
  return `${filled.toFixed(1)} ${circleC.toFixed(1)}`
}

// KPI 卡片定义
const kpiCards = computed(() => {
  const s = stats.value
  return [
    { icon: '✓', iconBg: '#f6ffed', label: '用例通过率', value: `${s.passRate || 0}%`, color: '#52c41a', footer: '' },
    { icon: '◎', iconBg: '#e6f7ff', label: '接口覆盖率', value: `${s.apiCoverageRate || 0}%`, color: '', footer: `${s.coveredApiCount || 0}/${s.apiCount || 0} 个接口` },
    { icon: '⚠', iconBg: '#fff2f0', label: '缺陷密度', value: `${s.defectDensity || 0}`, color: '', footer: '缺陷/接口' },
    { icon: '▶', iconBg: '#e6f7ff', label: '本周执行次数', value: `${s.weeklyExecutionCount || 0}`, color: '', footer: '' },
    { icon: '⏱', iconBg: '#fffbe6', label: '缺陷修复时效', value: `${s.defectFixTime || 0}`, color: '', suffix: ' 天', footer: '' },
    { icon: '📋', iconBg: '#f0e6ff', label: '套件完成率', value: `${s.suiteCompletionRate || 0}%`, color: '', footer: `${s.completedSuiteCount || 0}/${s.suiteCount || 0} 个套件` },
    { icon: '🐛', iconBg: '#fff2f0', label: '缺陷逃逸率', value: `${s.defectEscapeRate || 0}%`, color: s.defectEscapeRate > 5 ? '#faad14' : '', footer: '' },
    { icon: '↻', iconBg: '#f6ffed', label: '回归通过率', value: `${s.regressionPassRate || 0}%`, color: '#52c41a', footer: '' },
  ]
})

// KPI tooltip 说明
const kpiTips: Record<string, string> = {
  '用例通过率': '所有测试用例在最近一次完整执行中的通过比例',
  '接口覆盖率': '已有测试用例覆盖的 API 接口占项目总接口数的比例',
  '缺陷密度': '平均每个接口关联的缺陷数量，暂无数据',
  '本周执行次数': '当前自然周内所有测试计划的累计执行次数',
  '缺陷修复时效': '从缺陷发现到修复确认的平均耗时，暂无数据',
  '套件完成率': '已完成执行的测试套件占总测试套件的比例',
  '缺陷逃逸率': '上线后发现的缺陷占总缺陷数的比例，暂无数据',
  '回归通过率': '回归测试套件的用例通过率',
}

// ── ECharts 实例管理 ──
let passRateChart: echarts.ECharts | null = null
let execFreqChart: echarts.ECharts | null = null
const passRateRef = ref<HTMLElement>()
const execFreqRef = ref<HTMLElement>()
const timeRange = ref('day')

function initCharts() {
  // 通过率趋势
  if (passRateRef.value) {
    passRateChart = echarts.init(passRateRef.value)
    updatePassRateChart()
  }
  // 执行频次
  if (execFreqRef.value) {
    execFreqChart = echarts.init(execFreqRef.value)
    updateExecFreqChart()
  }
}

function updatePassRateChart() {
  if (!passRateChart) return
  const data = trend.value?.passRateTrend || []
  const dates = data.map((p: any) => p.date)
  const values = data.map((p: any) => p.value)
  passRateChart.setOption({
    grid: { left: 40, right: 16, top: 20, bottom: 30 },
    xAxis: { type: 'category', data: dates, axisLabel: { fontSize: 10, color: 'rgba(0,0,0,.45)' }, boundaryGap: false },
    yAxis: { type: 'value', min: 0, max: 100, axisLabel: { fontSize: 10, color: 'rgba(0,0,0,.45)', formatter: '{value}%' }, splitLine: { lineStyle: { color: '#f0f0f0' } } },
    tooltip: { trigger: 'axis', formatter: (p: any) => `${p[0]?.name}<br/>通过率：${p[0]?.value}%` },
    series: [
      {
        type: 'line', data: values, smooth: true,
        lineStyle: { color: '#409eff', width: 2 },
        areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(64,158,255,0.15)' },
          { offset: 1, color: 'rgba(64,158,255,0.01)' },
        ]) },
        itemStyle: { color: '#409eff' },
        symbolSize: 6,
        markLine: { silent: true, data: [{ yAxis: 95, label: { formatter: '目标 95%', position: 'insideEndTop', fontSize: 10, color: '#e6a23c' }, lineStyle: { color: '#e6a23c', type: 'dashed', opacity: 0.6 } }] },
      },
    ],
  })
}

function updateExecFreqChart() {
  if (!execFreqChart) return
  const data = trend.value?.executionFrequency || []
  const days = data.map((d: any) => d.day)
  const passed = data.map((d: any) => d.passed)
  const failed = data.map((d: any) => d.failed)
  execFreqChart.setOption({
    grid: { left: 30, right: 10, top: 10, bottom: 30 },
    xAxis: { type: 'category', data: days, axisLabel: { fontSize: 10, color: 'rgba(0,0,0,.45)' } },
    yAxis: { type: 'value', axisLabel: { fontSize: 10, color: 'rgba(0,0,0,.45)' }, splitLine: { lineStyle: { color: '#f0f0f0' } } },
    tooltip: { trigger: 'axis' },
    legend: { bottom: 0, textStyle: { fontSize: 11, color: 'rgba(0,0,0,.45)' }, itemWidth: 8, itemHeight: 8 },
    series: [
      { name: '通过', type: 'bar', stack: 'exec', data: passed, itemStyle: { color: '#52c41a' }, barWidth: '60%' },
      { name: '失败', type: 'bar', stack: 'exec', data: failed, itemStyle: { color: '#f56c6c' } },
    ],
  })
}

function handleResize() {
  passRateChart?.resize()
  execFreqChart?.resize()
}

async function fetchDashboard() {
  loading.value = true
  try {
    const res: any = await getProjectDashboard(projectId.value)
    dash.value = res.data || {}
    await nextTick()
    initCharts()
  } catch {
    dash.value = {}
  } finally {
    loading.value = false
  }
}

function goExecutions() {
  router.push(`/project/${projectId.value}/executions`)
}

onMounted(() => {
  fetchDashboard()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  passRateChart?.dispose()
  execFreqChart?.dispose()
  window.removeEventListener('resize', handleResize)
})

watch(projectId, fetchDashboard)
</script>

<template>
  <div class="dashboard" v-loading="loading">
    <!-- Layer 1: 项目头部 -->
    <div class="overview-header">
      <div class="overview-header-left">
        <h1>
          {{ dash.projectName || '项目' }}
          <span class="status-tag" :class="dash.isActive !== false ? '' : 'disabled'">
            {{ dash.isActive !== false ? '运行中' : '已停用' }}
          </span>
        </h1>
        <p class="overview-desc">{{ dash.projectDescription || '' }}</p>
      </div>
      <div class="overview-header-right">
        <div class="last-exec" v-if="trend.lastExecutionTime">
          最近执行：{{ trend.lastExecutionTime?.substring(0, 16).replace('T', ' ') }}
          <a @click="goExecutions">查看详情 →</a>
        </div>
        <div class="data-update-time" v-if="trend.dataUpdateTime">
          数据更新至 {{ trend.dataUpdateTime?.substring(0, 16).replace('T', ' ') }}
        </div>
      </div>
    </div>

    <!-- Layer 2: 质量健康度 -->
    <div class="health-panel">
      <div class="health-left">
        <div class="health-circle-wrap">
          <svg viewBox="0 0 120 120" width="120" height="120">
            <circle cx="60" cy="60" :r="circleR" fill="none" stroke="#f0f0f0" stroke-width="8" />
            <circle cx="60" cy="60" :r="circleR" fill="none" :stroke="healthColor(stats.healthScore || 0)" stroke-width="8"
              :stroke-dasharray="circleDash(stats.healthScore || 0)" stroke-linecap="round" transform="rotate(-90 60 60)" />
          </svg>
          <div class="health-center">
            <span class="health-num" :style="{ color: healthColor(stats.healthScore || 0) }">{{ stats.healthScore || 0 }}</span>
            <span class="health-grade">{{ healthGrade(stats.healthScore || 0) }}</span>
          </div>
        </div>
        <div class="health-title">质量健康度</div>
        <div class="health-subtitle">基于最近 30 天测试数据综合评估</div>
      </div>
      <div class="health-right">
        <div class="score-dim" v-for="dim in [
          { label: '通过率得分', value: stats.passRateScore || 0, weight: '35%', tip: '基于所有测试用例的通过率计算，权重 35%' },
          { label: '覆盖率得分', value: stats.coverageScore || 0, weight: '25%', tip: '基于接口覆盖率和模块覆盖率加权计算，权重 25%' },
          { label: '稳定性得分', value: stats.stabilityScore || 0, weight: '25%', tip: '基于用例回归通过率和缺陷逃逸率综合计算，权重 25%' },
          { label: '效率得分', value: stats.efficiencyScore || 0, weight: '15%', tip: '基于缺陷修复时效和测试执行频次综合计算，权重 15%' },
        ]" :key="dim.label">
          <div class="score-dim-header">
            <span class="score-dim-label">{{ dim.label }}</span>
            <el-tooltip :content="dim.tip" placement="top">
              <span class="help-icon">ⓘ</span>
            </el-tooltip>
            <span class="score-dim-value">{{ dim.value }}</span>
          </div>
          <div class="score-bar">
            <div class="score-fill" :style="{ width: dim.value + '%', background: healthColor(dim.value) }"></div>
          </div>
        </div>
      </div>
    </div>

    <!-- Layer 3: 8 KPI 卡片 -->
    <div class="kpi-grid">
      <div class="kpi-card" v-for="kpi in kpiCards" :key="kpi.label">
        <div class="kpi-icon" :style="{ background: kpi.iconBg }">{{ kpi.icon }}</div>
        <div class="kpi-info">
          <div class="kpi-label">
            {{ kpi.label }}
            <el-tooltip :content="kpiTips[kpi.label] || ''" placement="top">
              <span class="help-icon">ⓘ</span>
            </el-tooltip>
          </div>
          <div class="kpi-value" :style="{ color: kpi.color || '#303133' }">
            {{ kpi.value }}<span v-if="kpi.suffix" style="font-size:14px;font-weight:400;color:#909399;">{{ kpi.suffix }}</span>
          </div>
          <div class="kpi-footer" v-if="kpi.footer">{{ kpi.footer }}</div>
        </div>
      </div>
    </div>

    <!-- Layer 4: 趋势分析 -->
    <div class="trend-section">
      <!-- 通过率趋势 -->
      <div class="card trend-main">
        <div class="card-header">
          <span>通过率趋势</span>
          <div class="trend-controls">
            <div class="target-line"><span class="target-dot"></span>目标 95%</div>
            <el-radio-group v-model="timeRange" size="small">
              <el-radio-button label="day">天</el-radio-button>
              <el-radio-button label="week">周</el-radio-button>
              <el-radio-button label="month">月</el-radio-button>
            </el-radio-group>
          </div>
        </div>
        <div class="card-body">
          <div ref="passRateRef" style="width:100%;height:200px;"></div>
        </div>
      </div>
      <!-- 右侧两卡片 -->
      <div class="trend-side-col">
        <div class="card trend-side">
          <div class="card-header">
            <span style="font-size:13px;font-weight:600;">每日执行频次</span>
            <span style="font-size:11px;color:#909399;">近 7 天</span>
          </div>
          <div class="card-body">
            <div ref="execFreqRef" style="width:100%;height:140px;"></div>
          </div>
        </div>
        <!-- 缺陷趋势（暂无数据，显示占位） -->
        <div class="card trend-side">
          <div class="card-header">
            <span style="font-size:13px;font-weight:600;">缺陷趋势</span>
            <span style="font-size:11px;color:#909399;">暂无数据</span>
          </div>
          <div class="card-body" style="display:flex;align-items:center;justify-content:center;min-height:140px;color:#c0c4cc;font-size:13px;">
            缺陷追踪模块暂未上线
          </div>
        </div>
      </div>
    </div>

    <!-- Layer 5: 覆盖率 & 风险 -->
    <div class="coverage-risk-section">
      <!-- 模块覆盖率 -->
      <div class="card">
        <div class="card-header">模块测试覆盖率</div>
        <div class="card-body">
          <div v-if="trend.moduleCoverage?.length">
            <div class="coverage-item" v-for="m in trend.moduleCoverage" :key="m.moduleName">
              <span class="cov-module">
                {{ m.moduleName }}
                <el-tooltip content="该模块的接口被测试用例覆盖的比例" placement="top">
                  <span class="help-icon">ⓘ</span>
                </el-tooltip>
              </span>
              <div class="cov-bar-wrap">
                <div class="cov-bar" :style="{ width: (m.percentage || 0) + '%', background: (m.percentage || 0) >= 80 ? '#52c41a' : '#faad14' }"></div>
              </div>
              <span class="cov-count">{{ m.count }}</span>
              <span class="cov-pct" :style="{ color: (m.percentage || 0) >= 80 ? '#52c41a' : '#faad14' }">{{ m.percentage || 0 }}%</span>
            </div>
          </div>
          <el-empty v-else description="暂无模块数据" :image-size="60" />
        </div>
      </div>
      <!-- 质量风险 Top 5 -->
      <div class="card">
        <div class="card-header">
          <span>质量风险 Top 5</span>
          <a style="font-size:12px;color:#409eff;cursor:pointer;" @click="router.push(`/project/${projectId}/cases`)">查看全部 →</a>
        </div>
        <div class="card-body">
          <div v-if="trend.qualityRiskTop5?.length">
            <div class="risk-item" v-for="r in trend.qualityRiskTop5" :key="r.rank">
              <div class="risk-rank" :class="r.rank <= 3 ? 'top' : 'normal'">{{ r.rank }}</div>
              <div class="risk-info">
                <div class="risk-name">{{ r.caseName }}</div>
                <div class="risk-meta">{{ r.suiteName }} · 近 30 天失败 {{ r.failCount }} 次 · 失败率 {{ r.failRate }}%</div>
              </div>
              <div class="risk-fail">{{ r.failRate }}%</div>
            </div>
            <div class="risk-footer">
              <span>持续失败用例：</span>
              <span class="risk-count">{{ trend.continuousFailCount || 0 }}</span>
              <span>个用例连续 3 次以上执行失败</span>
            </div>
          </div>
          <el-empty v-else description="暂无风险数据" :image-size="60" />
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.dashboard { padding: 0; }

/* Layer 1 */
.overview-header {
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: 16px; padding: 20px 24px;
  background: #fff; border-radius: 6px;
  border: 1px solid #f0f0f0;
  box-shadow: 0 1px 2px rgba(0,0,0,.03), 0 1px 6px -1px rgba(0,0,0,.02), 0 2px 4px rgba(0,0,0,.02);
}
.overview-header-left h1 {
  display: flex; align-items: center; gap: 10px;
  font-size: 20px; font-weight: 600; margin: 0;
}
.overview-header-left .status-tag {
  font-size: 12px; font-weight: 500;
  background: #f6ffed; color: #52c41a;
  padding: 2px 10px; border-radius: 3px;
}
.overview-header-left .status-tag.disabled {
  background: #f5f5f5; color: #909399;
}
.overview-desc {
  font-size: 14px; color: #909399; margin-top: 4px;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.overview-header-right { text-align: right; flex-shrink: 0; padding-left: 20px; }
.last-exec { font-size: 12px; color: #909399; }
.last-exec a { color: #409eff; cursor: pointer; margin-left: 6px; }
.data-update-time { font-size: 11px; color: #c0c4cc; margin-top: 4px; }

/* Layer 2 */
.health-panel {
  display: flex; align-items: center; gap: 40px;
  margin-bottom: 16px; padding: 24px 28px;
  background: #fff; border-radius: 6px;
  border: 1px solid #f0f0f0;
  box-shadow: 0 1px 2px rgba(0,0,0,.03), 0 1px 6px -1px rgba(0,0,0,.02), 0 2px 4px rgba(0,0,0,.02);
}
.health-left {
  display: flex; flex-direction: column; align-items: center;
  flex-shrink: 0; min-width: 160px;
}
.health-circle-wrap { position: relative; width: 120px; height: 120px; }
.health-center {
  position: absolute; top: 50%; left: 50%;
  transform: translate(-50%, -50%); text-align: center;
  display: flex; flex-direction: column; align-items: center;
}
.health-num { font-size: 36px; font-weight: 800; line-height: 1; }
.health-grade { font-size: 12px; font-weight: 600; color: #909399; margin-top: 2px; }
.health-title { margin-top: 12px; font-size: 15px; font-weight: 600; color: #303133; }
.health-subtitle { font-size: 11px; color: #909399; margin-top: 2px; }
.health-right { flex: 1; display: flex; flex-direction: column; gap: 14px; }
.score-dim-header { display: flex; align-items: center; gap: 6px; margin-bottom: 6px; }
.score-dim-label { font-size: 13px; color: #303133; font-weight: 500; }
.score-dim-value { margin-left: auto; font-size: 14px; font-weight: 700; color: #303133; }
.score-bar { height: 6px; background: #f0f0f0; border-radius: 3px; overflow: hidden; }
.score-fill { height: 100%; border-radius: 3px; transition: width 0.4s ease; }
.help-icon {
  display: inline-flex; align-items: center; justify-content: center;
  width: 15px; height: 15px; border-radius: 50%;
  font-size: 10px; color: #c0c4cc; background: #f0f0f0;
  cursor: help; font-style: normal; line-height: 1;
}

/* Layer 3 */
.kpi-grid {
  display: grid; grid-template-columns: repeat(4, 1fr);
  gap: 14px; margin-bottom: 16px;
}
.kpi-card {
  background: #fff; border-radius: 6px;
  border: 1px solid #f0f0f0;
  box-shadow: 0 1px 2px rgba(0,0,0,.03), 0 1px 6px -1px rgba(0,0,0,.02), 0 2px 4px rgba(0,0,0,.02);
  padding: 16px 18px; display: flex; align-items: center; gap: 14px;
  transition: box-shadow 0.2s;
}
.kpi-card:hover { box-shadow: 0 2px 8px rgba(0,0,0,.08); }
.kpi-icon {
  width: 44px; height: 44px; border-radius: 10px;
  display: flex; align-items: center; justify-content: center;
  font-size: 20px; flex-shrink: 0;
}
.kpi-info { flex: 1; min-width: 0; }
.kpi-label { font-size: 12px; color: #909399; margin-bottom: 4px; display: flex; align-items: center; gap: 4px; }
.kpi-value { font-size: 24px; font-weight: 700; line-height: 1.2; color: #303133; }
.kpi-footer { font-size: 12px; color: #909399; margin-top: 4px; }

/* Layer 4 */
.trend-section {
  display: grid; grid-template-columns: 1fr 1fr;
  gap: 16px; margin-bottom: 16px;
}
.trend-side-col { display: flex; flex-direction: column; gap: 16px; }
.card {
  background: #fff; border-radius: 6px;
  border: 1px solid #f0f0f0;
  box-shadow: 0 1px 2px rgba(0,0,0,.03), 0 1px 6px -1px rgba(0,0,0,.02), 0 2px 4px rgba(0,0,0,.02);
}
.card-header {
  padding: 12px 16px; border-bottom: 1px solid #f0f0f0;
  display: flex; align-items: center; justify-content: space-between;
  font-size: 14px; font-weight: 600;
}
.card-body { padding: 16px; }
.trend-controls { display: flex; align-items: center; gap: 12px; }
.target-line { display: inline-flex; align-items: center; gap: 4px; font-size: 11px; color: #e6a23c; }
.target-dot { width: 8px; height: 2px; background: #e6a23c; border-radius: 1px; }

/* Layer 5 */
.coverage-risk-section {
  display: grid; grid-template-columns: 1fr 1fr;
  gap: 16px; margin-bottom: 16px;
}
.coverage-item {
  display: flex; align-items: center; gap: 12px;
  padding: 10px 0; border-bottom: 1px solid #f0f0f0;
}
.coverage-item:last-child { border-bottom: none; }
.cov-module { font-size: 13px; color: #303133; width: 120px; flex-shrink: 0; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; display: flex; align-items: center; gap: 4px; }
.cov-bar-wrap { flex: 1; height: 8px; background: #f0f0f0; border-radius: 4px; overflow: hidden; }
.cov-bar { height: 100%; border-radius: 4px; transition: width 0.3s ease; }
.cov-count { font-size: 12px; color: #909399; width: 32px; text-align: right; flex-shrink: 0; }
.cov-pct { font-size: 12px; font-weight: 500; width: 42px; text-align: right; flex-shrink: 0; }

.risk-item {
  display: flex; align-items: center; gap: 12px;
  padding: 10px 0; border-bottom: 1px solid #f0f0f0;
}
.risk-item:last-child { border-bottom: none; }
.risk-rank {
  width: 22px; height: 22px; border-radius: 4px;
  display: flex; align-items: center; justify-content: center;
  font-size: 12px; font-weight: 700; flex-shrink: 0;
}
.risk-rank.top { background: #fff2f0; color: #f56c6c; }
.risk-rank.normal { background: #f0f0f0; color: #909399; }
.risk-info { flex: 1; min-width: 0; }
.risk-name { font-size: 13px; color: #303133; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.risk-meta { font-size: 11px; color: #909399; margin-top: 2px; }
.risk-fail { font-size: 12px; font-weight: 600; color: #f56c6c; flex-shrink: 0; }
.risk-footer {
  margin-top: 12px; padding-top: 12px; border-top: 1px solid #f0f0f0;
  display: flex; align-items: center; gap: 8px;
  font-size: 13px; color: #606266;
}
.risk-count { font-weight: 600; color: #f56c6c; }

@media (max-width: 1200px) {
  .kpi-grid { grid-template-columns: repeat(2, 1fr); }
}
</style>
