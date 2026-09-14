<!--
 @author HXN
 @date 2026-08-23
 @description 自动化用例编辑视图
-->
<script setup lang="ts">
/**
 * 自动化用例编辑 - M8
 * Tab 布局：基础信息 / 步骤编排器 / 参数化
 * 对齐原型 case-edit.html
 */
import { reactive, ref, onMounted, computed, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getAutoCase, createAutoCase, updateAutoCase, debugAutoCase } from '@/api/autoCase'
import { getKeywords, getKeyword } from '@/api/keyword'
import { getActions } from '@/api/action'
import { getEnvironments } from '@/api/environment'
import { useDict } from '@/composables/useDict'
import { usePermission } from '@/composables/usePermission'
import { useRouteTab } from '@/composables/useRouteTab'
import PageHeader from '@/components/PageHeader/index.vue'
import { tryFormatJson, jsonSyntaxError } from '@/utils/jsonFormat'

const route = useRoute()
const router = useRouter()
const { hasPermission } = usePermission()
const projectId = computed(() => Number(route.params.id))
const autoCaseId = computed(() => Number(route.params.autoCaseId))
const queryAutoSuiteId = computed(() => Number(route.query.autoSuiteId) || 0)
const isEdit = computed(() => !!autoCaseId.value)
const { options: priorityOptions } = useDict('priority')

// ===== 关键字数据源 =====
const apiKeywords = ref<any[]>([])
const toolKeywords = ref<any[]>([])
const actionKeywords = ref<any[]>([])

async function loadKeywords() {
  if (!projectId.value) return
  try {
    const [kwRes, actRes]: any[] = await Promise.all([
      getKeywords(projectId.value, { pageSize: 1000 }),
      getActions(projectId.value, { pageSize: 1000 }),
    ])
    const allKw = (kwRes.data?.records || kwRes.data || []) as any[]
    apiKeywords.value = allKw.filter((k: any) => k.type === 'API')
    toolKeywords.value = allKw.filter((k: any) => k.type === 'TOOL')
    actionKeywords.value = (actRes.data?.records || actRes.data || []) as any[]
  } catch { /* ignore */ }
}

// ===== 表单 =====
const form = reactive({
  name: '',
  description: '',
  preconditions: '',
  setupSteps: '[]',
  teardownSteps: '[]',
  steps: '[]',
  priority: 'P1',
  timeout: 30,
  autoSuiteId: null as number | null,
  groupId: null as number | null,
  tags: '[]',
})

// ===== 标签管理 =====
const tagsList = ref<string[]>([])
const tagInputVisible = ref(false)
const tagInputValue = ref('')

function removeTag(index: number) {
  tagsList.value.splice(index, 1)
  syncTagsToJson()
}

function addTag() {
  const val = tagInputValue.value.trim()
  if (val && !tagsList.value.includes(val)) {
    tagsList.value.push(val)
    syncTagsToJson()
  }
  tagInputVisible.value = false
  tagInputValue.value = ''
}

function syncTagsToJson() {
  form.tags = JSON.stringify(tagsList.value)
}

function parseTagsFromJson() {
  try {
    const arr = JSON.parse(form.tags || '[]')
    tagsList.value = Array.isArray(arr) ? arr : []
  } catch { tagsList.value = [] }
}

// ===== Tab 管理（与 URL ?tab= 参数同步，刷新后停留在当前选项卡） =====
const activeTab = useRouteTab(['basic', 'orchestrator', 'params'], 'basic')

// ===== Setup/Teardown 步骤数据 =====
interface StepItem {
  stepType: string
  keywordType?: string
  keywordId?: number
  name: string
}

const setupSteps = ref<StepItem[]>([])
const teardownSteps = ref<StepItem[]>([])

function parseSetupTeardown() {
  try { setupSteps.value = JSON.parse(form.setupSteps || '[]') } catch { setupSteps.value = [] }
  try { teardownSteps.value = JSON.parse(form.teardownSteps || '[]') } catch { teardownSteps.value = [] }
}

function syncSetupTeardown() {
  form.setupSteps = JSON.stringify(setupSteps.value)
  form.teardownSteps = JSON.stringify(teardownSteps.value)
}

// ===== Setup/Teardown 关键字选择器 =====
const pickerState = reactive({
  visible: false,
  type: '' as 'setup' | 'teardown',
  tab: 'action' as 'api' | 'tool' | 'action' | 'logic',
  keyword: '',
})

const logicTypes = [
  { type: 'serial', icon: '&#9654;', color: '#67c23a', name: '串行执行', desc: '子节点按顺序依次执行' },
  { type: 'parallel', icon: '&#10744;', color: '#e6a23c', name: '并行执行', desc: '子节点同时执行' },
  { type: 'condition', icon: '?', color: '#722ed1', name: '条件判断', desc: '根据表达式判断分支' },
  { type: 'wait', icon: '&#9201;', color: '#f56c6c', name: '等待', desc: '固定等待指定时长' },
]

// ===== 步骤标签配置（对齐原型 step tag 渲染） =====
const keywordConfig: Record<string, { icon: string; color: string; tagType: any; label: string }> = {
  api: { icon: 'A', color: '#409eff', tagType: 'primary', label: 'API' },
  tool: { icon: 'T', color: '#e6a23c', tagType: 'warning', label: 'TOOL' },
  action: { icon: 'A', color: '#409eff', tagType: 'primary', label: 'Action关键字' },
}
const logicConfig: Record<string, { icon: string; color: string; tagType: any; label: string }> = {
  serial: { icon: '&#9654;', color: '#67c23a', tagType: 'success', label: '串行执行' },
  parallel: { icon: '&#10744;', color: '#e6a23c', tagType: 'warning', label: '并行执行' },
  condition: { icon: '?', color: '#722ed1', tagType: 'info', label: '条件判断' },
  wait: { icon: '&#9201;', color: '#f56c6c', tagType: 'danger', label: '等待' },
}

