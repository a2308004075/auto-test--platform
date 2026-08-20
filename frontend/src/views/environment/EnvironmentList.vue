<script setup lang="ts">
/**
 * 环境配置列表 - M3
 */
import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import {
  getEnvironments, createEnvironment, updateEnvironment,
  deleteEnvironment, activateEnvironment, testEnvironment,
} from '@/api/environment'

const route = useRoute()
const projectId = computed(() => route.params.id as string)

const loading = ref(false)
const list = ref<any[]>([])
const modalVisible = ref(false)
const editingId = ref('')
const testLoading = ref('')
const form = reactive({ name: '', host: '', port: 3306, databaseName: '', username: '', password: '', configJson: '' })

const columns = [
  { title: '环境名称', dataIndex: 'name', width: 160 },
  { title: '主机', key: 'host', width: 200 },
  { title: '数据库', dataIndex: 'databaseName', width: 120 },
  { title: '状态', key: 'status', width: 80 },
  { title: '操作', key: 'action', width: 220 },
]

async function fetchList() {
  loading.value = true
  try {
    const res: any = await getEnvironments(projectId.value)
    list.value = res.data || []
  } catch { list.value = [] } finally { loading.value = false }
}

function openCreate() {
  editingId.value = ''
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
  if (!form.name) { message.warning('请输入环境名称'); return }
  try {
    if (editingId.value) {
      await updateEnvironment(projectId.value, editingId.value, form)
      message.success('更新成功')
    } else {
      await createEnvironment(projectId.value, { ...form, projectId: projectId.value })
      message.success('创建成功')
    }
    modalVisible.value = false; fetchList()
  } catch (e: any) { message.error(e?.response?.data?.message || '操作失败') }
}

async function handleActivate(record: any) {
  try {
    await activateEnvironment(projectId.value, record.id)
    message.success(record.isCurrent ? '已取消激活' : '已激活')
    fetchList()
  } catch (e: any) { message.error(e?.response?.data?.message || '操作失败') }
}

async function handleTest(record: any) {
  testLoading.value = record.id
  try {
    const res: any = await testEnvironment(projectId.value, record.id)
    const d = res.data
    if (d?.success) {
      message.success(`连接成功 (耗时 ${d.responseTimeMs || 0}ms)`)
    } else {
      message.error(`连接失败: ${d?.errorMessage || '未知错误'}`)
    }
  } catch (e: any) { message.error('测试失败') } finally { testLoading.value = '' }
}

function handleDelete(record: any) {
  Modal.confirm({
    title: '确认删除', content: `确定删除环境「${record.name}」？`,
    onOk: async () => { await deleteEnvironment(projectId.value, record.id); message.success('删除成功'); fetchList() },
  })
}

onMounted(fetchList)
</script>

<template>
  <div>
    <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:16px">
      <h2>环境配置</h2>
      <a-button type="primary" @click="openCreate">新建环境</a-button>
    </div>
    <a-table :columns="columns" :data-source="list" :loading="loading" row-key="id" size="middle" :pagination="false">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'host'">
          {{ record.host ? `${record.host}${record.port ? ':' + record.port : ''}` : '-' }}
        </template>
        <template v-if="column.key === 'status'">
          <a-tag :color="record.isCurrent ? 'green' : 'default'">{{ record.isCurrent ? '已激活' : '未激活' }}</a-tag>
        </template>
        <template v-if="column.key === 'action'">
          <a-space>
            <a @click="handleActivate(record)">{{ record.isCurrent ? '取消激活' : '激活' }}</a>
            <a @click="handleTest(record)">{{ testLoading === record.id ? '测试中...' : '测试' }}</a>
            <a @click="openEdit(record)">编辑</a>
            <a style="color:#ff4d4f" @click="handleDelete(record)">删除</a>
          </a-space>
        </template>
      </template>
    </a-table>

    <a-modal v-model:open="modalVisible" :title="editingId ? '编辑环境' : '新建环境'" @ok="handleSubmit" :width="560">
      <a-form layout="vertical" style="margin-top:16px">
        <a-form-item label="环境名称" required>
          <a-input v-model:value="form.name" placeholder="如: 开发环境、测试环境" />
        </a-form-item>
        <a-divider orientation="left" plain>连接配置</a-divider>
        <a-row :gutter="12">
          <a-col :span="16">
            <a-form-item label="主机地址" required>
              <a-input v-model:value="form.host" placeholder="如: localhost 或 192.168.1.100" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="端口">
              <a-input-number v-model:value="form.port" :min="1" :max="65535" style="width:100%" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="12">
          <a-col :span="12">
            <a-form-item label="数据库名">
              <a-input v-model:value="form.databaseName" placeholder="如: test_db" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="用户名">
              <a-input v-model:value="form.username" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="密码"><a-input-password v-model:value="form.password" /></a-form-item>
        <a-form-item label="额外配置 (JSON)">
          <a-textarea v-model:value="form.configJson" :rows="3" placeholder='{"headers":{}}' style="font-family:monospace" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>
