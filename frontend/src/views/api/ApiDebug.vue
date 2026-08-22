<!--
 @author HXN
 @date 2026-08-20 15:34
 @description API 调试视图
-->
<script setup lang="ts">
/**
 * 接口在线调试 - M4
 */
import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
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
const form = reactive({ environmentId: null as number | null, body: '', queryParams: '{}', headers: '{}' })

async function fetchApi() {
  try {
    const res: any = await getApi(projectId.value, apiId.value)
    apiInfo.value = res.data || {}
  } catch { ElMessage.error('加载接口信息失败') }
}

async function fetchEnvironments() {
  try {
    const res: any = await getEnvironments(projectId.value)
    environments.value = res.data || []
  } catch { /* ignore */ }
}

async function handleDebug() {
  if (!form.environmentId) { ElMessage.warning('请选择环境'); return }
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
    debugResult.value = { success: 0, errorMessage: e?.message || '请求失败' }
  } finally { loading.value = false }
}

onMounted(() => { fetchApi(); fetchEnvironments() })
</script>

<template>
  <div>
    <div style="display:flex;align-items:center;gap:12px;margin-bottom:20px">
      <el-button type="primary" link @click="router.back()">← 返回</el-button>
      <h2 style="margin:0">调试: {{ apiInfo.name }}</h2>
      <el-tag :type="apiInfo.httpMethod === 'GET' ? '' : 'success'" size="small">{{ apiInfo.httpMethod }}</el-tag>
      <code style="color:#606266">{{ apiInfo.path }}</code>
    </div>
    <el-row :gutter="24">
      <el-col :span="12">
        <el-card>
          <template #header><span>请求配置</span></template>
          <el-form label-position="top">
            <el-form-item label="环境">
              <el-select v-model="form.environmentId" placeholder="选择环境" style="width:100%">
                <el-option v-for="e in environments" :key="e.id" :value="e.id" :label="e.name" />
              </el-select>
            </el-form-item>
            <el-form-item label="查询参数 (JSON)">
              <el-input v-model="form.queryParams" type="textarea" :rows="3" style="font-family:monospace" />
            </el-form-item>
            <el-form-item label="请求头 (JSON)">
              <el-input v-model="form.headers" type="textarea" :rows="3" style="font-family:monospace" />
            </el-form-item>
            <el-form-item label="请求体">
              <el-input v-model="form.body" type="textarea" :rows="5" style="font-family:monospace" />
            </el-form-item>
            <el-button type="primary" :loading="loading" @click="handleDebug" style="width:100%">发送请求</el-button>
          </el-form>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header><span>响应结果</span></template>
          <div v-if="!debugResult" style="color:#909399;text-align:center;padding:40px">点击"发送请求"开始调试</div>
          <div v-else>
            <el-descriptions :column="2" size="small" border style="margin-bottom:12px">
              <el-descriptions-item label="状态码">
                <el-tag :type="debugResult.success === 1 ? 'success' : 'danger'" size="small">{{ debugResult.statusCode || 'ERROR' }}</el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="耗时">{{ debugResult.responseTimeMs }}ms</el-descriptions-item>
            </el-descriptions>
            <div v-if="debugResult.errorMessage" style="color:#f56c6c;margin-bottom:8px">{{ debugResult.errorMessage }}</div>
            <h5 style="margin:12px 0 8px">响应体</h5>
            <pre style="background:#f5f7fa;padding:12px;border-radius:4px;max-height:400px;overflow:auto;font-size:12px">{{ debugResult.responseBody }}</pre>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>
