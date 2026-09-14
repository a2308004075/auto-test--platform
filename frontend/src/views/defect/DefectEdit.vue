<!--
 @author HXN
 @date 2026-08-30
 @description 缺陷编辑视图
-->
<script setup lang="ts">
/**
 * 缺陷编辑
 * 表单：标题、内容（富文本）、负责人、计划完成时间、发现版本、所属模块、严重级别、缺陷根源、环境、原因描述、责任人等
 */
import { ref, reactive, onMounted, computed, shallowRef } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getDefect, createDefect, updateDefect, getDefectGroups } from '@/api/defect'
import { getEnvironments } from '@/api/environment'
import PageHeader from '@/components/PageHeader/index.vue'
import { useDict } from '@/composables/useDict'
import { Editor, Toolbar } from '@wangeditor/editor-for-vue'
import '@wangeditor/editor/dist/css/style.css'

const route = useRoute()
const router = useRouter()
const projectId = computed(() => Number(route.params.id))
const defectId = computed(() => route.params.defectId ? Number(route.params.defectId) : null)
const isNew = computed(() => route.params.defectId === 'new' || !route.params.defectId)
const { options: severityOptions } = useDict('defect_severity')
const { options: sourceOptions } = useDict('defect_source')

const loading = ref(false)
const saving = ref(false)
const groups = ref<any[]>([])
const environments = ref<any[]>([])

const form = reactive({
  groupId: null as number | null,
  title: '',
  content: '',
  assigneeId: null as number | null,
  dueDate: null as string | null,
  foundVersion: '',
  moduleName: '',
  severity: '一般',
  source: '开发修改引入',
  environmentId: null as number | null,
  reasonDescription: '',
  responsibleId: null as number | null,
  fixedVersion: '',
  planTestDate: null as string | null,
  parentId: null as number | null,
  estimatedHours: 0,
  actualHours: 0,
  remainingHours: 0,
})

const userGroups = computed(() => groups.value.filter((g) => g.isSystem !== 1))

// WangEditor
const editorRef = shallowRef<any>(null)
const editorConfig = {
  placeholder: '请输入缺陷内容...',
  MENU_CONF: {
    uploadImage: { disabled: true },
    uploadVideo: { disabled: true },
  },
}
function onEditorCreated(editor: any) { editorRef.value = editor }
function onEditorChange(editor: any) { form.content = editor.getHtml() }

async function fetchGroups() {
  try {
    const res: any = await getDefectGroups(projectId.value)
    groups.value = res.data || []
  } catch { groups.value = [] }
}

async function fetchEnvironments() {
  try {
    const res: any = await getEnvironments(projectId.value)
    environments.value = res.data || []
  } catch { environments.value = [] }
}

async function fetchDetail() {
  if (isNew.value || !defectId.value) return
  loading.value = true
  try {
    const res: any = await getDefect(projectId.value, defectId.value)
    const data = res.data
    if (data) {
      form.groupId = data.groupId ?? null
      form.title = data.title || ''
      form.content = data.content || ''
      form.assigneeId = data.assigneeId ?? null
      form.dueDate = data.dueDate || null
      form.foundVersion = data.foundVersion || ''
      form.moduleName = data.moduleName || ''
      form.severity = data.severity || '一般'
      form.source = data.source || '开发修改引入'
      form.environmentId = data.environmentId ?? null
      form.reasonDescription = data.reasonDescription || ''
      form.responsibleId = data.responsibleId ?? null
      form.fixedVersion = data.fixedVersion || ''
      form.planTestDate = data.planTestDate || null
      form.parentId = data.parentId ?? null
      form.estimatedHours = data.estimatedHours || 0
      form.actualHours = data.actualHours || 0
      form.remainingHours = data.remainingHours || 0
    }
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '加载缺陷详情失败')
  } finally {
    loading.value = false
  }
}

