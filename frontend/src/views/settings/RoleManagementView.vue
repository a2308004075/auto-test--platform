<!--
 @author HXN
 @date 2026-08-22 13:28
 @description 角色管理视图
-->
<script setup lang="ts">
/**
 * 角色管理页面（仅 ADMIN）
 * 对齐 UserManagementView.vue 的 UI 模式
 * 包含：搜索 + 表格 + 弹窗（新建/编辑/权限分配/删除）+ Excel 导入导出
 */
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import type { ElTree } from 'element-plus'
import PageHeader from '@/components/PageHeader/index.vue'
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
  assignRolePermissions,
  exportRoles,
  importRoles,
} from '@/api/role'

const { hasPermission } = usePermission()

// ===== 列表数据 =====
const loading = ref(false)
const roleList = ref<any[]>([])
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const searchKeyword = ref('')

// ===== 新建/编辑角色弹窗 =====
const formVisible = ref(false)
const formMode = ref<'create' | 'edit'>('create')
const editingId = ref<number>(0)
const roleForm = reactive({
  roleName: '',
  roleCode: '',
  description: '',
  sortOrder: 0,
})
const formErrors = reactive({
  roleName: '',
  roleCode: '',
})

// ===== 权限分配弹窗 =====
const permVisible = ref(false)
const permRoleId = ref<number>(0)
const permRoleName = ref('')
const permissionTree = ref<any[]>([])
const checkedPermissionIds = ref<number[]>([])
const treeRef = ref<InstanceType<typeof ElTree>>()
const permLoading = ref(false)
const permSaving = ref(false)
const permIsBuiltin = ref(false)

// 树配置
const treeProps = {
  label: 'permissionName',
  children: 'children',
}

// ===== 确认删除弹窗 =====
const deleteVisible = ref(false)
const deleteRoleRef = ref<any>(null)

// ===== 确认禁用/启用弹窗 =====
const toggleVisible = ref(false)
const toggleRoleRef = ref<any>(null)
const toggleAction = ref<'disable' | 'enable'>('disable')

// ===== Excel 导入 =====
const importInputRef = ref<HTMLInputElement | null>(null)
const importing = ref(false)

// ===== 获取角色列表 =====
async function fetchRoles() {
  loading.value = true
  try {
    const res: any = await getRolePage({
      keyword: searchKeyword.value || undefined,
      page: pagination.current,
      pageSize: pagination.pageSize,
    })
    roleList.value = res.data?.items || []
    pagination.total = res.data?.total || 0
  } catch {
    roleList.value = []
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.current = 1
  fetchRoles()
}

function handleReset() {
  searchKeyword.value = ''
  pagination.current = 1
  fetchRoles()
}

function handlePageChange(p: number) {
  pagination.current = p
  fetchRoles()
}

// ===== 新建/编辑角色 =====
function openCreateRole() {
  formMode.value = 'create'
  roleForm.roleName = ''
  roleForm.roleCode = ''
  roleForm.description = ''
  roleForm.sortOrder = 0
  formErrors.roleName = ''
  formErrors.roleCode = ''
  formVisible.value = true
}

async function openEditRole(row: any) {
  formMode.value = 'edit'
  editingId.value = row.id
  formErrors.roleName = ''
  formErrors.roleCode = ''
  try {
    const res: any = await getRoleDetail(row.id)
    const data = res.data
    roleForm.roleName = data.roleName || ''
    roleForm.roleCode = data.roleCode || ''
    roleForm.description = data.description || ''
    roleForm.sortOrder = data.sortOrder ?? 0
    formVisible.value = true
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '获取角色详情失败')
  }
}

function clearFormError(field: keyof typeof formErrors) {
  formErrors[field] = ''
}

async function handleSaveRole() {
  let valid = true
  formErrors.roleName = ''
  formErrors.roleCode = ''

  if (!roleForm.roleName.trim()) {
    formErrors.roleName = '请输入角色名称'
    valid = false
  }
  if (!roleForm.roleCode.trim()) {
    formErrors.roleCode = '请输入角色编码'
    valid = false
  }
  if (!valid) return

  const payload = {
    roleName: roleForm.roleName.trim(),
    roleCode: roleForm.roleCode.trim(),
    description: roleForm.description || undefined,
    sortOrder: roleForm.sortOrder ?? 0,
  }

  try {
    if (formMode.value === 'create') {
      await createRole(payload)
      ElMessage.success('角色 ' + payload.roleName + ' 创建成功')
    } else {
      await updateRole(editingId.value, payload)
      ElMessage.success('角色 ' + payload.roleName + ' 更新成功')
    }
    formVisible.value = false
    fetchRoles()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '操作失败')
  }
}

