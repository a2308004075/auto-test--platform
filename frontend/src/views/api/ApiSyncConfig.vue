<!--
 @author HXN
 @date 2026-08-24
 @description Swagger 同步配置独立页面
-->
<script setup lang="ts">
/**
 * Swagger 同步配置管理页
 * 从弹窗迁移为独立页面，支持新增/编辑/删除/单条同步/全部同步
 */
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getSyncConfigs, createSyncConfig, updateSyncConfig, deleteSyncConfig,
  syncOneConfig, syncAllConfigs, getModules,
} from '@/api/apidoc'
import EditPageHeader from '@/components/EditPageHeader/index.vue'
import { usePermission } from '@/composables/usePermission'

const route = useRoute()
const projectId = computed(() => Number(route.params.id))
const { hasPermission } = usePermission()

// ===== 分组数据（用于目标分组下拉和表格显示） =====
const modules = ref<any[]>([])
const moduleMap = computed<Record<number, any>>(() => {
  const m: Record<number, any> = {}
  modules.value.forEach((mod) => { m[mod.id] = mod })
  return m
})

async function fetchModules() {
  try {
    const res: any = await getModules(projectId.value)
    modules.value = res.data || []
  } catch { modules.value = [] }
}

// ===== 同步配置列表 =====
const syncConfigs = ref<any[]>([])
const loading = ref(false)
const syncAllLoading = ref(false)
const syncOneLoadingId = ref<number | null>(null)

async function fetchList() {
  loading.value = true
  try {
    const res: any = await getSyncConfigs(projectId.value)
    syncConfigs.value = res.data || []
  } catch { syncConfigs.value = [] } finally { loading.value = false }
}

// ===== 新增/编辑弹窗 =====
const formVisible = ref(false)
const formMode = ref<'add' | 'edit'>('add')
const form = reactive({ id: null as number | null, name: '', url: '', moduleId: null as number | null, headers: '', authUsername: '', authPassword: '' })
const saving = ref(false)

function openAdd() {
  formMode.value = 'add'
  form.id = null
  form.name = ''
  form.url = ''
  form.moduleId = null
  form.headers = ''
  form.authUsername = ''
  form.authPassword = ''
  formVisible.value = true
}

function openEdit(row: any) {
  formMode.value = 'edit'
  form.id = row.id
  form.name = row.name
  form.url = row.url
  form.moduleId = row.moduleId
  form.headers = row.headers || ''
  form.authUsername = row.authUsername || ''
  form.authPassword = row.authPassword || ''
  formVisible.value = true
}

