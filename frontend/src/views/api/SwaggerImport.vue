<script setup lang="ts">
/**
 * Swagger 导入向导 - M4
 */
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { importSwagger, getModules } from '@/api/apidoc'

const route = useRoute()
const router = useRouter()
const projectId = computed(() => Number(route.params.id))

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
  if (!selectedModuleId.value) { message.warning('请选择目标分组'); return }
  if (!swaggerJson.value) { message.warning('请提供 Swagger JSON'); return }
  loading.value = true
  try {
    const res: any = await importSwagger(projectId.value, {
      projectId: projectId.value, moduleId: selectedModuleId.value, swaggerJson: swaggerJson.value, importMode: 'INCREMENTAL',
    })
    importResult.value = res.data
    currentStep.value = 2
    message.success('导入成功')
  } catch (e: any) { message.error(e?.response?.data?.message || '导入失败') } finally { loading.value = false }
}

fetchModules()
</script>

<template>
  <div>
    <div style="display:flex;align-items:center;gap:12px;margin-bottom:24px">
      <a @click="router.back()">← 返回</a>
      <h2 style="margin:0">Swagger 导入</h2>
    </div>
    <a-steps :current="currentStep" style="max-width:600px;margin:0 auto 32px">
      <a-step title="选择分组" />
      <a-step title="粘贴/上传 JSON" />
      <a-step title="导入结果" />
    </a-steps>

    <div style="max-width:600px;margin:0 auto">
      <div v-if="currentStep === 0">
        <a-form-item label="目标分组">
          <a-select v-model:value="selectedModuleId" placeholder="选择接口分组">
            <a-select-option v-for="m in modules" :key="m.id" :value="m.id">{{ m.name }}</a-select-option>
          </a-select>
        </a-form-item>
        <a-button type="primary" :disabled="!selectedModuleId" @click="currentStep = 1">下一步</a-button>
      </div>

      <div v-if="currentStep === 1">
        <a-form-item label="Swagger 2.0 JSON">
          <a-textarea v-model:value="swaggerJson" :rows="12" placeholder="粘贴 Swagger JSON 内容" style="font-family:monospace" />
        </a-form-item>
        <a-form-item>
          <input type="file" accept=".json" @change="handleFileUpload" />
        </a-form-item>
        <a-space>
          <a-button @click="currentStep = 0">上一步</a-button>
          <a-button type="primary" :loading="loading" @click="handleImport">开始导入</a-button>
        </a-space>
      </div>

      <div v-if="currentStep === 2 && importResult">
        <a-result status="success" title="导入完成">
          <template #extra>
            <a-descriptions :column="2" bordered size="small">
              <a-descriptions-item label="总计">{{ importResult.total }}</a-descriptions-item>
              <a-descriptions-item label="新增">{{ importResult.created }}</a-descriptions-item>
              <a-descriptions-item label="更新">{{ importResult.updated }}</a-descriptions-item>
              <a-descriptions-item label="跳过">{{ importResult.skipped }}</a-descriptions-item>
            </a-descriptions>
            <a-button type="primary" style="margin-top:16px" @click="router.push(`/project/${projectId}/apis`)">查看接口列表</a-button>
          </template>
        </a-result>
      </div>
    </div>
  </div>
</template>
