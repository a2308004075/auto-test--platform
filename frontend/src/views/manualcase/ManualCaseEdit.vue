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
    } else if (caseId.value) {
      await updateManualCase(projectId.value, caseId.value, form)
      ElMessage.success('更新成功')
    }
    router.push(`/project/${projectId.value}/manual-cases`)
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

function handleCancel() {
  router.push(`/project/${projectId.value}/manual-cases`)
}

onMounted(() => {
  fetchGroups()
  fetchDetail()
})
</script>

<template>
  <div>
    <PageHeader :title="isNew ? '新建手动化用例' : '编辑手动化用例'">
      <el-button @click="handleCancel">取消</el-button>
      <el-button v-if="hasPermission('project:manual-case:edit') || isNew" type="primary" :loading="saving" @click="handleSave">保存</el-button>
    </PageHeader>

    <div v-loading="loading" class="edit-form">
      <el-form label-position="top" :model="form">
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
  </div>
</template>

<style scoped>
.edit-form {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  padding: 20px 24px;
  max-width: 900px;
}
.form-row {
  display: flex;
  gap: 16px;
}
.form-row > * {
  min-width: 0;
}
</style>
