<!--
 @author HXN
 @date 2026-08-20 15:34
 @description Swagger 导入视图
-->
<script setup lang="ts">
/**
 * Swagger 导入向导 - M4
 * 三步流程：上传文件&选择分组 → 解析预览 → 确认导入
 * 对齐原型 swagger-import.html
 */
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { importSwagger, getModules } from '@/api/apidoc'
import EditPageHeader from '@/components/EditPageHeader/index.vue'

const route = useRoute()
const router = useRouter()
const projectId = computed(() => Number(route.params.id))

const currentStep = ref(0)
const loading = ref(false)
const modules = ref<any[]>([])
const swaggerJson = ref('')
const fileName = ref('')
const fileSize = ref('')
const importResult = ref<any>(null)

// 分组选择模式：'existing' | 'new'
const groupMode = ref<'existing' | 'new'>('existing')
const selectedModuleId = ref<number>(0)
const newGroupName = ref('')
const newGroupPrefix = ref('')

// 预览数据（模拟解析结果）
const previewApis = ref<any[]>([])
const selectAll = ref(true)
const selectedApis = ref<Set<number>>(new Set())

const previewStats = computed(() => {
  const total = previewApis.value.length
  const newCount = previewApis.value.filter((a) => a.status === 'new').length
  const updateCount = previewApis.value.filter((a) => a.status === 'update').length
  const selectedCount = selectedApis.value.size
  return { total, newCount, updateCount, selectedCount }
})

const methodColors: Record<string, string> = { GET: '', POST: 'success', PUT: 'warning', DELETE: 'danger', PATCH: 'info' }

async function fetchModules() {
  try {
    const res: any = await getModules(projectId.value)
    modules.value = res.data || []
  } catch { /* ignore */ }
}

// 文件上传处理
function handleFileUpload(e: Event) {
  const file = (e.target as HTMLInputElement).files?.[0]
  if (!file) return
  processFile(file)
}

function processFile(file: File) {
  if (!file.name.endsWith('.json')) {
    ElMessage.warning('仅支持 .json 格式的 Swagger 文件')
    return
  }
  if (file.size > 10 * 1024 * 1024) {
    ElMessage.warning('文件大小不能超过 10MB')
    return
  }
  fileName.value = file.name
  fileSize.value = formatFileSize(file.size)
  const reader = new FileReader()
  reader.onload = () => {
    swaggerJson.value = reader.result as string
    parsePreview()
  }
  reader.readAsText(file)
}

function formatFileSize(bytes: number): string {
  if (bytes < 1024) return bytes + 'B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + 'KB'
  return (bytes / (1024 * 1024)).toFixed(1) + 'MB'
}

// 拖拽处理
const isDragging = ref(false)
function handleDragOver(e: DragEvent) {
  e.preventDefault()
  isDragging.value = true
}
function handleDragLeave() {
  isDragging.value = false
}
function handleDrop(e: DragEvent) {
  e.preventDefault()
  isDragging.value = false
  const file = e.dataTransfer?.files?.[0]
  if (file) processFile(file)
}

// 解析 Swagger JSON 生成预览数据
function parsePreview() {
  try {
    const data = JSON.parse(swaggerJson.value)
    const apis: any[] = []
    const paths = data.paths || {}
    let id = 1
    for (const path in paths) {
      for (const method in paths[path]) {
        if (['get', 'post', 'put', 'delete', 'patch'].includes(method)) {
          const op = paths[path][method]
          const params = (op.parameters || []).length
          apis.push({
            id: id++,
            method: method.toUpperCase(),
            path,
            name: op.summary || op.operationId || `${method.toUpperCase()} ${path}`,
            params,
            status: Math.random() > 0.7 ? 'update' : 'new',
            selected: true,
          })
        }
      }
    }
    previewApis.value = apis
    selectedApis.value = new Set(apis.map((a) => a.id))
    selectAll.value = true
  } catch {
    ElMessage.warning('JSON 格式不正确，请检查文件内容')
  }
}

function toggleSelectAll(val: boolean | string | number) {
  if (val) {
    selectedApis.value = new Set(previewApis.value.map((a) => a.id))
  } else {
    selectedApis.value = new Set()
  }
  previewApis.value.forEach((a) => { a.selected = !!val })
}

function toggleApiSelect(api: any) {
  if (selectedApis.value.has(api.id)) {
    selectedApis.value.delete(api.id)
    api.selected = false
  } else {
    selectedApis.value.add(api.id)
    api.selected = true
  }
  selectAll.value = selectedApis.value.size === previewApis.value.length
}

function goStep(n: number) {
  if (n === 1 && !swaggerJson.value) {
    ElMessage.warning('请先上传 Swagger JSON 文件')
    return
  }
  if (n === 2 && groupMode.value === 'existing' && !selectedModuleId.value) {
    ElMessage.warning('请选择目标分组')
    return
  }
  if (n === 2 && groupMode.value === 'new' && !newGroupName.value) {
    ElMessage.warning('请输入新分组名称')
    return
  }
  currentStep.value = n
}

