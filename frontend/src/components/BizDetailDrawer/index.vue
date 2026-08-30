<!--
 @author HXN
 @date 2026-08-30
 @description 业务对象详情抽屉（评论、状态变更、变更记录）
-->
<script setup lang="ts">
import { ref, computed } from 'vue'
import CommentPanel from '@/components/CommentPanel/index.vue'
import ChangeLogPanel from '@/components/ChangeLogPanel/index.vue'

export interface BizDetailDrawerProps {
  visible: boolean
  title: string
  bizType: string
  bizId: number | null | undefined
  statusFieldName: string
  fieldLabelMap?: Record<string, string>
  valueLabelMap?: Record<string, Record<string, string>>
}

const props = defineProps<BizDetailDrawerProps>()
const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
}>()

const activeTab = ref('comment')

const drawerVisible = computed({
  get: () => props.visible,
  set: (value) => emit('update:visible', value),
})
</script>

<template>
  <el-drawer
    v-model="drawerVisible"
    :title="title"
    size="520px"
    destroy-on-close
  >
    <el-tabs v-model="activeTab" type="card">
      <el-tab-pane label="评论" name="comment">
        <CommentPanel :biz-type="bizType" :biz-id="bizId" />
      </el-tab-pane>
      <el-tab-pane label="状态变更" name="status">
        <ChangeLogPanel
          :biz-type="bizType"
          :biz-id="bizId"
          :field-name="statusFieldName"
          :field-label-map="fieldLabelMap"
          :value-label-map="valueLabelMap"
        />
      </el-tab-pane>
      <el-tab-pane label="变更记录" name="changeLog">
        <ChangeLogPanel
          :biz-type="bizType"
          :biz-id="bizId"
          :field-label-map="fieldLabelMap"
          :value-label-map="valueLabelMap"
        />
      </el-tab-pane>
    </el-tabs>
  </el-drawer>
</template>
