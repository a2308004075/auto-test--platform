<script setup lang="ts">
/**
 * 系统设置页 - M1
 * Tab 1: 用户管理（ADMIN）
 * Tab 2: 全局设置（预留）
 */
import { ref, reactive, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { getUsers, createUser, updateUser, deleteUser, toggleUserStatus, resetPassword, getRoles } from '@/api/user'
import { getSettings, updateSetting, type GlobalConfigItem } from '@/api/settings'

const activeTab = ref('users')

// ===== 用户管理 =====
const loading = ref(false)
const userList = ref<any[]>([])
const userKeyword = ref('')
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const modalVisible = ref(false)
const editingId = ref('')
const form = reactive({ username: '', password: '', roleId: '', displayName: '' })
const resetVisible = ref(false)
const resetUserId = ref('')
const newPassword = ref('')

// 角色列表
const roleList = ref<any[]>([])

const userColumns = [
  { title: '用户名', dataIndex: 'username', width: 140 },
  { title: '角色', dataIndex: 'roleName', width: 100 },
  { title: '状态', key: 'status', width: 80 },
  { title: '创建时间', dataIndex: 'createdAt', width: 120 },
  { title: '操作', key: 'action', width: 220 },
]

async function fetchUsers() {
  loading.value = true
  try {
    const res: any = await getUsers({ keyword: userKeyword.value, page: pagination.current, pageSize: pagination.pageSize })
    userList.value = res.data?.items || []
    pagination.total = res.data?.total || 0
  } catch { userList.value = [] } finally { loading.value = false }
}

async function fetchRoles() {
  try {
    const res: any = await getRoles()
    roleList.value = res.data || []
  } catch { roleList.value = [] }
}

function openCreateUser() {
  editingId.value = ''
  Object.assign(form, { username: '', password: '', roleId: roleList.value[0]?.id || '', displayName: '' })
  modalVisible.value = true
}

function openEditUser(record: any) {
  editingId.value = record.id
  Object.assign(form, { username: record.username, password: '', roleId: record.roleId || '', displayName: record.displayName || '' })
  modalVisible.value = true
}

async function handleSubmitUser() {
  if (!form.username) { message.warning('请输入用户名'); return }
  if (!editingId.value && !form.password) { message.warning('请输入密码'); return }
  try {
    if (editingId.value) {
      await updateUser(editingId.value, { ...form, password: form.password || undefined })
      message.success('更新成功')
    } else {
      await createUser(form)
      message.success('创建成功')
    }
    modalVisible.value = false; fetchUsers()
  } catch (e: any) { message.error(e?.response?.data?.message || '操作失败') }
}

async function handleToggleStatus(record: any) {
  try {
    await toggleUserStatus(record.id, { isActive: record.isActive === 1 ? 0 : 1 })
    message.success(record.isActive === 1 ? '已禁用' : '已启用')
    fetchUsers()
  } catch (e: any) { message.error('操作失败') }
}

function openResetPassword(record: any) {
  resetUserId.value = record.id
  newPassword.value = ''
  resetVisible.value = true
}

async function handleResetPassword() {
  if (!newPassword.value || newPassword.value.length < 6) { message.warning('密码至少 6 位'); return }
  try {
    await resetPassword(resetUserId.value, { newPassword: newPassword.value })
    message.success('密码已重置')
    resetVisible.value = false
  } catch (e: any) { message.error('重置失败') }
}

function handleDeleteUser(record: any) {
  Modal.confirm({
    title: '确认删除', content: `确定删除用户「${record.username}」？`,
    onOk: async () => { await deleteUser(record.id); message.success('删除成功'); fetchUsers() },
  })
}

function handleSearchUser() { pagination.current = 1; fetchUsers() }

// ===== 全局配置 =====
const configLoading = ref(false)
const configList = ref<GlobalConfigItem[]>([])
const configModalVisible = ref(false)
const editingConfigKey = ref('')
const configForm = reactive({ configValue: '', description: '' })

const configColumns = [
  { title: '配置键', dataIndex: 'configKey', width: 220 },
  { title: '配置值', dataIndex: 'configValue', ellipsis: true },
  { title: '说明', dataIndex: 'description', ellipsis: true },
  { title: '更新时间', dataIndex: 'updatedAt', width: 160 },
  { title: '操作', key: 'action', width: 100 },
]

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
  if (!configForm.configValue) { message.warning('配置值不能为空'); return }
  try {
    await updateSetting(editingConfigKey.value, { configValue: configForm.configValue, description: configForm.description })
    message.success('更新成功')
    configModalVisible.value = false; fetchSettings()
  } catch (e: any) { message.error(e?.response?.data?.message || '更新失败') }
}

