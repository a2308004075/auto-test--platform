<script setup lang="ts">
/**
 * 测试计划编辑 - M9
 * 基本信息 + 套件选择 + 环境选择 + cron
 */
import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { getPlan, createPlan, updatePlan } from '@/api/plan'
import { getSuites } from '@/api/suite'
import { getEnvironments } from '@/api/environment'

const route = useRoute()
const router = useRouter()
const projectId = computed(() => Number(route.params.id))
const planId = computed(() => Number(route.params.planId))
const isEdit = computed(() => !!planId.value)

const form = reactive({
  name: '',
  description: '',
  suiteIds: [] as number[],
  environmentId: 0 as number,
  scheduleCron: '',
})

const suites = ref<any[]>([])
const environments = ref<any[]>([])

async function loadSuites() {
  try {
    const res: any = await getSuites(projectId.value, { pageSize: 200 })
    suites.value = res.data?.items || []
  } catch { suites.value = [] }
}

async function loadEnvironments() {
  try {
    const res: any = await getEnvironments(projectId.value)
    environments.value = res.data || []
  } catch { environments.value = [] }
}

async function loadPlan() {
  if (!planId.value) return
  try {
    const res: any = await getPlan(planId.value)
    const p = res.data
    Object.assign(form, {
      name: p.name || '',
      description: p.description || '',
      suiteIds: p.suiteIds || [],
      environmentId: p.environmentId ?? null,
      scheduleCron: p.scheduleCron || '',
    })
  } catch { message.error('加载计划失败') }
}

async function handleSave() {
  if (!form.name) { message.warning('请输入计划名称'); return }
  try {
    if (isEdit.value) {
      await updatePlan(planId.value, { ...form })
      message.success('保存成功')
    } else {
      await createPlan(projectId.value, { ...form })
      message.success('创建成功')
      router.push(`/project/${projectId}/plans`)
    }
  } catch (e: any) {
    message.error(e?.response?.data?.message || '保存失败')
  }
}

onMounted(() => {
  loadSuites()
  loadEnvironments()
  if (isEdit.value) loadPlan()
})
</script>

<template>
  <div>
    <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:16px">
      <h2 style="margin:0">{{ isEdit ? '编辑计划' : '新建计划' }}</h2>
      <a-space>
        <a-button @click="router.back()">返回</a-button>
        <a-button type="primary" @click="handleSave">保存</a-button>
      </a-space>
    </div>

    <a-card title="基本信息" size="small">
      <a-form layout="vertical">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="计划名称" required><a-input v-model:value="form.name" /></a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="定时执行 Cron">
              <a-input v-model:value="form.scheduleCron" placeholder="如 0 0 2 * * ?" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="描述"><a-textarea v-model:value="form.description" :rows="2" /></a-form-item>
      </a-form>
    </a-card>

    <a-card title="执行配置" size="small" style="margin-top:16px">
      <a-form layout="vertical">
        <a-form-item label="执行环境">
          <a-select v-model:value="form.environmentId" placeholder="选择执行环境" allow-clear>
            <a-select-option v-for="env in environments" :key="env.id" :value="env.id">
              {{ env.name }}{{ env.isCurrent ? '（当前）' : '' }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="关联测试套件">
          <a-select v-model:value="form.suiteIds" mode="multiple" placeholder="选择套件" allow-clear>
            <a-select-option v-for="s in suites" :key="s.id" :value="s.id">
              {{ s.name }}（{{ s.priority }}）
            </a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </a-card>
  </div>
</template>
