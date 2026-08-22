<!--
 @author HXN
 @date 2026-08-22 13:28
 @description 角色管理视图
-->
<script setup lang="ts">
/**
 * 角色管理页面（仅 ADMIN）
 * 左右分栏布局，对标 svc-manager-web Role.vue
 * 左侧角色列表 + 右侧编辑/权限树
 */
import { ref, reactive, onMounted, nextTick, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { ElTree } from 'element-plus'
import { Delete } from '@element-plus/icons-vue'
import { usePermission } from '@/composables/usePermission'
import {
  getRolePage,
  getRoleDetail,
  createRole,
  updateRole,
  deleteRole,
  toggleRoleStatus,
  getPermissionTree,
  getRolePermissionIds,
  exportRoles,
  importRoles,
} from '@/api/role'

const { hasPermission } = usePermission()

// ===== 列表数据 =====
const loading = ref(false)
const roleList = ref<any[]>([])
const searchKeyword = ref('')
const currentRole = ref<any>(null)
const isEdit = ref(false)
const permIsBuiltin = ref(false)

// ===== 表单 =====
const form = reactive({
  roleName: '',
  roleCode: '',
  description: '',
  sortOrder: 0,
})
const formErrors = reactive({ roleName: '', roleCode: '' })

// ===== 权限树 =====
const permissionTree = ref<any[]>([])
const checkedPermissionIds = ref<number[]>([])
const treeRef = ref<InstanceType<typeof ElTree>>()
const treeProps = { label: 'permissionName', children: 'children' }

// ===== Excel 导入 =====
const importInputRef = ref<HTMLInputElement | null>(null)
const importing = ref(false)

// ===== 计算属性 =====
const roleCodeDisabled = computed(() => {
  if (!isEdit.value) return true
  return !!(currentRole.value && currentRole.value.id !== -1)
})

// ===== 辅助函数 =====
function isBuiltinRole(row: any): boolean {
  return row?.roleCode?.toUpperCase() === 'ADMIN'
}

function clearFormError(field: keyof typeof formErrors) {
  formErrors[field] = ''
}

function setTreeDisabled(tree: any[], disabled: boolean) {
  tree.forEach((node) => {
    node.disabled = disabled
    if (node.children) setTreeDisabled(node.children, disabled)
  })
}

// ===== 获取角色列表 =====
async function fetchRoles() {
  loading.value = true
  try {
    const res: any = await getRolePage({
      keyword: searchKeyword.value || undefined,
      pageSize: 9999,
    })
    roleList.value = res.data?.items || []
    if (roleList.value.length && !currentRole.value) {
      await selectRole(roleList.value[0])
    }
  } catch {
    roleList.value = []
  } finally {
    loading.value = false
  }
}

// ===== 获取权限树 =====
async function fetchPermissionTree() {
  try {
    const res: any = await getPermissionTree()
    permissionTree.value = res.data || []
  } catch {
    permissionTree.value = []
  }
}

// ===== 选中角色 =====
async function selectRole(role: any) {
  if (isEdit.value) return
  if (role.id === -1) return

  loading.value = true
  try {
    currentRole.value = role
    permIsBuiltin.value = isBuiltinRole(role)

    const [detailRes, idsRes]: any[] = await Promise.all([
      getRoleDetail(role.id),
      getRolePermissionIds(role.id),
    ])

    const data = detailRes.data
    form.roleName = data.roleName || ''
    form.roleCode = data.roleCode || ''
    form.description = data.description || ''
    form.sortOrder = data.sortOrder ?? 0

    checkedPermissionIds.value = (idsRes.data || []).map(Number)

    setTreeDisabled(permissionTree.value, true)
    await nextTick()
    treeRef.value?.setCheckedKeys(checkedPermissionIds.value)
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '获取角色详情失败')
  } finally {
    loading.value = false
  }
}

// ===== 搜索 =====
function handleSearch() {
  currentRole.value = null
  fetchRoles()
}

