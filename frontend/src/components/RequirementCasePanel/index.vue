<!--
 @author HXN
 @date 2026-08-30
 @description 需求条目-用例关联面板（需求视角：查看/添加/解除关联的用例）
-->
<script setup lang="ts">
/**
 * 需求条目-用例关联面板
 * 展示需求条目下已关联的用例列表，支持搜索选择用例批量添加、解除关联
 */
import { ref, computed, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getRequirementCaseRelations,
  addRequirementCaseRelation,
  deleteRequirementCaseRelation,
} from '@/api/relation'
import CaseSelectDialog from '@/components/CaseSelectDialog/index.vue'
import { useDict } from '@/composables/useDict'

const props = defineProps<{
  projectId: number
  itemId: number | null | undefined
}>()

const { options: caseTypeOptions } = useDict('case_type')
const caseTypeLabelMap = computed(() => {
  const map: Record<string, string> = {}
  caseTypeOptions.value.forEach((o) => {
    map[o.value] = o.label
  })
  return map
})

const loading = ref(false)
const list = ref<any[]>([])
const selectVisible = ref(false)
const submitting = ref(false)

async function fetchList() {
  if (!props.itemId) {
    list.value = []
    return
  }
  loading.value = true
  try {
    const res: any = await getRequirementCaseRelations(props.itemId)
    list.value = res.data || []
  } catch {
    list.value = []
  } finally {
    loading.value = false
  }
}

async function handleConfirm(rows: Array<{ id: number; caseType: string }>) {
  if (!props.itemId || rows.length === 0) return
  submitting.value = true
  let failed = 0
  try {
    for (const row of rows) {
      try {
        await addRequirementCaseRelation(props.itemId, { caseType: row.caseType, caseId: row.id })
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
  ElMessageBox.confirm(`确定解除与「${row.caseTitle}」的关联？`, '解除关联', {
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

function formatTime(time: string | null | undefined) {
  if (!time) return '-'
  return time.replace('T', ' ').substring(0, 16)
}

watch(() => props.itemId, fetchList, { immediate: true })
</script>

<template>
  <div v-loading="loading" class="relation-panel">
    <div class="panel-toolbar">
      <el-button type="primary" size="small" :loading="submitting" @click="selectVisible = true">
        添加关联
      </el-button>
    </div>

    <el-table :data="list" border stripe size="small">
      <el-table-column label="用例类型" width="100">
        <template #default="{ row }">
          <el-tag :type="row.caseType === 'MANUAL_CASE' ? 'primary' : 'success'" size="small">
            {{ caseTypeLabelMap[row.caseType] || row.caseType }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="caseId" label="用例 ID" width="90" />
      <el-table-column prop="caseTitle" label="用例标题" min-width="200" show-overflow-tooltip />
      <el-table-column prop="createdByName" label="关联人" width="110">
        <template #default="{ row }">{{ row.createdByName || '-' }}</template>
      </el-table-column>
      <el-table-column label="关联时间" width="150">
        <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="80" fixed="right">
        <template #default="{ row }">
          <el-button type="danger" link size="small" @click="handleRemove(row)">解除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-empty v-if="!loading && list.length === 0" description="暂无关联用例" :image-size="60" />

    <CaseSelectDialog v-model:visible="selectVisible" :project-id="projectId" multiple @confirm="handleConfirm" />
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
