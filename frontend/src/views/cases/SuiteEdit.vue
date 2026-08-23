<!--
 @author HXN
 @date 2026-08-23 10:00
 @description 测试套件编辑视图（含三面板 Setup/Teardown 编排器）
-->
<script setup lang="ts">
/**
 * 测试套件编辑 - M8
 * 基本信息 + 三面板 Setup/Teardown 可视化编排器
 * 对齐原型 suite-st-edit.html
 */
import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getSuite, updateSuite, getSuiteLifecycle, saveSuiteLifecycle } from '@/api/suite'
import { getCases } from '@/api/case'
import { useDict } from '@/composables/useDict'
import { usePermission } from '@/composables/usePermission'

const route = useRoute()
const router = useRouter()
const { hasPermission } = usePermission()
const projectId = computed(() => Number(route.params.id))
const suiteId = computed(() => Number(route.params.suiteId))

const loading = ref(false)
const saving = ref(false)
const { options: priorityOptions } = useDict('priority')
const activeTab = ref<'setup' | 'teardown'>('setup')

// 套件内用例列表和生命周期配置
const suiteCases = ref<any[]>([])
const lifecycleItems = ref<any[]>([])

const form = reactive({
  name: '',
  description: '',
  priority: 'P2',
  enableOnceSetupTeardown: 0,
  enablePerCaseSetupTeardown: 0,
})

// ===== 编排器数据结构 =====
interface StepNode {
  type: 'keyword' | 'wait' | 'condition'
  kwType?: 'api' | 'tool' | 'action'
  id?: number
  name: string
  duration?: number
  expr?: string
}

interface SetupTeardownData {
  type: 'serial'
  name: string
  children: StepNode[]
}

const setupData = reactive<SetupTeardownData>({ type: 'serial', name: '串行执行', children: [] })
const teardownData = reactive<SetupTeardownData>({ type: 'serial', name: '串行执行', children: [] })
const selectedIndex = reactive<{ setup: number | null; teardown: number | null }>({ setup: null, teardown: null })

// 节点类型配置
const kwConfigs: Record<string, { icon: string; color: string; bg: string; badge: string; label: string }> = {
  api: { icon: 'A', color: '#1890ff', bg: '#e6f7ff', badge: '', label: 'API' },
  tool: { icon: 'T', color: '#fa8c16', bg: '#fff7e6', badge: 'warning', label: 'TOOL' },
  action: { icon: 'A', color: '#1890ff', bg: '#e6f7ff', badge: '', label: 'Action' },
}
const logicConfigs: Record<string, { icon: string; color: string; bg: string; badge: string; label: string }> = {
  wait: { icon: '⏱', color: '#f5222d', bg: '#fff1f0', badge: 'danger', label: '等待' },
  condition: { icon: '?', color: '#722ed1', bg: '#f9f0ff', badge: '', label: '条件' },
}

// 示例关键字（后续可从后端加载）
const sampleKw: Record<string, { id: number; name: string }[]> = {
  api: [{ id: 101, name: '用户登录接口' }, { id: 102, name: '查询电池状态' }],
  tool: [{ id: 201, name: '等待指定毫秒' }, { id: 203, name: '断言校验' }],
  action: [{ id: 1, name: '用户登录并获取令牌' }, { id: 5, name: '初始化测试仓位' }, { id: 8, name: '环境初始化检查' }],
}

function getData(tab: 'setup' | 'teardown') {
  return tab === 'setup' ? setupData : teardownData
}

function getSelectedIndex(tab: 'setup' | 'teardown') {
  return selectedIndex[tab]
}

function selectNode(tab: 'setup' | 'teardown', idx: number | null) {
  selectedIndex[tab] = idx
}

