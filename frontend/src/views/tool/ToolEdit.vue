<!--
 @author HXN
 @date 2026-08-23
 @description 工具方法新建/编辑视图（对齐原型 tool-create.html / tool-edit.html）
-->
<script setup lang="ts">
/**
 * 工具方法新建/编辑 - M6
 * 3 Tab：基础信息 / 代码编辑 / 引用关系（仅编辑模式）
 * 对齐原型 tool-create.html、tool-edit.html
 */
import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getTool, createTool, updateTool, testTool, getTools } from '@/api/tool'
import CodeEditor from '@/components/CodeEditor/index.vue'
import { usePermission } from '@/composables/usePermission'

const route = useRoute()
const router = useRouter()
const { hasPermission } = usePermission()
const projectId = computed(() => Number(route.params.id))

// ===== 路由参数 =====
const toolId = ref(0)
const isEdit = computed(() => toolId.value > 0)
const loading = ref(false)
const activeTab = ref('basic')

// ===== 已有分组列表（从已有工具方法中提取） =====
const categoryOptions = ref<string[]>([])
async function fetchCategoryOptions() {
  try {
    const res: any = await getTools(projectId.value, { page: 1, pageSize: 10000 })
    const items = res.data?.items || []
    const categories = new Set<string>()
    items.forEach((t: any) => {
      if (t.category && t.category !== 'CUSTOM' && t.category !== 'BUILTIN') {
        categories.add(t.category)
      }
    })
    categoryOptions.value = Array.from(categories)
  } catch { categoryOptions.value = [] }
}

// ===== 表单 =====
const DEFAULT_CODE = '// 在此编写 Groovy 代码\n// 可使用 request, response, context, vars 等内置变量\n\ndef execute() {\n    return "Hello"\n}'
const form = reactive({
  name: '',
  category: '',
  description: '',
  code: DEFAULT_CODE,
  returnType: '',
  paramDefinitions: '[]',
})

// ===== 在线测试 =====
const testVisible = ref(false)
const testLoading = ref(false)
const testResult = ref<any>(null)
const testParams = ref<any[]>([])
const testValues = reactive<Record<string, string>>({})

// ===== Groovy 代码模板 =====
const TEMPLATE = `def generate_sn(String prefix, int length) {
    def chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
    def random = new Random()
    def sb = new StringBuilder()
    def remaining = length - prefix.length()
    for (int i = 0; i < remaining; i++) {
        sb.append(chars.charAt(random.nextInt(chars.length())))
    }
    return prefix + sb.toString()
}`

function insertTemplate() {
  form.code = TEMPLATE
  activeTab.value = 'code'
}

// ===== Groovy 代码格式化 =====
function formatCode() {
  const lines = form.code.split('\n')
  const out: string[] = []
  let indent = 0
  for (const raw of lines) {
    const trimmed = raw.trim()
    if (!trimmed) { out.push(''); continue }
    // 减少缩进：以 } 开头
    if (trimmed.startsWith('}')) indent = Math.max(0, indent - 1)
    out.push('    '.repeat(indent) + trimmed)
    // 增加缩进：以 { 结尾
    if (trimmed.endsWith('{')) indent++
  }
  form.code = out.join('\n').replace(/\n{3,}/g, '\n\n').replace(/\n+$/, '\n')
}

// ===== 从 Groovy 代码提取参数 =====
interface ParamInfo {
  name: string
  type: string
  required: boolean
  defaultValue: string
}

function extractParams(code: string): ParamInfo[] {
  // 匹配: def methodName(params) {  或  ReturnType methodName(params) {
  const match = code.match(/def\s+(\w+)\s*\(([\s\S]*?)\)\s*\{/)
  if (!match) return []
  const paramsPart = match[2].trim()
  if (!paramsPart) return []

  return paramsPart.split(',').map((p) => {
    p = p.trim()
    if (!p) return null
    let name = '', type = '', required = true, defaultValue = ''
    const eqIndex = p.indexOf('=')
    let leftPart = p
    if (eqIndex !== -1) {
      leftPart = p.substring(0, eqIndex).trim()
      defaultValue = p.substring(eqIndex + 1).trim()
      required = false
      // 去除引号
      if ((defaultValue.startsWith('"') && defaultValue.endsWith('"')) ||
          (defaultValue.startsWith("'") && defaultValue.endsWith("'"))) {
        defaultValue = defaultValue.slice(1, -1)
      }
    }
    const parts = leftPart.split(/\s+/)
    if (parts.length >= 2) {
      type = parts[0]
      name = parts[1]
    } else {
      name = parts[0]
    }
    return { name, type, required, defaultValue }
  }).filter((p): p is ParamInfo => p !== null)
}

function generateParamDefinitions(code: string): string {
  return JSON.stringify(extractParams(code))
}

// ===== 在线测试 =====
function openTestModal() {
  testParams.value = extractParams(form.code)
  testResult.value = null
  Object.keys(testValues).forEach((k) => delete testValues[k])
  testParams.value.forEach((p) => {
    testValues[p.name] = p.defaultValue || ''
  })
  testVisible.value = true
}

async function saveForTest(): Promise<boolean> {
  if (!form.name) { ElMessage.warning('请填写工具方法名称'); activeTab.value = 'basic'; return false }
  if (!form.code.trim()) { ElMessage.warning('代码编辑内容不能为空'); activeTab.value = 'code'; return false }
  try {
    const payload = {
      ...form,
      paramDefinitions: generateParamDefinitions(form.code),
      projectId: projectId.value,
    }
    if (toolId.value) {
      await updateTool(projectId.value, toolId.value, payload)
    } else {
      const res: any = await createTool(projectId.value, payload)
      toolId.value = res.data?.id || 0
    }
    return true
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '保存失败')
    return false
  }
}