const pickerTabs = [
  { key: 'api' as const, label: '接口关键字' },
  { key: 'tool' as const, label: '工具方法' },
  { key: 'action' as const, label: 'Action关键字' },
  { key: 'logic' as const, label: '逻辑控制' },
]

function togglePicker(type: 'setup' | 'teardown') {
  if (pickerState.visible && pickerState.type === type) {
    pickerState.visible = false
    return
  }
  pickerState.visible = true
  pickerState.type = type
  pickerState.tab = 'action'
  pickerState.keyword = ''
}

function closePicker() {
  pickerState.visible = false
}

function switchPickerTab(tab: 'api' | 'tool' | 'action' | 'logic') {
  pickerState.tab = tab
  pickerState.keyword = ''
}

const pickerSearchPlaceholder = computed(() => {
  const labels: Record<string, string> = { api: '搜索接口...', tool: '搜索工具方法...', action: '搜索 Action关键字...' }
  return labels[pickerState.tab] || '搜索关键字...'
})

const filteredPickerKeywords = computed(() => {
  const tab = pickerState.tab
  const kw = pickerState.keyword.toLowerCase()
  let source: any[] = []
  if (tab === 'api') source = apiKeywords.value
  else if (tab === 'tool') source = toolKeywords.value
  else if (tab === 'action') source = actionKeywords.value
  else return []
  const steps = pickerState.type === 'setup' ? setupSteps.value : teardownSteps.value
  const usedIds = steps.filter((s) => s.stepType === 'keyword' && s.keywordType === tab).map((s) => s.keywordId)
  return source.filter((k: any) => {
    if (usedIds.includes(k.id)) return false
    if (kw && !k.name.toLowerCase().includes(kw) && !(k.description || '').toLowerCase().includes(kw)) return false
    return true
  })
})

// 按分组归类关键字（对齐原型 st-picker 分组展示）
const groupedPickerKeywords = computed(() => {
  const kws = filteredPickerKeywords.value
  const groupMap: Record<string, any[]> = {}
  const groupOrder: string[] = []
  for (const kw of kws) {
    const g = kw.group || kw.category || '未分组'
    if (!groupMap[g]) { groupMap[g] = []; groupOrder.push(g) }
    groupMap[g].push(kw)
  }
  return groupOrder.map(g => ({ name: g, items: groupMap[g] }))
})

function addKeywordStep(keywordType: string, keywordId: number, name: string) {
  const steps = pickerState.type === 'setup' ? setupSteps.value : teardownSteps.value
  steps.push({ stepType: 'keyword', keywordType, keywordId, name })
  syncSetupTeardown()
  closePicker()
}

function addLogicStep(logicType: string) {
  const lt = logicTypes.find((l) => l.type === logicType)
  if (!lt) return
  const targetSteps = pickerState.type === 'setup' ? setupSteps.value : teardownSteps.value
  const step: StepItem = { stepType: logicType, name: lt.name }
  if (logicType === 'wait') step.name = '等待 2000ms'
  if (logicType === 'condition') step.name = '条件判断'
  targetSteps.push(step)
  syncSetupTeardown()
  closePicker()
}

function removeSTStep(type: 'setup' | 'teardown', index: number) {
  const steps = type === 'setup' ? setupSteps.value : teardownSteps.value
  steps.splice(index, 1)
  syncSetupTeardown()
}

// 点击外部关闭内联选择器
function onDocClick(e: MouseEvent) {
  const target = e.target as HTMLElement
  if (!target.closest('.st-add-wrapper')) {
    closePicker()
  }
}

// ===== 步骤编排器（主画布）=====
const stepsArray = ref<any[]>([])
const selectedStepIndex = ref<number>(-1)
const stepMode = ref<'basic' | 'advanced'>('basic')

function parseStepsToArray() {
  try { stepsArray.value = JSON.parse(form.steps || '[]') } catch { stepsArray.value = [] }
}

function syncStepsToJson() {
  form.steps = JSON.stringify(stepsArray.value, null, 2)
}

function addCanvasStep() {
  stepsArray.value.push({ keywordId: null, name: '', params: {}, assertions: [] })
  syncStepsToJson()
}

function removeCanvasStep(index: number) {
  stepsArray.value.splice(index, 1)
  if (selectedStepIndex.value === index) selectedStepIndex.value = -1
  syncStepsToJson()
}

function selectCanvasStep(index: number) {
  selectedStepIndex.value = index
  syncStepParamRows()
  // 加载选中步骤的关键字默认参数
  const step = stepsArray.value[index]
  if (step?.keywordId) {
    loadStepKeywordDefaults(step.keywordId)
  } else {
    stepDefaultParams.value = []
  }
}

// ===== 属性面板 args 参数映射（键值表格式，对齐原型） =====
const stepParamRows = ref<{ key: string; value: string }[]>([])

function syncStepParamRows() {
  const step = stepsArray.value[selectedStepIndex.value]
  if (!step || !step.params || typeof step.params !== 'object' || Array.isArray(step.params)) {
    stepParamRows.value = []
    return
  }
  stepParamRows.value = Object.entries(step.params).map(([key, value]) => ({ key, value: String(value) }))
}

function commitStepParamRows() {
  const step = stepsArray.value[selectedStepIndex.value]
  if (!step) return
  const newParams: Record<string, string> = {}
  for (const row of stepParamRows.value) {
    if (row.key.trim()) newParams[row.key.trim()] = row.value
  }
  step.params = newParams
  syncStepsToJson()
}

function addStepParamRow() {
  stepParamRows.value.push({ key: '', value: '' })
}

function removeStepParamRow(index: number) {
  stepParamRows.value.splice(index, 1)
  commitStepParamRows()
}

// ===== 步骤关键字默认参数（只读参考） =====
const stepDefaultParams = ref<{ key: string; value: string }[]>([])