onMounted(() => { fetchUsers(); fetchRoles(); fetchSettings() })
</script>

<template>
  <div>
    <h2 style="margin-bottom:16px">系统设置</h2>
    <a-tabs v-model:activeKey="activeTab">
      <a-tab-pane key="users" tab="用户管理">
        <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:16px">
          <a-input-search v-model:value="userKeyword" placeholder="搜索用户" style="width:240px" allow-clear @search="handleSearchUser" />
          <a-button type="primary" @click="openCreateUser">新建用户</a-button>
        </div>
        <a-table :columns="userColumns" :data-source="userList" :loading="loading" row-key="id" size="middle"
          :pagination="{ current: pagination.current, pageSize: pagination.pageSize, total: pagination.total, onChange: (p: number) => { pagination.current = p; fetchUsers() } }">
          <template #bodyCell="{ column, record }">
            <template v-if="column.dataIndex === 'roleName'">
              <a-tag :color="record.role === 'ADMIN' ? 'red' : 'blue'">{{ record.roleName || record.role }}</a-tag>
            </template>
            <template v-if="column.key === 'status'">
              <a-tag :color="record.isActive === 1 ? 'green' : 'default'">{{ record.isActive === 1 ? '启用' : '禁用' }}</a-tag>
            </template>
            <template v-if="column.dataIndex === 'createdAt'">{{ record.createdAt?.substring(0, 10) }}</template>
            <template v-if="column.key === 'action'">
              <a-space>
                <a @click="openEditUser(record)">编辑</a>
                <a @click="handleToggleStatus(record)">{{ record.isActive === 1 ? '禁用' : '启用' }}</a>
                <a @click="openResetPassword(record)">重置密码</a>
                <a style="color:#ff4d4f" @click="handleDeleteUser(record)">删除</a>
              </a-space>
            </template>
          </template>
        </a-table>
      </a-tab-pane>
      <a-tab-pane key="global" tab="全局配置">
        <a-table :columns="configColumns" :data-source="configList" :loading="configLoading" row-key="id" size="middle" :pagination="false">
          <template #bodyCell="{ column, record }">
            <template v-if="column.dataIndex === 'updatedAt'">
              {{ record.updatedAt?.replace('T', ' ').substring(0, 19) }}
            </template>
            <template v-if="column.key === 'action'">
              <a @click="openEditConfig(record)">编辑</a>
            </template>
          </template>
        </a-table>
      </a-tab-pane>
    </a-tabs>

    <!-- 新建/编辑用户弹窗 -->
    <a-modal v-model:open="modalVisible" :title="editingId ? '编辑用户' : '新建用户'" @ok="handleSubmitUser">
      <a-form layout="vertical" style="margin-top:16px">
        <a-form-item label="用户名" required><a-input v-model:value="form.username" /></a-form-item>
        <a-form-item :label="editingId ? '新密码（留空不修改）' : '密码'" :required="!editingId">
          <a-input-password v-model:value="form.password" />
        </a-form-item>
        <a-form-item label="显示名"><a-input v-model:value="form.displayName" /></a-form-item>
        <a-form-item label="角色" required>
          <a-select v-model:value="form.roleId" placeholder="请选择角色">
            <a-select-option v-for="role in roleList" :key="role.id" :value="role.id">
              {{ role.roleName }}（{{ role.roleCode }}）
            </a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 重置密码弹窗 -->
    <a-modal v-model:open="resetVisible" title="重置密码" @ok="handleResetPassword">
      <a-form-item label="新密码" style="margin-top:16px">
        <a-input-password v-model:value="newPassword" placeholder="至少 6 位" />
      </a-form-item>
    </a-modal>

    <!-- 编辑配置弹窗 -->
    <a-modal v-model:open="configModalVisible" title="编辑配置" @ok="handleSubmitConfig">
      <a-form layout="vertical" style="margin-top:16px">
        <a-form-item label="配置键"><a-input :value="editingConfigKey" disabled /></a-form-item>
        <a-form-item label="配置值" required><a-input v-model:value="configForm.configValue" /></a-form-item>
        <a-form-item label="说明"><a-input v-model:value="configForm.description" /></a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>
