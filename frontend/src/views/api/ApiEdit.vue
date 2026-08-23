<!--
 @author HXN
 @date 2026-08-20 15:34
 @description API 编辑视图
-->
<script setup lang="ts">
/**
 * 接口编辑/新建 - M4
 * 6 Tab：基础信息 / 请求参数 / 请求体 / 响应定义 / 调试 / 引用关系
 * 对齐原型 api-edit.html（请求参数/响应改为可视化表格编辑，数据仍序列化为 JSON 存储）
 */
import { ref, reactive, onMounted, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getApi, createApi, updateApi, getModules, getApiReferences, debugApi } from '@/api/apidoc'
import { getEnvironments } from '@/api/environment'
import { useDict } from '@/composables/useDict'
import { usePermission } from '@/composables/usePermission'

const route = useRoute()
const router = useRouter()
const projectId = computed(() => Number(route.params.id))
const apiId = computed(() => Number(route.params.apiId))
const isEdit = computed(() => !!apiId.value)
const { hasPermission } = usePermission()

const activeTab = ref('basic')
const loading = ref(false)
const modules = ref<any[]>([])
const environments = ref<any[]>([])
const { options: httpMethodOptions } = useDict('http_method')
const { options: paramTypeOptions } = useDict('param_type')

const form = reactive({
  name: '', httpMethod: 'GET', path: '', service: '', moduleId: null as number | null,
  description: '', requestParams: '[]', requestBody: '{}', responseBody: '[]', headers: '[]',
  sourceType: 'MANUAL',
})

// ===== 参数可视化编辑（序列化为 JSON 存入 form） =====
function parseArr(raw?: string): any[] {
  if (!raw) return []
  try { const a = JSON.parse(raw); return Array.isArray(a) ? a : [] } catch { return [] }
}
const queryParams = ref<any[]>([])
const headerParams = ref<any[]>([])
const responseFields = ref<any[]>([])
const bodyText = ref('{}')

function syncToForm() {
  form.requestParams = JSON.stringify(queryParams.value)
  form.headers = JSON.stringify(headerParams.value)
  form.responseBody = JSON.stringify(responseFields.value)
  form.requestBody = bodyText.value
}

watch([queryParams, headerParams, responseFields, bodyText], syncToForm, { deep: true })

function addRow(arr: any[]) {
  arr.push({ name: '', type: 'string', required: false, description: '' })
}
function removeRow(arr: any[], idx: number) {
  arr.splice(idx, 1)
}

// Path 参数从路径解析（只读展示）
const pathParams = computed(() => {
  const matches = form.path.match(/\{(\w+)\}/g) || []
  return matches.map((m) => m.slice(1, -1))
})

// ===== 数据加载 =====
async function fetchModules() {
  try {
    const res: any = await getModules(projectId.value)
    // 排除"全部"系统分组（接口不应直接归属"全部"），保留"未分类"及其他用户分组
    modules.value = (res.data || []).filter((m: any) => !(m.isSystem === 1 && m.name === '全部'))
    if (!form.moduleId && modules.value.length) form.moduleId = modules.value[0].id
  } catch { modules.value = [] }
}

async function fetchEnvironments() {
  try {
    const res: any = await getEnvironments(projectId.value)
    environments.value = res.data || []
  } catch { environments.value = [] }
}

async function fetchApi() {
  if (!isEdit.value) return
  loading.value = true
  try {
    const res: any = await getApi(projectId.value, apiId.value)
    Object.assign(form, res.data)
    queryParams.value = parseArr(form.requestParams)
    headerParams.value = parseArr(form.headers)
    responseFields.value = parseArr(form.responseBody)
    bodyText.value = form.requestBody || '{}'
  } catch { ElMessage.error('加载接口失败') } finally { loading.value = false }
}

// ===== 引用关系 =====
const references = ref<any[]>([])
const refsLoading = ref(false)
async function fetchReferences() {
  if (!isEdit.value) return
  refsLoading.value = true
  try {
    const res: any = await getApiReferences(projectId.value, apiId.value)
    references.value = res.data || []
  } catch { references.value = [] } finally { refsLoading.value = false }
}

// ===== 调试 =====
const debugEnvId = ref<number | null>(null)
const debugParamValues = ref<Record<string, string>>({})
const debugResult = ref<any>(null)
const debugLoading = ref(false)
function initDebugParams() {
  const map: Record<string, string> = {}
  queryParams.value.forEach((p) => { map[p.name] = '' })
  debugParamValues.value = map
}
async function sendDebug() {
  debugLoading.value = true
  debugResult.value = null
  try {
    const res: any = await debugApi(projectId.value, apiId.value, {
      environmentId: debugEnvId.value || undefined,
      params: debugParamValues.value,
    })
    debugResult.value = res.data
  } catch (e: any) {
    debugResult.value = { success: 0, error: e?.response?.data?.message || e?.message || '调试失败' }
  } finally { debugLoading.value = false }
}

