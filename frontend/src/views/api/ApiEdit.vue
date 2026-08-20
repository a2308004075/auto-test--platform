<script setup lang="ts">
/**
 * 接口编辑/新建 - M4
 */
import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getApi, createApi, updateApi, getModules } from '@/api/apidoc'

const route = useRoute()
const router = useRouter()
const projectId = computed(() => Number(route.params.id))
const apiId = computed(() => Number(route.params.apiId))
const isEdit = computed(() => !!apiId.value)

const loading = ref(false)
const modules = ref<any[]>([])
const form = reactive({
  name: '', httpMethod: 'GET', path: '', service: '', moduleId: null as number | null,
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
  } catch { ElMessage.error('加载接口失败') } finally { loading.value = false }
}

async function handleSubmit() {
  if (!form.name || !form.path) { ElMessage.warning('请填写必填项'); return }
  try {
    if (isEdit.value) {
      await updateApi(projectId.value, apiId.value, { ...form, projectId: projectId.value })
      ElMessage.success('更新成功')
    } else {
      await createApi(projectId.value, { ...form, projectId: projectId.value })
      ElMessage.success('创建成功')
    }
    router.push(`/project/${projectId.value}/apis`)
  } catch (e: any) { ElMessage.error(e?.response?.data?.message || '操作失败') }
}

onMounted(() => { fetchModules(); fetchApi() })
</script>

<template>
  <div>
    <div style="display:flex;align-items:center;gap:12px;margin-bottom:20px">
      <el-button type="primary" link @click="router.back()">← 返回</el-button>
      <h2 style="margin:0">{{ isEdit ? '编辑接口' : '新建接口' }}</h2>
    </div>
    <div v-loading="loading">
      <el-form label-position="top" style="max-width:800px">
        <el-row :gutter="16">
          <el-col :span="16">
            <el-form-item label="接口名称">
              <el-input v-model="form.name" placeholder="请输入接口名称" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="所属分组">
              <el-select v-model="form.moduleId" placeholder="选择分组" style="width:100%">
                <el-option v-for="m in modules" :key="m.id" :value="m.id" :label="m.name" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="6">
            <el-form-item label="HTTP 方法">
              <el-select v-model="form.httpMethod" style="width:100%">
                <el-option v-for="m in ['GET','POST','PUT','DELETE','PATCH']" :key="m" :value="m" :label="m" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="18">
            <el-form-item label="路径">
              <el-input v-model="form.path" placeholder="/api/v1/example" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="服务名">
          <el-input v-model="form.service" placeholder="服务名（可选）" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="请求参数 (JSON)">
          <el-input v-model="form.requestParams" type="textarea" :rows="4" style="font-family:monospace" />
        </el-form-item>
        <el-form-item label="请求体 (JSON)">
          <el-input v-model="form.requestBody" type="textarea" :rows="4" style="font-family:monospace" />
        </el-form-item>
        <el-form-item label="响应体 (JSON)">
          <el-input v-model="form.responseBody" type="textarea" :rows="4" style="font-family:monospace" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSubmit">{{ isEdit ? '保存' : '创建' }}</el-button>
          <el-button @click="router.back()">取消</el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>
