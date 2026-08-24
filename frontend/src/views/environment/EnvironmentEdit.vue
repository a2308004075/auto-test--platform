<!--
 @author HXN
 @date 2026-08-23
 @description 环境变量编辑视图
-->
<script setup lang="ts">
/**
 * 环境变量编辑页 - 独立页面
 * 对齐原型 environment-edit.html
 */
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { InfoFilled } from '@element-plus/icons-vue'
import { getEnvironment, updateEnvironment } from '@/api/environment'
import EditPageHeader from '@/components/EditPageHeader/index.vue'
import CodeEditor from '@/components/CodeEditor/index.vue'
import { useProjectStore } from '@/stores/modules/project'

const route = useRoute()
const router = useRouter()
const projectStore = useProjectStore()

const projectId = computed(() => Number(route.params.id))
const envId = computed(() => Number(route.params.envId))

const loading = ref(false)
const saving = ref(false)
const envName = ref('')
const envDescription = ref('')

interface VarRow {
  varKey: string
  varValue: string
  dataType: string
  description: string
  isFixed?: boolean
}

const variables = ref<VarRow[]>([])
const varCount = computed(() => variables.value.length)

async function fetchDetail() {
  loading.value = true
  try {
    const res: any = await getEnvironment(projectId.value, envId.value)
    const data = res.data
    if (data) {
      envName.value = data.name || ''
      envDescription.value = data.description || ''
      const apiVars: VarRow[] = (data.variables || []).map((v: any) => ({
        varKey: v.varKey || '',
        varValue: v.varValue || '',
        dataType: v.dataType || 'text',
        description: v.description || '',
      }))
      // 确保固定变量 host、authorization 始终在顶部
      const hostVar = apiVars.find(v => v.varKey === 'host') || { varKey: 'host', varValue: '', dataType: 'text', description: '' }
      const authorizationVar = apiVars.find(v => v.varKey === 'authorization') || { varKey: 'authorization', varValue: '', dataType: 'text', description: '' }
      const customVars = apiVars.filter(v => v.varKey !== 'host' && v.varKey !== 'authorization')
      variables.value = [
        { ...hostVar, isFixed: true, dataType: 'text' },
        { ...authorizationVar, isFixed: true, dataType: 'text' },
        ...customVars.map(v => ({ ...v, isFixed: false })),
      ]
    }
  } catch (e: any) {
    ElMessage.error('加载环境详情失败')
  } finally {
    loading.value = false
  }
}

