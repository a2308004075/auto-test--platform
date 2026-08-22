<!--
 @author HXN
 @date 2026-08-21 15:30
 @description 关键字编辑视图
-->
<script setup lang="ts">
/**
 * 接口关键字编辑/新建 - M5
 * 5 Tab：基础信息 / 关联接口 / 测试数据 / 预期响应 / 引用关系
 * 对齐原型 keyword-edit.html（测试数据/断言为可视化表格编辑，数据序列化为 JSON 存储）
 */
import { ref, reactive, onMounted, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getKeyword, createKeyword, updateKeyword } from '@/api/keyword'
import { getApis, getModules } from '@/api/apidoc'
import { useDict } from '@/composables/useDict'
import { usePermission } from '@/composables/usePermission'

const route = useRoute()
const router = useRouter()
const { hasPermission } = usePermission()
const projectId = computed(() => Number(route.params.id))
const keywordId = computed(() => Number(route.params.keywordId))
const isEdit = computed(() => !!keywordId.value)

const methodColors: Record<string, string> = { GET: '', POST: 'success', PUT: 'warning', DELETE: 'danger', PATCH: 'info' }
const { options: paramTypeOptions } = useDict('param_type')

const activeTab = ref('basic')
const loading = ref(false)
const apis = ref<any[]>([])
const modules = ref<any[]>([])

const form = reactive({
  name: '', apiId: null as number | null, description: '',
  category: '', tags: '[]',
  testData: '[]', responseAssertion: '{}',
})

// ===== 测试数据可视化编辑（序列化为 JSON 存入 form.testData） =====
function parseArr(raw?: string): any[] {
  if (!raw) return []
  try { const a = JSON.parse(raw); return Array.isArray(a) ? a : [] } catch { return [] }
}
const testDataRows = ref<any[]>([])
watch(testDataRows, () => { form.testData = JSON.stringify(testDataRows.value) }, { deep: true })

// ===== 断言字段可视化编辑（序列化为 JSON 存入 form.responseAssertion） =====
const expectedStatusCode = ref('')
const assertionFields = ref<any[]>([])
watch([expectedStatusCode, assertionFields], () => {
  form.responseAssertion = JSON.stringify({
    statusCode: expectedStatusCode.value || undefined,
    fields: assertionFields.value,
  })
}, { deep: true })

// ===== 标签编辑（序列化为 JSON 数组存入 form.tags） =====
const tagInput = ref('')
const tags = ref<string[]>([])
watch(tags, () => { form.tags = JSON.stringify(tags.value) }, { deep: true })
function addTag() {
  const v = tagInput.value.trim()
  if (v && !tags.value.includes(v)) {
    tags.value.push(v)
    tagInput.value = ''
  }
}
function removeTag(idx: number) { tags.value.splice(idx, 1) }

// ===== 数据加载 =====
async function fetchApis() {
  try {
    const res: any = await getApis(projectId.value, { page: 1, pageSize: 1000 })
    apis.value = res.data?.items || []
  } catch { apis.value = [] }
}

async function fetchModules() {
  try {
    const res: any = await getModules(projectId.value)
    modules.value = res.data || []
  } catch { modules.value = [] }
}

async function fetchKeyword() {
  if (!isEdit.value) return
  loading.value = true
  try {
    const res: any = await getKeyword(projectId.value, keywordId.value)
    const data = res.data
    Object.assign(form, {
      name: data.name || '',
      apiId: data.apiId ?? null,
      description: data.description || '',
      category: data.category || '',
      tags: data.tags || '[]',
      testData: data.testData || '[]',
      responseAssertion: data.responseAssertion || '{}',
    })
    testDataRows.value = parseArr(form.testData)
    // 解析断言
    try {
      const assertion = JSON.parse(form.responseAssertion)
      expectedStatusCode.value = assertion.statusCode || ''
      assertionFields.value = Array.isArray(assertion.fields) ? assertion.fields : []
    } catch {
      expectedStatusCode.value = ''
      assertionFields.value = []
    }
    tags.value = parseArr(form.tags)
  } catch { ElMessage.error('加载关键字失败') } finally { loading.value = false }
}

// 关联接口信息
const currentApi = computed(() => apis.value.find((a: any) => a.id === form.apiId))
const currentModule = computed(() => {
  const api = currentApi.value
  if (!api?.moduleId) return null
  return modules.value.find((m: any) => m.id === api.moduleId)
})

// ===== 保存 =====
async function handleSubmit() {
  if (!form.name) { ElMessage.warning('请填写关键字名称'); activeTab.value = 'basic'; return }
  if (!form.apiId) { ElMessage.warning('请选择关联接口'); activeTab.value = 'api'; return }
  try {
    const payload = { ...form, projectId: projectId.value }
    if (isEdit.value) {
      await updateKeyword(projectId.value, keywordId.value, payload)
      ElMessage.success('更新成功')
    } else {
      await createKeyword(projectId.value, payload)
      ElMessage.success('创建成功')
    }
    router.push(`/project/${projectId.value}/keywords`)
  } catch (e: any) { ElMessage.error(e?.response?.data?.message || '操作失败') }
}

onMounted(() => {
  fetchApis()
  fetchModules()
  fetchKeyword()
})
</script>

