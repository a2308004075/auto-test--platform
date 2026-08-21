<script setup lang="ts">
/**
 * 测试套件编辑 - M8
 * 基本信息 + 套件级生命周期步骤树（Once Setup/Teardown + Per-Case Setup/Teardown）
 */
import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getSuite, updateSuite } from '@/api/suite'

const route = useRoute()
const router = useRouter()
const projectId = computed(() => Number(route.params.id))
const suiteId = computed(() => Number(route.params.suiteId))

const loading = ref(false)
const saving = ref(false)

const form = reactive({
  name: '',
  description: '',
  priority: 'P2',
  tags: '[]',
  enableOnceSetupTeardown: 0,
  onceSetupSteps: '[]',
  onceTeardownSteps: '[]',
  enablePerCaseSetupTeardown: 0,
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
      enableOnceSetupTeardown: s.enableOnceSetupTeardown ?? 0,
      onceSetupSteps: s.onceSetupSteps || '[]',
      onceTeardownSteps: s.onceTeardownSteps || '[]',
      enablePerCaseSetupTeardown: s.enablePerCaseSetupTeardown ?? 0,
      perCaseSetupSteps: s.perCaseSetupSteps || '[]',
      perCaseTeardownSteps: s.perCaseTeardownSteps || '[]',
    })
  } catch { ElMessage.error('加载套件失败') } finally { loading.value = false }
}

function formatJson(field: keyof typeof form) {
  try {
    (form as any)[field] = JSON.stringify(JSON.parse((form as any)[field] || '[]'), null, 2)
  } catch {
    ElMessage.warning('JSON 格式错误，无法格式化')
  }
}

function validateJson(): boolean {
  const jsonFields = ['onceSetupSteps', 'onceTeardownSteps', 'perCaseSetupSteps', 'perCaseTeardownSteps', 'tags'] as const
  for (const f of jsonFields) {
    try {
      JSON.parse((form as any)[f] || '[]')
    } catch {
      ElMessage.warning(`${f} 不是有效的 JSON`)
      return false
    }
  }
  return true
}

async function handleSave() {
  if (!form.name) { ElMessage.warning('请输入套件名称'); return }
  if (!validateJson()) return
  saving.value = true
  try {
    await updateSuite(projectId.value, suiteId.value, { ...form })
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
      <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:16px">
        <h2 style="margin:0">编辑套件</h2>
        <div style="display:flex;gap:8px">
          <el-button @click="router.back()">返回</el-button>
          <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
        </div>
      </div>

      <!-- 基本信息 -->
      <el-card style="margin-bottom:16px">
        <template #header><span>基本信息</span></template>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="套件名称" required>
              <el-input v-model="form.name" />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="优先级">
              <el-select v-model="form.priority" style="width:100%">
                <el-option value="P0" label="P0" />
                <el-option value="P1" label="P1" />
                <el-option value="P2" label="P2" />
                <el-option value="P3" label="P3" />
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

      <!-- 套件级整体生命周期 -->
      <el-card style="margin-bottom:16px">
        <template #header>
          <div style="display:flex;align-items:center;justify-content:space-between">
            <span>套件级·整体 Setup / Teardown</span>
            <el-switch v-model="form.enableOnceSetupTeardown" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="禁用" />
          </div>
        </template>
        <div v-if="form.enableOnceSetupTeardown === 1">
          <el-row :gutter="16">
            <el-col :span="12">
              <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:6px">
                <span style="font-weight:600">Once Setup</span>
                <el-button size="small" @click="formatJson('onceSetupSteps')">格式化</el-button>
              </div>
              <el-input v-model="form.onceSetupSteps" type="textarea" :rows="10" style="font-family:monospace;font-size:12px" />
            </el-col>
            <el-col :span="12">
              <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:6px">
                <span style="font-weight:600">Once Teardown</span>
                <el-button size="small" @click="formatJson('onceTeardownSteps')">格式化</el-button>
              </div>
              <el-input v-model="form.onceTeardownSteps" type="textarea" :rows="10" style="font-family:monospace;font-size:12px" />
            </el-col>
          </el-row>
          <div style="color:#909399;font-size:12px;margin-top:8px">
            整体生命周期在套件执行开始时运行 Setup，结束时运行 Teardown，仅执行一次。
          </div>
        </div>
        <div v-else style="color:#909399;text-align:center;padding:20px">未启用套件级整体生命周期</div>
      </el-card>

      <!-- 套件级每条用例生命周期 -->
      <el-card>
        <template #header>
          <div style="display:flex;align-items:center;justify-content:space-between">
            <span>套件级·每条用例 Setup / Teardown</span>
            <el-switch v-model="form.enablePerCaseSetupTeardown" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="禁用" />
          </div>
        </template>
        <div v-if="form.enablePerCaseSetupTeardown === 1">
          <el-row :gutter="16">
            <el-col :span="12">
              <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:6px">
                <span style="font-weight:600">Per-Case Setup</span>
                <el-button size="small" @click="formatJson('perCaseSetupSteps')">格式化</el-button>
              </div>
              <el-input v-model="form.perCaseSetupSteps" type="textarea" :rows="10" style="font-family:monospace;font-size:12px" />
            </el-col>
            <el-col :span="12">
              <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:6px">
                <span style="font-weight:600">Per-Case Teardown</span>
                <el-button size="small" @click="formatJson('perCaseTeardownSteps')">格式化</el-button>
              </div>
              <el-input v-model="form.perCaseTeardownSteps" type="textarea" :rows="10" style="font-family:monospace;font-size:12px" />
            </el-col>
          </el-row>
          <div style="color:#909399;font-size:12px;margin-top:8px">
            每条用例执行前运行 Setup，执行后运行 Teardown。适用于每条用例都需要登录/清理数据的场景。
          </div>
        </div>
        <div v-else style="color:#909399;text-align:center;padding:20px">未启用套件级每条用例生命周期</div>
      </el-card>
    </div>
  </div>
</template>
