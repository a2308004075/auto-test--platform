<!--
 @author HXN
 @date 2026-08-21 15:30
 @description 批量操作栏组件
-->
<script setup lang="ts">
/**
 * 批量操作栏
 * 有选中记录时显示：选中计数 + 批量操作下拉 + 取消选择
 * 对齐原型列表页的 batch-bar（api-list.html 等）
 */
export interface BatchAction {
  /** 操作标识 */
  key: string
  /** 显示文案 */
  label: string
  /** 危险操作（标红） */
  danger?: boolean
}

interface Props {
  /** 选中记录数 */
  selectedCount: number
  /** 批量操作项 */
  actions: BatchAction[]
}

defineProps<Props>()

const emit = defineEmits<{
  (e: 'action', key: string): void
  (e: 'clear'): void
}>()

function onCommand(key: string | number | object) {
  emit('action', String(key))
}
</script>

<template>
  <div v-if="selectedCount > 0" class="batch-bar">
    <span>
      已选中 <span class="batch-count">{{ selectedCount }}</span> 条记录
    </span>
    <div class="batch-actions">
      <el-dropdown @command="onCommand">
        <el-button size="small" type="primary">
          批量操作 <span class="caret">▾</span>
        </el-button>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item
              v-for="a in actions"
              :key="a.key"
              :command="a.key"
              :style="a.danger ? { color: 'var(--el-color-danger, #f56c6c)' } : undefined"
            >
              {{ a.label }}
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
      <el-button size="small" @click="emit('clear')">取消选择</el-button>
    </div>
  </div>
</template>

<style scoped>
.batch-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  margin-bottom: 8px;
  background: var(--el-color-primary-light-9, #ecf5ff);
  border: 1px solid var(--el-color-primary-light-7, #d9ecff);
  border-radius: 4px;
  font-size: 13px;
}
.batch-count {
  font-weight: 600;
  color: var(--el-color-primary, #409eff);
}
.batch-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}
.caret {
  font-size: 10px;
  margin-left: 2px;
}
</style>