async function loadStepKeywordDefaults(kwId: number) {
  if (!kwId) { stepDefaultParams.value = []; return }
  try {
    const res: any = await getKeyword(projectId.value, kwId)
    const kw = res.data
    if (kw?.testData) {
      const arr = JSON.parse(kw.testData)
      if (Array.isArray(arr)) {
        stepDefaultParams.value = arr
          .filter((r: any) => r.name && r.name !== '__body__')
          .map((r: any) => ({ key: r.name, value: r.value || '' }))
        return
      }
    }
  } catch { /* ignore */ }
  stepDefaultParams.value = []
}

function onStepKeywordChange(kwId: number) {
  syncStepsToJson()
  if (kwId) {
    loadStepKeywordDefaults(kwId)
  } else {
    stepDefaultParams.value = []
  }
}

// 用默认值填充当前参数行（仅填充尚未定义的参数）
function fillStepFromDefaults() {
  const existingKeys = new Set(stepParamRows.value.map(r => r.key))
  for (const def of stepDefaultParams.value) {
    if (!existingKeys.has(def.key)) {
      stepParamRows.value.push({ key: def.key, value: def.value })
    }
  }
  commitStepParamRows()
}

// ===== 参数化 =====
const dataDriven = ref(false)
const paramMode = ref<'manual' | 'csv'>('manual')
const paramHeaders = ref<string[]>(['param1', 'param2', 'param3'])
const paramRows = ref<string[][]>([
  ['', '', ''],
])

function addParamRow() {
  paramRows.value.push(new Array(paramHeaders.value.length).fill(''))
}

function removeParamRow(index: number) {
  paramRows.value.splice(index, 1)
}

function addParamColumn() {
  const name = `param${paramHeaders.value.length + 1}`
  paramHeaders.value.push(name)
  paramRows.value.forEach((row) => row.push(''))
}

// ===== CSV 导入 =====
const csvFileName = ref('')

function handleCsvUpload(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return

  if (!file.name.toLowerCase().endsWith('.csv')) {
    ElMessage.warning('请选择 CSV 文件')
    return
  }

  csvFileName.value = file.name
  const reader = new FileReader()
  reader.onload = (e) => {
    try {
      const text = e.target?.result as string
      const lines = text.split(/\r?\n/).filter((line) => line.trim())
      if (lines.length === 0) {
        ElMessage.warning('CSV 文件为空')
        return
      }

      // 解析 CSV 行（支持引号包裹的字段）
      function parseCsvLine(line: string): string[] {
        const result: string[] = []
        let current = ''
        let inQuotes = false
        for (let i = 0; i < line.length; i++) {
          const ch = line[i]
          if (inQuotes) {
            if (ch === '"' && line[i + 1] === '"') { current += '"'; i++ }
            else if (ch === '"') { inQuotes = false }
            else { current += ch }
          } else {
            if (ch === '"') { inQuotes = true }
            else if (ch === ',') { result.push(current.trim()); current = '' }
            else { current += ch }
          }
        }
        result.push(current.trim())
        return result
      }

      // 第一行为表头
      const headers = parseCsvLine(lines[0])
      if (headers.length === 0 || headers.every((h) => !h)) {
        ElMessage.warning('CSV 表头为空')
        return
      }
      paramHeaders.value = headers

      // 其余行为数据
      const rows: string[][] = []
      for (let i = 1; i < lines.length; i++) {
        const values = parseCsvLine(lines[i])
        // 补齐列数
        while (values.length < headers.length) values.push('')
        rows.push(values.slice(0, headers.length))
      }
      paramRows.value = rows.length > 0 ? rows : [new Array(headers.length).fill('')]

      ElMessage.success(`CSV 导入成功：${headers.length} 列，${rows.length} 行数据`)
    } catch {
      ElMessage.error('CSV 文件解析失败')
    }
  }
  reader.readAsText(file, 'UTF-8')
  // 重置 input 以允许重复选择同一文件
  input.value = ''
}

// ===== 加载用例 =====
async function loadCase() {
  if (!autoCaseId.value) return
  try {
    const res: any = await getAutoCase(projectId.value, autoCaseId.value)
    const c = res.data
    Object.assign(form, {
      name: c.name || '',
      description: c.description || '',
      preconditions: c.preconditions || '',
      setupSteps: c.setupSteps || '[]',
      teardownSteps: c.teardownSteps || '[]',
      steps: c.steps || '[]',
      priority: c.priority || 'P1',
      timeout: c.timeout || 30,
      autoSuiteId: c.autoSuiteId || null,
      groupId: c.groupId || null,
      tags: c.tags || '[]',
    })
    parseTagsFromJson()
    parseStepsToArray()
    parseSetupTeardown()
  } catch { ElMessage.error('加载自动化用例失败') }
}

function initCreateMode() {
  form.name = ''
  form.description = ''
  form.preconditions = ''
  form.priority = 'P1'
  form.timeout = 30
  form.steps = '[]'
  form.setupSteps = '[]'
  form.teardownSteps = '[]'
  form.tags = '[]'
  tagsList.value = []
  stepsArray.value = []
  setupSteps.value = []
  teardownSteps.value = []
}

// ===== 格式化 =====
function formatJson(field: 'setupSteps' | 'teardownSteps' | 'steps') {
  const formatted = tryFormatJson((form as any)[field] || '[]')
  if (formatted === null) {
    ElMessage.warning(`JSON 语法错误：${jsonSyntaxError((form as any)[field] || '[]')}`)
    return
  }
  ;(form as any)[field] = formatted
}

function validateJson(): boolean {
  for (const f of ['setupSteps', 'teardownSteps', 'steps'] as const) {
    try { JSON.parse(form[f] || '[]') } catch { ElMessage.warning(`${f} 不是有效的 JSON`); return false }
  }
  return true
}