function addNode(tab: 'setup' | 'teardown', type: string) {
  const data = getData(tab)
  if (type === 'api' || type === 'tool' || type === 'action') {
    const kws = sampleKw[type] || []
    const usedIds = data.children.filter(c => c.type === 'keyword' && c.kwType === type).map(c => c.id)
    const kw = kws.find(k => !usedIds.includes(k.id)) || kws[0]
    if (kw) {
      data.children.push({ type: 'keyword', kwType: type, id: kw.id, name: kw.name })
      selectedIndex[tab] = data.children.length - 1
    }
  } else if (type === 'wait') {
    data.children.push({ type: 'wait', name: '等待 2000ms', duration: 2000 })
    selectedIndex[tab] = data.children.length - 1
  } else if (type === 'condition') {
    data.children.push({ type: 'condition', name: '条件判断', expr: '${var} == "value"' })
    selectedIndex[tab] = data.children.length - 1
  }
}

function deleteNode(tab: 'setup' | 'teardown', idx: number) {
  const data = getData(tab)
  data.children.splice(idx, 1)
  if (selectedIndex[tab] === idx) {
    selectedIndex[tab] = null
  } else if (selectedIndex[tab] !== null && selectedIndex[tab] > idx) {
    selectedIndex[tab]!--
  }
}

function moveNode(tab: 'setup' | 'teardown', idx: number, direction: 'up' | 'down') {
  const data = getData(tab)
  const target = direction === 'up' ? idx - 1 : idx + 1
  if (target < 0 || target >= data.children.length) return
  const temp = data.children[idx]
  data.children[idx] = data.children[target]
  data.children[target] = temp
  if (selectedIndex[tab] === idx) selectedIndex[tab] = target
  else if (selectedIndex[tab] === target) selectedIndex[tab] = idx
}

// 属性面板数据
const selectedNode = computed(() => {
  const tab = activeTab.value
  const idx = selectedIndex[tab]
  if (idx === null || idx === undefined) return null
  return getData(tab).children[idx] || null
})

function updateNodeProp(prop: string, value: any) {
  const tab = activeTab.value
  const idx = selectedIndex[tab]
  if (idx === null || idx === undefined) return
  const node = getData(tab).children[idx]
  if (!node) return
  if (prop === 'duration') {
    node.duration = parseInt(value) || 2000
    node.name = `等待 ${node.duration}ms`
  } else if (prop === 'expr') {
    node.expr = value
  }
}

// ===== 数据加载 =====
async function loadSuite() {
  loading.value = true
  try {
    const res: any = await getSuite(projectId.value, suiteId.value)
    const s = res.data
    Object.assign(form, {
      name: s.name || '',
      description: s.description || '',
      priority: s.priority || 'P2',
      enableOnceSetupTeardown: s.enableOnceSetupTeardown ?? 0,
      enablePerCaseSetupTeardown: s.enablePerCaseSetupTeardown ?? 0,
    })
    // 解析步骤树 JSON
    try {
      const setupSteps = JSON.parse(s.onceSetupSteps || '[]')
      if (Array.isArray(setupSteps)) {
        setupData.children = setupSteps.map(parseStepNode)
      }
    } catch { /* ignore */ }
    try {
      const teardownSteps = JSON.parse(s.onceTeardownSteps || '[]')
      if (Array.isArray(teardownSteps)) {
        teardownData.children = teardownSteps.map(parseStepNode)
      }
    } catch { /* ignore */ }
    await Promise.all([loadSuiteCases(), loadLifecycle()])
  } catch { ElMessage.error('加载套件失败') } finally { loading.value = false }
}

function parseStepNode(raw: any): StepNode {
  if (raw.type === 'wait') return { type: 'wait', name: raw.name || `等待 ${raw.duration || 2000}ms`, duration: raw.duration || 2000 }
  if (raw.type === 'condition') return { type: 'condition', name: raw.name || '条件判断', expr: raw.expr || '' }
  return { type: 'keyword', kwType: raw.kwType || 'action', id: raw.id, name: raw.name || '未知关键字' }
}