// ===== 新增角色 =====
function addRole() {
  const tempRole = { id: -1, roleName: '新角色', roleCode: '', isActive: 1 }
  roleList.value.push(tempRole)
  currentRole.value = tempRole
  isEdit.value = true
  permIsBuiltin.value = false
  form.roleName = ''
  form.roleCode = ''
  form.description = ''
  form.sortOrder = 0
  formErrors.roleName = ''
  formErrors.roleCode = ''
  setTreeDisabled(permissionTree.value, false)
  nextTick(() => {
    treeRef.value?.setCheckedKeys([])
  })
}

// ===== 编辑 =====
function enterEdit() {
  if (!currentRole.value) return
  if (permIsBuiltin.value) {
    ElMessage.warning('系统内置 ADMIN 角色不可编辑')
    return
  }
  isEdit.value = true
  formErrors.roleName = ''
  formErrors.roleCode = ''
  setTreeDisabled(permissionTree.value, false)
}

// ===== 取消 =====
async function cancel() {
  isEdit.value = false
  formErrors.roleName = ''
  formErrors.roleCode = ''
  setTreeDisabled(permissionTree.value, true)

  if (currentRole.value?.id === -1) {
    roleList.value = roleList.value.filter((r) => r.id !== -1)
    currentRole.value = null
    if (roleList.value.length) {
      await selectRole(roleList.value[0])
    } else {
      form.roleName = ''
      form.roleCode = ''
      form.description = ''
      form.sortOrder = 0
      treeRef.value?.setCheckedKeys([])
    }
  } else if (currentRole.value) {
    await selectRole(currentRole.value)
  }
}

// ===== 保存 =====
async function save() {
  let valid = true
  formErrors.roleName = ''
  formErrors.roleCode = ''

  if (!form.roleName.trim()) {
    formErrors.roleName = '请输入角色名称'
    valid = false
  }
  if (!form.roleCode.trim()) {
    formErrors.roleCode = '请输入角色编码'
    valid = false
  }
  if (!valid) return

  const checkedKeys = treeRef.value?.getCheckedKeys(false) || []
  const halfCheckedKeys = treeRef.value?.getHalfCheckedKeys() || []
  const allIds = [...checkedKeys, ...halfCheckedKeys].map(Number)

  const payload = {
    roleName: form.roleName.trim(),
    roleCode: form.roleCode.trim(),
    description: form.description || undefined,
    sortOrder: form.sortOrder ?? 0,
    permissionIds: allIds,
  }

  loading.value = true
  try {
    if (currentRole.value.id === -1) {
      await createRole(payload)
      ElMessage.success('角色 ' + payload.roleName + ' 创建成功')
    } else {
      await updateRole(currentRole.value.id, payload)
      ElMessage.success('角色 ' + payload.roleName + ' 更新成功')
    }
    isEdit.value = false
    currentRole.value = null
    await fetchRoles()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '操作失败')
  } finally {
    loading.value = false
  }
}

// ===== 删除角色 =====
async function deleteRoleItem(item: any) {
  if (isBuiltinRole(item)) {
    ElMessage.error('系统内置角色不允许删除')
    return
  }
  try {
    await ElMessageBox.confirm(
      `确定删除角色「${item.roleName}」？删除后该角色数据将无法恢复。`,
      '删除确认',
      { confirmButtonText: '确定删除', cancelButtonText: '取消', type: 'warning' },
    )
  } catch {
    return
  }
  try {
    await deleteRole(item.id)
    ElMessage.success('角色 ' + item.roleName + ' 已删除')
    if (currentRole.value === item) {
      currentRole.value = null
    }
    await fetchRoles()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '删除失败')
  }
}

// ===== 禁用/启用 =====
async function toggleStatus() {
  if (!currentRole.value || permIsBuiltin.value) return
  const action = currentRole.value.isActive === 1 ? '禁用' : '启用'
  try {
    await ElMessageBox.confirm(
      `确定${action}角色「${currentRole.value.roleName}」？`,
      '操作确认',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' },
    )
  } catch {
    return
  }
  try {
    const isActive = currentRole.value.isActive === 1 ? 0 : 1
    await toggleRoleStatus(currentRole.value.id, { isActive })
    ElMessage.success('角色 ' + currentRole.value.roleName + ' 已' + action)
    await fetchRoles()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '操作失败')
  }
}

