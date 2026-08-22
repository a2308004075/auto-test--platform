<!--
 @author HXN
 @date 2026-08-20 15:34
 @description 测试用例编辑视图
-->
<script setup lang="ts">
/**
 * 测试用例编辑 - M8
 * 基本信息 + 步骤树（JSON）+ Setup/Teardown
 */
import { reactive, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getCase, createCase, updateCase } from '@/api/case'
import { useDict } from '@/composables/useDict'
import { usePermission } from '@/composables/usePermission'

const route = useRoute()
const router = useRouter()
const { hasPermission } = usePermission()
const projectId = computed(() => Number(route.params.id))
const caseId = computed(() => Number(route.params.caseId))
const querySuiteId = computed(() => Number(route.query.suiteId) || 0)
const isEdit = computed(() => !!caseId.value)
const { options: priorityOptions } = useDict('priority')

const form = reactive({
  name: '',
  description: '',
  preconditions: '',
  setupSteps: '[]',
  teardownSteps: '[]',
  steps: '[]',
  priority: 'P2',
  timeout: 30,
  suiteId: null,
})

async function loadCase() {
  if (!caseId.value) return
  try {
    const res: any = await getCase(projectId.value, caseId.value)
    const c = res.data
    Object.assign(form, {
      name: c.name || '',
      description: c.description || '',
      preconditions: c.preconditions || '',
      setupSteps: c.setupSteps || '[]',
      teardownSteps: c.teardownSteps || '[]',
      steps: c.steps || '[]',
      priority: c.priority || 'P2',
      timeout: c.timeout || 30,
      suiteId: c.suiteId || null,
    })
  } catch { ElMessage.error('加载用例失败') }
}

function formatJson(field: 'setupSteps' | 'teardownSteps' | 'steps') {
  try {
    form[field] = JSON.stringify(JSON.parse(form[field] || '[]'), null, 2)
  } catch {
    ElMessage.warning('JSON 格式错误，无法格式化')
  }
}

function validateJson(): boolean {
  for (const f of ['setupSteps', 'teardownSteps', 'steps'] as const) {
    try {
      JSON.parse(form[f] || '[]')
    } catch {
      ElMessage.warning(`${f} 不是有效的 JSON`)
      return false
    }
  }
  return true
}

async function handleSave() {
  if (!form.name) { ElMessage.warning('请输入用例名称'); return }
  if (!validateJson()) return
  try {
    if (isEdit.value) {
      await updateCase(projectId.value, caseId.value, {
        name: form.name, description: form.description, preconditions: form.preconditions,
        setupSteps: form.setupSteps, teardownSteps: form.teardownSteps, steps: form.steps,
        priority: form.priority, timeout: form.timeout,
      })
      ElMessage.success('保存成功')
    } else {
      await createCase(projectId.value, {
        ...form,
        suiteId: querySuiteId.value || form.suiteId,
      })
      ElMessage.success('创建成功')
      router.push(`/project/${projectId}/cases?suiteId=${querySuiteId.value}`)
    }
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '保存失败')
  }
}

onMounted(() => { if (isEdit.value) loadCase() })
</script>

<template>
  <div>
    <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:16px">
      <h2 style="margin:0">{{ isEdit ? '编辑用例' : '新建用例' }}</h2>
      <div style="display:flex;gap:8px">
        <el-button @click="router.back()">返回</el-button>
        <el-button v-if="hasPermission('project:case:edit')" type="primary" @click="handleSave">保存</el-button>
      </div>
    </div>

    <el-row :gutter="16">
      <el-col :span="10">
        <el-card>
          <template #header><span>基本信息</span></template>
          <el-form label-position="top">
            <el-form-item label="用例名称" required>
              <el-input v-model="form.name" />
            </el-form-item>
            <el-form-item label="描述">
              <el-input v-model="form.description" type="textarea" :rows="2" />
            </el-form-item>
            <el-form-item label="前置条件">
              <el-input v-model="form.preconditions" type="textarea" :rows="3" />
            </el-form-item>
            <el-row :gutter="12">
              <el-col :span="12">
                <el-form-item label="优先级">
                  <el-select v-model="form.priority" style="width:100%">
                    <el-option v-for="p in priorityOptions" :key="p.value" :value="p.value" :label="p.label" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="超时(秒)">
                  <el-input-number v-model="form.timeout" :min="1" :max="3600" style="width:100%" />
                </el-form-item>
              </el-col>
            </el-row>
          </el-form>
        </el-card>
      </el-col>

      <el-col :span="14">
        <el-card>
          <template #header>
            <div style="display:flex;justify-content:space-between;align-items:center">
              <span>步骤树</span>
              <el-button size="small" @click="formatJson('steps')">格式化</el-button>
            </div>
          </template>
          <div style="color:#909399;font-size:12px;margin-bottom:6px">
            JSON 数组，每个元素为一个关键字步骤，例如：
            <code>{ "keywordId": "xxx", "name": "步骤名", "params": {}, "assertions": [] }</code>
          </div>
          <el-input v-model="form.steps" type="textarea" :rows="22" style="font-family:monospace;font-size:12px" />
        </el-card>
      </el-col>
    </el-row>

    <el-card style="margin-top:16px">
      <template #header><span>Setup / Teardown 步骤树</span></template>
      <el-row :gutter="16">
        <el-col :span="12">
          <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:6px">
            <span style="font-weight:600">Setup</span>
            <el-button size="small" @click="formatJson('setupSteps')">格式化</el-button>
          </div>
          <el-input v-model="form.setupSteps" type="textarea" :rows="8" style="font-family:monospace;font-size:12px" />
        </el-col>
        <el-col :span="12">
          <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:6px">
            <span style="font-weight:600">Teardown</span>
            <el-button size="small" @click="formatJson('teardownSteps')">格式化</el-button>
          </div>
          <el-input v-model="form.teardownSteps" type="textarea" :rows="8" style="font-family:monospace;font-size:12px" />
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>