// ===== 权限分配 =====
async function openPermissionAssign(row: any) {
  permRoleId.value = row.id
  permRoleName.value = row.roleName
  permIsBuiltin.value = isBuiltinRole(row)
  permVisible.value = true
  permLoading.value = true
  try {
    const [treeRes, idsRes]: any[] = await Promise.all([
      getPermissionTree(),
      getRolePermissionIds(row.id),
    ])
    permissionTree.value = treeRes.data || []
    checkedPermissionIds.value = (idsRes.data || []).map(Number)
    // ADMIN 内置角色不允许修改权限
    if (row.roleCode?.toUpperCase() === 'ADMIN') {
      ElMessage.warning('系统内置 ADMIN 角色拥有全部权限，不可修改')
    }
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '加载权限数据失败')
  } finally {
    permLoading.value = false
  }
}

function isBuiltinRole(row: any): boolean {
  return row.roleCode?.toUpperCase() === 'ADMIN'
}

async function handleSavePermissions() {
  if (permIsBuiltin.value) {
    permVisible.value = false
    return
  }
  permSaving.value = true
  try {
    // el-tree check-strictly 模式下 getCheckedKeys 返回选中的节点 key
    const checkedKeys = treeRef.value?.getCheckedKeys(false) || []
    const halfCheckedKeys = treeRef.value?.getHalfCheckedKeys() || []
    // 合并完全选中和半选中的权限 ID
    const allIds = [...checkedKeys, ...halfCheckedKeys].map(Number)
    await assignRolePermissions(permRoleId.value, allIds)
    ElMessage.success('角色 ' + permRoleName.value + ' 权限分配成功')
    permVisible.value = false
    fetchRoles()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '权限分配失败')
  } finally {
    permSaving.value = false
  }
}

// ===== 删除角色 =====
function openDeleteRole(row: any) {
  if (isBuiltinRole(row)) {
    ElMessage.error('系统内置角色不允许删除')
    return
  }
  deleteRoleRef.value = row
  deleteVisible.value = true
}

async function handleDeleteRole() {
  if (!deleteRoleRef.value) return
  const row = deleteRoleRef.value
  try {
    await deleteRole(row.id)
    ElMessage.success('角色 ' + row.roleName + ' 已删除')
    deleteVisible.value = false
    fetchRoles()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '删除失败')
  }
}

// ===== 禁用/启用 =====
function openToggleStatus(row: any, action: 'disable' | 'enable') {
  if (isBuiltinRole(row)) {
    ElMessage.error('系统内置角色不可修改状态')
    return
  }
  toggleRoleRef.value = row
  toggleAction.value = action
  toggleVisible.value = true
}

async function handleToggleStatus() {
  if (!toggleRoleRef.value) return
  const row = toggleRoleRef.value
  const isActive = toggleAction.value === 'disable' ? 0 : 1
  try {
    await toggleRoleStatus(row.id, { isActive })
    ElMessage.success('角色 ' + row.roleName + (toggleAction.value === 'disable' ? ' 已禁用' : ' 已启用'))
    toggleVisible.value = false
    fetchRoles()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '操作失败')
  }
}

// ===== Excel 导出 =====
async function handleExport() {
  try {
    const res: any = await exportRoles()
    const blob = new Blob([res], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
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
      console.log('导入失败详情：', data.errors)
    } else {
      ElMessage.success(msg)
    }
    fetchRoles()
  } catch (err: any) {
    ElMessage.error(err?.response?.data?.message || '导入失败')
  } finally {
    importing.value = false
    // 重置 input 以便重复选择同一文件
    input.value = ''
  }
}

// ===== 辅助函数 =====
function formatDateTime(dt?: string): string {
  if (!dt) return '-'
  return dt.replace('T', ' ').substring(0, 19)
}

function getRoleTagClass(roleCode: string): string {
  return roleCode?.toUpperCase() === 'ADMIN' ? 'role-tag-admin' : 'role-tag-tester'
}

onMounted(() => {
  fetchRoles()
})
</script>

