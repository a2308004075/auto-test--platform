<script setup lang="ts">
/**
 * 接口编辑/新建 - M4
 */
import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { getApi, createApi, updateApi, getModules } from '@/api/apidoc'

const route = useRoute()
const router = useRouter()
const projectId = computed(() => route.params.id as string)
const apiId = computed(() => route.params.apiId as string)
const isEdit = computed(() => !!apiId.value)

const loading = ref(false)
const modules = ref<any[]>([])
const form = reactive({
  name: '', httpMethod: 'GET', path: '', service: '', moduleId: '',
  description: '', requestParams: '[]', requestBody: '{}', responseBody: '{}', headers: '[]',
})

async function fetchModules() {
  try {
    const res: any = await getModules(projectId.value)
    modules.value = res.data || []
    if (!form.moduleId && modules.value.length) form.moduleId = modules.value[0].id
  } catch { /* ignore */ }
}

async function fetchApi() {
  if (!isEdit.value) return
  loading.value = true
  try {
    const res: any = await getApi(projectId.value, apiId.value)
    Object.assign(form, res.data)
  } catch { message.error('加载接口失败') } finally { loading.value = false }
}

async function handleSubmit() {
  if (!form.name || !form.path) { message.warning('请填写必填项'); return }
  try {
    if (isEdit.value) {
      await updateApi(projectId.value, apiId.value, { ...form, projectId: projectId.value })
      message.success('更新成功')
    } else {
      await createApi(projectId.value, { ...form, projectId: projectId.value })
      message.success('创建成功')
    }
    router.push(`/project/${projectId.value}/apis`)
  } catch (e: any) { message.error(e?.response?.data?.message || '操作失败') }
}

onMounted(() => { fetchModules(); fetchApi() })
</script>

<template>
  <div>
    <div style="display:flex;align-items:center;gap:12px;margin-bottom:24px">
      <a @click="router.back()">← 返回</a>
      <h2 style="margin:0">{{ isEdit ? '编辑接口' : '新建接口' }}</h2>
    </div>
    <a-spin :spinning="loading">
      <a-form layout="vertical" style="max-width:800px">
        <a-row :gutter="16">
          <a-col :span="16">
            <a-form-item label="接口名称" required>
              <a-input v-model:value="form.name" placeholder="请输入接口名称" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="所属分组">
              <a-select v-model:value="form.moduleId" placeholder="选择分组">
                <a-select-option v-for="m in modules" :key="m.id" :value="m.id">{{ m.name }}</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="6">
            <a-form-item label="HTTP 方法">
              <a-select v-model:value="form.httpMethod">
                <a-select-option v-for="m in ['GET','POST','PUT','DELETE','PATCH']" :key="m" :value="m">{{ m }}</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="18">
            <a-form-item label="路径" required>
              <a-input v-model:value="form.path" placeholder="/api/v1/example" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="服务名">
          <a-input v-model:value="form.service" placeholder="服务名（可选）" />
        </a-form-item>
        <a-form-item label="描述">
          <a-textarea v-model:value="form.description" :rows="2" />
        </a-form-item>
        <a-form-item label="请求参数 (JSON)">
          <a-textarea v-model:value="form.requestParams" :rows="4" style="font-family:monospace" />
        </a-form-item>
        <a-form-item label="请求体 (JSON)">
          <a-textarea v-model:value="form.requestBody" :rows="4" style="font-family:monospace" />
        </a-form-item>
        <a-form-item label="响应体 (JSON)">
          <a-textarea v-model:value="form.responseBody" :rows="4" style="font-family:monospace" />
        </a-form-item>
        <a-form-item>
          <a-space>
            <a-button type="primary" @click="handleSubmit">{{ isEdit ? '保存' : '创建' }}</a-button>
            <a-button @click="router.back()">取消</a-button>
          </a-space>
        </a-form-item>
      </a-form>
    </a-spin>
  </div>
</template>
