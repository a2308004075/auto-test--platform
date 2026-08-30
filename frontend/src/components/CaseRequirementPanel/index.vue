<!--
 @author HXN
 @date 2026-08-30
 @description 用例-需求关联面板（用例视角：查看/添加/解除关联的需求条目）
-->
<script setup lang="ts">
/**
 * 用例-需求关联面板
 * 反查当前用例关联的需求条目列表，支持选择需求条目批量添加、解除关联
 */
import { ref, computed, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getCaseRequirementRelations,
  addRequirementCaseRelation,
  deleteRequirementCaseRelation,
} from '@/api/relation'
import RequirementItemSelectDialog from '@/components/RequirementItemSelectDialog/index.vue'
import { useDict } from '@/composables/useDict'

const props = defineProps<{
  projectId: number
  /** MANUAL_CASE-手动用例，TEST_CASE-自动用例 */
  caseType: string
  caseId: number | null | undefined
}>()

const { options: statusOptions } = useDict('requirement_item_status')
const statusLabelMap = computed(() => {
  const map: Record<string, string> = {}
  statusOptions.value.forEach((o) => {
    map[o.value] = o.label
  })
  return map
})

const loading = ref(false)
const list = ref<any[]>([])
const selectVisible = ref(false)
const submitting = ref(false)

async function fetchList() {
  if (!props.caseId) {
    list.value = []
    return
  }
  loading.value = true
  try {
    const res: any = await getCaseRequirementRelations(props.projectId, props.caseType, props.caseId)
    list.value = res.data || []
  } catch {
    list.value = []
  } finally {
    loading.value = false
  }
}

async function handleConfirm(rows: Array<{ id: number }>) {
  if (!props.caseId || rows.length === 0) return
  submitting.value = true
  let failed = 0
  try {
    for (const row of rows) {
      try {
        await addRequirementCaseRelation(row.id, { caseType: props.caseType, caseId: props.caseId })
      } catch {
        failed++
      }
    }
    if (failed > 0) {
      ElMessage.warning(`${rows.length - failed} 条关联添加成功，${failed} 条失败（可能已存在关联）`)
    } else {
      ElMessage.success(`已添加 ${rows.length} 条关联`)
    }
    await fetchList()
  } finally {
    submitting.value = false
  }
}

function handleRemove(row: any) {
  ElMessageBox.confirm(`确定解除与「${row.requirementItemTitle}」的关联？`, '解除关联', {
    type: 'warning',
    confirmButtonText: '解除',
    cancelButtonText: '取消',
  })
    .then(async () => {
      try {
        await deleteRequirementCaseRelation(row.id)
        ElMessage.success('已解除关联')
        await fetchList()
      } catch (e: any) {
        ElMessage.error(e?.response?.data?.message || '解除关联失败')
      }
    })
    .catch(() => {})
}

watch(() => props.caseId, fetchList, { immediate: true })
</script>

<template>
  <div v-loading="loading" class="relation-panel">
    <div class="panel-toolbar">
      <el-button type="primary" size="small" :loading="submitting" @click="selectVisible = true">
        添加关联
      </el-button>
    </div>

    <el-table :data="list" border stripe size="small">
      <el-table-column prop="requirementItemId" label="需求 ID" width="90" />
      <el-table-column prop="requirementItemTitle" label="需求标题" min-width="200" show-overflow-tooltip />
      <el-table-column prop="versionName" label="所属版本" width="120" show-overflow-tooltip />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag size="small">{{ statusLabelMap[row.requirementItemStatus] || row.requirementItemStatus }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdByName" label="关联人" width="110">
        <template #default="{ row }">{{ row.createdByName || '-' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="80" fixed="right">
        <template #default="{ row }">
          <el-button type="danger" link size="small" @click="handleRemove(row)">解除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-empty v-if="!loading && list.length === 0" description="暂无关联需求" :image-size="60" />

    <RequirementItemSelectDialog
      v-model:visible="selectVisible"
      :project-id="projectId"
      multiple
      @confirm="handleConfirm"
    />
  </div>
</template>

<style scoped>
.relation-panel {
  padding: 4px 0;
}
.panel-toolbar {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 8px;
}
</style>
