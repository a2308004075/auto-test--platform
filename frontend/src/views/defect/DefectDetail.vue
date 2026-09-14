<!--
 @author HXN
 @date 2026-08-30
 @description 缺陷详情视图
-->
<script setup lang="ts">
/**
 * 缺陷详情
 * 标签页：内容、字段、工时、层级、关联、附件、变更记录
 */
import { ref, reactive, onMounted, computed, shallowRef } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getDefect, deleteDefect, transitionDefectStatus,
  addDefectWorkLog, deleteDefectWorkLog,
  addDefectRelation, deleteDefectRelation,
  addDefectAttachment, deleteDefectAttachment
} from '@/api/defect'
import PageHeader from '@/components/PageHeader/index.vue'
import CaseSelectDialog from '@/components/CaseSelectDialog/index.vue'
import { useDict } from '@/composables/useDict'
import { Editor, Toolbar } from '@wangeditor/editor-for-vue'
import '@wangeditor/editor/dist/css/style.css'

const route = useRoute()
const router = useRouter()
const projectId = computed(() => Number(route.params.id))
const defectId = computed(() => Number(route.params.defectId))
const { options: relationTypeOptions } = useDict('defect_relation_type')
const { options: targetTypeOptions } = useDict('defect_target_type')

const relationTypeLabelMap = computed(() => {
  const map: Record<string, string> = {}
  relationTypeOptions.value.forEach((o) => { map[o.value] = o.label })
  return map
})
const targetTypeLabelMap = computed(() => {
  const map: Record<string, string> = {}
  targetTypeOptions.value.forEach((o) => { map[o.value] = o.label })
  return map
})

const loading = ref(false)
const detail = ref<any>({})
const activeTab = ref('content')

// 工时
const workLogForm = reactive({ logDate: '', hours: 0, workType: 'ACTUAL', description: '' })
const workLogVisible = ref(false)

// 关联
const relationForm = reactive({ relationType: 'RELATED', targetType: 'AUTO_CASE', targetId: undefined as number | undefined, targetTitle: '' })
const relationVisible = ref(false)
// 用例类目标（手动/自动化用例）支持搜索选择，其余类型手动输入
const isCaseTarget = computed(() => ['MANUAL_CASE', 'AUTO_CASE'].includes(relationForm.targetType))
const caseSelectVisible = ref(false)

function handleTargetTypeChange() {
  relationForm.targetId = undefined
  relationForm.targetTitle = ''
}

function handleCaseConfirm(rows: Array<{ id: number; title: string }>) {
  if (rows.length === 0) return
  relationForm.targetId = rows[0].id
  relationForm.targetTitle = rows[0].title
}

// 附件
const attachmentForm = reactive({ fileName: '', fileUrl: '', fileSize: undefined as number | undefined })
const attachmentVisible = ref(false)

// WangEditor 只读
const editorRef = shallowRef<any>(null)
const editorConfig = { readOnly: true }
function onEditorCreated(editor: any) { editorRef.value = editor }

const statusLabelMap: Record<string, string> = {
  NEW: '新建', PENDING: '待验证', COMPLETED: '已完成', REOPENED: '重新打开', CLOSED: '已关闭'
}
const statusTypeMap: Record<string, string> = {
  NEW: 'info', PENDING: 'warning', COMPLETED: 'success', REOPENED: 'danger', CLOSED: ''
}
const severityTypeMap: Record<string, string> = { '致命': 'danger', '严重': 'warning', '一般': 'info', '提示': '' }

async function fetchDetail() {
  loading.value = true
  try {
    const res: any = await getDefect(projectId.value, defectId.value)
    detail.value = res.data || {}
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '加载缺陷详情失败')
  } finally {
    loading.value = false
  }
}

function handleEdit() {
  router.push(`/project/${projectId.value}/defects/${defectId.value}/edit`)
}

function handleDelete() {
  ElMessageBox.confirm(`确定删除缺陷「${detail.value.defectNo}」？`, '确认删除', { type: 'warning' })
    .then(async () => {
      await deleteDefect(projectId.value, defectId.value)
      ElMessage.success('删除成功')
      router.push(`/project/${projectId.value}/defects`)
    })
    .catch(() => {})
}

async function handleTransition(targetStatus: string) {
  try {
    await transitionDefectStatus(projectId.value, defectId.value, { targetStatus })
    ElMessage.success('状态更新成功')
    fetchDetail()
  } catch { ElMessage.error('操作失败') }
}

