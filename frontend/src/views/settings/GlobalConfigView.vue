<script setup lang="ts">
/**
 * 全局配置页面（仅 ADMIN）
 */
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getSettings, updateSetting, type GlobalConfigItem } from '@/api/settings'

const configLoading = ref(false)
const configList = ref<GlobalConfigItem[]>([])
const configModalVisible = ref(false)
const editingConfigKey = ref('')
const configForm = reactive({ configValue: '', description: '' })

async function fetchSettings() {
  configLoading.value = true
  try {
    const res: any = await getSettings()
    configList.value = res.data || []
  } catch { configList.value = [] } finally { configLoading.value = false }
}

function openEditConfig(record: GlobalConfigItem) {
  editingConfigKey.value = record.configKey
  Object.assign(configForm, { configValue: record.configValue, description: record.description || '' })
  configModalVisible.value = true
}

async function handleSubmitConfig() {
  if (!configForm.configValue) { ElMessage.warning('配置值不能为空'); return }
  try {
    await updateSetting(editingConfigKey.value, { configValue: configForm.configValue, description: configForm.description })
    ElMessage.success('更新成功')
    configModalVisible.value = false; fetchSettings()
  } catch (e: any) { ElMessage.error(e?.response?.data?.message || '更新失败') }
}

onMounted(() => { fetchSettings() })
</script>

<template>
  <div>
    <h2 style="margin-bottom:16px">全局配置</h2>
    <el-table v-loading="configLoading" :data="configList" row-key="id" border style="width:100%">
      <el-table-column prop="configKey" label="配置键" width="220" />
      <el-table-column prop="configValue" label="配置值" show-overflow-tooltip />
      <el-table-column prop="description" label="说明" show-overflow-tooltip />
      <el-table-column label="更新时间" width="160">
        <template #default="{ row }">{{ row.updatedAt?.replace('T', ' ').substring(0, 19) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="100">
        <template #default="{ row }">
          <el-button type="primary" link size="small" @click="openEditConfig(row)">编辑</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 编辑配置弹窗 -->
    <el-dialog v-model="configModalVisible" title="编辑配置" width="500px">
      <el-form label-position="top">
        <el-form-item label="配置键">
          <el-input :model-value="editingConfigKey" disabled />
        </el-form-item>
        <el-form-item label="配置值" required>
          <el-input v-model="configForm.configValue" />
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="configForm.description" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="configModalVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmitConfig">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>
