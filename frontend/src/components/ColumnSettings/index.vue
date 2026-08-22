<!--
 @author HXN
 @date 2026-08-21 15:30
 @description 列设置组件
-->
<script setup lang="ts">
/**
 * 表字段调整弹出框
 * 参考 svc-manager-web TableHeaderField：
 *  - el-popover 点击触发按钮在按钮下方弹出
 *  - 顶部全选/反选 + 重置，el-divider 分隔
 *  - grid 网格布局，支持拖拽排序，拖拽时虚线边框高亮
 *  - locked 锁定列始终保持选中且不可隐藏
 */
import { computed, ref } from 'vue'

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
  /** 列配置 */
  columns: ColumnItem[]
  /** 触发按钮文字 */
  buttonText?: string
  /** 按钮是否禁用 */
  disabled?: boolean
  /** 弹出框宽度 */
  width?: number | string
}

const props = withDefaults(defineProps<Props>(), {
  buttonText: '表字段调整',
  disabled: false,
  width: 240,
})

const emit = defineEmits<{
  (e: 'update:columns', v: ColumnItem[]): void
  (e: 'reset'): void
}>()

// popover 显隐
const open = ref(false)

// 可勾选（非锁定）列
const flexibleCols = computed(() => props.columns.filter((c) => !c.locked))

// 全选：可勾选列全部可见（锁定列不参与）
const checkAll = computed({
  get: () =>
    flexibleCols.value.length > 0 && flexibleCols.value.every((c) => c.visible),
  set: (v: boolean) => {
    const next = props.columns.map((c) => (c.locked ? c : { ...c, visible: v }))
    emit('update:columns', next)
  },
})
// 半选：可勾选列部分可见
const isIndeterminate = computed(() => {
  const vis = flexibleCols.value.filter((c) => c.visible).length
  return vis > 0 && vis < flexibleCols.value.length
})

function toggle(col: ColumnItem) {
  if (col.locked) return
  const next = props.columns.map((c) =>
    c.key === col.key ? { ...c, visible: !c.visible } : c,
  )
  emit('update:columns', next)
}

// ===== 拖拽排序 =====
const draggingKey = ref<string | null>(null)

function onDragstart(col: ColumnItem, e: DragEvent) {
  if (!e.dataTransfer) return
  e.dataTransfer.effectAllowed = 'move'
  draggingKey.value = col.key
}
function onDragover(e: DragEvent) {
  // 允许 drop
  e.preventDefault()
  if (e.dataTransfer) e.dataTransfer.dropEffect = 'move'
}
function onDrop(target: ColumnItem, e: DragEvent) {
  e.preventDefault()
  const fromKey = draggingKey.value
  draggingKey.value = null
  if (!fromKey || fromKey === target.key) return
  const from = props.columns.findIndex((c) => c.key === fromKey)
  const to = props.columns.findIndex((c) => c.key === target.key)
  if (from === -1 || to === -1) return
  const next = [...props.columns]
  const [moved] = next.splice(from, 1)
  next.splice(to, 0, moved)
  emit('update:columns', next)
}
function onDragend() {
  draggingKey.value = null
}
</script>

<template>
  <el-popover
    v-model:visible="open"
    placement="bottom"
    :width="width"
    trigger="click"
    popper-class="col-settings-popover"
  >
    <template #reference>
      <slot name="reference">
        <el-button :disabled="disabled">{{ buttonText }}</el-button>
      </slot>
    </template>

    <div class="col-settings-toolbar">
      <el-checkbox
        v-model="checkAll"
        :indeterminate="isIndeterminate"
        :disabled="flexibleCols.length === 0"
      >全选</el-checkbox>
      <el-button link @click="emit('reset')">重置</el-button>
    </div>

    <el-divider class="col-settings-divider" />

    <div class="col-settings-list">
      <div
        v-for="col in columns"
        :key="col.key"
        class="col-setting-item"
        :class="{ 'is-locked': !!col.locked, 'is-drag': draggingKey === col.key }"
        draggable="true"
        @dragstart="onDragstart(col, $event)"
        @dragover="onDragover"
        @drop="onDrop(col, $event)"
        @dragend="onDragend"
      >
        <el-checkbox
          :model-value="!!col.visible"
          :disabled="!!col.locked"
          @change="toggle(col)"
        />
        <span class="col-label">{{ col.label }}</span>
      </div>
    </div>
  </el-popover>
</template>

<style scoped>
.col-settings-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.col-settings-divider {
  margin: 8px 0;
}
.col-settings-list {
  display: grid;
  gap: 4px;
  max-height: 320px;
  overflow-y: auto;
}
.col-setting-item {
  display: flex;
  align-items: center;
  padding: 4px;
  border: 1px dashed transparent;
  border-radius: 4px;
  font-size: 13px;
  color: var(--el-text-color-primary, #303133);
  cursor: grab;
}
.col-setting-item:active {
  cursor: grabbing;
}
.col-setting-item.is-locked {
  color: var(--el-text-color-disabled, #c0c4cc);
}
.col-setting-item.is-drag {
  border-color: var(--el-color-primary, #409eff);
}
.col-label {
  margin-left: 8px;
}
</style>
