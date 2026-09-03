<!--
 @author HXN
 @date 2026-08-30
 @description 手动化用例编辑视图
-->
<script setup lang="ts">
/**
 * 手动化用例编辑
 * 表单：标题、前置条件、操作步骤、预期结果、用例类型、优先级、环境执行、用例状态
 */
import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getManualCase, createManualCase, updateManualCase, getManualCaseGroups } from '@/api/manualCase'
import PageHeader from '@/components/PageHeader/index.vue'
import CommentPanel from '@/components/CommentPanel/index.vue'
import ChangeLogPanel from '@/components/ChangeLogPanel/index.vue'
import CaseRequirementPanel from '@/components/CaseRequirementPanel/index.vue'
import CaseDefectPanel from '@/components/CaseDefectPanel/index.vue'
import { useDict } from '@/composables/useDict'
import { usePermission } from '@/composables/usePermission'

const route = useRoute()
const router = useRouter()
const projectId = computed(() => Number(route.params.id))
const caseId = computed(() => route.params.caseId ? Number(route.params.caseId) : null)
const isNew = computed(() => route.params.caseId === 'new' || !route.params.caseId)
const { hasPermission } = usePermission()
const { options: priorityOptions } = useDict('priority')

const loading = ref(false)
const saving = ref(false)
const editing = ref(isNew.value)
const groups = ref<any[]>([])

const form = reactive({
  title: '',
  preconditions: '',
  operationSteps: '',
  expectedResult: '',
  caseType: 'NORMAL',
  priority: '中',
  groupId: null as number | null,
  runInTestEnv: 1,
  runInProdEnv: 0,
  caseStatus: 1,
})

const caseTypeOptions = [
  { value: 'NORMAL', label: '正常' },
  { value: 'EXCEPTION', label: '异常' },
]

const userGroups = computed(() => groups.value.filter((g) => g.isSystem !== 1))

async function fetchGroups() {
  try {
    const res: any = await getManualCaseGroups(projectId.value)
    groups.value = res.data || []
  } catch { groups.value = [] }
}

async function fetchDetail() {
  if (isNew.value || !caseId.value) return
  loading.value = true
  try {
    const res: any = await getManualCase(projectId.value, caseId.value)
    const data = res.data
    if (data) {
      form.title = data.title || ''
      form.preconditions = data.preconditions || ''
      form.operationSteps = data.operationSteps || ''
      form.expectedResult = data.expectedResult || ''
      form.caseType = data.caseType || 'NORMAL'
      form.priority = data.priority || '中'
      form.groupId = data.groupId ?? null
      form.runInTestEnv = data.runInTestEnv ?? 1
      form.runInProdEnv = data.runInProdEnv ?? 0
      form.caseStatus = data.caseStatus ?? 1
    }
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '加载用例详情失败')
  } finally {
    loading.value = false
  }
}

function startEdit() {
  editing.value = true
}