// ===== 保存 =====
async function handleSave() {
  if (!form.name) { ElMessage.warning('请输入自动化用例名称'); return }
  if (stepMode.value === 'basic') syncStepsToJson()
  syncSetupTeardown()
  syncTagsToJson()
  if (!validateJson()) return
  try {
    if (isEdit.value) {
      await updateAutoCase(projectId.value, autoCaseId.value, {
        name: form.name, description: form.description, preconditions: form.preconditions,
        setupSteps: form.setupSteps, teardownSteps: form.teardownSteps, steps: form.steps,
        priority: form.priority, timeout: form.timeout, groupId: form.groupId, tags: form.tags,
      })
      ElMessage.success('保存成功')
    } else {
      await createAutoCase(projectId.value, {
        ...form, autoSuiteId: queryAutoSuiteId.value || form.autoSuiteId,
      })
      ElMessage.success('创建成功')
      router.push(`/project/${projectId.value}/auto-cases?autoSuiteId=${queryAutoSuiteId.value}`)
    }
  } catch (e: any) { ElMessage.error(e?.response?.data?.message || '保存失败') }
}

// ===== ST 折叠 =====
const setupOpen = ref(true)
const teardownOpen = ref(false)

// ===== 调试弹窗 =====
const debugVisible = ref(false)
const debugLoading = ref(false)
const debugResult = ref<any>(null)
const debugEnvId = ref<number | undefined>(undefined)
const debugEnvs = ref<any[]>([])

async function openDebugModal() {
  if (!isEdit.value) { ElMessage.warning('请先保存自动化用例后再调试'); return }
  debugResult.value = null
  debugLoading.value = false
  debugEnvId.value = undefined
  debugVisible.value = true
  try {
    const res: any = await getEnvironments(projectId.value)
    debugEnvs.value = res.data || []
  } catch { debugEnvs.value = [] }
}

async function handleRunDebug() {
  debugLoading.value = true
  debugResult.value = null
  try {
    const res: any = await debugAutoCase(projectId.value, autoCaseId.value, { environmentId: debugEnvId.value })
    debugResult.value = res.data
  } catch (e: any) {
    debugResult.value = { status: 'ERROR', message: e?.response?.data?.message || '调试执行失败', stepLogs: [] }
  } finally {
    debugLoading.value = false
  }
}

function debugStatusType(status: string) {
  if (status === 'PASSED') return 'success'
  if (status === 'FAILED' || status === 'ERROR') return 'danger'
  return 'info'
}

// ===== 生命周期 =====
onMounted(() => {
  loadKeywords()
  if (isEdit.value) {
    loadCase()
  } else {
    initCreateMode()
  }
  document.addEventListener('click', onDocClick)
})

onBeforeUnmount(() => {
  document.removeEventListener('click', onDocClick)
})
</script>