async function handleSave() {
  if (!form.title.trim()) {
    ElMessage.warning('请输入缺陷标题')
    return
  }
  saving.value = true
  try {
    const payload = { ...form }
    if (isNew.value) {
      await createDefect(projectId.value, payload)
      ElMessage.success('创建成功')
    } else if (defectId.value) {
      await updateDefect(projectId.value, defectId.value, payload)
      ElMessage.success('更新成功')
    }
    router.push(`/project/${projectId.value}/defects`)
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

function handleCancel() {
  router.push(`/project/${projectId.value}/defects`)
}

onMounted(() => {
  fetchGroups()
  fetchEnvironments()
  fetchDetail()
})
</script>

<template>
  <div>
    <PageHeader :title="isNew ? '新建缺陷' : '编辑缺陷'">
      <el-button @click="handleCancel">取消</el-button>
      <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
    </PageHeader>

    <div v-loading="loading" class="edit-form">
      <el-form label-position="top" :model="form">
        <!-- 基本信息 -->
        <div class="form-section">
          <div class="form-section-title">基本信息</div>
          <el-form-item label="缺陷标题" required>
            <el-input v-model="form.title" placeholder="请输入缺陷标题" maxlength="500" show-word-limit />
          </el-form-item>

          <div class="form-row">
            <el-form-item label="所属分组" style="flex: 1">
              <el-select v-model="form.groupId" placeholder="未分组" clearable filterable style="width: 100%">
                <el-option v-for="g in userGroups" :key="g.id" :value="g.id" :label="g.name" />
              </el-select>
            </el-form-item>
            <el-form-item label="负责人" style="flex: 1">
              <el-input-number v-model="form.assigneeId" :controls="false" placeholder="用户 ID" style="width: 100%" />
            </el-form-item>
            <el-form-item label="计划完成时间" style="flex: 1">
              <el-date-picker v-model="form.dueDate" type="date" placeholder="选择日期" style="width: 100%" value-format="YYYY-MM-DD" />
            </el-form-item>
          </div>
        </div>

        <!-- 缺陷内容 -->
        <div class="form-section">
          <div class="form-section-title">内容</div>
          <div class="editor-wrapper">
            <Toolbar
              :editor="editorRef"
              :default-config="editorConfig"
              mode="default"
              style="border-bottom: 1px solid #ccc"
            />
            <Editor
              v-model="form.content"
              :default-config="editorConfig"
              mode="default"
              style="height: 300px; overflow-y: hidden"
              @on-created="onEditorCreated"
              @on-change="onEditorChange"
            />
          </div>
        </div>

        <!-- 字段信息 -->
        <div class="form-section">
          <div class="form-section-title">字段信息</div>
          <div class="form-row">
            <el-form-item label="发现的版本" style="flex: 1">
              <el-input v-model="form.foundVersion" placeholder="如：V2.8.7" />
            </el-form-item>
            <el-form-item label="所属模块" style="flex: 1">
              <el-input v-model="form.moduleName" placeholder="如：换电管理" />
            </el-form-item>
          </div>
          <div class="form-row">
            <el-form-item label="严重级别" style="flex: 1">
              <el-select v-model="form.severity" style="width: 100%">
                <el-option v-for="s in severityOptions" :key="s.value" :value="s.value" :label="s.label" />
              </el-select>
            </el-form-item>
            <el-form-item label="缺陷根源" style="flex: 1">
              <el-select v-model="form.source" style="width: 100%">
                <el-option v-for="s in sourceOptions" :key="s.value" :value="s.value" :label="s.label" />
              </el-select>
            </el-form-item>
          </div>
          <div class="form-row">
            <el-form-item label="环境信息" style="flex: 1">
              <el-select v-model="form.environmentId" placeholder="选择环境" clearable filterable style="width: 100%">
                <el-option v-for="e in environments" :key="e.id" :value="e.id" :label="e.name" />
              </el-select>
            </el-form-item>
            <el-form-item label="原因描述" style="flex: 1">
              <el-input v-model="form.reasonDescription" placeholder="原因描述" />
            </el-form-item>
          </div>
          <div class="form-row">
            <el-form-item label="责任人" style="flex: 1">
              <el-input-number v-model="form.responsibleId" :controls="false" placeholder="用户 ID" style="width: 100%" />
            </el-form-item>
            <el-form-item label="修改的版本" style="flex: 1">
              <el-input v-model="form.fixedVersion" placeholder="如：V2.8.8" />
            </el-form-item>
          </div>
          <div class="form-row">
            <el-form-item label="计划提测时间" style="flex: 1">
              <el-date-picker v-model="form.planTestDate" type="date" placeholder="选择日期" style="width: 100%" value-format="YYYY-MM-DD" />
            </el-form-item>
            <el-form-item label="父缺陷 ID" style="flex: 1">
              <el-input-number v-model="form.parentId" :controls="false" placeholder="父缺陷 ID" style="width: 100%" />
            </el-form-item>
          </div>
        </div>

        <!-- 汇总工时 -->
        <div class="form-section">
          <div class="form-section-title">汇总工时</div>
          <div class="form-row">
            <el-form-item label="总估算工时" style="flex: 1">
              <el-input-number v-model="form.estimatedHours" :min="0" :precision="2" style="width: 100%" />
            </el-form-item>
            <el-form-item label="总实际工时" style="flex: 1">
              <el-input-number v-model="form.actualHours" :min="0" :precision="2" style="width: 100%" />
            </el-form-item>
            <el-form-item label="总剩余工时" style="flex: 1">
              <el-input-number v-model="form.remainingHours" :min="0" :precision="2" style="width: 100%" />
            </el-form-item>
          </div>
        </div>
      </el-form>
    </div>
  </div>
</template>

<style scoped>
.edit-form {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  padding: 20px 24px;
}
.form-section {
  margin-bottom: 24px;
}
.form-section:last-child {
  margin-bottom: 0;
}
.form-section-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 16px;
  padding-bottom: 8px;
  border-bottom: 1px solid #ebeef5;
}
.form-row {
  display: flex;
  gap: 16px;
}
.form-row > * {
  min-width: 0;
}
.editor-wrapper {
  border: 1px solid #ccc;
  border-radius: 4px;
  overflow: hidden;
}
</style>