function addVarRow() {
  variables.value.push({ varKey: '', varValue: '', dataType: 'text', description: '', isFixed: false })
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

/**
 * 校验变量列表
 * 1. 变量名不能为空
 * 2. 变量名不能重复
 */
function validateVariables(): boolean {
  const customVars = variables.value.filter(v => !v.isFixed)
  for (let i = 0; i < customVars.length; i++) {
    if (!customVars[i].varKey.trim()) {
      const actualIndex = variables.value.indexOf(customVars[i])
      ElMessage.warning(`第 ${actualIndex + 1} 行的变量名不能为空`)
      return false
    }
  }
  const keys = customVars.map((v) => v.varKey.trim())
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
    await updateEnvironment(projectId.value, envId.value, {
      name: envName.value,
      description: envDescription.value,
      variables: variables.value,
    })
    ElMessage.success('保存成功')
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

function handleCancel() {
  router.push(`/project/${projectId.value}/environments`)
}

onMounted(fetchDetail)
</script>

<template>
  <div>
    <!-- 页面头部 -->
    <EditPageHeader title="编辑环境变量" :back-route="`/project/${projectId}/environments`">
      <el-button @click="handleCancel">取消</el-button>
      <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
    </EditPageHeader>

    <!-- 项目上下文栏 -->
    <div class="env-project-bar">
      <span>&#x1F4CC;</span>
      <span>当前项目：<span class="project-name">{{ projectStore.currentProjectName }}</span></span>
      <span class="bar-sep">|</span>
      <span>每个项目独立管理环境配置，环境切换不影响其他项目</span>
    </div>

    <!-- 环境信息卡片 -->
    <div v-loading="loading" class="env-edit-card">
      <div class="env-edit-header">
        <el-input v-model="envName" placeholder="环境名称" maxlength="50" show-word-limit style="font-size: 18px; font-weight: 600;" />
        <el-input v-model="envDescription" type="textarea" :rows="2" placeholder="环境描述" maxlength="200" show-word-limit style="margin-top: 8px;" />
      </div>

      <!-- 变量工具栏 -->
      <div class="env-edit-toolbar">
        <el-button type="primary" size="small" @click="addVarRow">+ 添加变量</el-button>
        <span class="var-hint">host、authorization 为固定变量，不可删除或重命名</span>
        <el-tooltip placement="bottom" effect="light">
          <template #content>
            <div style="max-width: 360px; line-height: 1.7;">
              <div style="font-weight: 600; margin-bottom: 4px;">如何引用环境变量</div>
              <div>在测试用例、接口调试、关键字、执行计划中，使用 <code style="background:#f5f5f5; padding:1px 4px; border-radius:3px;">${变量名}</code> 语法引用。</div>
              <div style="margin-top: 6px; color: #999;">示例：<code style="background:#f5f5f5; padding:1px 4px; border-radius:3px;">${host}</code> 替换为当前环境的 host 值</div>
              <div style="margin-top: 6px; color: #999;">环境变量仅在选中此环境时生效；项目全局变量在所有环境下均可引用，同名时环境变量优先</div>
            </div>
          </template>
          <el-icon class="var-help-icon"><InfoFilled /></el-icon>
        </el-tooltip>
        <span class="var-count">共 {{ varCount }} 个变量</span>
      </div>

      <!-- 变量表格 -->
      <div class="var-table-wrapper">
        <table class="var-table">
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
            <tr v-for="(row, index) in variables" :key="index" class="var-row">
              <td>
                <div class="key-cell">
                  <span class="required-dot" title="必填"></span>
                  <el-input v-model="row.varKey" placeholder="变量名" maxlength="100" :disabled="row.isFixed" />
                </div>
              </td>
              <td>
                <div class="type-cell">
                  <el-select v-model="row.dataType" :disabled="row.isFixed" size="small" style="flex: 1">
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
                <el-button v-if="!row.isFixed" type="danger" link size="small" @click="removeRow(index)">
                  删除
                </el-button>
                <span v-else class="fixed-tag">固定</span>
              </td>
            </tr>
            <tr v-if="variables.length === 0">
              <td colspan="5" class="empty-tip">暂无变量，点击"添加变量"开始配置</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<style scoped>
.env-project-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 16px;
  background: #ecf5ff;
  border: 1px solid #c6e2ff;
  border-radius: 6px;
  margin-bottom: 16px;
  font-size: 13px;
  color: rgba(0, 0, 0, 0.65);
}

.env-project-bar .project-name {
  font-weight: 600;
  color: #409eff;
}

.env-project-bar .bar-sep {
  color: rgba(0, 0, 0, 0.25);
}

.env-edit-card {
  background: #fff;
  border-radius: 8px;
  border: 1px solid #f0f0f0;
}

.env-edit-header {
  padding: 20px 24px;
  border-bottom: 1px solid #f0f0f0;
}

.env-edit-header h3 {
  font-size: 18px;
  font-weight: 600;
  margin: 0 0 4px;
}

.env-edit-header .env-desc {
  font-size: 13px;
  color: rgba(0, 0, 0, 0.45);
  margin: 0;
}

.env-edit-toolbar {
  padding: 12px 24px;
  display: flex;
  align-items: center;
  gap: 8px;
  border-bottom: 1px solid #f0f0f0;
  background: #fafafa;
}

.var-count {
  margin-left: auto;
  font-size: 12px;
  color: rgba(0, 0, 0, 0.45);
}

.var-hint {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.45);
}

.var-help-icon {
  color: #909399;
  cursor: pointer;
  font-size: 16px;
}

.fixed-tag {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.25);
}

.var-table-wrapper {
  overflow-x: auto;
}

.var-table {
  width: 100%;
  border-collapse: collapse;
  table-layout: fixed;
}

.var-table th {
  padding: 10px 16px;
  text-align: left;
  font-size: 13px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.85);
  background: #fafafa;
  border-bottom: 1px solid #f0f0f0;
}

.var-table .col-key {
  width: 180px;
}

.var-table .col-type {
  width: 190px;
}

.var-table .col-value {
  width: auto;
}

.var-table .col-desc {
  width: 220px;
}

.var-table .col-action {
  width: 80px;
  text-align: center;
}

.var-table td {
  padding: 8px 16px;
  vertical-align: middle;
  border-bottom: 1px solid #f0f0f0;
}

.var-row:hover {
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

.action-cell {
  text-align: center;
}

.empty-tip {
  text-align: center;
  padding: 32px 16px !important;
  color: rgba(0, 0, 0, 0.25);
  font-size: 13px;
}

.type-cell {
  display: flex;
  align-items: center;
  gap: 4px;
}
</style>
