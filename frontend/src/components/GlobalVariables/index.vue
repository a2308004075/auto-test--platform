<!--
 @author HXN
 @date 2026-08-24
 @description 项目全局变量组件（不绑定环境，整个项目任何地方可引用）
-->
<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { InfoFilled } from '@element-plus/icons-vue'
import { getProjectVariables, updateProjectVariables } from '@/api/environment'
import CodeEditor from '@/components/CodeEditor/index.vue'

const props = defineProps<{
  projectId: number
}>()

interface VarRow {
  varKey: string
  varValue: string
  dataType: string
  description: string
}

const loading = ref(false)
const saving = ref(false)
const variables = ref<VarRow[]>([])
const varCount = computed(() => variables.value.length)

async function fetchVariables() {
  loading.value = true
  try {
    const res: any = await getProjectVariables(props.projectId)
    const data = res.data || []
    variables.value = data.map((v: any) => ({
      varKey: v.varKey || '',
      varValue: v.varValue || '',
      dataType: v.dataType || 'text',
      description: v.description || '',
    }))
  } catch {
    variables.value = []
  } finally {
    loading.value = false
  }
}

function addVarRow() {
  variables.value.push({ varKey: '', varValue: '', dataType: 'text', description: '' })
}

function removeRow(index: number) {
  variables.value.splice(index, 1)
}

function formatJson(row: VarRow) {
  if (!row.varValue.trim()) return
  try {
    row.varValue = JSON.stringify(JSON.parse(row.varValue), null, 2)
  } catch {
    ElMessage.warning('JSON 格式不正确')
  }
}

function validateVariables(): boolean {
  for (let i = 0; i < variables.value.length; i++) {
    if (!variables.value[i].varKey.trim()) {
      ElMessage.warning(`第 ${i + 1} 行的变量名不能为空`)
      return false
    }
  }
  const keys = variables.value.map((v) => v.varKey.trim())
  const seen = new Set<string>()
  for (let i = 0; i < keys.length; i++) {
    if (seen.has(keys[i])) {
      ElMessage.warning(`变量名「${keys[i]}」重复，请检查`)
      return false
    }
    seen.add(keys[i])
  }
  return true
}

async function handleSave() {
  if (!validateVariables()) return
  saving.value = true
  try {
    await updateProjectVariables(props.projectId, variables.value)
    ElMessage.success('保存成功')
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

watch(() => props.projectId, fetchVariables, { immediate: true })
</script>

<template>
  <div class="gv-section">
    <!-- 标题栏 -->
    <div class="gv-header">
      <div class="gv-title">
        <h3>全局变量</h3>
        <span class="gv-subtitle">不区分环境，整个项目任何地方可引用；环境变量同名时优先环境变量</span>
        <el-tooltip placement="bottom" effect="light">
          <template #content>
            <div style="max-width: 360px; line-height: 1.7;">
              <div style="font-weight: 600; margin-bottom: 4px;">如何引用全局变量</div>
              <div>在测试用例、接口调试、关键字、执行计划中，使用 <code style="background:#f5f5f5; padding:1px 4px; border-radius:3px;">${变量名}</code> 语法引用。</div>
              <div style="margin-top: 6px; color: #999;">示例：<code style="background:#f5f5f5; padding:1px 4px; border-radius:3px;">${token}</code> 替换为全局变量 token 的值</div>
              <div style="margin-top: 6px; color: #999;">全局变量在所有环境下均可引用；当环境变量与全局变量同名时，环境变量优先</div>
            </div>
          </template>
          <el-icon class="gv-help-icon"><InfoFilled /></el-icon>
        </el-tooltip>
      </div>
      <div class="gv-actions">
        <el-button type="primary" size="small" @click="addVarRow">+ 添加变量</el-button>
        <el-button type="success" size="small" :loading="saving" @click="handleSave">保存</el-button>
      </div>
    </div>

    <!-- 变量表格 -->
    <div v-loading="loading" class="gv-table-wrapper">
      <table class="gv-table">
        <thead>
          <tr>
            <th class="col-key">变量名</th>
            <th class="col-type">数据类型</th>
            <th class="col-value">变量值</th>
            <th class="col-desc">描述</th>
            <th class="col-action">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(row, index) in variables" :key="index" class="gv-row">
            <td>
              <div class="key-cell">
                <span class="required-dot" title="必填"></span>
                <el-input v-model="row.varKey" placeholder="变量名" maxlength="100" />
              </div>
            </td>
            <td>
              <div class="type-cell">
                <el-select v-model="row.dataType" size="small" style="flex: 1">
                  <el-option label="文本" value="text" />
                  <el-option label="数字" value="number" />
                  <el-option label="JSON" value="json" />
                  <el-option label="脚本" value="script" />
                </el-select>
                <el-button v-if="row.dataType === 'json'" size="small" @click="formatJson(row)">格式化</el-button>
              </div>
            </td>
            <td>
              <CodeEditor
                v-if="row.dataType === 'json' || row.dataType === 'script'"
                v-model="row.varValue"
                :language="row.dataType === 'json' ? 'json' : 'javascript'"
                :min-height="120"
                :placeholder="row.dataType === 'json' ? 'JSON 值' : 'JavaScript 脚本，如：Math.random().toString(36).substring(2)'"
              />
              <el-input v-else v-model="row.varValue" placeholder="变量值" />
            </td>
            <td>
              <el-input v-model="row.description" placeholder="描述" />
            </td>
            <td class="action-cell">
              <el-button type="danger" link size="small" @click="removeRow(index)">
                删除
              </el-button>
            </td>
          </tr>
          <tr v-if="variables.length === 0">
            <td colspan="5" class="empty-tip">暂无全局变量，点击"添加变量"开始配置</td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<style scoped>
.gv-section {
  background: #fff;
  border-radius: 8px;
  border: 1px solid #f0f0f0;
  margin-top: 16px;
}

.gv-header {
  padding: 12px 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #f0f0f0;
  background: #fafafa;
}

.gv-title {
  display: flex;
  align-items: baseline;
  gap: 8px;
}

.gv-title h3 {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
}

.gv-subtitle {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.45);
}

.gv-help-icon {
  color: #909399;
  cursor: pointer;
  font-size: 16px;
}

.gv-actions {
  display: flex;
  gap: 8px;
}

.gv-table-wrapper {
  overflow-x: auto;
}

.gv-table {
  width: 100%;
  border-collapse: collapse;
  table-layout: fixed;
}

.gv-table th {
  padding: 10px 16px;
  text-align: left;
  font-size: 13px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.85);
  background: #fafafa;
  border-bottom: 1px solid #f0f0f0;
}

.gv-table .col-key { width: 180px; }
.gv-table .col-type { width: 190px; }
.gv-table .col-value { width: auto; }
.gv-table .col-desc { width: 220px; }
.gv-table .col-action { width: 80px; text-align: center; }

.gv-table td {
  padding: 8px 16px;
  vertical-align: middle;
  border-bottom: 1px solid #f0f0f0;
}

.gv-row:hover {
  background: #fafafa;
}

.key-cell {
  display: flex;
  align-items: center;
  gap: 4px;
}

.key-cell .required-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #f56c6c;
  flex-shrink: 0;
}

.key-cell :deep(.el-input) {
  flex: 1;
}

.type-cell {
  display: flex;
  align-items: center;
  gap: 4px;
}

.action-cell {
  text-align: center;
}

.empty-tip {
  text-align: center;
  padding: 32px 16px !important;
  color: rgba(0, 0, 0, 0.25);
  font-size: 13px;
}
</style>
