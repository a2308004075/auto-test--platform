<script setup lang="ts">
/**
 * 系统设置页 - M1
 * Tab 1: 用户管理（ADMIN）
 * Tab 2: 全局设置（预留）
 */
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getUsers, createUser, updateUser, deleteUser, toggleUserStatus, resetPassword, getRoles } from '@/api/user'
import { getSettings, updateSetting, type GlobalConfigItem } from '@/api/settings'

const activeTab = ref('users')

// ===== 用户管理 =====
const loading = ref(false)
const userList = ref<any[]>([])
const userKeyword = ref('')
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const modalVisible = ref(false)
const editingId = ref<number>(0)
const form = reactive({ username: '', password: '', roleId: '', displayName: '' })
const resetVisible = ref(false)
const resetUserId = ref<number>(0)
const newPassword = ref('')

// 角色列表
const roleList = ref<any[]>([])

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
  editingId.value = 0
  Object.assign(form, { username: '', password: '', roleId: roleList.value[0]?.id || '', displayName: '' })
  modalVisible.value = true
}

function openEditUser(record: any) {
  editingId.value = record.id
  Object.assign(form, { username: record.username, password: '', roleId: record.roleId || '', displayName: record.displayName || '' })
  modalVisible.value = true
}

async function handleSubmitUser() {
  if (!form.username) { ElMessage.warning('请输入用户名'); return }
  if (!editingId.value && !form.password) { ElMessage.warning('请输入密码'); return }
  try {
    if (editingId.value) {
      await updateUser(editingId.value, { ...form, password: form.password || undefined })
      ElMessage.success('更新成功')
    } else {
      await createUser(form)
      ElMessage.success('创建成功')
    }
    modalVisible.value = false; fetchUsers()
  } catch (e: any) { ElMessage.error(e?.response?.data?.message || '操作失败') }
}

async function handleToggleStatus(record: any) {
  try {
    await toggleUserStatus(record.id, { isActive: record.isActive === 1 ? 0 : 1 })
    ElMessage.success(record.isActive === 1 ? '已禁用' : '已启用')
    fetchUsers()
  } catch (e: any) { ElMessage.error('操作失败') }
}

function openResetPassword(record: any) {
  resetUserId.value = record.id
  newPassword.value = ''
  resetVisible.value = true
}

async function handleResetPassword() {
  if (!newPassword.value || newPassword.value.length < 6) { ElMessage.warning('密码至少 6 位'); return }
  try {
    await resetPassword(resetUserId.value, { newPassword: newPassword.value })
    ElMessage.success('密码已重置')
    resetVisible.value = false
  } catch (e: any) { ElMessage.error('重置失败') }
}

function handleDeleteUser(record: any) {
  ElMessageBox.confirm(`确定删除用户「${record.username}」？`, '确认删除', { type: 'warning' })
    .then(async () => { await deleteUser(record.id); ElMessage.success('删除成功'); fetchUsers() })
    .catch(() => {})
}

function handleSearchUser() { pagination.current = 1; fetchUsers() }

// ===== 全局配置 =====
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

onMounted(() => { fetchUsers(); fetchRoles(); fetchSettings() })
</script>

<template>
  <div>
    <h2 style="margin-bottom:16px">系统设置</h2>
    <el-tabs v-model="activeTab">
      <el-tab-pane label="用户管理" name="users">
        <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:16px">
          <el-input v-model="userKeyword" placeholder="搜索用户" style="width:240px" clearable @keyup.enter="handleSearchUser" @clear="handleSearchUser">
            <template #append><el-button @click="handleSearchUser">搜索</el-button></template>
          </el-input>
          <el-button type="primary" @click="openCreateUser">新建用户</el-button>
        </div>
        <el-table v-loading="loading" :data="userList" row-key="id" border style="width:100%">
          <el-table-column prop="username" label="用户名" width="140" />
          <el-table-column label="角色" width="100">
            <template #default="{ row }">
              <el-tag :type="row.role === 'ADMIN' ? 'danger' : ''" size="small">{{ row.roleName || row.role }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="80">
            <template #default="{ row }">
              <el-tag :type="row.isActive === 1 ? 'success' : 'info'" size="small">{{ row.isActive === 1 ? '启用' : '禁用' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="创建时间" width="120">
            <template #default="{ row }">{{ row.createdAt?.substring(0, 10) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="220">
            <template #default="{ row }">
              <el-button type="primary" link size="small" @click="openEditUser(row)">编辑</el-button>
              <el-button type="primary" link size="small" @click="handleToggleStatus(row)">{{ row.isActive === 1 ? '禁用' : '启用' }}</el-button>
              <el-button type="warning" link size="small" @click="openResetPassword(row)">重置密码</el-button>
              <el-button type="danger" link size="small" @click="handleDeleteUser(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div style="display:flex;justify-content:flex-end;margin-top:16px">
          <el-pagination background layout="total, prev, pager, next" :total="pagination.total"
            :page-size="pagination.pageSize" :current-page="pagination.current"
            @current-change="(p: number) => { pagination.current = p; fetchUsers() }" />
        </div>
      </el-tab-pane>
      <el-tab-pane label="全局配置" name="global">
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
      </el-tab-pane>
    </el-tabs>

    <!-- 新建/编辑用户弹窗 -->
    <el-dialog v-model="modalVisible" :title="editingId ? '编辑用户' : '新建用户'" width="500px">
      <el-form label-position="top">
        <el-form-item label="用户名" required>
          <el-input v-model="form.username" />
        </el-form-item>
        <el-form-item :label="editingId ? '新密码（留空不修改）' : '密码'" :required="!editingId">
          <el-input v-model="form.password" type="password" show-password />
        </el-form-item>
        <el-form-item label="显示名">
          <el-input v-model="form.displayName" />
        </el-form-item>
        <el-form-item label="角色" required>
          <el-select v-model="form.roleId" placeholder="请选择角色" style="width:100%">
            <el-option v-for="role in roleList" :key="role.id" :value="role.id"
              :label="`${role.roleName}（${role.roleCode}）`" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="modalVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmitUser">确定</el-button>
      </template>
    </el-dialog>

    <!-- 重置密码弹窗 -->
    <el-dialog v-model="resetVisible" title="重置密码" width="400px">
      <el-form-item label="新密码">
        <el-input v-model="newPassword" type="password" show-password placeholder="至少 6 位" />
      </el-form-item>
      <template #footer>
        <el-button @click="resetVisible = false">取消</el-button>
        <el-button type="primary" @click="handleResetPassword">确定</el-button>
      </template>
    </el-dialog>

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