// ===== Excel 导出 =====
async function handleExport() {
  try {
    const res: any = await exportRoles()
    const blob = new Blob([res], {
      type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = '角色列表.xlsx'
    link.click()
    window.URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '导出失败')
  }
}

// ===== Excel 导入 =====
function triggerImport() {
  importInputRef.value?.click()
}

async function handleImportFile(e: Event) {
  const input = e.target as HTMLInputElement
  if (!input.files || input.files.length === 0) return
  importing.value = true
  try {
    const res: any = await importRoles(input.files[0])
    const data = res.data
    const msg = `导入完成：成功 ${data.successCount} 条，失败 ${data.failCount} 条`
    if (data.failCount > 0) {
      ElMessage.warning(msg)
    } else {
      ElMessage.success(msg)
    }
    await fetchRoles()
  } catch (err: any) {
    ElMessage.error(err?.response?.data?.message || '导入失败')
  } finally {
    importing.value = false
    input.value = ''
  }
}

// ===== 初始化 =====
onMounted(async () => {
  loading.value = true
  try {
    await Promise.allSettled([fetchRoles(), fetchPermissionTree()])
    if (roleList.value.length && !currentRole.value) {
      await selectRole(roleList.value[0])
    }
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div v-loading="loading" class="role">
    <!-- 左面板：角色列表 -->
    <div class="left">
      <div class="header">
        <div class="title">角色列表</div>
        <div class="header-actions">
          <el-button
            v-if="hasPermission('system:role:add')"
            type="primary"
            size="small"
            :disabled="isEdit"
            @click="addRole"
          >新增</el-button>
          <el-button
            v-if="hasPermission('system:role:import')"
            size="small"
            :loading="importing"
            :disabled="isEdit"
            @click="triggerImport"
          >导入</el-button>
          <el-button
            v-if="hasPermission('system:role:export')"
            size="small"
            :disabled="isEdit"
            @click="handleExport"
          >导出</el-button>
        </div>
      </div>
      <div class="search-bar">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索角色名称/编码"
          clearable
          size="small"
          style="flex: 1;"
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        />
        <el-button type="primary" size="small" @click="handleSearch">查询</el-button>
      </div>
      <div class="list">
        <div
          v-for="item of roleList"
          :key="item.id"
          class="item"
          :class="{ disabled: isEdit, 'is-active': currentRole === item }"
          @click="selectRole(item)"
        >
          <span class="role-name">{{ item.roleName }}</span>
          <el-tag v-if="item.isActive === 0" size="small" type="danger">停用</el-tag>
          <el-button
            v-if="hasPermission('system:role:delete')"
            text
            :icon="Delete"
            :disabled="isEdit || isBuiltinRole(item)"
            @click.stop="deleteRoleItem(item)"
          />
        </div>
        <div v-if="!roleList.length" class="empty-hint">暂无角色数据</div>
      </div>
    </div>

    <!-- 右面板：角色详情 -->
    <div class="right">
      <div class="header">
        <div class="title">角色详情</div>
      </div>
      <el-form :model="form" class="form" label-width="auto">
        <el-form-item label="角色名称：">
          <el-input
            v-model="form.roleName"
            :disabled="!isEdit"
            placeholder="请输入角色名称"
            class="mxw-300"
            @input="clearFormError('roleName')"
          />
          <div v-if="formErrors.roleName" class="error-msg">{{ formErrors.roleName }}</div>
        </el-form-item>
        <el-form-item label="角色编码：">
          <el-input
            v-model="form.roleCode"
            :disabled="roleCodeDisabled"
            placeholder="请输入角色编码（如 DEVELOPER）"
            class="mxw-300"
            @input="clearFormError('roleCode')"
          />
          <div v-if="formErrors.roleCode" class="error-msg">{{ formErrors.roleCode }}</div>
          <div v-if="isEdit && currentRole && currentRole.id !== -1" class="form-hint">角色编码不可修改</div>
        </el-form-item>
        <el-form-item label="描述：">
          <el-input
            v-model="form.description"
            :disabled="!isEdit"
            type="textarea"
            :rows="2"
            placeholder="请输入角色描述"
            class="mxw-300"
          />
        </el-form-item>
        <el-form-item label="权限分配："></el-form-item>
      </el-form>
      <el-tree
        ref="treeRef"
        :data="permissionTree"
        :props="treeProps"
        node-key="id"
        show-checkbox
        check-strictly
        default-expand-all
        class="tree"
      />
      <div class="button-list">
        <template v-if="isEdit">
          <el-button type="primary" @click="save">保存</el-button>
          <el-button @click="cancel">取消</el-button>
        </template>
        <template v-else>
          <el-button
            v-if="hasPermission('system:role:edit')"
            type="primary"
            :disabled="!currentRole || permIsBuiltin"
            @click="enterEdit"
          >编辑</el-button>
          <el-button
            v-if="currentRole && !permIsBuiltin && hasPermission('system:role:edit')"
            :type="currentRole.isActive === 1 ? 'warning' : 'success'"
            @click="toggleStatus"
          >
            {{ currentRole.isActive === 1 ? '禁用' : '启用' }}
          </el-button>
        </template>
      </div>
    </div>

    <input
      ref="importInputRef"
      type="file"
      accept=".xlsx,.xls"
      style="display: none;"
      @change="handleImportFile"
    />
  </div>
</template>

<style scoped>
.mxw-300 {
  max-width: 300px;
}

.role {
  display: flex;
  margin: 24px;
  margin-bottom: 0;
  background-color: var(--color-white, #fff);
  height: calc(100vh - 120px);
  overflow: hidden;
}

.header {
  height: 56px;
  display: flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: space-between;
  padding-inline: 8px;
  background-color: var(--background-color-base, #f5f7fa);
  border-bottom: 1px solid var(--border-color-base, #dcdfe6);
}

.title {
  font-weight: bold;
}

.header-actions {
  display: flex;
  gap: 8px;
  align-items: center;
}

.left {
  display: flex;
  flex-direction: column;
  flex-basis: 320px;
  flex-shrink: 0;
  overflow: hidden;
  border: 1px solid var(--border-color-base, #dcdfe6);
}

.search-bar {
  display: flex;
  gap: 8px;
  padding: 8px;
  border-bottom: 1px solid var(--border-color-base, #dcdfe6);
}

.list {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 8px;
  overflow: auto;
  flex: 1;
}

.list .item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 12px;
  line-height: 36px;
  border-radius: 4px;
  cursor: pointer;
  transition: background-color 0.2s;
}

.list .item.disabled {
  cursor: not-allowed;
}

.list .item.is-active,
.list .item:hover {
  color: var(--color-primary, #409eff);
  font-weight: bold;
  background-color: var(--background-color-base, #f5f7fa);
}

.role-name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.empty-hint {
  text-align: center;
  padding: 24px;
  color: var(--color-text-secondary, #909399);
  font-size: 13px;
}

.right {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding-bottom: 16px;
  overflow: hidden;
  border: 1px solid var(--border-color-base, #dcdfe6);
  margin-left: -1px;
}

.right .form {
  margin-top: 16px;
  padding-inline: 8px;
}

.right .tree {
  flex: 1;
  padding: 8px;
  margin-inline: 4px;
  border: 1px solid var(--border-color-base, #dcdfe6);
  overflow: auto;
}

.right .button-list {
  margin-top: 16px;
  text-align: center;
}

.error-msg {
  color: #ff4d4f;
  font-size: 12px;
  margin-top: 4px;
}

.form-hint {
  font-size: 12px;
  color: var(--color-text-secondary, #909399);
  margin-top: 4px;
}
</style>
