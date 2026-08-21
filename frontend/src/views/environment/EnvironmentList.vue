<script setup lang="ts">
/**
 * 环境配置列表 - M3
 */
import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getEnvironments, createEnvironment, updateEnvironment,
  deleteEnvironment, activateEnvironment, testEnvironment,
} from '@/api/environment'

const route = useRoute()
const projectId = computed(() => Number(route.params.id))

const loading = ref(false)
const list = ref<any[]>([])
const modalVisible = ref(false)
const editingId = ref<number>(0)
const testLoading = ref('')
const form = reactive({ name: '', host: '', port: 3306, databaseName: '', username: '', password: '', configJson: '' })

async function fetchList() {
  loading.value = true
  try {
    const res: any = await getEnvironments(projectId.value)
    list.value = res.data || []
  } catch { list.value = [] } finally { loading.value = false }
}

function openCreate() {
  editingId.value = 0
  Object.assign(form, { name: '', host: '', port: 3306, databaseName: '', username: '', password: '', configJson: '' })
  modalVisible.value = true
}

function openEdit(record: any) {
  editingId.value = record.id
  Object.assign(form, {
    name: record.name || '', host: record.host || '', port: record.port || 3306,
    databaseName: record.databaseName || '', username: record.username || '',
    password: '', configJson: record.configJson || '',
  })
  modalVisible.value = true
}

async function handleSubmit() {
  if (!form.name) { ElMessage.warning('请输入环境名称'); return }
  try {
    if (editingId.value) {
      await updateEnvironment(projectId.value, editingId.value, form)
      ElMessage.success('更新成功')
    } else {
      await createEnvironment(projectId.value, { ...form, projectId: projectId.value })
      ElMessage.success('创建成功')
    }
    modalVisible.value = false; fetchList()
  } catch (e: any) { ElMessage.error(e?.response?.data?.message || '操作失败') }
}

async function handleActivate(record: any) {
  try {
    await activateEnvironment(projectId.value, record.id)
    ElMessage.success(record.isCurrent === 1 ? '已取消激活' : '已激活')
    fetchList()
  } catch (e: any) { ElMessage.error(e?.response?.data?.message || '操作失败') }
}

async function handleTest(record: any) {
  testLoading.value = record.id
  try {
    const res: any = await testEnvironment(projectId.value, record.id)
    const d = res.data
    if (d?.success) {
      ElMessage.success(`连接成功 (耗时 ${d.responseTimeMs || 0}ms)`)
    } else {
      ElMessage.error(`连接失败: ${d?.errorMessage || '未知错误'}`)
    }
  } catch (e: any) { ElMessage.error('测试失败') } finally { testLoading.value = '' }
}

function handleDelete(record: any) {
  ElMessageBox.confirm(`确定删除环境「${record.name}」？`, '确认删除', { type: 'warning' })
    .then(async () => { await deleteEnvironment(projectId.value, record.id); ElMessage.success('删除成功'); fetchList() })
    .catch(() => {})
}

onMounted(fetchList)
</script>

<template>
  <div>
    <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:16px">
      <h2>环境配置</h2>
      <el-button type="primary" @click="openCreate">新建环境</el-button>
    </div>
    <el-table v-loading="loading" :data="list" row-key="id" border style="width:100%">
      <el-table-column prop="name" label="环境名称" width="160" />
      <el-table-column label="主机" width="200">
        <template #default="{ row }">
          {{ row.host ? `${row.host}${row.port ? ':' + row.port : ''}` : '-' }}
        </template>
      </el-table-column>
      <el-table-column prop="databaseName" label="数据库" width="120" />
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.isCurrent === 1 ? 'success' : 'info'" size="small">{{ row.isCurrent === 1 ? '已激活' : '未激活' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="220">
        <template #default="{ row }">
          <el-button type="primary" link size="small" @click="handleActivate(row)">{{ row.isCurrent === 1 ? '取消激活' : '激活' }}</el-button>
          <el-button type="primary" link size="small" @click="handleTest(row)">{{ testLoading === row.id ? '测试中...' : '测试' }}</el-button>
          <el-button type="primary" link size="small" @click="openEdit(row)">编辑</el-button>
          <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="modalVisible" :title="editingId ? '编辑环境' : '新建环境'" width="560px">
      <el-form label-position="top">
        <el-form-item label="环境名称" required>
          <el-input v-model="form.name" placeholder="如: 开发环境、测试环境" />
        </el-form-item>
        <el-divider content-position="left">连接配置</el-divider>
        <el-row :gutter="12">
          <el-col :span="16">
            <el-form-item label="主机地址" required>
              <el-input v-model="form.host" placeholder="如: localhost 或 192.168.1.100" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="端口">
              <el-input-number v-model="form.port" :min="1" :max="65535" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="数据库名">
              <el-input v-model="form.databaseName" placeholder="如: test_db" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="用户名">
              <el-input v-model="form.username" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" show-password />
        </el-form-item>
        <el-form-item label="额外配置 (JSON)">
          <el-input v-model="form.configJson" type="textarea" :rows="3" placeholder='{"headers":{}}' style="font-family:monospace" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="modalVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>