function onTabChange(tab: string) {
  if (tab === 'debug') initDebugParams()
  if (tab === 'refs') fetchReferences()
}

// ===== 调试响应辅助 =====
const debugRespTab = ref('body')
const debugHeaderEntries = computed(() => {
  const h = debugResult.value?.responseHeaders
  if (!h || typeof h !== 'object') return []
  return Object.entries(h).map(([key, value]) => ({ key, value: String(value) }))
})
function formatDebugBody(result: any): string {
  const body = result?.responseBody ?? result?.output ?? result?.error ?? ''
  if (typeof body === 'string') return body
  return JSON.stringify(body, null, 2)
}

// ===== 保存 =====
async function handleSubmit() {
  syncToForm()
  if (!form.name || !form.path) { ElMessage.warning('请填写必填项'); activeTab.value = 'basic'; return }
  try {
    if (isEdit.value) {
      await updateApi(projectId.value, apiId.value, { ...form, projectId: projectId.value })
      ElMessage.success('更新成功')
    } else {
      await createApi(projectId.value, { ...form, projectId: projectId.value })
      ElMessage.success('创建成功')
    }
    router.push(`/project/${projectId.value}/apis`)
  } catch (e: any) { ElMessage.error(e?.response?.data?.message || '操作失败') }
}

onMounted(() => {
  fetchModules()
  fetchEnvironments()
  fetchApi()
})
</script>

