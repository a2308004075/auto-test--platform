<script setup lang="ts">
/**
 * 测试用例编辑 - M8
 * 基本信息 + 步骤树（JSON）+ Setup/Teardown
 */
import { reactive, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { getCase, createCase, updateCase } from '@/api/case'

const route = useRoute()
const router = useRouter()
const projectId = computed(() => route.params.id as string)
const caseId = computed(() => route.params.caseId as string)
const querySuiteId = computed(() => (route.query.suiteId as string) || '')
const isEdit = computed(() => !!caseId.value)

const form = reactive({
  name: '',
  description: '',
  preconditions: '',
  setupSteps: '[]',
  teardownSteps: '[]',
  steps: '[]',
  priority: 'P2',
  timeout: 30,
  suiteId: '',
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
      suiteId: c.suiteId || '',
    })
  } catch { message.error('加载用例失败') }
}

function formatJson(field: 'setupSteps' | 'teardownSteps' | 'steps') {
  try {
    form[field] = JSON.stringify(JSON.parse(form[field] || '[]'), null, 2)
  } catch {
    message.warning('JSON 格式错误，无法格式化')
  }
}

function validateJson(): boolean {
  for (const f of ['setupSteps', 'teardownSteps', 'steps'] as const) {
    try {
      JSON.parse(form[f] || '[]')
    } catch {
      message.warning(`${f} 不是有效的 JSON`)
      return false
    }
  }
  return true
}

async function handleSave() {
  if (!form.name) { message.warning('请输入用例名称'); return }
  if (!validateJson()) return
  try {
    if (isEdit.value) {
      await updateCase(projectId.value, caseId.value, {
        name: form.name, description: form.description, preconditions: form.preconditions,
        setupSteps: form.setupSteps, teardownSteps: form.teardownSteps, steps: form.steps,
        priority: form.priority, timeout: form.timeout,
      })
      message.success('保存成功')
    } else {
      await createCase(projectId.value, {
        ...form,
        suiteId: querySuiteId.value || form.suiteId,
      })
      message.success('创建成功')
      router.push(`/project/${projectId}/cases?suiteId=${querySuiteId.value}`)
    }
  } catch (e: any) {
    message.error(e?.response?.data?.message || '保存失败')
  }
}

onMounted(() => { if (isEdit.value) loadCase() })
</script>

<template>
  <div>
    <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:16px">
      <h2 style="margin:0">{{ isEdit ? '编辑用例' : '新建用例' }}</h2>
      <a-space>
        <a-button @click="router.back()">返回</a-button>
        <a-button type="primary" @click="handleSave">保存</a-button>
      </a-space>
    </div>

    <a-row :gutter="16">
      <a-col :span="10">
        <a-card title="基本信息" size="small">
          <a-form layout="vertical">
            <a-form-item label="用例名称" required><a-input v-model:value="form.name" /></a-form-item>
            <a-form-item label="描述"><a-textarea v-model:value="form.description" :rows="2" /></a-form-item>
            <a-form-item label="前置条件"><a-textarea v-model:value="form.preconditions" :rows="3" /></a-form-item>
            <a-row :gutter="12">
              <a-col :span="12">
                <a-form-item label="优先级">
                  <a-select v-model:value="form.priority">
                    <a-select-option value="P0">P0</a-select-option>
                    <a-select-option value="P1">P1</a-select-option>
                    <a-select-option value="P2">P2</a-select-option>
                    <a-select-option value="P3">P3</a-select-option>
                  </a-select>
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="超时(秒)">
                  <a-input-number v-model:value="form.timeout" :min="1" :max="3600" style="width:100%" />
                </a-form-item>
              </a-col>
            </a-row>
          </a-form>
        </a-card>
      </a-col>

      <a-col :span="14">
        <a-card size="small">
          <template #title>
            步骤树
          </template>
          <template #extra>
            <a-button size="small" @click="formatJson('steps')">格式化</a-button>
          </template>
          <div style="color:#999;font-size:12px;margin-bottom:6px">
            JSON 数组，每个元素为一个关键字步骤，例如：
            <code>{ "keywordId": "xxx", "name": "步骤名", "params": {}, "assertions": [] }</code>
          </div>
          <a-textarea
            v-model:value="form.steps"
            :rows="22"
            style="font-family:monospace;font-size:12px"
          />
        </a-card>
      </a-col>
    </a-row>

    <a-card size="small" style="margin-top:16px">
      <template #title>Setup / Teardown 步骤树</template>
      <a-row :gutter="16">
        <a-col :span="12">
          <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:6px">
            <span style="font-weight:600">Setup</span>
            <a-button size="small" @click="formatJson('setupSteps')">格式化</a-button>
          </div>
          <a-textarea v-model:value="form.setupSteps" :rows="8" style="font-family:monospace;font-size:12px" />
        </a-col>
        <a-col :span="12">
          <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:6px">
            <span style="font-weight:600">Teardown</span>
            <a-button size="small" @click="formatJson('teardownSteps')">格式化</a-button>
          </div>
          <a-textarea v-model:value="form.teardownSteps" :rows="8" style="font-family:monospace;font-size:12px" />
        </a-col>
      </a-row>
    </a-card>
  </div>
</template>