async function handleTest() {
  const ok = await saveForTest()
  if (!ok) return
  openTestModal()
}

async function runTest() {
  if (!toolId.value) return
  testLoading.value = true
  testResult.value = null
  try {
    const res: any = await testTool(projectId.value, toolId.value, {
      testInput: JSON.stringify(testValues),
    })
    testResult.value = res.data
  } catch (e: any) {
    testResult.value = { success: 0, error: e?.response?.data?.message || e?.message }
  } finally { testLoading.value = false }
}

// ===== 保存成功弹窗（新建模式） =====
const saveModalVisible = ref(false)
const saveModalName = ref('')
function closeSaveModal() {
  saveModalVisible.value = false
}
function continueCreate() {
  saveModalVisible.value = false
  form.name = ''
  form.description = ''
  form.code = DEFAULT_CODE
  toolId.value = 0
  activeTab.value = 'basic'
  fetchCategoryOptions()
}

// ===== 保存 =====
async function handleSubmit() {
  if (!form.name) { ElMessage.warning('请填写工具方法名称'); activeTab.value = 'basic'; return }
  if (!form.code.trim()) { ElMessage.warning('代码编辑内容不能为空'); activeTab.value = 'code'; return }
  try {
    const payload = {
      ...form,
      paramDefinitions: generateParamDefinitions(form.code),
      projectId: projectId.value,
    }
    if (toolId.value) {
      await updateTool(projectId.value, toolId.value, payload)
      ElMessage.success('更新成功')
      router.push(`/project/${projectId.value}/tools`)
    } else {
      await createTool(projectId.value, payload)
      saveModalName.value = form.name
      saveModalVisible.value = true
    }
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '操作失败')
  }
}

// ===== 加载工具数据 =====
async function fetchTool() {
  if (!toolId.value) return
  loading.value = true
  try {
    const res: any = await getTool(projectId.value, toolId.value)
    const data = res.data
    Object.assign(form, {
      name: data.name || '',
      category: data.category || '',
      description: data.description || '',
      code: data.code || DEFAULT_CODE,
      returnType: data.returnType || '',
      paramDefinitions: data.paramDefinitions || '[]',
    })
  } catch { ElMessage.error('加载工具方法失败') } finally { loading.value = false }
}

onMounted(() => {
  fetchCategoryOptions()
  const id = Number(route.params.toolId)
  if (id) {
    toolId.value = id
    fetchTool()
  }
})
</script>

