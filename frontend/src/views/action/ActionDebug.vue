<!--
 @author HXN
 @date 2026-08-20 15:34
 @description Action 调试视图
-->
<script setup lang="ts">
/**
 * Action 调试 - M7
 * 左侧结构化参数表 + 右侧执行结果（节点状态表 + 节点详情 + 变量值）
 * 对齐原型 action-debug.html
 */
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getAction, debugAction } from '@/api/action'
import { getEnvironments } from '@/api/environment'

const route = useRoute()
const router = useRouter()
const projectId = computed(() => Number(route.params.id))
const actionId = computed(() => Number(route.params.actionId))

const action = ref<any>(null)
const environments = ref<any[]>([])
const selectedEnvId = ref<number>(0)
const debugResult = ref<any>(null)
const loading = ref(false)
const executing = ref(false)

// ===== 输入参数结构化编辑 =====
interface ParamRow {
  name: string
  type: string
  value: string
  required: boolean
  description: string
}
const paramRows = ref<ParamRow[]>([])

const statusLabels: Record<string, string> = {
  PASSED: '通过',
  FAILED: '失败',
  SKIPPED: '跳过',
  ERROR: '错误',
  PENDING: '等待中',
}

const statusTypes: Record<string, string> = {
  PASSED: 'success',
  FAILED: 'danger',
  ERROR: 'danger',
  SKIPPED: 'warning',
  PENDING: 'info',
}

// 节点类型标签颜色映射
const nodeTypeColors: Record<string, string> = {
  API_KEYWORD: '',
  TOOL_METHOD: 'success',
  CONDITION: 'warning',
  LOOP: 'info',
  START: 'success',
  END: 'danger',
}

// 选中的节点索引（用于显示详情）
const selectedNodeIdx = ref<number>(-1)

const selectedNodeResult = computed(() => {
  if (selectedNodeIdx.value < 0 || !debugResult.value?.nodeResults) return null
  return debugResult.value.nodeResults[selectedNodeIdx.value] || null
})

// 从 Action 的 inputParams JSON 解析参数定义
function parseInputParams(raw?: string): ParamRow[] {
  if (!raw) return []
  try {
    const parsed = JSON.parse(raw)
    if (!Array.isArray(parsed)) return []
    return parsed.map((p: any) => ({
      name: p.name || p.paramName || '',
      type: p.type || p.paramType || 'string',
      value: p.default ?? p.defaultValue ?? '',
      required: p.required ?? false,
      description: p.description || p.desc || '',
    }))
  } catch {
    return []
  }
}

