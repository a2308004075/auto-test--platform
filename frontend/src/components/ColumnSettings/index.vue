<script setup lang="ts">
/**
 * 表字段调整侧滑面板
 * 右侧抽屉，勾选列显隐，锁定列不可取消，支持重置
 * 对齐原型列表页的 col-settings-panel（api-list.html 等）
 */
import { computed } from 'vue'

export interface ColumnItem {
  /** 列标识 */
  key: string
  /** 列名 */
  label: string
  /** 锁定（不可隐藏） */
  locked?: boolean
  /** 是否显示 */
  visible?: boolean
}

interface Props {
  /** 是否显示 */
  modelValue: boolean
  /** 列配置 */
  columns: ColumnItem[]
  /** 抽屉标题 */
  title?: string
}

const props = withDefaults(defineProps<Props>(), {
  title: '表字段调整',
})

const emit = defineEmits<{
  (e: 'update:modelValue', v: boolean): void
  (e: 'update:columns', v: ColumnItem[]): void
  (e: 'reset'): void
}>()

const visible = computed({
  get: () => props.modelValue,
  set: (v: boolean) => emit('update:modelValue', v),
})

function toggle(col: ColumnItem) {
  if (col.locked) return
  const next = props.columns.map((c) =>
    c.key === col.key ? { ...c, visible: !c.visible } : c,
  )
  emit('update:columns', next)
}
</script>

<template>
  <el-drawer v-model="visible" direction="rtl" size="280px" :with-header="false">
    <div class="col-settings">
      <div class="col-settings-header">
        <h3>{{ title }}</h3>
        <el-button link @click="emit('reset')">
          <span style="margin-right: 2px">↻</span> 重置
        </el-button>
      </div>
      <div class="col-settings-body">
        <label
          v-for="col in columns"
          :key="col.key"
          class="col-setting-item"
          :class="{ disabled: !!col.locked }"
        >
          <el-checkbox
            :model-value="!!col.visible"
            :disabled="!!col.locked"
            @change="toggle(col)"
          />
          <span class="col-label">{{ col.label }}</span>
        </label>
      </div>
    </div>
  </el-drawer>
</template>

<style scoped>
.col-settings {
  display: flex;
  flex-direction: column;
  height: 100%;
}
.col-settings-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid var(--el-border-color-light, #ebeef5);
}
.col-settings-header h3 {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
}
.col-settings-body {
  flex: 1;
  overflow-y: auto;
  padding: 12px 20px;
}
.col-setting-item {
  display: flex;
  align-items: center;
  padding: 8px 0;
  font-size: 13px;
  color: var(--el-text-color-primary, #303133);
}
.col-setting-item.disabled {
  color: var(--el-text-color-disabled, #c0c4cc);
}
.col-label {
  margin-left: 8px;
}
</style>