// 工时
async function handleAddWorkLog() {
  if (!workLogForm.hours) { ElMessage.warning('请输入工时'); return }
  try {
    await addDefectWorkLog(projectId.value, defectId.value, {
      logDate: workLogForm.logDate || undefined,
      hours: workLogForm.hours,
      workType: workLogForm.workType,
      description: workLogForm.description,
    })
    ElMessage.success('添加成功')
    workLogVisible.value = false
    Object.assign(workLogForm, { logDate: '', hours: 0, workType: 'ACTUAL', description: '' })
    fetchDetail()
  } catch (e: any) { ElMessage.error(e?.response?.data?.message || '添加失败') }
}

async function handleDeleteWorkLog(id: number) {
  try {
    await deleteDefectWorkLog(projectId.value, defectId.value, id)
    ElMessage.success('删除成功')
    fetchDetail()
  } catch { ElMessage.error('删除失败') }
}

// 关联
async function handleAddRelation() {
  if (!relationForm.targetId) {
    ElMessage.warning(isCaseTarget.value ? '请选择关联的用例' : '请输入关联目标 ID')
    return
  }
  try {
    await addDefectRelation(projectId.value, defectId.value, relationForm)
    ElMessage.success('添加成功')
    relationVisible.value = false
    Object.assign(relationForm, { relationType: 'RELATED', targetType: 'AUTO_CASE', targetId: undefined, targetTitle: '' })
    fetchDetail()
  } catch (e: any) { ElMessage.error(e?.response?.data?.message || '添加失败') }
}

async function handleDeleteRelation(id: number) {
  try {
    await deleteDefectRelation(projectId.value, defectId.value, id)
    ElMessage.success('删除成功')
    fetchDetail()
  } catch { ElMessage.error('删除失败') }
}

// 附件
async function handleAddAttachment() {
  if (!attachmentForm.fileName || !attachmentForm.fileUrl) { ElMessage.warning('请填写文件名和链接'); return }
  try {
    await addDefectAttachment(projectId.value, defectId.value, {
      fileName: attachmentForm.fileName,
      fileUrl: attachmentForm.fileUrl,
      fileSize: attachmentForm.fileSize,
    })
    ElMessage.success('添加成功')
    attachmentVisible.value = false
    Object.assign(attachmentForm, { fileName: '', fileUrl: '', fileSize: undefined })
    fetchDetail()
  } catch (e: any) { ElMessage.error(e?.response?.data?.message || '添加失败') }
}

async function handleDeleteAttachment(id: number) {
  try {
    await deleteDefectAttachment(projectId.value, defectId.value, id)
    ElMessage.success('删除成功')
    fetchDetail()
  } catch { ElMessage.error('删除失败') }
}

function openFile(url: string) {
  window.open(url, '_blank')
}

function formatDate(date: string) {
  return date || '-'
}

onMounted(() => {
  fetchDetail()
})
</script>