async function loadSuiteCases() {
  try {
    const res: any = await getCases(projectId.value, { suiteId: suiteId.value, pageSize: 1000 })
    suiteCases.value = (res.data?.records || res.data || []) as any[]
  } catch { /* ignore */ }
}

async function loadLifecycle() {
  try {
    const res: any = await getSuiteLifecycle(projectId.value, suiteId.value)
    const list = (res.data || []) as any[]
    const map = new Map<number, any>()
    for (const item of list) { map.set(item.caseId, item) }
    lifecycleItems.value = suiteCases.value.map(c => {
      const existing = map.get(c.id)
      return { caseId: c.id, caseName: c.name, setupSteps: existing?.setupSteps || '', teardownSteps: existing?.teardownSteps || '' }
    })
  } catch { /* ignore */ }
}

function serializeSteps(data: SetupTeardownData): string {
  return JSON.stringify(data.children.map(n => {
    if (n.type === 'wait') return { type: 'wait', name: n.name, duration: n.duration }
    if (n.type === 'condition') return { type: 'condition', name: n.name, expr: n.expr }
    return { type: 'keyword', kwType: n.kwType, id: n.id, name: n.name }
  }))
}

async function handleSave() {
  if (!form.name) { ElMessage.warning('请输入套件名称'); return }
  saving.value = true
  try {
    await updateSuite(projectId.value, suiteId.value, {
      ...form,
      onceSetupSteps: serializeSteps(setupData),
      onceTeardownSteps: serializeSteps(teardownData),
    })
    // 保存用例级生命周期
    const items = lifecycleItems.value
      .filter(item => item.setupSteps?.trim() || item.teardownSteps?.trim())
      .map(item => ({ caseId: item.caseId, setupSteps: item.setupSteps || null, teardownSteps: item.teardownSteps || null }))
    await saveSuiteLifecycle(projectId.value, suiteId.value, { items })
    ElMessage.success('保存成功')
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '保存失败')
  } finally { saving.value = false }
}

onMounted(loadSuite)
</script>