<template>
  <div v-loading="loading">
    <div class="edit-header">
      <div class="edit-title">
        <el-button type="primary" link @click="router.back()">← 返回</el-button>
        <h2 style="margin: 0">{{ isEdit ? '编辑接口' : '新建接口' }}</h2>
      </div>
      <div class="edit-actions">
        <el-button v-if="hasPermission('project:api:edit')" type="primary" @click="handleSubmit">保存</el-button>
        <el-button @click="router.back()">取消</el-button>
      </div>
    </div>

    <el-tabs v-model="activeTab" @tab-change="(t: string) => onTabChange(t)">
      <!-- Tab: 基础信息 -->
      <el-tab-pane label="基础信息" name="basic">
        <el-card shadow="never" style="max-width: 800px">
          <el-form label-position="top">
            <el-row :gutter="16" v-if="isEdit">
              <el-col :span="12">
                <el-form-item label="接口 ID">
                  <el-input :model-value="apiId" disabled />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="来源">
                  <el-input :model-value="form.sourceType === 'SWAGGER_IMPORT' ? 'Swagger 导入' : '手动创建'" disabled />
                </el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="16">
              <el-col :span="12">
                <el-form-item label="接口名称" required>
                  <el-input v-model="form.name" placeholder="请输入接口名称" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="HTTP 方法">
                  <el-select v-model="form.httpMethod" style="width: 100%">
                    <el-option v-for="m in httpMethodOptions" :key="m.value" :value="m.value" :label="m.label" />
                  </el-select>
                </el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="16">
              <el-col :span="12">
                <el-form-item label="接口路径" required>
                  <el-input v-model="form.path" placeholder="/api/v1/example" style="font-family: monospace" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="所属分组">
                  <el-select v-model="form.moduleId" placeholder="选择分组" style="width: 100%">
                    <el-option v-for="m in modules" :key="m.id" :value="m.id" :label="m.name" />
                  </el-select>
                </el-form-item>
              </el-col>
            </el-row>
            <el-form-item label="描述">
              <el-input v-model="form.description" type="textarea" :rows="2" placeholder="接口描述（可选）" />
            </el-form-item>
          </el-form>
        </el-card>
      </el-tab-pane>

      <!-- Tab: 请求参数 -->
      <el-tab-pane label="请求参数" name="params">
        <div class="params-section">
          <h4>Path 参数</h4>
          <p v-if="!pathParams.length" class="empty-hint">该接口无 Path 参数</p>
          <el-table v-else :data="pathParams.map(p => ({ name: p }))" size="small" border>
            <el-table-column prop="name" label="参数名" />
            <el-table-column label="类型"><template #default>string</template></el-table-column>
            <el-table-column label="说明"><template #default>路径参数</template></el-table-column>
          </el-table>
        </div>
        <div class="params-section">
          <div class="section-head">
            <h4>Query 参数</h4>
            <el-button size="small" @click="addRow(queryParams)">+ 添加参数</el-button>
          </div>
          <el-table :data="queryParams" size="small" border>
            <el-table-column label="参数名" width="160">
              <template #default="{ row }"><el-input v-model="row.name" size="small" placeholder="参数名" /></template>
            </el-table-column>
            <el-table-column label="类型" width="120">
              <template #default="{ row }">
                <el-select v-model="row.type" size="small">
                  <el-option v-for="t in paramTypeOptions" :key="t.value" :value="t.value" :label="t.label" />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="必填" width="70">
              <template #default="{ row }"><el-switch v-model="row.required" /></template>
            </el-table-column>
            <el-table-column label="说明">
              <template #default="{ row }"><el-input v-model="row.description" size="small" placeholder="说明" /></template>
            </el-table-column>
            <el-table-column label="操作" width="70">
              <template #default="{ $index }">
                <el-button link size="small" type="danger" @click="removeRow(queryParams, $index)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
        <div class="params-section">
          <div class="section-head">
            <h4>Header 参数</h4>
            <el-button size="small" @click="addRow(headerParams)">+ 添加参数</el-button>
          </div>
          <el-table :data="headerParams" size="small" border>
            <el-table-column label="参数名" width="180">
              <template #default="{ row }"><el-input v-model="row.name" size="small" placeholder="如 Authorization" /></template>
            </el-table-column>
            <el-table-column label="类型" width="120">
              <template #default="{ row }">
                <el-select v-model="row.type" size="small">
                  <el-option v-for="t in paramTypeOptions" :key="t.value" :value="t.value" :label="t.label" />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="必填" width="70">
              <template #default="{ row }"><el-switch v-model="row.required" /></template>
            </el-table-column>
            <el-table-column label="说明">
              <template #default="{ row }"><el-input v-model="row.description" size="small" /></template>
            </el-table-column>
            <el-table-column label="操作" width="70">
              <template #default="{ $index }">
                <el-button link size="small" type="danger" @click="removeRow(headerParams, $index)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>

      <!-- Tab: 请求体 -->
      <el-tab-pane label="请求体" name="body">
        <div v-if="form.httpMethod === 'GET'" class="empty-state">
          <div class="empty-icon">📋</div>
          <div>GET 请求不包含请求体</div>
        </div>
        <el-input v-else v-model="bodyText" type="textarea" :rows="14" placeholder='{"key":"value"}' style="font-family: monospace; max-width: 800px" />
      </el-tab-pane>

      <!-- Tab: 响应定义 -->
      <el-tab-pane label="响应定义" name="response">
        <div class="params-section">
          <div class="section-head">
            <h4>响应字段</h4>
            <el-button size="small" @click="addRow(responseFields)">+ 添加字段</el-button>
          </div>
          <el-table :data="responseFields" size="small" border style="max-width: 800px">
            <el-table-column label="字段名" width="200">
              <template #default="{ row }"><el-input v-model="row.name" size="small" placeholder="如 data.id" style="font-family: monospace" /></template>
            </el-table-column>
            <el-table-column label="类型" width="120">
              <template #default="{ row }">
                <el-select v-model="row.type" size="small">
                  <el-option v-for="t in paramTypeOptions" :key="t.value" :value="t.value" :label="t.label" />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="必填" width="70">
              <template #default="{ row }"><el-switch v-model="row.required" /></template>
            </el-table-column>
            <el-table-column label="描述">
              <template #default="{ row }"><el-input v-model="row.description" size="small" /></template>
            </el-table-column>
            <el-table-column label="操作" width="70">
              <template #default="{ $index }">
                <el-button link size="small" type="danger" @click="removeRow(responseFields, $index)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>

      <!-- Tab: 调试 -->
      <el-tab-pane label="调试" name="debug" :disabled="!isEdit">
        <el-card shadow="never" style="max-width: 900px">
          <div class="debug-env-row">
            <span class="debug-label">选择环境：</span>
            <el-select v-model="debugEnvId" placeholder="选择环境" style="width: 180px">
              <el-option v-for="env in environments" :key="env.id" :value="env.id" :label="env.name" />
            </el-select>
            <el-button type="primary" :loading="debugLoading" @click="sendDebug">发送请求</el-button>
          </div>
          <div class="debug-params">
            <h4>Query 参数</h4>
            <el-table v-if="queryParams.length" :data="queryParams" size="small" border style="max-width: 600px">
              <el-table-column label="参数名" width="180">
                <template #default="{ row }">
                  <span style="font-family: monospace">{{ row.name }}</span>
                </template>
              </el-table-column>
              <el-table-column label="值">
                <template #default="{ row }">
                  <el-input v-model="debugParamValues[row.name]" size="small" placeholder="值" />
                </template>
              </el-table-column>
            </el-table>
            <p v-else class="empty-hint">该接口无 Query 参数</p>
          </div>
          <div v-if="debugResult" class="debug-response">
            <div class="resp-status">
              <span :class="['resp-code', debugResult.success === 1 ? 'ok' : 'err']">
                {{ debugResult.statusCode || (debugResult.success === 1 ? '200 OK' : 'ERROR') }}
              </span>
              <span v-if="debugResult.responseTimeMs" class="resp-meta">耗时: {{ debugResult.responseTimeMs }}ms</span>
              <span v-if="debugResult.responseSize" class="resp-meta">大小: {{ debugResult.responseSize }}</span>
            </div>
            <el-tabs v-model="debugRespTab">
              <el-tab-pane label="响应体" name="body">
                <pre class="resp-body-code">{{ formatDebugBody(debugResult) }}</pre>
              </el-tab-pane>
              <el-tab-pane label="响应头" name="headers">
                <el-table v-if="debugHeaderEntries.length" :data="debugHeaderEntries" size="small" border>
                  <el-table-column prop="key" label="Header" width="200">
                    <template #default="{ row }">
                      <span style="font-family: monospace; font-weight: 500">{{ row.key }}</span>
                    </template>
                  </el-table-column>
                  <el-table-column prop="value" label="Value">
                    <template #default="{ row }">
                      <span style="font-family: monospace; word-break: break-all">{{ row.value }}</span>
                    </template>
                  </el-table-column>
                </el-table>
                <p v-else class="empty-hint" style="padding: 20px; text-align: center">无响应头信息</p>
              </el-tab-pane>
              <el-tab-pane label="状态信息" name="status">
                <el-descriptions :column="1" border size="small" style="max-width: 400px">
                  <el-descriptions-item label="状态码">{{ debugResult.statusCode ?? (debugResult.success === 1 ? '200 OK' : '-') }}</el-descriptions-item>
                  <el-descriptions-item label="响应时间">{{ debugResult.responseTimeMs ?? '-' }} ms</el-descriptions-item>
                  <el-descriptions-item label="响应大小">{{ debugResult.responseSize ?? '-' }}</el-descriptions-item>
                  <el-descriptions-item label="协议">{{ debugResult.protocol ?? 'HTTP/1.1' }}</el-descriptions-item>
                </el-descriptions>
              </el-tab-pane>
            </el-tabs>
          </div>
        </el-card>
      </el-tab-pane>

      <!-- Tab: 引用关系 -->
      <el-tab-pane label="引用关系" name="refs" :disabled="!isEdit">
        <el-table v-loading="refsLoading" :data="references" size="small" border style="max-width: 800px">
          <el-table-column prop="keywordName" label="关键字名称" />
          <el-table-column prop="createdBy" label="创建人" width="120" />
          <el-table-column label="修改时间" width="140">
            <template #default="{ row }">{{ row.updatedAt?.substring(0, 10) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="80">
            <template #default>
              <el-button type="primary" link size="small" @click="router.push(`/project/${projectId}/keywords`)">查看</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!references.length && !refsLoading" description="暂无引用" />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<style scoped>