<template>
  <div>
    <PageHeader :title="detail.defectNo || '缺陷详情'">
      <el-button type="primary" @click="handleEdit">编辑</el-button>
      <el-dropdown split-button type="primary" @command="handleTransition">
        状态流转
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="PENDING">待验证</el-dropdown-item>
            <el-dropdown-item command="COMPLETED">已完成</el-dropdown-item>
            <el-dropdown-item command="REOPENED">重新打开</el-dropdown-item>
            <el-dropdown-item command="CLOSED">已关闭</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
      <el-button type="danger" @click="handleDelete">删除</el-button>
    </PageHeader>

    <div v-loading="loading" class="detail-card">
      <div class="detail-header">
        <div class="detail-title">{{ detail.title }}</div>
        <div class="detail-meta">
          <el-tag :type="(statusTypeMap[detail.status] || 'info') as any" size="small">{{ statusLabelMap[detail.status] || detail.status }}</el-tag>
          <el-tag :type="(severityTypeMap[detail.severity] || 'info') as any" size="small" style="margin-left: 8px">{{ detail.severity }}</el-tag>
          <span class="meta-item">创建人：{{ detail.createdByName || '-' }}</span>
          <span class="meta-item">创建时间：{{ detail.createdAt }}</span>
        </div>
      </div>

      <el-tabs v-model="activeTab" type="border-card">
        <!-- 内容 -->
        <el-tab-pane label="内容" name="content">
          <div class="editor-wrapper">
            <Toolbar :editor="editorRef" :default-config="editorConfig" mode="default" style="border-bottom: 1px solid #ccc" />
            <Editor v-model="detail.content" :default-config="editorConfig" mode="default" style="height: 400px; overflow-y: hidden" @on-created="onEditorCreated" />
          </div>
        </el-tab-pane>

        <!-- 字段 -->
        <el-tab-pane label="字段" name="fields">
          <div class="field-grid">
            <div class="field-item"><span class="field-label">负责人：</span><span>{{ detail.assigneeName || '-' }}</span></div>
            <div class="field-item"><span class="field-label">计划完成时间：</span><span>{{ formatDate(detail.dueDate) }}</span></div>
            <div class="field-item"><span class="field-label">发现的版本：</span><span>{{ detail.foundVersion || '-' }}</span></div>
            <div class="field-item"><span class="field-label">所属模块：</span><span>{{ detail.moduleName || '-' }}</span></div>
            <div class="field-item"><span class="field-label">严重级别：</span><span>{{ detail.severity || '-' }}</span></div>
            <div class="field-item"><span class="field-label">缺陷根源：</span><span>{{ detail.source || '-' }}</span></div>
            <div class="field-item"><span class="field-label">环境信息：</span><span>{{ detail.environmentName || '-' }}</span></div>
            <div class="field-item"><span class="field-label">原因描述：</span><span>{{ detail.reasonDescription || '-' }}</span></div>
            <div class="field-item"><span class="field-label">责任人：</span><span>{{ detail.responsibleName || '-' }}</span></div>
            <div class="field-item"><span class="field-label">重新打开次数：</span><span>{{ detail.reopenCount ?? 0 }}</span></div>
            <div class="field-item"><span class="field-label">修改的版本：</span><span>{{ detail.fixedVersion || '-' }}</span></div>
            <div class="field-item"><span class="field-label">计划提测时间：</span><span>{{ formatDate(detail.planTestDate) }}</span></div>
          </div>

          <div class="summary-hours">
            <div class="summary-title">汇总工时</div>
            <div class="field-grid">
              <div class="field-item"><span class="field-label">总估算工时：</span><span>{{ detail.estimatedHours ?? 0 }} 小时</span></div>
              <div class="field-item"><span class="field-label">总实际工时：</span><span>{{ detail.actualHours ?? 0 }} 小时</span></div>
              <div class="field-item"><span class="field-label">总剩余工时：</span><span>{{ detail.remainingHours ?? 0 }} 小时</span></div>
            </div>
          </div>
        </el-tab-pane>

        <!-- 工时 -->
        <el-tab-pane label="工时" name="workLogs">
          <div class="tab-toolbar">
            <el-button type="primary" size="small" @click="workLogVisible = true">添加工时</el-button>
          </div>
          <el-table :data="detail.workLogs || []" border stripe>
            <el-table-column prop="logDate" label="日期" width="120" />
            <el-table-column prop="hours" label="工时（小时）" width="120" />
            <el-table-column prop="workType" label="类型" width="120" />
            <el-table-column prop="description" label="说明" />
            <el-table-column prop="userName" label="记录人" width="120" />
            <el-table-column label="操作" width="80">
              <template #default="{ row }">
                <el-button type="danger" link size="small" @click="handleDeleteWorkLog(row.id)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- 层级 -->
        <el-tab-pane label="层级" name="children">
          <el-table :data="detail.children || []" border stripe>
            <el-table-column prop="defectNo" label="缺陷编号" width="160" />
            <el-table-column prop="title" label="标题" />
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="(statusTypeMap[row.status] || 'info') as any" size="small">{{ statusLabelMap[row.status] || row.status }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="assigneeName" label="负责人" width="120" />
          </el-table>
        </el-tab-pane>

        <!-- 关联 -->
        <el-tab-pane label="关联" name="relations">
          <div class="tab-toolbar">
            <el-button type="primary" size="small" @click="relationVisible = true">添加关联</el-button>
          </div>
          <el-table :data="detail.relations || []" border stripe>
            <el-table-column label="关联类型" width="120">
              <template #default="{ row }">{{ relationTypeLabelMap[row.relationType] || row.relationType }}</template>
            </el-table-column>
            <el-table-column label="目标类型" width="140">
              <template #default="{ row }">{{ targetTypeLabelMap[row.targetType] || row.targetType }}</template>
            </el-table-column>
            <el-table-column prop="targetId" label="目标 ID" width="100" />
            <el-table-column prop="targetTitle" label="目标标题" />
            <el-table-column label="操作" width="80">
              <template #default="{ row }">
                <el-button type="danger" link size="small" @click="handleDeleteRelation(row.id)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- 附件 -->
        <el-tab-pane label="附件" name="attachments">
          <div class="tab-toolbar">
            <el-button type="primary" size="small" @click="attachmentVisible = true">添加附件</el-button>
          </div>
          <el-table :data="detail.attachments || []" border stripe>
            <el-table-column prop="fileName" label="文件名" />
            <el-table-column prop="fileSize" label="大小（字节）" width="130" />
            <el-table-column prop="createdByName" label="上传人" width="120" />
            <el-table-column label="操作" width="140">
              <template #default="{ row }">
                <el-button type="primary" link size="small" @click="openFile(row.fileUrl)">下载</el-button>
                <el-button type="danger" link size="small" @click="handleDeleteAttachment(row.id)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- 变更记录 -->
        <el-tab-pane label="变更记录" name="histories">
          <el-timeline>
            <el-timeline-item
              v-for="h in (detail.histories || [])"
              :key="h.id"
              :timestamp="h.createdAt"
            >
              <div><b>{{ h.changedByName || '系统' }}</b> 更新了 <b>{{ h.fieldName }}</b></div>
              <div v-if="h.oldValue" class="history-value old">{{ h.oldValue }}</div>
              <div v-if="h.newValue" class="history-value new">{{ h.newValue }}</div>
            </el-timeline-item>
          </el-timeline>
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- 添加工时弹窗 -->
    <el-dialog v-model="workLogVisible" title="添加工时" width="460px">
      <el-form label-position="top">
        <el-form-item label="日期">
          <el-date-picker v-model="workLogForm.logDate" type="date" placeholder="选择日期" style="width: 100%" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="工时（小时）" required>
          <el-input-number v-model="workLogForm.hours" :min="0" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="workLogForm.workType" style="width: 100%">
            <el-option value="ACTUAL" label="实际工时" />
            <el-option value="ESTIMATE" label="估算工时" />
            <el-option value="REMAINING" label="剩余工时" />
          </el-select>
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="workLogForm.description" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="workLogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleAddWorkLog">确定</el-button>
      </template>
    </el-dialog>

    <!-- 添加关联弹窗 -->
    <el-dialog v-model="relationVisible" title="添加关联" width="460px">
      <el-form label-position="top">
        <el-form-item label="关联类型">
          <el-select v-model="relationForm.relationType" style="width: 100%">
            <el-option v-for="r in relationTypeOptions" :key="r.value" :value="r.value" :label="r.label" />
          </el-select>
        </el-form-item>
        <el-form-item label="目标类型">
          <el-select v-model="relationForm.targetType" style="width: 100%" @change="handleTargetTypeChange">
            <el-option v-for="t in targetTypeOptions" :key="t.value" :value="t.value" :label="t.label" />
          </el-select>
        </el-form-item>
        <!-- 用例类目标：搜索选择，自动带出 ID/标题 -->
        <el-form-item v-if="isCaseTarget" label="关联目标" required>
          <div style="display: flex; gap: 8px; width: 100%">
            <el-input :model-value="relationForm.targetTitle" placeholder="点击右侧按钮选择用例" readonly style="flex: 1" />
            <el-button type="primary" @click="caseSelectVisible = true">选择用例</el-button>
          </div>
        </el-form-item>
        <!-- 其余目标类型：保持手动输入 -->
        <template v-else>
          <el-form-item label="目标 ID" required>
            <el-input-number v-model="relationForm.targetId" :controls="false" style="width: 100%" />
          </el-form-item>
          <el-form-item label="目标标题">
            <el-input v-model="relationForm.targetTitle" placeholder="关联目标标题快照" />
          </el-form-item>
        </template>
      </el-form>
      <template #footer>
        <el-button @click="relationVisible = false">取消</el-button>
        <el-button type="primary" @click="handleAddRelation">确定</el-button>
      </template>
    </el-dialog>

    <!-- 用例选择弹窗（单选） -->
    <CaseSelectDialog v-model:visible="caseSelectVisible" :project-id="projectId" @confirm="handleCaseConfirm" />

    <!-- 添加附件弹窗 -->
    <el-dialog v-model="attachmentVisible" title="添加附件" width="460px">
      <el-form label-position="top">
        <el-form-item label="文件名" required>
          <el-input v-model="attachmentForm.fileName" />
        </el-form-item>
        <el-form-item label="文件链接" required>
          <el-input v-model="attachmentForm.fileUrl" placeholder="文件访问 URL" />
        </el-form-item>
        <el-form-item label="文件大小（字节）">
          <el-input-number v-model="attachmentForm.fileSize" :controls="false" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="attachmentVisible = false">取消</el-button>
        <el-button type="primary" @click="handleAddAttachment">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.detail-card {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  padding: 20px 24px;
}
.detail-header {
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid #ebeef5;
}
.detail-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 10px;
}
.detail-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}
.meta-item {
  font-size: 13px;
  color: #909399;
}
.editor-wrapper {
  border: 1px solid #ccc;
  border-radius: 4px;
  overflow: hidden;
}
.field-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px 32px;
}
.field-item {
  display: flex;
  font-size: 14px;
  color: #606266;
}
.field-label {
  color: #909399;
  min-width: 100px;
}
.summary-hours {
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid #ebeef5;
}
.summary-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 16px;
}
.tab-toolbar {
  margin-bottom: 12px;
}
.history-value {
  margin-top: 4px;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 13px;
}
.history-value.old {
  background: #f4f4f5;
  color: #909399;
  text-decoration: line-through;
}
.history-value.new {
  background: #f0f9ff;
  color: #409eff;
}
</style>