<template>
  <div v-loading="loading">
    <div v-if="!loading">
      <!-- 页头 -->
      <div class="edit-header">
        <h2 style="margin: 0; font-size: 18px">Setup/Teardown 配置</h2>
        <div class="edit-header-actions">
          <el-button v-if="hasPermission('project:suite:edit')" type="primary" :loading="saving" @click="handleSave">保存</el-button>
          <el-button @click="router.back()">取消</el-button>
        </div>
      </div>

      <!-- 上下文信息条 -->
      <div class="context-bar">
        <span class="ctx-icon">ℹ</span>
        <span>套件级 · 整体 — 整个套件执行前后各调用一次</span>
      </div>

      <!-- 基本信息 -->
      <el-card style="margin-bottom: 16px">
        <template #header><span>基本信息</span></template>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="套件名称" required>
              <el-input v-model="form.name" />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="优先级">
              <el-select v-model="form.priority" style="width: 100%">
                <el-option v-for="p in priorityOptions" :key="p.value" :value="p.value" :label="p.label" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="10">
            <el-form-item label="描述">
              <el-input v-model="form.description" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-card>

      <!-- Setup/Teardown 编排器 -->
      <el-card style="margin-bottom: 16px">
        <template #header>
          <div style="display: flex; align-items: center; justify-content: space-between">
            <span>套件级 · 整体 Setup / Teardown</span>
            <el-switch v-model="form.enableOnceSetupTeardown" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="禁用" />
          </div>
        </template>

        <div v-if="form.enableOnceSetupTeardown === 1">
          <!-- Tabs -->
          <el-tabs v-model="activeTab">
            <el-tab-pane label="Setup" name="setup" />
            <el-tab-pane label="Teardown" name="teardown" />
          </el-tabs>

          <!-- 三面板编排器 -->
          <div class="orchestrator">
            <!-- 左侧：节点类型 -->
            <div class="node-panel">
              <h4 class="node-panel-title">节点类型</h4>
              <div class="node-drag" @click="addNode(activeTab, 'api')">
                <div class="nd-icon" style="background: #e6f7ff; color: #1890ff">A</div>
                <span>接口关键字</span>
              </div>
              <div class="node-drag" @click="addNode(activeTab, 'tool')">
                <div class="nd-icon" style="background: #fff7e6; color: #fa8c16">T</div>
                <span>工具方法</span>
              </div>
              <div class="node-drag" @click="addNode(activeTab, 'action')">
                <div class="nd-icon" style="background: #e6f7ff; color: #1890ff">A</div>
                <span>Action关键字</span>
              </div>
              <div class="node-drag" @click="addNode(activeTab, 'wait')">
                <div class="nd-icon" style="background: #fff1f0; color: #f5222d">⏱</div>
                <span>等待节点</span>
              </div>
              <div class="node-drag" @click="addNode(activeTab, 'condition')">
                <div class="nd-icon" style="background: #f9f0ff; color: #722ed1">?</div>
                <span>条件节点</span>
              </div>
            </div>

            <!-- 中间：画布 -->
            <div class="canvas">
              <div class="canvas-tree">
                <!-- 根节点 -->
                <div
                  :class="['tree-node', { selected: getSelectedIndex(activeTab) === null }]"
                  @click="selectNode(activeTab, null)"
                >
                  <span class="tn-icon" style="color: #52c41a">▶</span>
                  <span>{{ getData(activeTab).name }}</span>
                  <span class="tn-count">{{ getData(activeTab).children.length }} 个子节点</span>
                </div>
                <!-- 子节点 -->
                <div v-if="getData(activeTab).children.length > 0" class="tree-node-child">
                  <div
                    v-for="(node, idx) in getData(activeTab).children"
                    :key="idx"
                    :class="['tree-node', { selected: getSelectedIndex(activeTab) === idx }]"
                    @click="selectNode(activeTab, idx)"
                  >
                    <span v-if="node.type === 'keyword'" class="tn-icon" :style="{ color: kwConfigs[node.kwType || 'action']?.color || '#1890ff' }">
                      {{ kwConfigs[node.kwType || 'action']?.icon || 'A' }}
                    </span>
                    <span v-else class="tn-icon" :style="{ color: logicConfigs[node.type]?.color || '#909399' }">
                      {{ logicConfigs[node.type]?.icon || '•' }}
                    </span>
                    <span class="tn-name">{{ node.name }}</span>
                    <el-tag v-if="node.type === 'keyword'" :type="(kwConfigs[node.kwType || 'action']?.badge || '') as any" size="small" style="font-size: 10px">
                      {{ kwConfigs[node.kwType || 'action']?.label || '' }}
                    </el-tag>
                    <el-tag v-else :type="(logicConfigs[node.type]?.badge || '') as any" size="small" style="font-size: 10px">
                      {{ logicConfigs[node.type]?.label || '' }}
                    </el-tag>
                    <span v-if="node.type === 'wait'" class="tn-extra">{{ node.duration || 2000 }}ms</span>
                    <span v-if="node.type === 'condition'" class="tn-extra">{{ node.expr }}</span>
                    <span class="tn-actions">
                      <el-button link size="small" :disabled="idx === 0" @click.stop="moveNode(activeTab, idx, 'up')">↑</el-button>
                      <el-button link size="small" :disabled="idx === getData(activeTab).children.length - 1" @click.stop="moveNode(activeTab, idx, 'down')">↓</el-button>
                      <el-button link size="small" type="danger" @click.stop="deleteNode(activeTab, idx)">×</el-button>
                    </span>
                  </div>
                </div>
              </div>
              <div v-if="getData(activeTab).children.length === 0" class="canvas-empty">
                <span>📭</span>
                <div>暂无节点，点击左侧节点类型添加</div>
              </div>
            </div>

            <!-- 右侧：属性面板 -->
            <div class="prop-panel">
              <template v-if="!selectedNode">
                <!-- 根节点属性 -->
                <h4>节点属性：串行执行</h4>
                <p class="prop-hint">串行节点按顺序依次执行子节点，无需额外配置。</p>
                <div v-if="getData(activeTab).children.length > 0" class="prop-child-list">
                  <h4 class="prop-sub-title">子节点列表</h4>
                  <div class="prop-child-items">
                    <div v-for="(n, i) in getData(activeTab).children" :key="i" class="prop-child-item">
                      {{ i + 1 }}. {{ n.name }}
                    </div>
                  </div>
                </div>
              </template>
              <template v-else-if="selectedNode.type === 'keyword'">
                <h4>节点属性：关键字</h4>
                <el-form label-position="top" size="small">
                  <el-form-item label="关键字名称">
                    <el-input :model-value="selectedNode.name" readonly />
                  </el-form-item>
                  <el-form-item label="类型">
                    <el-tag :type="(kwConfigs[selectedNode.kwType || 'action']?.badge || '') as any" size="small">
                      {{ kwConfigs[selectedNode.kwType || 'action']?.label || '' }}
                    </el-tag>
                  </el-form-item>
                </el-form>
                <h4 class="prop-sub-title" style="margin-top: 12px">参数映射</h4>
                <p class="prop-hint">该关键字无需额外参数映射</p>
              </template>
              <template v-else-if="selectedNode.type === 'wait'">
                <h4>节点属性：等待</h4>
                <el-form label-position="top" size="small">
                  <el-form-item label="等待时长">
                    <div style="display: flex; gap: 4px; align-items: center">
                      <el-input :model-value="selectedNode.duration || 2000" style="width: 120px" @input="(v: string) => updateNodeProp('duration', v)" />
                      <span style="font-size: 12px; color: #909399">毫秒</span>
                    </div>
                  </el-form-item>
                </el-form>
              </template>
              <template v-else-if="selectedNode.type === 'condition'">
                <h4>节点属性：条件</h4>
                <el-form label-position="top" size="small">
                  <el-form-item label="条件表达式">
                    <el-input :model-value="selectedNode.expr || ''" style="font-family: monospace" @input="(v: string) => updateNodeProp('expr', v)" />
                  </el-form-item>
                  <el-form-item label="then 分支">
                    <p class="prop-hint">条件为真时执行（继续后续节点）</p>
                  </el-form-item>
                  <el-form-item label="else 分支">
                    <p class="prop-hint">条件为假时执行（跳过后续节点）</p>
                  </el-form-item>
                </el-form>
              </template>
            </div>
          </div>
        </div>
        <div v-else style="color: #909399; text-align: center; padding: 20px">未启用套件级整体生命周期</div>
      </el-card>

      <!-- 套件内用例级差异化生命周期 -->
      <el-card>
        <template #header>
          <span>套件内用例级差异化 Setup / Teardown</span>
        </template>
        <div style="color: #909399; font-size: 12px; margin-bottom: 8px">
          为套件内每条用例配置差异化的 Setup/Teardown 步骤。留空则使用用例自身的配置。
        </div>
        <div v-if="lifecycleItems.length === 0" style="color: #909399; text-align: center; padding: 20px">
          套件下暂无用例
        </div>
        <el-collapse v-else accordion>
          <el-collapse-item v-for="item in lifecycleItems" :key="item.caseId" :name="item.caseId">
            <template #title>
              <span style="font-weight: 600">{{ item.caseName }}</span>
              <el-tag v-if="item.setupSteps?.trim() || item.teardownSteps?.trim()" size="small" type="success" style="margin-left: 8px">已配置</el-tag>
            </template>
            <el-row :gutter="16" style="margin-top: 8px">
              <el-col :span="12">
                <span style="font-weight: 600; font-size: 13px">差异化 Setup</span>
                <el-input v-model="item.setupSteps" type="textarea" :rows="6" placeholder="留空则使用用例自身 Setup" style="margin-top: 6px; font-family: monospace; font-size: 12px" />
              </el-col>
              <el-col :span="12">
                <span style="font-weight: 600; font-size: 13px">差异化 Teardown</span>
                <el-input v-model="item.teardownSteps" type="textarea" :rows="6" placeholder="留空则使用用例自身 Teardown" style="margin-top: 6px; font-family: monospace; font-size: 12px" />
              </el-col>
            </el-row>
          </el-collapse-item>
        </el-collapse>
      </el-card>
    </div>
  </div>