async function loadData() {
  if (!projectId.value || !actionId.value) return
  loading.value = true
  try {
    const [actionRes, envRes] = await Promise.all([
      getAction(projectId.value, actionId.value),
      getEnvironments(projectId.value),
    ])
    action.value = (actionRes as any).data || {}
    environments.value = (envRes as any).data || []
    // 解析输入参数定义
    paramRows.value = parseInputParams(action.value.inputParams)
  } catch {
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

async function handleDebug() {
  if (!selectedEnvId.value) {
    ElMessage.warning('请选择执行环境')
    return
  }
  // 将参数表转换为 Map
  const params: Record<string, any> = {}
  for (const p of paramRows.value) {
    if (!p.name) continue
    let val: any = p.value
    // 类型转换
    if (p.type === 'int' || p.type === 'number') {
      val = p.value === '' ? null : Number(p.value)
    } else if (p.type === 'bool' || p.type === 'boolean') {
      val = p.value === 'true' || p.value === '1'
    }
    params[p.name] = val
  }

  executing.value = true
  debugResult.value = null
  selectedNodeIdx.value = -1
  try {
    const res: any = await debugAction(projectId.value, actionId.value, {
      environmentId: selectedEnvId.value,
      inputParams: params,
    })
    debugResult.value = res.data
    // 默认选中第一个节点
    if (debugResult.value?.nodeResults?.length) {
      selectedNodeIdx.value = 0
    }
    if (debugResult.value?.success === 1) {
      ElMessage.success('调试执行成功')
    } else {
      ElMessage.error('调试执行失败')
    }
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '调试失败')
  } finally {
    executing.value = false
  }
}

// 变量列表（从 output.variables 提取，匹配来源节点）
const variableList = computed(() => {
  const vars = debugResult.value?.output?.variables
  if (!vars || typeof vars !== 'object') return []
  // 从 nodeResults 构建 save_as → nodeKey 映射
  const sourceMap: Record<string, string> = {}
  const nodeResults = debugResult.value?.nodeResults || []
  for (const nr of nodeResults) {
    const saveAs = nr.saveAs || nr.save_as || ''
    const nodeKey = nr.nodeKey || nr.name || ''
    if (saveAs) sourceMap[saveAs] = nodeKey
  }
  return Object.entries(vars).map(([name, value]) => ({
    name,
    value: typeof value === 'object' ? JSON.stringify(value) : String(value),
    source: sourceMap[name] || '',
  }))
})

// 判断是否全部通过
const allPassed = computed(() => {
  if (!debugResult.value?.nodeResults) return false
  return debugResult.value.nodeResults.every(
    (n: any) => n.status === 'PASSED',
  )
})

onMounted(loadData)
</script>

<template>
  <div v-loading="loading">
    <div v-if="!loading">
      <!-- 页头：面包屑导航 + 环境选择 + 执行按钮 -->
      <div class="debug-header">
        <el-breadcrumb separator="/">
          <el-breadcrumb-item>
            <a @click.prevent="router.push(`/project/${projectId}/actions`)" href="javascript:void(0)">Action关键字</a>
          </el-breadcrumb-item>
          <el-breadcrumb-item>
            <a @click.prevent="router.push(`/project/${projectId}/actions/${actionId}/edit`)" href="javascript:void(0)">{{ action?.name || '' }}</a>
          </el-breadcrumb-item>
          <el-breadcrumb-item>调试</el-breadcrumb-item>
        </el-breadcrumb>
        <div style="margin-left: auto; display: flex; gap: 8px; align-items: center">
          <el-select
            v-model="selectedEnvId"
            placeholder="选择环境"
            style="width: 160px"
          >
            <el-option
              v-for="env in environments"
              :key="env.id"
              :value="env.id"
              :label="`${env.name}${env.isCurrent === 1 ? ' [当前]' : ''}`"
            />
          </el-select>
          <el-button
            type="primary"
            :loading="executing"
            @click="handleDebug"
          >
            ▶ 执行
          </el-button>
        </div>
      </div>

      <!-- 双栏布局 -->
      <div class="debug-layout">
        <!-- 左侧：请求参数 -->
        <div class="debug-panel">
          <div class="debug-panel-header">
            <h3>请求参数</h3>
          </div>
          <div class="debug-panel-body">
            <el-table
              v-if="paramRows.length > 0"
              :data="paramRows"
              size="small"
              border
              style="width: 100%"
            >
              <el-table-column label="参数名" width="140">
                <template #default="{ row }">
                  <code>{{ row.name }}</code>
                </template>
              </el-table-column>
              <el-table-column label="类型" width="80">
                <template #default="{ row }">
                  <el-tag size="small" type="info">{{ row.type }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="测试值">
                <template #default="{ row }">
                  <el-input
                    v-model="row.value"
                    size="small"
                    :placeholder="row.description || '输入测试值'"
                  />
                </template>
              </el-table-column>
            </el-table>
            <div
              v-else
              style="text-align: center; padding: 48px; color: #909399"
            >
              <div style="font-size: 32px; margin-bottom: 8px; opacity: 0.3">
                📝
              </div>
              <div>该 Action 未定义输入参数</div>
              <div style="font-size: 12px; color: #c0c4cc; margin-top: 4px">
                请在编辑器的「I/O 参数」Tab 中定义
              </div>
            </div>

            <!-- Action 说明 -->
            <div v-if="action?.description" class="action-desc-card">
              <div class="action-desc-title">Action 说明</div>
              <p style="color: #606266; margin: 0; font-size: 13px">
                {{ action.description }}
              </p>
            </div>
          </div>
        </div>

        <!-- 右侧：响应结果 -->
        <div class="debug-panel">
          <div class="debug-panel-header">
            <h3>响应结果</h3>
            <el-tag
              v-if="debugResult"
              :type="allPassed ? 'success' : 'danger'"
              size="small"
            >
              {{ allPassed ? '全部通过' : '存在失败' }}
            </el-tag>
          </div>
          <div class="debug-panel-body">
            <!-- 空状态 -->
            <div
              v-if="!debugResult"
              class="empty-state"
            >
              <div style="font-size: 36px; margin-bottom: 12px; opacity: 0.3">
                📡
              </div>
              <div>填写参数后点击「执行」</div>
            </div>

            <div v-else>
              <!-- 执行概览 -->
              <div class="result-overview">
                <span class="overview-label">节点执行状态</span>
                <el-tag :type="allPassed ? 'success' : 'danger'" size="small">
                  {{ allPassed ? '全部通过' : '执行失败' }}
                </el-tag>
                <span
                  v-if="debugResult.executionTimeMs != null"
                  style="font-size: 12px; color: #909399; margin-left: auto"
                >
                  总耗时：{{ debugResult.executionTimeMs }}ms
                </span>
              </div>

              <!-- 错误信息 -->
              <el-alert
                v-if="debugResult.errorMessage"
                type="error"
                :title="debugResult.errorMessage"
                show-icon
                :closable="false"
                style="margin-bottom: 12px"
              />

              <!-- 节点执行状态表 -->
              <div
                v-if="debugResult.nodeResults?.length"
                style="overflow-x: auto; margin-bottom: 16px"
              >
                <el-table
                  :data="debugResult.nodeResults"
                  size="small"
                  border
                  highlight-current-row
                  @row-click="(_row: any, _col: any, idx: number) => (selectedNodeIdx = idx)"
                >
                  <el-table-column label="#" width="50" align="center">
                    <template #default="{ $index }">{{ $index + 1 }}</template>
                  </el-table-column>
                  <el-table-column label="节点名称" width="160">
                    <template #default="{ row }">
                      <span style="font-weight: 500">
                        {{ row.nodeKey || row.name }}
                      </span>
                    </template>
                  </el-table-column>
                  <el-table-column label="类型" width="100">
                    <template #default="{ row }">
                      <el-tag
                        size="small"
                        :type="nodeTypeColors[row.nodeType] || 'info'"
                      >
                        {{ row.nodeType || '--' }}
                      </el-tag>
                    </template>
                  </el-table-column>
                  <el-table-column label="状态" width="100">
                    <template #default="{ row }">
                      <span :class="['status-dot', statusTypes[row.status] || 'info']">
                        {{ statusLabels[row.status] || row.status }}
                      </span>
                    </template>
                  </el-table-column>
                  <el-table-column label="耗时" width="90">
                    <template #default="{ row }">
                      {{ row.durationMs != null ? row.durationMs + 'ms' : '--' }}
                    </template>
                  </el-table-column>
                  <el-table-column label="操作" width="90">
                    <template #default="{ row }">
                      <el-button
                        v-if="row.response || row.request"
                        link
                        size="small"
                        type="primary"
                      >
                        查看日志
                      </el-button>
                      <span v-else style="color: #c0c4cc">-</span>
                    </template>
                  </el-table-column>
                </el-table>
              </div>

              <!-- 节点详情 + 变量值 -->
              <div class="node-detail-grid">
                <!-- 节点详情 -->
                <div class="node-detail-card">
                  <div class="card-header">
                    节点详情：{{
                      selectedNodeResult?.nodeKey ||
                      selectedNodeResult?.name ||
                      '未选择'
                    }}
                  </div>
                  <div class="card-body">
                    <template v-if="selectedNodeResult">
                      <div
                        v-if="selectedNodeResult.request"
                        style="margin-bottom: 8px; font-size: 13px"
                      >
                        <b>请求：</b>
                        <code style="font-size: 12px">{{
                          typeof selectedNodeResult.request === 'object'
                            ? JSON.stringify(selectedNodeResult.request)
                            : selectedNodeResult.request
                        }}</code>
                      </div>
                      <div v-if="selectedNodeResult.message" style="font-size: 13px; color: #909399; margin-bottom: 8px">
                        {{ selectedNodeResult.message }}
                      </div>
                      <pre
                        v-if="selectedNodeResult.response"
                        class="code-block"
                      >{{ JSON.stringify(selectedNodeResult.response, null, 2) }}</pre>
                      <div
                        v-else
                        style="color: #c0c4cc; font-size: 13px; text-align: center; padding: 24px"
                      >
                        该节点无响应数据
                      </div>
                    </template>
                    <div
                      v-else
                      style="color: #c0c4cc; font-size: 13px; text-align: center; padding: 24px"
                    >
                      点击上方表格中的节点查看详情
                    </div>
                  </div>
                </div>

                <!-- 变量值 -->
                <div class="node-detail-card">
                  <div class="card-header">变量值</div>
                  <div class="card-body" style="padding: 0">
                    <el-table
                      v-if="variableList.length > 0"
                      :data="variableList"
                      size="small"
                      border
                      style="width: 100%"
                    >
                      <el-table-column label="变量名" width="120">
                        <template #default="{ row }">
                          <code>{{ row.name }}</code>
                        </template>
                      </el-table-column>
                      <el-table-column label="来源节点" width="120">
                        <template #default="{ row }">
                          <span v-if="row.source" style="font-size: 12px; color: #409eff">{{ row.source }}</span>
                          <span v-else style="color: #c0c4cc; font-size: 12px">--</span>
                        </template>
                      </el-table-column>
                      <el-table-column label="值">
                        <template #default="{ row }">
                          <span style="font-size: 12px; font-family: monospace">
                            {{ row.value }}
                          </span>
                        </template>
                      </el-table-column>
                    </el-table>
                    <div
                      v-else
                      style="color: #c0c4cc; font-size: 13px; text-align: center; padding: 24px"
                    >
                      暂无变量
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.debug-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}
.debug-layout {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  min-height: calc(100vh - 200px);
}
.debug-panel {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}
.debug-panel-header {
  padding: 12px 16px;
  border-bottom: 1px solid #ebeef5;
  background: #fafafa;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.debug-panel-header h3 {
  font-size: 14px;
  font-weight: 600;
  margin: 0;
}
.debug-panel-body {
  padding: 16px;
  flex: 1;
  overflow: auto;
}
.action-desc-card {
  margin-top: 16px;
  padding: 12px;
  background: #fafafa;
  border-radius: 4px;
}
.action-desc-title {
  font-size: 13px;
  font-weight: 600;
  margin-bottom: 4px;
  color: #303133;
}
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 300px;
  color: #909399;
}
.result-overview {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}
.overview-label {
  font-size: 13px;
  font-weight: 600;
  color: #606266;
}
.status-dot {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
}
.status-dot::before {
  content: '';
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
}
.status-dot.success::before {
  background: #67c23a;
}
.status-dot.danger::before {
  background: #f56c6c;
}
.status-dot.warning::before {
  background: #e6a23c;
}
.status-dot.info::before {
  background: #909399;
}
.node-detail-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  margin-top: 12px;
}
.node-detail-card {
  border: 1px solid #ebeef5;
  border-radius: 4px;
  overflow: hidden;
}
.node-detail-card .card-header {
  padding: 8px 12px;
  font-size: 13px;
  font-weight: 600;
  background: #fafafa;
  border-bottom: 1px solid #ebeef5;
}
.node-detail-card .card-body {
  padding: 12px;
}
.code-block {
  font-size: 12px;
  background: #f5f7fa;
  padding: 10px;
  border-radius: 4px;
  max-height: 200px;
  overflow: auto;
  margin: 0;
  font-family: Consolas, Monaco, monospace;
}
</style>
