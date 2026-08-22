<!--
 @author HXN
 @date 2026-08-20 15:34
 @description Swagger 导入视图
-->
<script setup lang="ts">
/**
 * Swagger 导入向导 - M4
 */
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { importSwagger, getModules } from '@/api/apidoc'
import { usePermission } from '@/composables/usePermission'

const route = useRoute()
const router = useRouter()
const projectId = computed(() => Number(route.params.id))
const { hasPermission } = usePermission()

const currentStep = ref(0)
const loading = ref(false)
const modules = ref<any[]>([])
const selectedModuleId = ref<number>(0)
const swaggerJson = ref('')
const importResult = ref<any>(null)

async function fetchModules() {
  try {
    const res: any = await getModules(projectId.value)
    modules.value = res.data || []
  } catch { /* ignore */ }
}

function handleFileUpload(e: Event) {
  const file = (e.target as HTMLInputElement).files?.[0]
  if (!file) return
  const reader = new FileReader()
  reader.onload = () => { swaggerJson.value = reader.result as string }
  reader.readAsText(file)
}

async function handleImport() {
  if (!selectedModuleId.value) { ElMessage.warning('请选择目标分组'); return }
  if (!swaggerJson.value) { ElMessage.warning('请提供 Swagger JSON'); return }
  loading.value = true
  try {
    const res: any = await importSwagger(projectId.value, {
      projectId: projectId.value, moduleId: selectedModuleId.value, swaggerJson: swaggerJson.value, importMode: 'INCREMENTAL',
    })
    importResult.value = res.data
    currentStep.value = 2
    ElMessage.success('导入成功')
  } catch (e: any) { ElMessage.error(e?.response?.data?.message || '导入失败') } finally { loading.value = false }
}

fetchModules()
</script>

<template>
  <div>
    <div style="display:flex;align-items:center;gap:12px;margin-bottom:20px">
      <el-button type="primary" link @click="router.back()">← 返回</el-button>
      <h2 style="margin:0">Swagger 导入</h2>
    </div>
    <el-steps :active="currentStep" style="max-width:600px;margin:0 auto 32px" align-center>
      <el-step title="选择分组" />
      <el-step title="粘贴/上传 JSON" />
      <el-step title="导入结果" />
    </el-steps>

    <div style="max-width:600px;margin:0 auto">
      <div v-if="currentStep === 0">
        <el-form-item label="目标分组">
          <el-select v-model="selectedModuleId" placeholder="选择接口分组" style="width:100%">
            <el-option v-for="m in modules" :key="m.id" :value="m.id" :label="m.name" />
          </el-select>
        </el-form-item>
        <el-button type="primary" :disabled="!selectedModuleId" @click="currentStep = 1">下一步</el-button>
      </div>

      <div v-if="currentStep === 1">
        <el-form-item label="Swagger 2.0 JSON">
          <el-input v-model="swaggerJson" type="textarea" :rows="12" placeholder="粘贴 Swagger JSON 内容" style="font-family:monospace" />
        </el-form-item>
        <el-form-item>
          <input type="file" accept=".json" @change="handleFileUpload" />
        </el-form-item>
        <div style="display:flex;gap:8px">
          <el-button @click="currentStep = 0">上一步</el-button>
          <el-button v-if="hasPermission('project:api:swagger')" type="primary" :loading="loading" @click="handleImport">开始导入</el-button>
        </div>
      </div>

      <div v-if="currentStep === 2 && importResult">
        <el-result icon="success" title="导入完成">
          <template #sub-title>
            <el-descriptions :column="2" border size="small" style="margin-top:16px">
              <el-descriptions-item label="总计">{{ importResult.total }}</el-descriptions-item>
              <el-descriptions-item label="新增">{{ importResult.created }}</el-descriptions-item>
              <el-descriptions-item label="更新">{{ importResult.updated }}</el-descriptions-item>
              <el-descriptions-item label="跳过">{{ importResult.skipped }}</el-descriptions-item>
            </el-descriptions>
          </template>
          <template #extra>
            <el-button type="primary" @click="router.push(`/project/${projectId}/apis`)">查看接口列表</el-button>
          </template>
        </el-result>
      </div>
    </div>
  </div>
</template>
