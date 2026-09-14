<!--
 @author HXN
 @date 2026-08-30
 @description 需求条目选择弹窗（版本下拉 + 条目表格 + 勾选）
-->
<script setup lang="ts">
/**
 * 需求条目选择弹窗
 * 通过版本下拉切换版本，展示该版本下的需求条目列表，支持多选/单选
 * 确认后 emit 选中行数组
 */
import { ref, computed, watch } from 'vue'
import { getRequirementVersions, getRequirementItems } from '@/api/requirement'
import { useDict } from '@/composables/useDict'

const props = defineProps<{
  visible: boolean
  projectId: number
  /** 是否多选（默认多选） */
  multiple?: boolean
}>()

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
  (e: 'confirm', rows: Array<{ id: number; title: string; versionName: string }>): void
}>()

const { options: statusOptions } = useDict('requirement_item_status')
const statusLabelMap = computed(() => {
  const map: Record<string, string> = {}
  statusOptions.value.forEach((o) => {
    map[o.value] = o.label
  })
  return map
})

const dialogVisible = computed({
  get: () => props.visible,
  set: (value) => emit('update:visible', value),
})

const versions = ref<any[]>([])
const selectedVersionId = ref<number | null>(null)
const loading = ref(false)
const list = ref<any[]>([])
const selectedRows = ref<any[]>([])

watch(
  () => props.visible,
  (visible) => {
    if (visible) {
      selectedRows.value = []
      fetchVersions()
    }
  },
)

async function fetchVersions() {
  try {
    const res: any = await getRequirementVersions(props.projectId)
    versions.value = res.data || []
    selectedVersionId.value = versions.value.length > 0 ? versions.value[0].id : null
    fetchItems()
  } catch {
    versions.value = []
    list.value = []
  }
}

async function fetchItems() {
  if (!selectedVersionId.value) {
    list.value = []
    return
  }
  loading.value = true
  try {
    const res: any = await getRequirementItems(selectedVersionId.value)
    list.value = res.data || []
  } catch {
    list.value = []
  } finally {
    loading.value = false
  }
}

function handleVersionChange() {
  selectedRows.value = []
  fetchItems()
}

function handleSelectionChange(rows: any[]) {
  selectedRows.value = rows
}

function handleRowClick(row: any) {
  // 单选模式：点击行即选中并确认
  if (!props.multiple) {
    const version = versions.value.find((v) => v.id === selectedVersionId.value)
    emit('confirm', [{ id: row.id, title: row.title, versionName: version?.versionName || '' }])
    dialogVisible.value = false
  }
}

function handleConfirm() {
  if (selectedRows.value.length === 0) return
  const version = versions.value.find((v) => v.id === selectedVersionId.value)
  emit(
    'confirm',
    selectedRows.value.map((row) => ({
      id: row.id,
      title: row.title,
      versionName: version?.versionName || '',
    })),
  )
  dialogVisible.value = false
}
</script>

<template>
  <el-dialog v-model="dialogVisible" title="选择需求条目" width="640px" destroy-on-close>
    <div style="display: flex; gap: 8px; margin-bottom: 12px">
      <el-select
        v-model="selectedVersionId"
        placeholder="选择需求版本"
        style="width: 280px"
        @change="handleVersionChange"
      >
        <el-option
          v-for="v in versions"
          :key="v.id"
          :value="v.id"
          :label="`${v.versionName}（${v.itemCount || 0} 个需求）`"
        />
      </el-select>
    </div>

    <el-table
      :data="list"
      v-loading="loading"
      border
      stripe
      max-height="360"
      @selection-change="handleSelectionChange"
      @row-click="handleRowClick"
    >
      <el-table-column v-if="multiple" type="selection" width="45" />
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="title" label="需求标题" min-width="200" show-overflow-tooltip />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag size="small">{{ statusLabelMap[row.status] || row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="assignee" label="负责人" width="100">
        <template #default="{ row }">{{ row.assignee || '-' }}</template>
      </el-table-column>
    </el-table>

    <div v-if="versions.length === 0" style="padding: 32px 0; color: #909399; text-align: center; font-size: 13px">
      当前项目暂无需求版本，请先在「需求文档」页面创建版本和需求条目
    </div>

    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button v-if="multiple" type="primary" :disabled="selectedRows.length === 0" @click="handleConfirm">
        确定{{ selectedRows.length ? `（${selectedRows.length}）` : '' }}
      </el-button>
    </template>
  </el-dialog>
</template>