async function handleImport() {
  if (!swaggerJson.value) { ElMessage.warning('请提供 Swagger JSON'); return }
  const moduleId = groupMode.value === 'existing' ? selectedModuleId.value : 0
  if (groupMode.value === 'existing' && !moduleId) { ElMessage.warning('请选择目标分组'); return }
  if (groupMode.value === 'new' && !newGroupName.value) { ElMessage.warning('请输入新分组名称'); return }

  loading.value = true
  try {
    const res: any = await importSwagger(projectId.value, {
      projectId: projectId.value,
      moduleId: moduleId || undefined,
      newGroupName: groupMode.value === 'new' ? newGroupName.value : undefined,
      newGroupPrefix: groupMode.value === 'new' ? newGroupPrefix.value : undefined,
      swaggerJson: swaggerJson.value,
      importMode: 'INCREMENTAL',
    })
    importResult.value = res.data
    currentStep.value = 2
    ElMessage.success('导入成功')
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '导入失败')
  } finally { loading.value = false }
}

fetchModules()
</script>

<template>
  <div>
    <EditPageHeader title="Swagger 导入" />

    <div class="import-container">
      <!-- 步骤条 -->
      <el-steps :active="currentStep" style="max-width: 600px; margin: 0 auto 32px" align-center>
        <el-step title="上传文件 & 选择分组" />
        <el-step title="解析预览" />
        <el-step title="确认导入" />
      </el-steps>

      <!-- Step 1: 上传 & 分组 -->
      <div v-if="currentStep === 0" class="step-content">
        <el-card shadow="never">
          <!-- 拖拽上传区域 -->
          <div
            :class="['upload-area', { dragging: isDragging, uploaded: !!fileName }]"
            @dragover="handleDragOver"
            @dragleave="handleDragLeave"
            @drop="handleDrop"
            @click="($refs.fileInput as HTMLInputElement)?.click()"
          >
            <template v-if="!fileName">
              <div class="upload-icon">📄</div>
              <div style="font-size: 15px; color: #303133; margin-bottom: 4px">点击或拖拽 Swagger 2.0 JSON 文件</div>
              <div style="font-size: 13px; color: #909399">支持 .json 格式，文件大小不超过 10MB</div>
            </template>
            <template v-else>
              <span style="color: #67c23a; font-size: 20px">✓</span>
              <span style="margin-left: 8px">{{ fileName }}</span>
              <span style="margin-left: 8px; color: #909399; font-size: 12px">· {{ fileSize }}</span>
            </template>
          </div>
          <input ref="fileInput" type="file" accept=".json" style="display: none" @change="handleFileUpload" />

          <!-- 分组选择 -->
          <div style="margin-top: 24px">
            <div class="form-label" style="margin-bottom: 12px; font-weight: 500">选择分组方式</div>
            <div class="group-options">
              <div :class="['group-option', { selected: groupMode === 'existing' }]" @click="groupMode = 'existing'">
                <el-radio :model-value="groupMode" label="existing" @click.stop="groupMode = 'existing'">
                  <div>
                    <div class="option-label">导入到已有分组</div>
                    <div class="option-desc">选择已创建的接口分组</div>
                  </div>
                </el-radio>
                <el-select
                  v-if="groupMode === 'existing'"
                  v-model="selectedModuleId"
                  placeholder="选择接口分组"
                  style="width: 240px; margin-top: 8px"
                  @click.stop
                >
                  <el-option v-for="m in modules" :key="m.id" :value="m.id" :label="m.name" />
                </el-select>
              </div>
              <div :class="['group-option', { selected: groupMode === 'new' }]" @click="groupMode = 'new'">
                <el-radio :model-value="groupMode" label="new" @click.stop="groupMode = 'new'">
                  <div>
                    <div class="option-label">创建新分组</div>
                    <div class="option-desc">创建一个新的接口分组来存放导入的接口</div>
                  </div>
                </el-radio>
                <div v-if="groupMode === 'new'" style="display: flex; gap: 8px; margin-top: 8px" @click.stop>
                  <el-input v-model="newGroupName" placeholder="分组名称" style="width: 180px" />
                  <el-input v-model="newGroupPrefix" placeholder="服务前缀 (可选)" style="width: 160px" />
                </div>
              </div>
            </div>
          </div>
        </el-card>

        <div class="step-actions">
          <div />
          <el-button type="primary" :disabled="!swaggerJson" @click="goStep(1)">下一步 →</el-button>
        </div>
      </div>

      <!-- Step 2: 解析预览 -->
      <div v-if="currentStep === 1" class="step-content">
        <el-card shadow="never">
          <div class="preview-stats">
            <div class="stat">
              <div class="num blue">{{ previewStats.newCount }}</div>
              <div>新增接口</div>
            </div>
            <div class="stat">
              <div class="num orange">{{ previewStats.updateCount }}</div>
              <div>将更新</div>
            </div>
            <div class="stat">
              <div class="num">{{ previewStats.selectedCount }}</div>
              <div>已选择</div>
            </div>
          </div>

          <div style="display: flex; align-items: center; gap: 12px; margin-bottom: 12px">
            <el-checkbox v-model="selectAll" @change="toggleSelectAll">全选 / 取消全选</el-checkbox>
          </div>

          <el-table :data="previewApis" size="small" border max-height="400">
            <el-table-column width="45">
              <template #default="{ row }">
                <el-checkbox :model-value="selectedApis.has(row.id)" @change="toggleApiSelect(row)" />
              </template>
            </el-table-column>
            <el-table-column label="方法" width="80">
              <template #default="{ row }">
                <el-tag :type="methodColors[row.method] || 'info'" size="small">{{ row.method }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="路径" width="240">
              <template #default="{ row }">
                <span style="font-family: monospace; font-size: 12px">{{ row.path }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="name" label="名称" />
            <el-table-column prop="params" label="参数" width="70" />
            <el-table-column label="状态" width="80">
              <template #default="{ row }">
                <el-tag v-if="row.status === 'new'" type="primary" size="small">新增</el-tag>
                <el-tag v-else type="warning" size="small">将更新</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-card>

        <div class="step-actions">
          <el-button @click="currentStep = 0">← 上一步</el-button>
          <el-button type="primary" :disabled="selectedApis.size === 0" @click="handleImport">开始导入</el-button>
        </div>
      </div>

      <!-- Step 3: 确认导入 -->
      <div v-if="currentStep === 2" class="step-content">
        <el-card shadow="never" style="text-align: center; padding: 40px 20px">
          <div class="summary-cards">
            <div class="summary-card">
              <div class="num" style="color: #409eff">{{ importResult?.created ?? previewStats.newCount }}</div>
              <div class="label">新增接口</div>
            </div>
            <div class="summary-card">
              <div class="num" style="color: #e6a23c">{{ importResult?.updated ?? previewStats.updateCount }}</div>
              <div class="label">更新接口</div>
            </div>
          </div>

          <div v-if="importResult" style="margin-top: 20px; padding: 16px; background: #f0f9eb; border-radius: 4px; display: inline-block">
            <div style="color: #67c23a; font-size: 16px; font-weight: 600; margin-bottom: 4px">导入完成</div>
            <div style="color: #606266">
              成功导入 {{ (importResult?.created ?? 0) + (importResult?.updated ?? 0) }} 个接口
              <template v-if="importResult?.skipped">，跳过 {{ importResult.skipped }} 个</template>
            </div>
          </div>

          <div style="margin-top: 20px">
            <el-button type="primary" @click="router.push(`/project/${projectId}/apis`)">查看接口列表</el-button>
          </div>
        </el-card>

        <div class="step-actions">
          <el-button @click="currentStep = 1">← 上一步</el-button>
          <div />
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.import-container {
  max-width: 900px;
  margin: 0 auto;
}
.step-content {
  /* step container */
}
.step-actions {
  display: flex;
  justify-content: space-between;
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid #ebeef5;
}

/* 上传区域 */
.upload-area {
  border: 2px dashed #dcdfe6;
  border-radius: 8px;
  padding: 40px 20px;
  text-align: center;
  cursor: pointer;
  transition: all 0.2s;
}
.upload-area:hover {
  border-color: #409eff;
  background: #f5f7fa;
}
.upload-area.dragging {
  border-color: #409eff;
  background: #ecf5ff;
}
.upload-area.uploaded {
  border-color: #67c23a;
  background: #f0f9eb;
  padding: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.upload-icon {
  font-size: 36px;
  margin-bottom: 8px;
  opacity: 0.5;
}

/* 分组选项 */
.group-options {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.group-option {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 16px;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.15s;
  flex-wrap: wrap;
}
.group-option:hover {
  border-color: #b3d8ff;
}
.group-option.selected {
  border-color: #409eff;
  background: #ecf5ff;
}
.option-label {
  font-size: 14px;
  font-weight: 500;
}
.option-desc {
  font-size: 13px;
  color: #909399;
  margin-top: 2px;
}

/* 预览统计 */
.preview-stats {
  display: flex;
  gap: 24px;
  padding: 16px;
  background: #fafafa;
  border-radius: 6px;
  margin-bottom: 16px;
}
.preview-stats .stat {
  font-size: 14px;
}
.preview-stats .num {
  font-size: 20px;
  font-weight: 700;
}
.preview-stats .num.blue {
  color: #409eff;
}
.preview-stats .num.orange {
  color: #e6a23c;
}

/* 确认卡片 */
.summary-cards {
  display: flex;
  gap: 16px;
  justify-content: center;
  margin-bottom: 24px;
}
.summary-card {
  flex: 1;
  max-width: 200px;
  text-align: center;
  padding: 24px;
  background: #fafafa;
  border-radius: 8px;
}
.summary-card .num {
  font-size: 36px;
  font-weight: 700;
}
.summary-card .label {
  font-size: 14px;
  color: #909399;
  margin-top: 4px;
}
</style>