<template>
  <div>
    <PageHeader :title="isEdit ? '编辑自动化用例' : '新建自动化用例'">
      <el-button v-if="isEdit" @click="openDebugModal">调试</el-button>
      <el-button v-if="hasPermission('project:auto-case:edit')" type="primary" @click="handleSave">{{ isEdit ? '保存' : '创建' }}</el-button>
    </PageHeader>

    <el-tabs v-model="activeTab" type="card">
      <!-- ====== Tab: 基础信息 ====== -->
      <el-tab-pane label="基础信息" name="basic">
        <el-card style="margin-bottom:12px">
          <el-row :gutter="20">
            <el-col :span="8">
              <el-form-item label="自动化用例名称" required>
                <el-input v-model="form.name" placeholder="请输入自动化用例名称" />
              </el-form-item>
            </el-col>
            <el-col :span="4">
              <el-form-item label="优先级">
                <el-select v-model="form.priority" style="width:100%">
                  <el-option v-for="p in priorityOptions" :key="p.value" :value="p.value" :label="p.label" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="标签">
                <div style="display:flex;gap:4px;flex-wrap:wrap;align-items:center">
                  <el-tag
                    v-for="(tag, i) in tagsList" :key="i" closable size="default"
                    @close="removeTag(i)"
                  >{{ tag }}</el-tag>
                  <el-input
                    v-if="tagInputVisible"
                    v-model="tagInputValue"
                    size="small" style="width:80px"
                    @keyup.enter="addTag" @blur="addTag"
                  />
                  <el-button v-else size="small" @click="tagInputVisible = true">+</el-button>
                </div>
              </el-form-item>
            </el-col>
          </el-row>
          <el-row>
            <el-col :span="24">
              <el-form-item label="前置条件">
                <el-input v-model="form.preconditions" placeholder="可选，描述自动化用例执行的前置条件" />
              </el-form-item>
            </el-col>
          </el-row>
        </el-card>
      </el-tab-pane>

      <!-- ====== Tab: 步骤编排器 ====== -->
      <el-tab-pane label="步骤编排器" name="orchestrator">
        <el-card style="margin-bottom:12px">
          <template #header>
            <div style="display:flex;justify-content:space-between;align-items:center">
              <span>步骤编排器</span>
              <el-radio-group v-model="stepMode" size="small" @change="stepMode === 'basic' ? parseStepsToArray() : syncStepsToJson()">
                <el-radio-button value="basic">基础模式</el-radio-button>
                <el-radio-button value="advanced">高级模式</el-radio-button>
              </el-radio-group>
            </div>
          </template>

          <!-- Test Setup -->
          <div class="st-section">
            <div class="st-section-header" @click="setupOpen = !setupOpen">
              <span>
                <span style="color:#67c23a;margin-right:4px">&#9654;</span>
                Test Setup
                <span style="font-size:12px;color:#909399;font-weight:400;margin-left:8px">自动化用例执行前调用的关键字与逻辑控制序列</span>
              </span>
              <span class="st-arrow" :style="{ transform: setupOpen ? 'rotate(90deg)' : '' }">&#9654;</span>
            </div>
            <div v-show="setupOpen" class="st-section-body">
              <div class="st-tags-row">
                <span v-for="(step, idx) in setupSteps" :key="idx" class="st-step-tag">
                  <template v-if="step.stepType === 'keyword'">
                    <span :style="{ color: keywordConfig[step.keywordType ?? '']?.color, fontWeight: 600 }">{{ keywordConfig[step.keywordType ?? '']?.icon }}</span>
                    {{ step.name }}
                    <el-tag :type="keywordConfig[step.keywordType ?? '']?.tagType" size="small" style="font-size:9px;margin:0;">{{ keywordConfig[step.keywordType ?? '']?.label }}</el-tag>
                  </template>
                  <template v-else>
                    <span :style="{ color: logicConfig[step.stepType]?.color, fontWeight: 600 }" v-html="logicConfig[step.stepType]?.icon"></span>
                    {{ step.name }}
                    <el-tag :type="logicConfig[step.stepType]?.tagType" size="small" style="font-size:9px;margin:0;">{{ logicConfig[step.stepType]?.label }}</el-tag>
                  </template>
                  <span class="st-remove" @click="removeSTStep('setup', idx)">&#10005;</span>
                </span>
                <span v-if="setupSteps.length === 0" class="st-empty">暂无 Setup 步骤，请点击下方按钮添加关键字或逻辑控制</span>
                <!-- 内联选择器 -->
                <div class="st-add-wrapper">
                  <button class="st-add-btn" @click="togglePicker('setup')">+ 添加步骤</button>
                  <div v-show="pickerState.visible && pickerState.type === 'setup'" class="st-picker">
                    <div class="st-picker-tabs">
                      <div v-for="tab in pickerTabs" :key="tab.key"
                           :class="['st-picker-tab', { active: pickerState.tab === tab.key }]"
                           @click="switchPickerTab(tab.key)">{{ tab.label }}</div>
                    </div>
                    <div v-if="pickerState.tab !== 'logic'" class="st-picker-search">
                      <el-input v-model="pickerState.keyword" size="small" :placeholder="pickerSearchPlaceholder" clearable />
                    </div>
                    <div class="st-picker-body">
                      <template v-if="pickerState.tab !== 'logic'">
                        <div v-if="groupedPickerKeywords.length === 0" class="st-picker-empty">无匹配的关键字</div>
                        <template v-for="group in groupedPickerKeywords" :key="group.name">
                          <div class="st-picker-group">{{ group.name }}</div>
                          <div v-for="kw in group.items" :key="kw.id" class="st-picker-item"
                               @click="addKeywordStep(pickerState.tab, kw.id, kw.name)">
                            <span class="pi-icon">{{ pickerState.tab === 'api' ? 'A' : pickerState.tab === 'tool' ? 'T' : 'A' }}</span>
                            <span class="pi-name">{{ kw.name }}</span>
                            <span class="pi-desc">{{ kw.description || '' }}</span>
                          </div>
                        </template>
                      </template>
                      <template v-else>
                        <div v-for="lt in logicTypes" :key="lt.type" class="st-picker-item" @click="addLogicStep(lt.type)">
                          <span class="pi-icon" :style="{ background: lt.color + '15', color: lt.color }" v-html="lt.icon"></span>
                          <span class="pi-name">{{ lt.name }}</span>
                          <span class="pi-desc">{{ lt.desc }}</span>
                        </div>
                      </template>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 主编排器 -->
          <div v-if="stepMode === 'basic'" class="orchestrator">
            <!-- 左侧步骤类型 -->
            <div class="node-panel">
              <h4 style="font-size:12px;color:#909399;margin-bottom:8px">步骤类型</h4>
              <div class="node-drag" @click="addCanvasStep()">
                <div class="nd-icon" style="background:#ecf5ff;color:#409eff">K</div> 关键字步骤
              </div>
              <div class="node-drag">
                <div class="nd-icon" style="background:#f0f9eb;color:#67c23a">&#9654;</div> 串行步骤
              </div>
              <div class="node-drag">
                <div class="nd-icon" style="background:#fdf6ec;color:#e6a23c">&#10744;</div> 并行步骤
              </div>
              <div class="node-drag">
                <div class="nd-icon" style="background:#f4ecff;color:#722ed1">?</div> 条件步骤
              </div>
              <div class="node-drag">
                <div class="nd-icon" style="background:#fef0f0;color:#f56c6c">&#9201;</div> 等待步骤
              </div>
            </div>

            <!-- 中间画布 -->
            <div class="canvas">
              <div v-if="stepsArray.length === 0" style="text-align:center;color:#c0c4cc;padding:80px 20px;font-size:13px">
                点击左侧步骤类型添加到编排画布
              </div>
              <div
                v-for="(step, idx) in stepsArray" :key="idx"
                :class="['step-node', { selected: selectedStepIndex === idx }]"
                @click="selectCanvasStep(idx)"
              >
                <span style="color:#409eff">K</span>
                <span style="flex:1">{{ step.name || `步骤 ${idx + 1}` }}</span>
                <el-tag size="small" type="info" style="margin-left:auto;font-size:10px">关键字</el-tag>
                <el-button type="danger" link size="small" style="margin-left:8px" @click.stop="removeCanvasStep(idx)">删除</el-button>
              </div>
            </div>

            <!-- 右侧属性面板 -->
            <div class="prop-panel">
              <template v-if="selectedStepIndex >= 0 && stepsArray[selectedStepIndex]">
                <h4 style="font-size:12px;font-weight:600;margin-bottom:8px">
                  步骤属性：{{ stepsArray[selectedStepIndex].name || `步骤 ${selectedStepIndex + 1}` }}
                </h4>
                <el-form label-position="top" size="small">
                  <el-form-item label="步骤名称">
                    <el-input v-model="stepsArray[selectedStepIndex].name" @input="syncStepsToJson" />
                  </el-form-item>
                  <el-form-item label="关键字">
                    <el-select v-model="stepsArray[selectedStepIndex].keywordId" placeholder="选择关键字" filterable style="width:100%" @change="onStepKeywordChange">
                      <el-option v-for="kw in [...apiKeywords, ...toolKeywords, ...actionKeywords]" :key="kw.id" :value="kw.id" :label="kw.name" />
                    </el-select>
                  </el-form-item>

                  <!-- 接口关键字默认参数（只读参考） -->
                  <div v-if="stepDefaultParams.length" class="param-defaults">
                    <div class="param-defaults-header">
                      <span>接口默认参数</span>
                      <el-button link size="small" @click="fillStepFromDefaults">填充为传参</el-button>
                    </div>
                    <div v-for="(row, ri) in stepDefaultParams" :key="ri" class="param-default-row">
                      <span class="pd-key">{{ row.key }}</span>
                      <span class="pd-value">{{ row.value || '(空)' }}</span>
                    </div>
                  </div>

                  <el-form-item label="args 参数映射">
                    <div class="param-mapping">
                      <div v-for="(row, ri) in stepParamRows" :key="ri" class="param-mapping-row">
                        <el-input v-model="row.key" size="small" placeholder="参数名" style="width:80px;font-size:11px;" @input="commitStepParamRows" />
                        <el-input v-model="row.value" size="small" placeholder="参数值" style="flex:1;font-size:11px;" @input="commitStepParamRows" />
                        <el-button type="danger" link size="small" @click="removeStepParamRow(ri)">删除</el-button>
                      </div>
                      <el-button size="small" link @click="addStepParamRow">+ 添加参数</el-button>
                    </div>
                    <div style="color: #909399; font-size: 11px; margin-top: 4px">
                      参数名需与关键字内 $ref{参数名} 接收点或路径 {参数名} 占位符对应；值支持 ${变量名} 引用上下文变量
                    </div>
                  </el-form-item>
                  <el-form-item label="save_as">
                    <el-input v-model="stepsArray[selectedStepIndex].saveAs" placeholder="变量名" @input="syncStepsToJson" />
                  </el-form-item>
                </el-form>
                <el-divider style="margin:8px 0" />
                <h4 style="font-size:12px;font-weight:600;margin-bottom:8px">校验配置</h4>
                <el-form label-position="top" size="small">
                  <el-form-item label="状态码断言">
                    <el-input v-model="stepsArray[selectedStepIndex].statusCode" placeholder="如 200" @input="syncStepsToJson" />
                  </el-form-item>
                </el-form>
              </template>
              <div v-else style="text-align:center;color:#c0c4cc;padding:40px 10px;font-size:12px">
                选中步骤后在此处配置属性
              </div>
            </div>
          </div>

          <!-- 高级模式：JSON 编辑器 -->
          <div v-else>
            <div style="color:#909399;font-size:12px;margin-bottom:6px">
              JSON 数组格式，每个元素为一个步骤对象
            </div>
            <el-input v-model="form.steps" type="textarea" :rows="20" style="font-family:monospace;font-size:12px" />
            <el-button size="small" style="margin-top:4px" @click="formatJson('steps')">格式化</el-button>
          </div>

          <!-- Test Teardown -->
          <div class="st-section" style="border-top:1px solid #ebeef5">
            <div class="st-section-header" @click="teardownOpen = !teardownOpen">
              <span>
                <span style="color:#f56c6c;margin-right:4px">&#9632;</span>
                Test Teardown
                <span style="font-size:12px;color:#909399;font-weight:400;margin-left:8px">自动化用例执行后调用的关键字与逻辑控制序列</span>
              </span>
              <span class="st-arrow" :style="{ transform: teardownOpen ? 'rotate(90deg)' : '' }">&#9654;</span>
            </div>
            <div v-show="teardownOpen" class="st-section-body">
              <div class="st-tags-row">
                <span v-for="(step, idx) in teardownSteps" :key="idx" class="st-step-tag">
                  <template v-if="step.stepType === 'keyword'">
                    <span :style="{ color: keywordConfig[step.keywordType ?? '']?.color, fontWeight: 600 }">{{ keywordConfig[step.keywordType ?? '']?.icon }}</span>
                    {{ step.name }}
                    <el-tag :type="keywordConfig[step.keywordType ?? '']?.tagType" size="small" style="font-size:9px;margin:0;">{{ keywordConfig[step.keywordType ?? '']?.label }}</el-tag>
                  </template>
                  <template v-else>
                    <span :style="{ color: logicConfig[step.stepType]?.color, fontWeight: 600 }" v-html="logicConfig[step.stepType]?.icon"></span>
                    {{ step.name }}
                    <el-tag :type="logicConfig[step.stepType]?.tagType" size="small" style="font-size:9px;margin:0;">{{ logicConfig[step.stepType]?.label }}</el-tag>
                  </template>
                  <span class="st-remove" @click="removeSTStep('teardown', idx)">&#10005;</span>
                </span>
                <span v-if="teardownSteps.length === 0" class="st-empty">暂无 Teardown 步骤，请点击下方按钮添加关键字或逻辑控制</span>
                <div class="st-add-wrapper">
                  <button class="st-add-btn" @click="togglePicker('teardown')">+ 添加步骤</button>
                  <div v-show="pickerState.visible && pickerState.type === 'teardown'" class="st-picker">
                    <div class="st-picker-tabs">
                      <div v-for="tab in pickerTabs" :key="tab.key"
                           :class="['st-picker-tab', { active: pickerState.tab === tab.key }]"
                           @click="switchPickerTab(tab.key)">{{ tab.label }}</div>
                    </div>
                    <div v-if="pickerState.tab !== 'logic'" class="st-picker-search">
                      <el-input v-model="pickerState.keyword" size="small" :placeholder="pickerSearchPlaceholder" clearable />
                    </div>
                    <div class="st-picker-body">
                      <template v-if="pickerState.tab !== 'logic'">
                        <div v-if="groupedPickerKeywords.length === 0" class="st-picker-empty">无匹配的关键字</div>
                        <template v-for="group in groupedPickerKeywords" :key="group.name">
                          <div class="st-picker-group">{{ group.name }}</div>
                          <div v-for="kw in group.items" :key="kw.id" class="st-picker-item"
                               @click="addKeywordStep(pickerState.tab, kw.id, kw.name)">
                            <span class="pi-icon">{{ pickerState.tab === 'api' ? 'A' : pickerState.tab === 'tool' ? 'T' : 'A' }}</span>
                            <span class="pi-name">{{ kw.name }}</span>
                            <span class="pi-desc">{{ kw.description || '' }}</span>
                          </div>
                        </template>
                      </template>
                      <template v-else>
                        <div v-for="lt in logicTypes" :key="lt.type" class="st-picker-item" @click="addLogicStep(lt.type)">
                          <span class="pi-icon" :style="{ background: lt.color + '15', color: lt.color }" v-html="lt.icon"></span>
                          <span class="pi-name">{{ lt.name }}</span>
                          <span class="pi-desc">{{ lt.desc }}</span>
                        </div>
                      </template>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </el-card>
      </el-tab-pane>

      <!-- ====== Tab: 参数化 ====== -->
      <el-tab-pane label="参数化" name="params">
        <el-card>
          <template #header>
            <div style="display:flex;align-items:center;gap:12px">
              <span>参数化</span>
              <span style="display:flex;align-items:center;gap:6px;font-size:12px;font-weight:400;color:#606266">
                数据驱动：<el-switch v-model="dataDriven" size="small" />
              </span>
            </div>
          </template>
          <div style="display:flex;gap:8px;margin-bottom:12px">
            <el-radio-group v-model="paramMode" size="small">
              <el-radio-button value="manual">手动输入</el-radio-button>
              <el-radio-button value="csv">CSV 导入</el-radio-button>
            </el-radio-group>
          </div>
          <div v-if="paramMode === 'manual'">
            <el-table :data="paramRows" border size="small" style="width:100%">
              <el-table-column label="#" width="50">
                <template #default="{ $index }">{{ $index + 1 }}</template>
              </el-table-column>
              <el-table-column v-for="(header, ci) in paramHeaders" :key="ci" :label="header" min-width="150">
                <template #default="{ row }">
                  <el-input v-model="row[ci]" size="small" style="font-size:12px" />
                </template>
              </el-table-column>
              <el-table-column label="操作" width="60">
                <template #default="{ $index }">
                  <el-button type="danger" link size="small" @click="removeParamRow($index)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
            <div style="display:flex;gap:8px;margin-top:8px">
              <el-button size="small" @click="addParamRow">+ 添加数据行</el-button>
              <el-button size="small" @click="addParamColumn">+ 添加参数列</el-button>
            </div>
          </div>
          <div v-else>
            <div style="display:flex;align-items:center;gap:12px;margin-bottom:12px">
              <el-upload
                :auto-upload="false"
                :show-file-list="false"
                accept=".csv"
                @change="handleCsvUpload"
              >
                <el-button size="small" type="primary">选择 CSV 文件</el-button>
              </el-upload>
              <span v-if="csvFileName" style="font-size:12px;color:#606266">
                已导入：<strong>{{ csvFileName }}</strong>（{{ paramRows.length }} 行数据）
              </span>
            </div>
            <div v-if="csvFileName" style="margin-bottom:8px;font-size:12px;color:#909399">
              导入的数据已填充到下方表格，可手动编辑后保存。
              <el-button link type="primary" size="small" @click="paramMode = 'manual'">切换到手动编辑</el-button>
            </div>
            <div v-if="!csvFileName" style="text-align:center;padding:32px;color:#c0c4cc">
              <div style="font-size:28px;margin-bottom:8px;opacity:0.4">📄</div>
              <div>请选择 CSV 文件导入参数化数据</div>
              <div style="font-size:12px;margin-top:4px">CSV 第一行为参数名（表头），后续行为数据值</div>
            </div>
          </div>
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <!-- 自动化用例调试弹窗 -->
    <el-dialog v-model="debugVisible" title="自动化用例调试" width="720px" destroy-on-close>
      <div style="display: flex; gap: 12px; align-items: center; margin-bottom: 16px">
        <span style="font-size: 13px; color: #606266">执行环境：</span>
        <el-select v-model="debugEnvId" placeholder="不选择环境" clearable style="width: 200px" size="small">
          <el-option v-for="env in debugEnvs" :key="env.id" :value="env.id" :label="env.name" />
        </el-select>
        <el-button type="primary" size="small" :loading="debugLoading" @click="handleRunDebug">
          {{ debugLoading ? '执行中...' : '执行调试' }}
        </el-button>
      </div>
      <div v-if="debugResult">
        <el-divider style="margin: 8px 0" />
        <div style="display: flex; gap: 12px; align-items: center; margin-bottom: 12px">
          <el-tag :type="debugStatusType(debugResult.status) as any" size="large">{{ debugResult.status }}</el-tag>
          <span style="color: #606266">{{ debugResult.message }}</span>
          <span style="margin-left: auto; color: #909399; font-size: 13px">耗时：{{ debugResult.durationMs }}ms</span>
        </div>
        <div v-if="debugResult.stepLogs && debugResult.stepLogs.length" style="max-height: 300px; overflow-y: auto">
          <el-table :data="debugResult.stepLogs" border size="small" style="width: 100%">
            <el-table-column prop="stepName" label="步骤名称" min-width="150" show-overflow-tooltip />
            <el-table-column prop="phase" label="阶段" width="80" />
            <el-table-column label="状态" width="80">
              <template #default="{ row }">
                <el-tag :type="debugStatusType(row.status) as any" size="small">{{ row.status }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="message" label="消息" min-width="150" show-overflow-tooltip />
            <el-table-column prop="durationMs" label="耗时" width="80" />
          </el-table>
        </div>
        <div v-else-if="debugResult.status !== 'ERROR'" style="text-align: center; color: #c0c4cc; padding: 20px">无步骤日志</div>
      </div>
      <template #footer>
        <el-button @click="debugVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
/* ST 折叠区域 */
.st-section {
  background: #fafbfc;
  border-bottom: 1px solid #ebeef5;
}
.st-section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  cursor: pointer;
  font-size: 13px;
  font-weight: 500;
  color: #606266;
  user-select: none;
}
.st-section-header:hover { background: #f5f7fa; }
.st-arrow {
  font-size: 10px;
  color: #c0c4cc;
  transition: transform 0.2s;
  display: inline-block;
}
.st-section-body {
  padding: 8px 12px 10px;
  border-top: 1px solid #ebeef5;
}
.st-tags-row {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}
.st-step-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 3px 8px;
  background: #fff;
  border: 1px solid #dcdfe6;
  border-radius: 3px;
  font-size: 12px;
  cursor: default;
}
.st-step-tag .st-remove {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 14px;
  height: 14px;
  border-radius: 50%;
  font-size: 9px;
  color: #c0c4cc;
  cursor: pointer;
  margin-left: 4px;
  transition: all 0.15s;
}
.st-step-tag .st-remove:hover { background: #f56c6c; color: #fff; }
.st-empty { font-size: 12px; color: #c0c4cc; }

/* 编排器三栏 */
.orchestrator {
  display: flex;
  height: 400px;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  overflow: hidden;
  margin: 8px 0;
}
.node-panel {
  width: 170px;
  background: #fafafa;
  border-right: 1px solid #ebeef5;
  padding: 10px;
  overflow-y: auto;
}
.node-drag {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 7px 10px;
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  margin-bottom: 5px;
  cursor: pointer;
  font-size: 12px;
  transition: border-color 0.15s;
}
.node-drag:hover { border-color: #409eff; }
.nd-icon {
  width: 20px;
  height: 20px;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 10px;
  flex-shrink: 0;
}
.canvas {
  flex: 1;
  background: #fff;
  padding: 12px;
  overflow: auto;
}
.step-node {
  border: 1px solid #ebeef5;
  border-radius: 4px;
  padding: 8px 10px;
  margin-bottom: 5px;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  transition: all 0.15s;
}
.step-node:hover { border-color: #409eff; background: #ecf5ff; }
.step-node.selected { border-color: #409eff; background: #ecf5ff; }
.prop-panel {
  width: 260px;
  border-left: 1px solid #ebeef5;
  background: #fff;
  overflow-y: auto;
  padding: 10px;
}
.prop-panel h4 { font-size: 12px; font-weight: 600; margin-bottom: 8px; }
.prop-panel :deep(.el-form-item) { margin-bottom: 8px; }
.prop-panel :deep(.el-form-item__label) { font-size: 11px; margin-bottom: 2px; }

/* 内联关键字选择器（对齐原型 st-picker） */
.st-add-wrapper { position: relative; display: inline-flex; }
.st-add-btn {
  display: inline-flex; align-items: center; gap: 4px;
  padding: 3px 10px; background: #fff;
  border: 1px dashed #409eff; border-radius: 3px;
  font-size: 12px; color: #409eff; cursor: pointer;
  transition: all 0.15s; white-space: nowrap;
}
.st-add-btn:hover { background: #ecf5ff; }
.st-picker {
  position: absolute; top: calc(100% + 4px); left: 0;
  width: 280px; background: #fff;
  border: 1px solid #dcdfe6; border-radius: 4px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  z-index: 200; overflow: hidden;
}
.st-picker-tabs { display: flex; border-bottom: 1px solid #ebeef5; }
.st-picker-tab {
  padding: 6px 12px; font-size: 11px; color: #909399;
  cursor: pointer; border-bottom: 2px solid transparent;
  transition: all 0.15s; white-space: nowrap;
}
.st-picker-tab:hover { color: #303133; }
.st-picker-tab.active { color: #409eff; border-bottom-color: #409eff; font-weight: 500; }
.st-picker-search { padding: 8px; border-bottom: 1px solid #ebeef5; }
.st-picker-body { max-height: 220px; overflow-y: auto; }
.st-picker-group { padding: 4px 8px 2px; font-size: 10px; color: #909399; font-weight: 600; text-transform: uppercase; letter-spacing: 0.5px; }
.st-picker-item {
  display: flex; align-items: center; gap: 6px;
  padding: 6px 12px; font-size: 12px; cursor: pointer;
  transition: background 0.1s;
}
.st-picker-item:hover { background: #ecf5ff; }
.st-picker-empty { padding: 16px; text-align: center; font-size: 12px; color: #c0c4cc; }
.pi-icon {
  width: 18px; height: 18px; border-radius: 3px;
  background: #ecf5ff; color: #409eff;
  display: flex; align-items: center; justify-content: center;
  font-size: 9px; font-weight: 700; flex-shrink: 0;
}
.pi-name { flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.pi-desc { font-size: 10px; color: #c0c4cc; max-width: 100px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

/* args 参数映射表 */
.param-mapping { display: flex; flex-direction: column; gap: 4px; }
.param-mapping-row { display: flex; gap: 4px; align-items: center; }

/* 接口默认参数只读区域 */
.param-defaults {
  background: #f5f7fa;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  padding: 8px;
  margin-bottom: 8px;
}
.param-defaults-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 11px;
  font-weight: 600;
  color: #606266;
  margin-bottom: 6px;
}
.param-default-row {
  display: flex;
  gap: 8px;
  font-size: 11px;
  padding: 2px 0;
}
.pd-key {
  font-family: monospace;
  color: #e6a23c;
  font-weight: 500;
  min-width: 70px;
}
.pd-value {
  color: #909399;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
