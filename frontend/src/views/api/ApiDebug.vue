<script setup lang="ts">
/**
 * 接口在线调试 - M4
 */
import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { getApi, debugApi } from '@/api/apidoc'
import { getEnvironments } from '@/api/environment'

const route = useRoute()
const router = useRouter()
const projectId = computed(() => Number(route.params.id))
const apiId = computed(() => Number(route.params.apiId))

const loading = ref(false)
const apiInfo = ref<any>({})
const environments = ref<any[]>([])
const debugResult = ref<any>(null)
const form = reactive({ environmentId: null, body: '', queryParams: '{}', headers: '{}' })

async function fetchApi() {
  try {
    const res: any = await getApi(projectId.value, apiId.value)
    apiInfo.value = res.data || {}
  } catch { message.error('加载接口信息失败') }
}

async function fetchEnvironments() {
  try {
    const res: any = await getEnvironments(projectId.value)
    environments.value = res.data || []
  } catch { /* ignore */ }
}

async function handleDebug() {
  if (!form.environmentId) { message.warning('请选择环境'); return }
  loading.value = true
  debugResult.value = null
  try {
    let queryParams: Record<string, string> = {}
    let headers: Record<string, string> = {}
    try { queryParams = JSON.parse(form.queryParams || '{}') } catch { /* ignore */ }
    try { headers = JSON.parse(form.headers || '{}') } catch { /* ignore */ }
    const res: any = await debugApi(projectId.value, apiId.value, {
      environmentId: form.environmentId, body: form.body, queryParams, headers,
    })
    debugResult.value = res.data
  } catch (e: any) {
    debugResult.value = { success: false, errorMessage: e?.message || '请求失败' }
  } finally { loading.value = false }
}

onMounted(() => { fetchApi(); fetchEnvironments() })
</script>

<template>
  <div>
    <div style="display:flex;align-items:center;gap:12px;margin-bottom:24px">
      <a @click="router.back()">← 返回</a>
      <h2 style="margin:0">调试: {{ apiInfo.name }}</h2>
      <a-tag :color="apiInfo.httpMethod === 'GET' ? 'blue' : 'green'">{{ apiInfo.httpMethod }}</a-tag>
      <code>{{ apiInfo.path }}</code>
    </div>
    <a-row :gutter="24">
      <a-col :span="12">
        <a-card title="请求配置" size="small">
          <a-form layout="vertical">
            <a-form-item label="环境">
              <a-select v-model:value="form.environmentId" placeholder="选择环境">
                <a-select-option v-for="e in environments" :key="e.id" :value="e.id">{{ e.name }}</a-select-option>
              </a-select>
            </a-form-item>
            <a-form-item label="查询参数 (JSON)">
              <a-textarea v-model:value="form.queryParams" :rows="3" style="font-family:monospace" />
            </a-form-item>
            <a-form-item label="请求头 (JSON)">
              <a-textarea v-model:value="form.headers" :rows="3" style="font-family:monospace" />
            </a-form-item>
            <a-form-item label="请求体">
              <a-textarea v-model:value="form.body" :rows="5" style="font-family:monospace" />
            </a-form-item>
            <a-button type="primary" :loading="loading" @click="handleDebug" block>发送请求</a-button>
          </a-form>
        </a-card>
      </a-col>
      <a-col :span="12">
        <a-card title="响应结果" size="small">
          <div v-if="!debugResult" style="color:#999;text-align:center;padding:40px">点击"发送请求"开始调试</div>
          <div v-else>
            <a-descriptions :column="2" size="small" bordered style="margin-bottom:12px">
              <a-descriptions-item label="状态码">
                <a-tag :color="debugResult.success ? 'green' : 'red'">{{ debugResult.statusCode || 'ERROR' }}</a-tag>
              </a-descriptions-item>
              <a-descriptions-item label="耗时">{{ debugResult.responseTimeMs }}ms</a-descriptions-item>
            </a-descriptions>
            <div v-if="debugResult.errorMessage" style="color:red;margin-bottom:8px">{{ debugResult.errorMessage }}</div>
            <a-typography-title :level="5">响应体</a-typography-title>
            <pre style="background:#f5f5f5;padding:12px;border-radius:4px;max-height:400px;overflow:auto;font-size:12px">{{ debugResult.responseBody }}</pre>
          </div>
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>