</template>

<style scoped>
.edit-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.edit-header-actions {
  display: flex;
  gap: 8px;
}
.context-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 16px;
  background: #f0f5ff;
  border: 1px solid #adc6ff;
  border-radius: 4px;
  margin-bottom: 14px;
  font-size: 13px;
  color: #1d39c4;
}
.ctx-icon {
  font-size: 16px;
}

/* ===== 三面板编排器 ===== */
.orchestrator {
  display: flex;
  height: 420px;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  overflow: hidden;
}

/* 左侧节点面板 */
.node-panel {
  width: 180px;
  background: #fafafa;
  border-right: 1px solid #ebeef5;
  padding: 12px;
  overflow-y: auto;
}
.node-panel-title {
  font-size: 12px;
  color: #909399;
  margin-bottom: 10px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}
.node-drag {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  margin-bottom: 6px;
  cursor: pointer;
  font-size: 13px;
  transition: all 0.15s;
}
.node-drag:hover {
  border-color: #409eff;
  box-shadow: 0 1px 4px rgba(24, 144, 255, 0.15);
}
.nd-icon {
  width: 22px;
  height: 22px;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  flex-shrink: 0;
  font-weight: 600;
}

/* 中间画布 */
.canvas {
  flex: 1;
  background: #fff;
  padding: 16px;
  overflow: auto;
  position: relative;
}
.canvas-tree {
  padding: 8px;
}
.tree-node {
  border: 1px solid #ebeef5;
  border-radius: 4px;
  padding: 8px 12px;
  margin-bottom: 6px;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  position: relative;
  transition: all 0.15s;
}
.tree-node:hover {
  border-color: #409eff;
  background: #ecf5ff;
}
.tree-node.selected {
  border-color: #409eff;
  background: #ecf5ff;
  box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.15);
}
.tn-icon {
  width: 20px;
  text-align: center;
  font-size: 12px;
  font-weight: 600;
}
.tn-name {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.tn-count {
  font-size: 11px;
  color: #909399;
  margin-left: auto;
}
.tn-extra {
  font-size: 11px;
  color: #909399;
  margin-left: 4px;
}
.tn-actions {
  display: none;
  gap: 2px;
  margin-left: auto;
  flex-shrink: 0;
}
.tree-node:hover .tn-actions {
  display: flex;
}
.tree-node:hover .tn-count {
  display: none;
}
.tree-node-child {
  margin-left: 28px;
  border-left: 1px dashed #dcdfe6;
  padding-left: 12px;
}
.canvas-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #909399;
  font-size: 13px;
  gap: 8px;
}
.canvas-empty span {
  font-size: 28px;
}

/* 右侧属性面板 */
.prop-panel {
  width: 280px;
  border-left: 1px solid #ebeef5;
  background: #fff;
  overflow-y: auto;
  padding: 14px;
}
.prop-panel h4 {
  font-size: 13px;
  font-weight: 600;
  margin-bottom: 10px;
  color: #303133;
}
.prop-hint {
  font-size: 12px;
  color: #909399;
  margin: 4px 0;
}
.prop-sub-title {
  font-size: 12px;
  color: #606266;
}
.prop-child-list {
  margin-top: 12px;
}
.prop-child-items {
  font-size: 12px;
  color: #606266;
  line-height: 2;
}
.prop-child-item {
  padding: 0 4px;
}
</style>
