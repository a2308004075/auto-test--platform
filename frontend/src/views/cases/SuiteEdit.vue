<script setup lang="ts">
/**
 * 测试套件编辑 - M8
 * 基本信息 + 套件级生命周期步骤树（Once Setup/Teardown + Per-Case Setup/Teardown）
 */
import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { getSuite, updateSuite } from '@/api/suite'

const route = useRoute()
const router = useRouter()
const projectId = computed(() => route.params.id as string)
const suiteId = computed(() => route.params.suiteId as string)

const loading = ref(false)
const saving = ref(false)

const form = reactive({
  name: '',
  description: '',
  priority: 'P2',
  tags: '[]',
  enableOnceSetupTeardown: false,
  onceSetupSteps: '[]',
  onceTeardownSteps: '[]',
  enablePerCaseSetupTeardown: false,
  perCaseSetupSteps: '[]',
  perCaseTeardownSteps: '[]',
})

async function loadSuite() {
  loading.value = true
  try {
    const res: any = await getSuite(projectId.value, suiteId.value)
    const s = res.data
    Object.assign(form, {
      name: s.name || '',
      description: s.description || '',
      priority: s.priority || 'P2',
      tags: s.tags || '[]',
      enableOnceSetupTeardown: s.enableOnceSetupTeardown ?? false,
      onceSetupSteps: s.onceSetupSteps || '[]',
      onceTeardownSteps: s.onceTeardownSteps || '[]',
      enablePerCaseSetupTeardown: s.enablePerCaseSetupTeardown ?? false,
      perCaseSetupSteps: s.perCaseSetupSteps || '[]',
      perCaseTeardownSteps: s.perCaseTeardownSteps || '[]',
    })
  } catch { message.error('加载套件失败') } finally { loading.value = false }
}

function formatJson(field: keyof typeof form) {
  try {
    (form as any)[field] = JSON.stringify(JSON.parse((form as any)[field] || '[]'), null, 2)
  } catch {
    message.warning('JSON 格式错误，无法格式化')
  }
}

function validateJson(): boolean {
  const jsonFields = ['onceSetupSteps', 'onceTeardownSteps', 'perCaseSetupSteps', 'perCaseTeardownSteps', 'tags'] as const
  for (const f of jsonFields) {
    try {
      JSON.parse((form as any)[f] || '[]')
    } catch {
      message.warning(`${f} 不是有效的 JSON`)
      return false
    }
  }
  return true
}

async function handleSave() {
  if (!form.name) { message.warning('请输入套件名称'); return }
  if (!validateJson()) return
  saving.value = true
  try {
    await updateSuite(projectId.value, suiteId.value, { ...form })
    message.success('保存成功')
  } catch (e: any) {
    message.error(e?.response?.data?.message || '保存失败')
  } finally { saving.value = false }
}

onMounted(loadSuite)
</script>

<template>
  <div v-if="loading" style="text-align:center;padding:60px"><a-spin size="large" /></div>
  <div v-else>
    <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:16px">
      <h2 style="margin:0">编辑套件</h2>
      <a-space>
        <a-button @click="router.back()">返回</a-button>
        <a-button type="primary" :loading="saving" @click="handleSave">保存</a-button>
      </a-space>
    </div>

    <!-- 基本信息 -->
    <a-card title="基本信息" size="small" style="margin-bottom:16px">
      <a-row :gutter="16">
        <a-col :span="8">
          <a-form-item label="套件名称" required>
            <a-input v-model:value="form.name" />
          </a-form-item>
        </a-col>
        <a-col :span="6">
          <a-form-item label="优先级">
            <a-select v-model:value="form.priority">
              <a-select-option value="P0">P0</a-select-option>
              <a-select-option value="P1">P1</a-select-option>
              <a-select-option value="P2">P2</a-select-option>
              <a-select-option value="P3">P3</a-select-option>
            </a-select>
          </a-form-item>
        </a-col>
        <a-col :span="10">
          <a-form-item label="描述"><a-input v-model:value="form.description" /></a-form-item>
        </a-col>
      </a-row>
    </a-card>

    <!-- 套件级整体生命周期 -->
    <a-card size="small" style="margin-bottom:16px">
      <template #title>
        <div style="display:flex;align-items:center;justify-content:space-between">
          <span>套件级·整体 Setup / Teardown</span>
          <a-switch v-model:checked="form.enableOnceSetupTeardown" checked-children="启用" un-checked-children="禁用" />
        </div>
      </template>
      <div v-if="form.enableOnceSetupTeardown">
        <a-row :gutter="16">
          <a-col :span="12">
            <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:6px">
              <span style="font-weight:600">Once Setup</span>
              <a-button size="small" @click="formatJson('onceSetupSteps')">格式化</a-button>
            </div>
            <a-textarea v-model:value="form.onceSetupSteps" :rows="10" style="font-family:monospace;font-size:12px" />
          </a-col>
          <a-col :span="12">
            <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:6px">
              <span style="font-weight:600">Once Teardown</span>
              <a-button size="small" @click="formatJson('onceTeardownSteps')">格式化</a-button>
            </div>
            <a-textarea v-model:value="form.onceTeardownSteps" :rows="10" style="font-family:monospace;font-size:12px" />
          </a-col>
        </a-row>
        <div style="color:#999;font-size:12px;margin-top:8px">
          整体生命周期在套件执行开始时运行 Setup，结束时运行 Teardown，仅执行一次。
        </div>
      </div>
      <div v-else style="color:#999;text-align:center;padding:20px">未启用套件级整体生命周期</div>
    </a-card>

    <!-- 套件级每条用例生命周期 -->
    <a-card size="small">
      <template #title>
        <div style="display:flex;align-items:center;justify-content:space-between">
          <span>套件级·每条用例 Setup / Teardown</span>
          <a-switch v-model:checked="form.enablePerCaseSetupTeardown" checked-children="启用" un-checked-children="禁用" />
        </div>
      </template>
      <div v-if="form.enablePerCaseSetupTeardown">
        <a-row :gutter="16">
          <a-col :span="12">
            <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:6px">
              <span style="font-weight:600">Per-Case Setup</span>
              <a-button size="small" @click="formatJson('perCaseSetupSteps')">格式化</a-button>
            </div>
            <a-textarea v-model:value="form.perCaseSetupSteps" :rows="10" style="font-family:monospace;font-size:12px" />
          </a-col>
          <a-col :span="12">
            <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:6px">
              <span style="font-weight:600">Per-Case Teardown</span>
              <a-button size="small" @click="formatJson('perCaseTeardownSteps')">格式化</a-button>
            </div>
            <a-textarea v-model:value="form.perCaseTeardownSteps" :rows="10" style="font-family:monospace;font-size:12px" />
          </a-col>
        </a-row>
        <div style="color:#999;font-size:12px;margin-top:8px">
          每条用例执行前运行 Setup，执行后运行 Teardown。适用于每条用例都需要登录/清理数据的场景。
        </div>
      </div>
      <div v-else style="color:#999;text-align:center;padding:20px">未启用套件级每条用例生命周期</div>
    </a-card>
  </div>
</template>