<template>
  <div v-loading="loading">
    <div class="edit-header">
      <div class="edit-title">
        <el-button type="primary" link @click="router.push(`/project/${projectId}/tools`)">← 返回</el-button>
        <h2 style="margin: 0">{{ isEdit ? '编辑工具方法' : '新建工具方法' }}</h2>
      </div>
      <div class="edit-actions">
        <el-button v-if="hasPermission('project:tool:test')" @click="handleTest">测试</el-button>
        <el-button v-if="hasPermission('project:tool:edit')" type="primary" @click="handleSubmit">保存</el-button>
        <el-button @click="router.push(`/project/${projectId}/tools`)">取消</el-button>
      </div>
    </div>

    <el-tabs v-model="activeTab">
      <!-- Tab: 基础信息 -->
      <el-tab-pane label="基础信息" name="basic">
        <el-form label-position="top" style="max-width: 800px">
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item v-if="isEdit" label="ID">
                <el-input :model-value="toolId" disabled />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="工具方法" required>
                <el-input v-model="form.name" placeholder="请输入工具方法名称" maxlength="100" show-word-limit />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="分组" required>
                <el-select
                  v-model="form.category"
                  placeholder="请选择或输入分组名称"
                  filterable
                  allow-create
                  default-first-option
                  style="width: 100%"
                >
                  <el-option v-for="cat in categoryOptions" :key="cat" :value="cat" :label="cat" />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>
          <el-form-item label="描述">
            <el-input v-model="form.description" type="textarea" :rows="2" placeholder="请输入描述" maxlength="500"
              show-word-limit />
          </el-form-item>
        </el-form>
      </el-tab-pane>

      <!-- Tab: 代码编辑 -->
      <el-tab-pane label="代码编辑" name="code">
        <el-card shadow="never">
          <template #header>
            <div class="code-card-header">
              <span class="code-card-title">代码编辑 <span style="color: var(--el-color-danger)">*</span></span>
              <div class="code-card-actions">
                <span class="lang-badge">Groovy</span>
                <el-button size="small" @click="insertTemplate">插入模板</el-button>
                <el-button size="small" @click="formatCode">格式化</el-button>
              </div>
            </div>
          </template>
          <CodeEditor v-model="form.code" :min-height="320" language="groovy"
            placeholder="请输入 Groovy 代码..." />
        </el-card>
      </el-tab-pane>

      <!-- Tab: 引用关系 -->
      <el-tab-pane v-if="isEdit" label="引用关系" name="refs">
        <div class="empty-state">
          <div class="empty-icon">📋</div>
          <div>引用关系详情需后端补充端点支持</div>
          <div style="font-size: 12px; color: #c0c4cc; margin-top: 4px">当前引用次数可在列表页查看</div>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 保存成功弹窗（新建模式） -->
    <el-dialog v-model="saveModalVisible" title="保存工具方法" width="380px" :close-on-click-modal="false">
      <p style="font-size: 14px; color: var(--el-text-color-secondary, #606266); line-height: 1.6;">
        工具方法 <strong style="color: var(--el-color-primary, #409eff);">{{ saveModalName }}</strong> 保存成功！
      </p>
      <template #footer>
        <el-button @click="router.push(`/project/${projectId}/tools`)">返回列表</el-button>
        <el-button type="primary" @click="continueCreate">继续新建</el-button>
      </template>
    </el-dialog>

    <!-- 在线测试弹窗 -->
    <el-dialog v-model="testVisible" title="在线测试" width="600px">
      <div v-if="testParams.length > 0" class="test-params-section">
        <h4 class="test-section-title">测试参数</h4>
        <el-table :data="testParams" border size="small">
          <el-table-column label="参数名" width="160">
            <template #default="{ row }">
              <code>{{ row.name }}</code>
              <span v-if="row.required" style="color: var(--el-color-danger); margin-left: 4px">*</span>
              <el-tag v-if="row.type" size="small" type="info" style="margin-left: 6px">{{ row.type }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="测试值">
            <template #default="{ row }">
              <el-input v-model="testValues[row.name]" :placeholder="row.required ? '必填' : '可选'"
                size="small" style="width: 240px" />
            </template>
          </el-table-column>
        </el-table>
      </div>
      <div v-else class="test-empty">
        <el-empty description="未检测到函数参数" :image-size="60" />
      </div>

      <div v-if="testResult" class="test-result-section">
        <h4 class="test-section-title">执行结果</h4>
        <div class="test-result-meta">
          <span v-if="testResult.executionTimeMs != null">耗时：<b style="color: var(--el-color-success)">{{ testResult.executionTimeMs }}ms</b></span>
          <span>状态：<el-tag :type="testResult.success === 1 ? 'success' : 'danger'" size="small">{{ testResult.success === 1 ? '成功' : '失败' }}</el-tag></span>
        </div>
        <div class="test-output">
          <pre>{{ testResult.output || testResult.error }}</pre>
        </div>
      </div>

      <template #footer>
        <el-button @click="testVisible = false">关闭</el-button>
        <el-button type="primary" :loading="testLoading" @click="runTest">▶ 执行测试</el-button>
      </template>
    </el-dialog>
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
.code-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.code-card-title {
  font-size: 14px;
  font-weight: 600;
}
.code-card-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}
.lang-badge {
  font-size: 11px;
  background: #4299d7;
  color: #fff;
  padding: 1px 8px;
  border-radius: 3px;
  font-weight: 600;
  line-height: 20px;
}
.empty-state {
  text-align: center;
  padding: 48px;
  color: var(--el-text-color-secondary, #909399);
}
.empty-icon {
  font-size: 36px;
  margin-bottom: 8px;
  opacity: 0.4;
}
.test-params-section {
  margin-bottom: 16px;
}
.test-section-title {
  font-size: 13px;
  font-weight: 600;
  margin: 0 0 12px;
}
.test-empty {
  padding: 24px 0;
}
.test-result-section {
  margin-top: 16px;
}
.test-result-meta {
  display: flex;
  gap: 16px;
  margin-bottom: 8px;
  font-size: 13px;
}
.test-output {
  background: #1e1e1e;
  border-radius: 4px;
  padding: 12px;
  min-height: 60px;
}
.test-output pre {
  margin: 0;
  color: #d4d4d4;
  font-size: 12px;
  font-family: Consolas, 'Courier New', monospace;
  white-space: pre-wrap;
  word-break: break-all;
}
</style>