<template>
  <div v-loading="loading">
    <div class="edit-header">
      <div class="edit-title">
        <el-button type="primary" link @click="router.back()">← 返回</el-button>
        <h2 style="margin: 0">{{ isEdit ? '编辑关键字' : '新建关键字' }}</h2>
      </div>
      <div class="edit-actions">
        <el-button v-if="hasPermission('project:keyword:edit')" type="primary" @click="handleSubmit">保存</el-button>
        <el-button @click="router.back()">取消</el-button>
      </div>
    </div>

    <el-tabs v-model="activeTab">
      <!-- Tab: 基础信息 -->
      <el-tab-pane label="基础信息" name="basic">
        <el-form label-position="top" style="max-width: 800px">
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="关键字 ID" v-if="isEdit">
                <el-input :model-value="keywordId" disabled />
              </el-form-item>
            </el-col>
          </el-row>
          <el-form-item label="关键字名称" required>
            <el-input v-model="form.name" placeholder="请输入关键字名称" />
          </el-form-item>
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="分类">
                <el-input v-model="form.category" placeholder="如：用户管理" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="标签">
                <div class="tag-editor">
                  <el-tag v-for="(t, i) in tags" :key="t" closable size="small" style="margin-right: 4px" @close="removeTag(i)">{{ t }}</el-tag>
                  <el-input v-model="tagInput" placeholder="+ 添加" size="small" style="width: 100px" @keyup.enter="addTag" />
                </div>
              </el-form-item>
            </el-col>
          </el-row>
          <el-form-item label="描述">
            <el-input v-model="form.description" type="textarea" :rows="2" />
          </el-form-item>
        </el-form>
      </el-tab-pane>

      <!-- Tab: 关联接口 -->
      <el-tab-pane label="关联接口" name="api">
        <div v-if="currentApi" class="api-info-bar">
          <el-tag :type="methodColors[currentApi.httpMethod] || 'info'" size="small">{{ currentApi.httpMethod }}</el-tag>
          <code style="font-size: 13px">{{ currentApi.path }}</code>
          <el-tag v-if="currentModule" size="small" type="info">{{ currentModule.name }}</el-tag>
          <span style="color: #909399">{{ currentApi.description }}</span>
          <el-button link type="primary" style="margin-left: auto" @click="router.push(`/project/${projectId}/apis/${currentApi.id}/edit`)">查看接口 →</el-button>
        </div>
        <el-form label-position="top" style="max-width: 800px; margin-top: 16px">
          <el-form-item label="关联接口" required>
            <el-select v-model="form.apiId" placeholder="选择关联接口" filterable :disabled="isEdit" style="width: 100%">
              <el-option v-for="api in apis" :key="api.id" :value="api.id" :label="`[${api.httpMethod}] ${api.path} - ${api.name}`" />
            </el-select>
          </el-form-item>
          <p v-if="isEdit" class="empty-hint">关联接口在创建后不可更改</p>
        </el-form>
      </el-tab-pane>

      <!-- Tab: 测试数据 -->
      <el-tab-pane label="测试数据" name="testdata">
        <div class="params-section">
          <div class="section-head">
            <h4>请求参数</h4>
            <el-button size="small" @click="testDataRows.push({ name: '', type: 'string', value: '', description: '' })">+ 添加数据组</el-button>
          </div>
          <el-table :data="testDataRows" size="small" border style="max-width: 800px">
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
            <el-table-column label="预设值" width="200">
              <template #default="{ row }"><el-input v-model="row.value" size="small" placeholder="预设值" /></template>
            </el-table-column>
            <el-table-column label="说明">
              <template #default="{ row }"><el-input v-model="row.description" size="small" placeholder="说明" /></template>
            </el-table-column>
            <el-table-column label="操作" width="70">
              <template #default="{ $index }">
                <el-button link size="small" type="danger" @click="testDataRows.splice($index, 1)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>

      <!-- Tab: 预期响应 -->
      <el-tab-pane label="预期响应" name="response">
        <div class="params-section">
          <div class="status-code-header">
            <h4 style="margin: 0">预期状态码</h4>
            <el-input v-model="expectedStatusCode" placeholder="如 200" style="width: 80px" />
            <el-button size="small" @click="assertionFields.push({ path: '', expected: '', description: '' })">+ 添加断言字段</el-button>
          </div>
          <el-table :data="assertionFields" size="small" border style="max-width: 800px; margin-top: 12px">
            <el-table-column label="字段路径" width="200">
              <template #default="{ row }"><el-input v-model="row.path" size="small" placeholder="如 data.token" style="font-family: monospace" /></template>
            </el-table-column>
            <el-table-column label="预期值" width="200">
              <template #default="{ row }"><el-input v-model="row.expected" size="small" placeholder="预期值" /></template>
            </el-table-column>
            <el-table-column label="说明">
              <template #default="{ row }"><el-input v-model="row.description" size="small" placeholder="说明" /></template>
            </el-table-column>
            <el-table-column label="操作" width="70">
              <template #default="{ $index }">
                <el-button link size="small" type="danger" @click="assertionFields.splice($index, 1)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>

      <!-- Tab: 引用关系 -->
      <el-tab-pane label="引用关系" name="refs" :disabled="!isEdit">
        <div class="empty-state">
          <div class="empty-icon">📋</div>
          <div>引用关系详情需后端补充端点支持</div>
          <div style="font-size: 12px; color: #c0c4cc; margin-top: 4px">当前引用次数可在列表页"引用"列查看</div>
        </div>
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
.status-code-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 10px;
}
.api-info-bar {
  padding: 10px 12px;
  background: #fafafa;
  border-radius: 4px;
  display: flex;
  gap: 12px;
  font-size: 13px;
  align-items: center;
}
.tag-editor {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-wrap: wrap;
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
</style>