<template>
  <div class="role-mgmt-view">
    <PageHeader title="角色管理">
      <el-button v-if="hasPermission('system:role:add')" type="primary" @click="openCreateRole">+ 新建角色</el-button>
      <el-button v-if="hasPermission('system:role:import')" :loading="importing" @click="triggerImport">导入</el-button>
      <el-button v-if="hasPermission('system:role:export')" @click="handleExport">导出</el-button>
      <input ref="importInputRef" type="file" accept=".xlsx,.xls" style="display: none;" @change="handleImportFile" />
    </PageHeader>

    <!-- 搜索工具栏 -->
    <div class="rm-toolbar">
      <el-input
        v-model="searchKeyword"
        placeholder="搜索角色名称/编码"
        style="width: 220px;"
        clearable
        @keyup.enter="handleSearch"
        @clear="handleSearch"
      />
      <el-button type="primary" @click="handleSearch">查询</el-button>
      <el-button @click="handleReset">重置</el-button>
    </div>

    <!-- 角色表格 -->
    <div class="rm-card">
      <div class="rm-table-wrapper">
        <el-table v-loading="loading" :data="roleList" row-key="id" style="width: 100%;" :header-cell-style="{ background: '#fafafa' }">
          <el-table-column prop="roleName" label="角色名称" min-width="120">
            <template #default="{ row }">
              <b>{{ row.roleName }}</b>
            </template>
          </el-table-column>
          <el-table-column prop="roleCode" label="角色编码" width="140">
            <template #default="{ row }">
              <span class="rm-role-tag" :class="getRoleTagClass(row.roleCode)">{{ row.roleCode }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="description" label="描述" min-width="180" show-overflow-tooltip />
          <el-table-column prop="sortOrder" label="排序号" width="90" />
          <el-table-column label="状态" width="80">
            <template #default="{ row }">
              <span class="rm-status-tag" :class="row.isActive === 1 ? 'status-active' : 'status-disabled'">
                {{ row.isActive === 1 ? '启用' : '禁用' }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="创建时间" min-width="170">
            <template #default="{ row }">
              <span class="rm-datetime">{{ formatDateTime(row.createdAt) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="280" fixed="right">
            <template #default="{ row }">
              <div class="rm-actions">
                <template v-if="isBuiltinRole(row)">
                  <el-button type="primary" link size="small" @click="openPermissionAssign(row)">查看权限</el-button>
                </template>
                <template v-else>
                  <el-button v-if="hasPermission('system:role:edit')" type="primary" link size="small" @click="openEditRole(row)">编辑</el-button>
                  <el-button v-if="hasPermission('system:role:permission')" type="primary" link size="small" @click="openPermissionAssign(row)">分配权限</el-button>
                  <el-button v-if="row.isActive === 1 && hasPermission('system:role:edit')" type="warning" link size="small" @click="openToggleStatus(row, 'disable')">禁用</el-button>
                  <el-button v-else-if="row.isActive !== 1 && hasPermission('system:role:edit')" type="success" link size="small" @click="openToggleStatus(row, 'enable')">启用</el-button>
                  <el-button v-if="hasPermission('system:role:delete')" type="danger" link size="small" @click="openDeleteRole(row)">删除</el-button>
                </template>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </div>
      <div class="rm-card-footer">
        <el-pagination
          background
          layout="total, prev, pager, next, jumper"
          :total="pagination.total"
          :page-size="pagination.pageSize"
          :current-page="pagination.current"
          @current-change="handlePageChange"
        />
      </div>
    </div>

    <!-- 新建/编辑角色弹窗 -->
    <el-dialog v-model="formVisible" :title="formMode === 'create' ? '新建角色' : '编辑角色'" width="480px">
      <el-form label-position="top">
        <el-form-item label="角色名称" required>
          <el-input v-model="roleForm.roleName" placeholder="请输入角色名称" @input="clearFormError('roleName')" />
          <div v-if="formErrors.roleName" class="rm-error-msg">{{ formErrors.roleName }}</div>
        </el-form-item>
        <el-form-item label="角色编码" required>
          <el-input
            v-model="roleForm.roleCode"
            :disabled="formMode === 'edit'"
            placeholder="请输入角色编码（如 DEVELOPER）"
            @input="clearFormError('roleCode')"
          />
          <div v-if="formErrors.roleCode" class="rm-error-msg">{{ formErrors.roleCode }}</div>
          <div v-if="formMode === 'edit'" class="rm-form-hint">角色编码不可修改</div>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="roleForm.description" type="textarea" :rows="2" placeholder="请输入角色描述" />
        </el-form-item>
        <el-form-item label="排序号">
          <el-input-number v-model="roleForm.sortOrder" :min="0" :max="9999" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSaveRole">确定</el-button>
      </template>
    </el-dialog>

    <!-- 权限分配弹窗 -->
    <el-dialog v-model="permVisible" :title="`分配权限 - ${permRoleName}`" width="520px">
      <div v-loading="permLoading" class="rm-perm-tree-wrapper">
        <el-tree
          ref="treeRef"
          :data="permissionTree"
          :props="treeProps"
          show-checkbox
          check-strictly
          node-key="id"
          default-expand-all
          :default-checked-keys="checkedPermissionIds"
        >
          <template #default="{ data }">
            <span class="rm-tree-node">
              <span>{{ data.permissionName }}</span>
              <el-tag v-if="data.type === 'BUTTON'" size="small" type="info" class="rm-tree-tag">按钮</el-tag>
              <el-tag v-else size="small" class="rm-tree-tag">菜单</el-tag>
              <span class="rm-tree-code">{{ data.permissionCode }}</span>
            </span>
          </template>
        </el-tree>
      </div>
      <template #footer>
        <el-button @click="permVisible = false">取消</el-button>
        <el-button type="primary" :loading="permSaving" :disabled="permIsBuiltin" @click="handleSavePermissions">保存</el-button>
      </template>
    </el-dialog>

    <!-- 确认禁用/启用弹窗 -->
    <el-dialog v-model="toggleVisible" :title="toggleAction === 'disable' ? '确认禁用' : '确认启用'" width="400px">
      <div class="rm-confirm-message">
        确定{{ toggleAction === 'disable' ? '禁用' : '启用' }}角色 <b>{{ toggleRoleRef?.roleName }}</b>（{{ toggleRoleRef?.roleCode }}）吗？
        <br />
        <span class="rm-confirm-hint">{{ toggleAction === 'disable' ? '禁用后该角色下的用户将无法登录系统。' : '启用后该角色下的用户将可以正常登录系统。' }}</span>
      </div>
      <template #footer>
        <el-button @click="toggleVisible = false">取消</el-button>
        <el-button :type="toggleAction === 'disable' ? 'danger' : 'primary'" @click="handleToggleStatus">
          确认{{ toggleAction === 'disable' ? '禁用' : '启用' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 确认删除弹窗 -->
    <el-dialog v-model="deleteVisible" title="确认删除" width="400px">
      <div class="rm-confirm-message">
        确定删除角色 <b>{{ deleteRoleRef?.roleName }}</b>（{{ deleteRoleRef?.roleCode }}）吗？
        <br />
        <span class="rm-confirm-hint rm-confirm-danger">删除后该角色数据将无法恢复，请谨慎操作。</span>
      </div>
      <template #footer>
        <el-button @click="deleteVisible = false">取消</el-button>
        <el-button type="danger" @click="handleDeleteRole">确认删除</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.role-mgmt-view {
  width: 100%;
}

/* 工具栏 */
.rm-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

/* 卡片 */
.rm-card {
  background: #fff;
  border-radius: 6px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.03), 0 1px 6px -1px rgba(0, 0, 0, 0.02), 0 2px 4px rgba(0, 0, 0, 0.02);
  border: 1px solid #f0f0f0;
}
.rm-table-wrapper {
  overflow-x: auto;
}
.rm-card-footer {
  padding: 12px 20px;
  border-top: 1px solid #f0f0f0;
  display: flex;
  justify-content: flex-end;
}

/* 角色标签 */
.rm-role-tag {
  display: inline-flex;
  align-items: center;
  height: 22px;
  padding: 0 8px;
  border-radius: 4px;
  font-size: 12px;
  border: 1px solid transparent;
  white-space: nowrap;
}
.role-tag-admin {
  background: #f9f0ff;
  color: #722ed1;
  border-color: #d3adf7;
}
.role-tag-tester {
  background: #e6f7ff;
  color: #1890ff;
  border-color: #91d5ff;
}

/* 状态标签 */
.rm-status-tag {
  display: inline-flex;
  align-items: center;
  height: 22px;
  padding: 0 8px;
  border-radius: 4px;
  font-size: 12px;
  border: 1px solid transparent;
  white-space: nowrap;
}
.status-active {
  background: #f6ffed;
  color: #52c41a;
  border-color: #b7eb8f;
}
.status-disabled {
  background: #fafafa;
  color: rgba(0, 0, 0, 0.45);
  border-color: #d9d9d9;
}

/* 时间 */
.rm-datetime {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.45);
}

/* 操作按钮 */
.rm-actions {
  white-space: nowrap;
  display: flex;
  justify-content: flex-start;
  gap: 4px;
}

/* 错误提示 */
.rm-error-msg {
  color: #ff4d4f;
  font-size: 12px;
  margin-top: 4px;
}
.rm-form-hint {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.45);
  margin-top: 4px;
}

/* 权限树 */
.rm-perm-tree-wrapper {
  max-height: 420px;
  overflow-y: auto;
  padding: 8px 0;
}
.rm-tree-node {
  display: flex;
  align-items: center;
  gap: 6px;
}
.rm-tree-tag {
  margin-left: 4px;
}
.rm-tree-code {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.45);
  margin-left: 8px;
}

/* 确认消息 */
.rm-confirm-message {
  font-size: 14px;
  color: rgba(0, 0, 0, 0.88);
  line-height: 1.6;
}
.rm-confirm-hint {
  color: rgba(0, 0, 0, 0.45);
  font-size: 13px;
}
.rm-confirm-danger {
  color: #ff4d4f;
}
</style>
