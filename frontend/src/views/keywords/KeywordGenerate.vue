<!--
 @author HXN
 @date 2026-08-27
 @description 从接口快速生成关键字 - 独立页面
-->
<script setup lang="ts">
/**
 * 从接口快速生成关键字 - 独立编辑页
 * 替代原弹窗方式，用户选择接口后生成关键字并自动跳转到编辑页
 */
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { generateKeyword } from '@/api/keyword'
import { getApis, getModules } from '@/api/apidoc'
import { usePermission } from '@/composables/usePermission'
import EditPageHeader from '@/components/EditPageHeader/index.vue'

const route = useRoute()
const router = useRouter()
const { hasPermission } = usePermission()
const projectId = computed(() => Number(route.params.id))

const loading = ref(false)
const generating = ref(false)
const apis = ref<any[]>([])
const modules = ref<any[]>([])
const selectedApiId = ref<number>(0)

const groupedApis = computed(() => {
  const allModules = modules.value
  const allApis = apis.value
  const apisByMod: Record<number, any[]> = {}
  allApis.forEach((a: any) => {
    const mid = a.moduleId || 0
    if (!apisByMod[mid]) apisByMod[mid] = []
    apisByMod[mid].push(a)
  })
  const rootMods = allModules
    .filter((m: any) => m.name !== '全部' && (m.parentId == null || m.parentId === 0))
    .map((m: any) => ({ ...m, childModules: [] as any[] }))
  const rootMap: Record<number, any> = {}
  rootMods.forEach((m: any) => { rootMap[m.id] = m })
  allModules
    .filter((m: any) => m.parentId != null && m.parentId !== 0 && rootMap[m.parentId])
    .forEach((m: any) => { rootMap[m.parentId].childModules.push(m) })
  return rootMods
    .map((root: any) => ({
      label: root.name,
      apis: apisByMod[root.id] || [],
      children: (root.childModules || [])
        .map((child: any) => ({ label: child.name, apis: apisByMod[child.id] || [] }))
        .filter((c: any) => c.apis.length > 0),
    }))
    .filter((g: any) => g.apis.length > 0 || g.children.length > 0)
    .concat(
      apisByMod[0]?.length ? [{ label: '未分类', apis: apisByMod[0], children: [] }] : [],
    )
})

async function fetchData() {
  loading.value = true
  try {
    const [apiRes, modRes]: any[] = await Promise.all([
      getApis(projectId.value, { page: 1, pageSize: 1000 }),
      getModules(projectId.value),
    ])
    apis.value = apiRes.data?.items || []
    modules.value = modRes.data || []
  } catch {
    apis.value = []
    modules.value = []
  } finally {
    loading.value = false
  }
}

async function handleGenerate() {
  if (!selectedApiId.value) { ElMessage.warning('请选择接口'); return }
  generating.value = true
  try {
    const res: any = await generateKeyword(projectId.value, selectedApiId.value)
    const keywordId = res.data?.id || res.data?.keywordId
    ElMessage.success('生成成功')
    if (keywordId) {
      router.push(`/project/${projectId.value}/keywords/${keywordId}/edit`)
    } else {
      router.push(`/project/${projectId.value}/keywords`)
    }
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '生成失败')
  } finally {
    generating.value = false
  }
}

onMounted(fetchData)
</script>

<template>
  <div v-loading="loading">
    <EditPageHeader title="从接口生成关键字" :back-route="`/project/${projectId}/keywords`">
      <el-button v-if="hasPermission('project:keyword:from-api')" type="primary" :loading="generating" @click="handleGenerate">生成</el-button>
      <el-button @click="router.push(`/project/${projectId}/keywords`)">取消</el-button>
    </EditPageHeader>

    <el-form label-position="top" style="max-width: 600px; margin-top: 24px">
      <el-form-item label="选择接口" required>
        <el-select v-model="selectedApiId" placeholder="选择要生成关键字的接口" filterable style="width: 100%">
          <el-option-group v-for="group in groupedApis" :key="group.label" :label="group.label">
            <el-option v-for="api in group.apis" :key="api.id" :value="api.id" :label="`[${api.httpMethod}] ${api.name}`" />
            <el-option-group v-for="child in group.children" :key="child.label" :label="`  ${child.label}`">
              <el-option v-for="api in child.apis" :key="api.id" :value="api.id" :label="`[${api.httpMethod}] ${api.name}`" />
            </el-option-group>
          </el-option-group>
        </el-select>
      </el-form-item>
      <el-form-item v-if="selectedApiId">
        <div class="api-preview" v-if="apis.find((a: any) => a.id === selectedApiId)">
          <el-tag size="small" :type="apis.find((a: any) => a.id === selectedApiId)?.httpMethod === 'GET' ? '' : 'success'">
            {{ apis.find((a: any) => a.id === selectedApiId)?.httpMethod }}
          </el-tag>
          <code>{{ apis.find((a: any) => a.id === selectedApiId)?.path }}</code>
          <span style="color: #909399; font-size: 12px">{{ apis.find((a: any) => a.id === selectedApiId)?.description }}</span>
        </div>
      </el-form-item>
    </el-form>
  </div>
</template>

<style scoped>
.api-preview {
  padding: 10px 12px;
  background: #fafafa;
  border-radius: 4px;
  display: flex;
  gap: 12px;
  font-size: 13px;
  align-items: center;
  width: 100%;
}
</style>