async function handleSave() {
  if (!form.title.trim()) {
    ElMessage.warning('请输入用例标题')
    return
  }
  saving.value = true
  try {
    if (isNew.value) {
      await createManualCase(projectId.value, form)
      ElMessage.success('创建成功')
      router.push(`/project/${projectId.value}/manual-cases`)
    } else if (caseId.value) {
      await updateManualCase(projectId.value, caseId.value, form)
      ElMessage.success('更新成功')
      editing.value = false
    }
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

async function handleCancel() {
  if (isNew.value) {
    router.push(`/project/${projectId.value}/manual-cases`)
    return
  }
  // 编辑模式：恢复数据并退出编辑
  await fetchDetail()
  editing.value = false
}

function handleBack() {
  router.push(`/project/${projectId.value}/manual-cases`)
}

// ===== 详情标签页（仅编辑模式） =====
const activeTab = ref('comment')

const fieldLabelMap: Record<string, string> = {
  title: '用例标题',
  preconditions: '前置条件',
  operationSteps: '操作步骤',
  expectedResult: '预期结果',
  caseType: '用例类型',
  priority: '优先级',
  groupId: '所属分组',
  runInTestEnv: '测试环境执行',
  runInProdEnv: '生产环境执行',
  caseStatus: '用例状态',
}

const valueLabelMap: Record<string, Record<string, string>> = {
  caseType: { NORMAL: '正常', EXCEPTION: '异常' },
  priority: { '高': '高', '中': '中', '低': '低' },
  runInTestEnv: { '0': '否', '1': '是' },
  runInProdEnv: { '0': '否', '1': '是' },
  caseStatus: { '0': '废弃', '1': '使用' },
}

onMounted(() => {
  fetchGroups()
  fetchDetail()
})
</script>

<template>
  <div>
    <PageHeader :title="isNew ? '新建手动化用例' : '编辑手动化用例'">
      <el-button @click="handleBack">返回</el-button>
    </PageHeader>

    <div class="edit-layout">
      <!-- 左侧：编辑表单 -->
      <div v-loading="loading" class="edit-form" :class="{ 'edit-form-full': isNew }">
        <div v-if="isNew || hasPermission('project:manual-case:edit')" class="form-toolbar">
          <el-button v-if="isNew" @click="handleCancel">取消</el-button>
          <el-button v-if="isNew" type="primary" :loading="saving" @click="handleSave">保存</el-button>
          <el-button v-if="!isNew && editing" @click="handleCancel">取消</el-button>
          <el-button v-if="!isNew && editing" type="primary" :loading="saving" @click="handleSave">保存</el-button>
          <el-button v-if="!isNew && !editing" type="primary" @click="startEdit">编辑</el-button>
        </div>
        <el-form label-position="top" :model="form" :disabled="!editing">
          <el-form-item label="用例标题" required>
            <el-input v-model="form.title" placeholder="请输入用例标题" maxlength="200" show-word-limit />
          </el-form-item>
          <el-form-item label="前置条件">
            <el-input v-model="form.preconditions" type="textarea" :rows="3" placeholder="执行该用例前需要满足的条件" />
          </el-form-item>
          <el-form-item label="操作步骤">
            <el-input v-model="form.operationSteps" type="textarea" :rows="5" placeholder="详细的操作步骤描述" />
          </el-form-item>
          <el-form-item label="预期结果">
            <el-input v-model="form.expectedResult" type="textarea" :rows="3" placeholder="执行操作步骤后预期的结果" />
          </el-form-item>

          <div class="form-row">
            <el-form-item label="用例类型" style="flex: 1">
              <el-select v-model="form.caseType" style="width: 100%">
                <el-option v-for="opt in caseTypeOptions" :key="opt.value" :value="opt.value" :label="opt.label" />
              </el-select>
            </el-form-item>
            <el-form-item label="优先级" style="flex: 1">
              <el-select v-model="form.priority" style="width: 100%">
                <el-option v-for="p in priorityOptions" :key="p.value" :value="p.value" :label="p.label" />
              </el-select>
            </el-form-item>
            <el-form-item label="所属分组" style="flex: 1">
              <el-select v-model="form.groupId" placeholder="未分组" clearable filterable style="width: 100%">
                <el-option v-for="g in userGroups" :key="g.id" :value="g.id" :label="g.name" />
              </el-select>
            </el-form-item>
          </div>

          <div class="form-row">
            <el-form-item label="测试环境是否执行" style="flex: 1">
              <el-select v-model="form.runInTestEnv" placeholder="请选择" style="width: 100%">
                <el-option :value="1" label="是" />
                <el-option :value="0" label="否" />
              </el-select>
            </el-form-item>
            <el-form-item label="生产环境是否执行" style="flex: 1">
              <el-select v-model="form.runInProdEnv" placeholder="请选择" style="width: 100%">
                <el-option :value="1" label="是" />
                <el-option :value="0" label="否" />
              </el-select>
            </el-form-item>
            <el-form-item label="用例状态" style="flex: 1">
              <el-select v-model="form.caseStatus" placeholder="请选择" style="width: 100%">
                <el-option :value="1" label="使用" />
                <el-option :value="0" label="废弃" />
              </el-select>
            </el-form-item>
          </div>
        </el-form>
      </div>

      <!-- 右侧：详情标签页（仅编辑模式） -->
      <div v-if="!isNew && caseId" class="detail-tabs">
        <el-tabs v-model="activeTab" type="card">
          <el-tab-pane label="评论" name="comment">
            <CommentPanel biz-type="MANUAL_CASE" :biz-id="caseId" />
          </el-tab-pane>
          <el-tab-pane label="状态变更" name="status">
            <ChangeLogPanel
              biz-type="MANUAL_CASE"
              :biz-id="caseId"
              field-name="caseStatus"
              :field-label-map="fieldLabelMap"
              :value-label-map="valueLabelMap"
            />
          </el-tab-pane>
          <el-tab-pane label="关联需求" name="requirementRelation">
            <CaseRequirementPanel :project-id="projectId" case-type="MANUAL_CASE" :case-id="caseId" />
          </el-tab-pane>
          <el-tab-pane label="关联缺陷" name="defectRelation">
            <CaseDefectPanel :project-id="projectId" target-type="MANUAL_CASE" :target-id="caseId" />
          </el-tab-pane>
          <el-tab-pane label="变更记录" name="changeLog">
            <ChangeLogPanel
              biz-type="MANUAL_CASE"
              :biz-id="caseId"
              :field-label-map="fieldLabelMap"
              :value-label-map="valueLabelMap"
            />
          </el-tab-pane>
        </el-tabs>
      </div>
    </div>
  </div>
</template>

<style scoped>
.edit-layout {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  align-items: flex-start;
}
.edit-form {
  flex: 1 1 560px;
  min-width: 420px;
  max-width: 900px;
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  padding: 20px 24px;
}
.form-toolbar {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-bottom: 20px;
}
.edit-form.edit-form-full {
  max-width: none;
}
.detail-tabs {
  flex: 1 1 480px;
  min-width: 360px;
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  padding: 12px 24px 20px;
}
.form-row {
  display: flex;
  gap: 16px;
}
.form-row > * {
  min-width: 0;
}
</style>