.edit-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.edit-title {
  display: flex;
  align-items: center;
  gap: 12px;
}
.edit-actions {
  display: flex;
  gap: 8px;
}
.params-section {
  margin-bottom: 24px;
}
.params-section h4 {
  font-size: 14px;
  font-weight: 600;
  margin: 0 0 8px;
}
.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}
.empty-hint {
  color: #909399;
  font-size: 12px;
  margin: 0;
}
.empty-state {
  text-align: center;
  padding: 48px;
  color: #909399;
}
.empty-icon {
  font-size: 36px;
  margin-bottom: 8px;
  opacity: 0.4;
}
.debug-section {
  max-width: 900px;
}
.debug-env-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}
.debug-label {
  font-size: 13px;
  color: #606266;
  white-space: nowrap;
}
.debug-params h4 {
  font-size: 13px;
  font-weight: 600;
  margin: 0 0 8px;
}
.debug-response {
  margin-top: 16px;
}
.resp-status {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 8px;
}
.resp-code {
  font-weight: 700;
}
.resp-code.ok {
  color: #67c23a;
}
.resp-code.err {
  color: #f56c6c;
}
.resp-meta {
  font-size: 12px;
  color: #909399;
}
.resp-body-code {
  background: #f5f7fa;
  padding: 12px;
  border-radius: 4px;
  max-height: 360px;
  overflow: auto;
  font-size: 12px;
  font-family: monospace;
  margin: 0;
  white-space: pre-wrap;
  word-break: break-all;
}
</style>
