<script setup lang="ts">
/**
 * 测试计划编辑 - M9
 * 基本信息 + 套件选择 + 环境选择 + cron
 */
import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
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
  } catch { ElMessage.error('加载计划失败') }
}

async function handleSave() {
  if (!form.name) { ElMessage.warning('请输入计划名称'); return }
  try {
    if (isEdit.value) {
      await updatePlan(planId.value, { ...form })
      ElMessage.success('保存成功')
    } else {
      await createPlan(projectId.value, { ...form })
      ElMessage.success('创建成功')
      router.push(`/project/${projectId}/plans`)
    }
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '保存失败')
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
      <div style="display:flex;gap:8px">
        <el-button @click="router.back()">返回</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </div>
    </div>

    <el-card style="margin-bottom:16px">
      <template #header><span>基本信息</span></template>
      <el-form label-position="top">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="计划名称" required>
              <el-input v-model="form.name" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="定时执行 Cron">
              <el-input v-model="form.scheduleCron" placeholder="如 0 0 2 * * ?" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
    </el-card>

    <el-card>
      <template #header><span>执行配置</span></template>
      <el-form label-position="top">
        <el-form-item label="执行环境">
          <el-select v-model="form.environmentId" placeholder="选择执行环境" clearable style="width:100%">
            <el-option v-for="env in environments" :key="env.id" :value="env.id"
              :label="`${env.name}${env.isCurrent === 1 ? '（当前）' : ''}`" />
          </el-select>
        </el-form-item>
        <el-form-item label="关联测试套件">
          <el-select v-model="form.suiteIds" multiple placeholder="选择套件" clearable style="width:100%">
            <el-option v-for="s in suites" :key="s.id" :value="s.id"
              :label="`${s.name}（${s.priority}）`" />
          </el-select>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>
