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
import { QuestionFilled } from '@element-plus/icons-vue'
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
const form = reactive({ id: null as number | null, name: '', url: '', moduleId: null as number | null, headers: '', hostPrefix: '', authUsername: '', authPassword: '' })
const saving = ref(false)

// 默认请求头键值对编辑（界面友好，保存时序列化为 Key: Value 文本）
const headerPairs = ref<{ key: string; value: string }[]>([])

function parseHeadersText(text?: string): { key: string; value: string }[] {
  if (!text) return []
  const pairs: { key: string; value: string }[] = []
  for (const raw of text.split('\n')) {
    const line = raw.trim()
    if (!line) continue
    const idx = line.indexOf(':')
    if (idx > 0) {
      pairs.push({ key: line.substring(0, idx).trim(), value: line.substring(idx + 1).trim() })
    }
  }
  return pairs
}

function buildHeadersText(pairs: { key: string; value: string }[]): string {
  return pairs.filter(p => p.key.trim()).map(p => `${p.key.trim()}: ${p.value}`).join('\n')
}

function addHeaderRow() {
  headerPairs.value.push({ key: '', value: '' })
}

function removeHeaderRow(idx: number) {
  headerPairs.value.splice(idx, 1)
}

function openAdd() {
  formMode.value = 'add'
  form.id = null
  form.name = ''
  form.url = ''
  form.moduleId = null
  form.headers = ''
  headerPairs.value = []
  form.hostPrefix = ''
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
  headerPairs.value = parseHeadersText(row.headers)
  form.hostPrefix = row.hostPrefix || ''
  form.authUsername = row.authUsername || ''
  form.authPassword = row.authPassword || ''
  formVisible.value = true
}

async function handleSave() {
  if (!form.name) { ElMessage.warning('请输入配置名称'); return }
  if (!form.url) { ElMessage.warning('请输入文档 URL'); return }
  if (!form.moduleId) { ElMessage.warning('请选择目标分组'); return }
  if (!form.hostPrefix) { ElMessage.warning('请输入导入附加默认 host'); return }
  saving.value = true
  try {
    const headersText = buildHeadersText(headerPairs.value)
    const data = {
      name: form.name, url: form.url, moduleId: form.moduleId!,
      headers: headersText || undefined,
      hostPrefix: form.hostPrefix || undefined,
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
    <el-dialog v-model="formVisible" width="520px" destroy-on-close append-to-body>
      <template #header>
        <span>{{ formMode === 'add' ? '新增同步配置' : '编辑同步配置' }}</span>
        <el-tooltip
          content="认证账号/密码仅用于拉取 Swagger 文档；默认请求头会在导入后附加到各接口。系统将自动从 URL 拉取 OpenAPI/Swagger JSON 文档（支持 doc.html 自动探测），以增量方式导入到选定分组。"
          placement="top"
        >
          <el-icon style="margin-left: 8px; vertical-align: middle; cursor: pointer"><QuestionFilled /></el-icon>
        </el-tooltip>
      </template>
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
        <el-form-item label="导入附加默认 host" required>
          <el-input v-model="form.hostPrefix" placeholder="如 ${host}" clearable />
          <div class="form-item-hint">导入时附加到各接口 URL 前，支持 ${变量} 占位符，留空不附加</div>
        </el-form-item>
        <el-form-item>
          <template #label>
            <span>导入附加请求头（可选）</span>
            <el-button size="small" type="primary" @click="addHeaderRow" style="margin-left: 8px">+ 添加请求头</el-button>
          </template>
          <div class="header-pairs-section">
            <div class="header-pairs-header">
              <span class="header-key-col">请求头 Key</span>
              <span class="header-value-col">请求头 Value</span>
              <span class="header-action-col">操作</span>
            </div>
            <div v-for="(row, idx) in headerPairs" :key="idx" class="header-pair-row">
              <div class="header-key-col">
                <el-input v-model="row.key" size="small" placeholder="如 Authorization" />
              </div>
              <div class="header-value-col">
                <el-input v-model="row.value" size="small" placeholder="如 ${authorization}" />
              </div>
              <div class="header-action-col">
                <el-button link size="small" type="danger" @click="removeHeaderRow(idx)">删除</el-button>
              </div>
            </div>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>
<style scoped>
.form-item-hint {
  font-size: 12px;
  color: #909399;
  line-height: 1.4;
  margin-top: 4px;
}
.header-pairs-section {
  width: 100%;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  overflow: hidden;
  box-sizing: border-box;
}
.header-pairs-header,
.header-pair-row {
  display: flex;
  align-items: center;
  padding: 8px 12px;
  border-bottom: 1px solid #ebeef5;
}
.header-pairs-header {
  background-color: #f5f7fa;
  color: #606266;
  font-size: 13px;
  font-weight: 500;
}
.header-pair-row:last-child {
  border-bottom: none;
}
.header-key-col {
  width: 35%;
  min-width: 140px;
  flex-shrink: 0;
  padding-right: 12px;
}
.header-value-col {
  flex: 1;
  min-width: 140px;
  padding-right: 12px;
}
.header-action-col {
  width: 70px;
  flex-shrink: 0;
  text-align: center;
}
</style>
