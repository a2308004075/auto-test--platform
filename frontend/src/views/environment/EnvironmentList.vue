<!--
 @author HXN
 @date 2026-08-23
 @description 环境配置列表视图
-->
<script setup lang="ts">
/**
 * 环境配置列表 - 键值变量模型
 */
import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getEnvironments, createEnvironment, deleteEnvironment } from '@/api/environment'
import { useProjectStore } from '@/stores/modules/project'
import { usePermission } from '@/composables/usePermission'

const route = useRoute()
const router = useRouter()
const { hasPermission } = usePermission()
const projectStore = useProjectStore()
const projectId = computed(() => Number(route.params.id))

const loading = ref(false)
const list = ref<any[]>([])
const modalVisible = ref(false)
const form = reactive({ name: '', description: '' })

async function fetchList() {
  loading.value = true
  try {
    const res: any = await getEnvironments(projectId.value)
    list.value = res.data || []
  } catch {
    list.value = []
  } finally {
    loading.value = false
  }
}

function openCreate() {
  Object.assign(form, { name: '', description: '' })
  modalVisible.value = true
}

async function handleCreate() {
  if (!form.name) {
    ElMessage.warning('请输入环境名称')
    return
  }
  try {
    await createEnvironment(projectId.value, { ...form, projectId: projectId.value })
    ElMessage.success('创建成功')
    modalVisible.value = false
    fetchList()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '创建失败')
  }
}

function handleEdit(record: any) {
  router.push(`/project/${projectId.value}/environments/${record.id}/edit`)
}

function handleDelete(record: any) {
  ElMessageBox.confirm(
    `确定删除环境「${record.name}」？删除后该项目下相关配置将不可恢复。`,
    '确认删除',
    { type: 'warning', confirmButtonText: '确认删除', cancelButtonText: '取消' }
  )
    .then(async () => {
      await deleteEnvironment(projectId.value, record.id)
      ElMessage.success('删除成功')
      fetchList()
    })
    .catch(() => {})
}

onMounted(fetchList)
</script>

<template>
  <div>
    <!-- 页面头部 -->
    <div class="page-header">
      <h2>环境配置</h2>
      <el-button v-if="hasPermission('project:env:add')" type="primary" @click="openCreate">
        + 新建环境
      </el-button>
    </div>

    <!-- 项目上下文栏 -->
    <div class="env-project-bar">
      <span>&#x1F4CC;</span>
      <span>当前项目：<span class="project-name">{{ projectStore.currentProjectName }}</span></span>
      <span class="bar-sep">|</span>
      <span>每个项目独立管理环境配置，环境切换不影响其他项目</span>
    </div>

    <!-- 环境列表表格 -->
    <div class="env-table-section">
      <el-table v-loading="loading" :data="list" row-key="id" style="width: 100%">
        <el-table-column prop="name" label="环境名称" min-width="140">
          <template #default="{ row }">
            <strong>{{ row.name }}</strong>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="240" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.description || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="180">
          <template #default="{ row }">
            {{ row.createdAt ? row.createdAt.substring(0, 10) : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="hasPermission('project:env:edit')"
              type="primary"
              link
              size="small"
              @click="handleEdit(row)"
            >
              编辑
            </el-button>
            <el-button
              v-if="hasPermission('project:env:delete')"
              type="danger"
              link
              size="small"
              @click="handleDelete(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 新建环境弹窗 -->
    <el-dialog v-model="modalVisible" title="新建环境" width="480px">
      <el-form label-position="top">
        <el-form-item label="环境名称" required>
          <el-input v-model="form.name" placeholder="如 test / staging / prod" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="2"
            placeholder="环境描述"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="modalVisible = false">取消</el-button>
        <el-button type="primary" @click="handleCreate">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.page-header h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
}

.env-project-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 16px;
  background: #f0f5ff;
  border: 1px solid #d6e4ff;
  border-radius: 6px;
  margin-bottom: 16px;
  font-size: 13px;
  color: rgba(0, 0, 0, 0.65);
}

.env-project-bar .project-name {
  font-weight: 600;
  color: #1890ff;
}

.env-project-bar .bar-sep {
  color: rgba(0, 0, 0, 0.25);
}

.env-table-section {
  background: #fff;
  border-radius: 8px;
  border: 1px solid #f0f0f0;
}
</style>