async function handleSave() {
  if (!form.name) { ElMessage.warning('请输入配置名称'); return }
  if (!form.url) { ElMessage.warning('请输入文档 URL'); return }
  if (!form.moduleId) { ElMessage.warning('请选择目标分组'); return }
  saving.value = true
  try {
    const data = {
      name: form.name, url: form.url, moduleId: form.moduleId!,
      headers: form.headers || undefined,
      authUsername: form.authUsername || undefined,
      authPassword: form.authPassword || undefined,
    }
    if (formMode.value === 'add') {
      await createSyncConfig(projectId.value, data)
      ElMessage.success('配置已保存')
    } else {
      await updateSyncConfig(projectId.value, form.id!, data)
      ElMessage.success('配置已更新')
    }
    formVisible.value = false
    await fetchList()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

async function handleDelete(row: any) {
  try {
    await ElMessageBox.confirm(`确定删除配置「${row.name}」？`, '提示', { type: 'warning' })
    await deleteSyncConfig(projectId.value, row.id)
    ElMessage.success('已删除')
    await fetchList()
  } catch { /* 取消 */ }
}

async function handleSyncOne(row: any) {
  syncOneLoadingId.value = row.id
  try {
    const res: any = await syncOneConfig(projectId.value, row.id)
    const r = res.data
    ElMessage.success(`「${row.name}」同步完成：新增 ${r.created} 条，更新 ${r.updated} 条，共 ${r.total} 条`)
    await fetchList()
  } catch (e: any) {
    ElMessage.error(`「${row.name}」同步失败：${e?.response?.data?.message || e.message}`)
  } finally {
    syncOneLoadingId.value = null
  }
}

async function handleSyncAll() {
  if (syncConfigs.value.length === 0) { ElMessage.warning('暂无同步配置'); return }
  syncAllLoading.value = true
  try {
    const res: any = await syncAllConfigs(projectId.value)
    const results = res.data || []
    let ok = 0, fail = 0, created = 0, updated = 0
    for (const r of results) {
      if (r.error) { fail++ } else { ok++; created += r.created; updated += r.updated }
    }
    if (fail === 0) {
      ElMessage.success(`全部同步完成：共 ${ok} 个配置，新增 ${created} 条，更新 ${updated} 条`)
    } else {
      ElMessage.warning(`同步完成：成功 ${ok} 个，失败 ${fail} 个，新增 ${created} 条，更新 ${updated} 条`)
    }
    await fetchList()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '同步失败')
  } finally {
    syncAllLoading.value = false
  }
}

onMounted(() => {
  fetchModules()
  fetchList()
})
</script>

<template>
  <div>
    <EditPageHeader title="Swagger 同步配置" :back-route="`/project/${projectId}/apis`">
      <el-button v-if="hasPermission('project:api:swagger')" type="primary" :loading="syncAllLoading" @click="handleSyncAll">全部同步</el-button>
      <el-button v-if="hasPermission('project:api:swagger')" @click="openAdd">+ 新增配置</el-button>
    </EditPageHeader>

    <el-table :data="syncConfigs" v-loading="loading" border style="width: 100%" empty-text="暂无同步配置，点击右上角新增">
      <el-table-column prop="name" label="名称" width="160" show-overflow-tooltip />
      <el-table-column prop="url" label="URL" min-width="200" show-overflow-tooltip />
      <el-table-column label="目标分组" width="120">
        <template #default="{ row }">{{ moduleMap[row.moduleId]?.name || '--' }}</template>
      </el-table-column>
      <el-table-column label="最后同步" width="160">
        <template #default="{ row }">{{ row.lastSyncAt?.replace('T', ' ').substring(0, 19) || '未同步' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link size="small" :loading="syncOneLoadingId === row.id" @click="handleSyncOne(row)">同步</el-button>
          <el-button v-if="hasPermission('project:api:swagger')" type="primary" link size="small" @click="openEdit(row)">编辑</el-button>
          <el-button v-if="hasPermission('project:api:swagger')" type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 新增/编辑同步配置 -->
    <el-dialog v-model="formVisible" :title="formMode === 'add' ? '新增同步配置' : '编辑同步配置'" width="520px" destroy-on-close append-to-body>
      <el-form label-position="top">
        <el-form-item label="配置名称" required>
          <el-input v-model="form.name" placeholder="如：测试环境-用户服务" clearable />
        </el-form-item>
        <el-form-item label="文档 URL" required>
          <el-input v-model="form.url" placeholder="支持 doc.html 页面地址或 /v3/api-docs JSON 端点" clearable />
        </el-form-item>
        <el-form-item label="目标分组" required>
          <el-select v-model="form.moduleId" placeholder="选择导入目标分组" filterable style="width: 100%">
            <el-option v-for="m in modules.filter((x: any) => x.isSystem !== 1 || x.name === '未分类')" :key="m.id" :value="m.id" :label="m.name" />
          </el-select>
        </el-form-item>
        <el-form-item label="认证账号（可选）">
          <el-input v-model="form.authUsername" placeholder="用于拉取 Swagger 文档的 Basic Auth 账号" clearable />
        </el-form-item>
        <el-form-item label="认证密码（可选）">
          <el-input v-model="form.authPassword" type="password" show-password placeholder="用于拉取 Swagger 文档的 Basic Auth 密码" clearable />
        </el-form-item>
        <el-form-item label="默认请求头（可选）">
          <el-input v-model="form.headers" type="textarea" :rows="3" placeholder="导入后统一附加到各接口的请求头&#10;每行一个，格式：Key: Value&#10;如 Authorization: ${authorization}" />
        </el-form-item>
      </el-form>
      <div style="color: #909399; font-size: 12px; margin-top: -8px">
        认证账号/密码仅用于拉取 Swagger 文档；默认请求头会在导入后附加到各接口。系统将自动从 URL 拉取 OpenAPI/Swagger JSON 文档（支持 doc.html 自动探测），以增量方式导入到选定分组。
      </div>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>
