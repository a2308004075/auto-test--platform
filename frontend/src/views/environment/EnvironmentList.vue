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
import type { FormInstance, FormRules } from 'element-plus'
import { getEnvironments, createEnvironment, deleteEnvironment, activateEnvironment, testEnvironmentConnection } from '@/api/environment'
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
const formRef = ref<FormInstance>()
const form = reactive({ name: '', description: '' })

const rules = reactive<FormRules>({
  name: [
    { required: true, message: '请输入环境名称', trigger: 'blur' },
    { max: 50, message: '环境名称长度不能超过 50 个字符', trigger: 'blur' },
  ],
})

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

function handleCreate() {
  formRef.value?.validate(async (valid) => {
    if (!valid) return
    try {
      await createEnvironment(projectId.value, { ...form, projectId: projectId.value })
      ElMessage.success('创建成功')
      modalVisible.value = false
      fetchList()
    } catch (e: any) {
      ElMessage.error(e?.response?.data?.message || '创建失败')
    }
  })
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

function handleDialogClosed() {
  formRef.value?.resetFields()
}

// ===== 激活/取消激活环境 =====
async function handleActivate(record: any) {
  const action = record.isCurrent === 1 ? '取消激活' : '激活'
  ElMessageBox.confirm(
    `确定${action}环境「${record.name}」？${record.isCurrent === 1 ? '' : '激活后其他环境将自动取消激活。'}`,
    `确认${action}`,
    { type: 'warning', confirmButtonText: '确认', cancelButtonText: '取消' }
  )
    .then(async () => {
      await activateEnvironment(projectId.value, record.id)
      ElMessage.success(`${action}成功`)
      fetchList()
    })
    .catch(() => {})
}

// ===== 测试连接 =====
const testModalVisible = ref(false)
const testLoading = ref(false)
const testResult = ref<any>(null)
const testEnvId = ref(0)
const testUrl = ref('')
const testMethod = ref('GET')

function openTestModal(record: any) {
  testEnvId.value = record.id
  testUrl.value = ''
  testMethod.value = 'GET'
  testResult.value = null
  testModalVisible.value = true
}

async function handleTestConnection() {
  if (!testUrl.value) {
    ElMessage.warning('请输入目标地址')
    return
  }
  testLoading.value = true
  testResult.value = null
  try {
    const res: any = await testEnvironmentConnection(projectId.value, testEnvId.value, {
      url: testUrl.value,
      method: testMethod.value,
    })
    testResult.value = res.data
  } catch (e: any) {
    testResult.value = { success: false, message: e?.response?.data?.message || '连接失败' }
  } finally {
    testLoading.value = false
  }
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
        <el-table-column prop="name" label="环境名称" min-width="160">
          <template #default="{ row }">
            <strong>{{ row.name }}</strong>
            <el-tag v-if="row.isCurrent === 1" type="success" size="small" style="margin-left: 8px;">当前</el-tag>
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
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="hasPermission('project:env:edit')"
              :type="row.isCurrent === 1 ? 'success' : 'primary'"
              link
              size="small"
              @click="handleActivate(row)"
            >
              {{ row.isCurrent === 1 ? '取消激活' : '激活' }}
            </el-button>
            <el-button
              v-if="hasPermission('project:env:edit')"
              type="primary"
              link
              size="small"
              @click="openTestModal(row)"
            >
              测试连接
            </el-button>
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
        <template #empty>
          <div class="empty-text">暂无环境配置，点击「新建环境」开始创建</div>
        </template>
      </el-table>
    </div>

    <!-- 新建环境弹窗 -->
    <el-dialog v-model="modalVisible" title="新建环境" width="480px" @closed="handleDialogClosed">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="环境名称" prop="name">
          <el-input v-model="form.name" placeholder="如 test / staging / prod" maxlength="50" show-word-limit />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="2"
            placeholder="环境描述"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="modalVisible = false">取消</el-button>
        <el-button type="primary" @click="handleCreate">确定</el-button>
      </template>
    </el-dialog>

    <!-- 测试连接弹窗 -->
    <el-dialog v-model="testModalVisible" title="测试目标服务连通性" width="520px">
      <el-form label-position="top">
        <el-form-item label="请求方式">
          <el-select v-model="testMethod" style="width: 120px">
            <el-option label="GET" value="GET" />
            <el-option label="POST" value="POST" />
            <el-option label="HEAD" value="HEAD" />
          </el-select>
        </el-form-item>
        <el-form-item label="目标地址">
          <el-input v-model="testUrl" placeholder="如 https://api.example.com/health" clearable />
        </el-form-item>
      </el-form>
      <div v-if="testResult" style="margin-top: 12px">
        <el-alert
          :title="testResult.success ? '连接成功' : '连接失败'"
          :type="testResult.success ? 'success' : 'error'"
          :description="testResult.message || (testResult.success ? `响应状态码：${testResult.statusCode}，耗时：${testResult.durationMs}ms` : '请检查目标地址是否可访问')"
          show-icon
          :closable="false"
        />
      </div>
      <template #footer>
        <el-button @click="testModalVisible = false">关闭</el-button>
        <el-button type="primary" :loading="testLoading" @click="handleTestConnection">测试</el-button>
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
  background: #ecf5ff;
  border: 1px solid #c6e2ff;
  border-radius: 6px;
  margin-bottom: 16px;
  font-size: 13px;
  color: rgba(0, 0, 0, 0.65);
}

.env-project-bar .project-name {
  font-weight: 600;
  color: #409eff;
}

.env-project-bar .bar-sep {
  color: rgba(0, 0, 0, 0.25);
}

.env-table-section {
  background: #fff;
  border-radius: 8px;
  border: 1px solid #f0f0f0;
}

.empty-text {
  padding: 32px 0;
  color: rgba(0, 0, 0, 0.25);
  font-size: 13px;
}
</style>
