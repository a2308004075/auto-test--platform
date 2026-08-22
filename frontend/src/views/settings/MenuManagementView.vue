<!--
 @author HXN
 @date 2026-08-22 13:28
 @description 菜单管理视图
-->
<script setup lang="ts">
/**
 * 菜单管理页面（仅 ADMIN）
 * 树形结构 + Popover 右键菜单（新增/编辑/删除/启停）
 * 对标 svc-manager-web Menu.vue
 */
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getMenuTree,
  addMenu,
  updateMenu,
  deleteMenu,
  toggleMenuStatus,
  type MenuTreeNode,
  type MenuCreateRequest,
} from '@/api/menu'

// ===== 树形数据 =====
const loading = ref(false)
const treeData = ref<MenuTreeNode[]>([])
const defaultExpandedKeys = ref<number[]>([])

// ===== 弹窗 =====
const dialogVisible = ref(false)
const isEdit = ref(false)
const editingId = ref<number | null>(null)
const parentMenu = ref<MenuTreeNode | null>(null)
const flatMenuList = ref<MenuTreeNode[]>([])

const form = reactive<MenuCreateRequest & { parentId: number }>({
  parentId: 0,
  name: '',
  menuType: 2,
  icon: '',
  routePath: '',
  sortNo: 0,
})

// ===== 菜单类型选项 =====
const menuTypeOptions = [
  { value: 1, label: '目录' },
  { value: 2, label: '菜单' },
  { value: 3, label: '按钮' },
]

// ===== 加载菜单树 =====
async function fetchTree() {
  loading.value = true
  try {
    const res: any = await getMenuTree()
    treeData.value = res.data || []
    defaultExpandedKeys.value = treeData.value.map((n) => n.id)
    flatMenuList.value = flattenTree(treeData.value)
  } catch {
    treeData.value = []
  } finally {
    loading.value = false
  }
}

function flattenTree(nodes: MenuTreeNode[]): MenuTreeNode[] {
  const result: MenuTreeNode[] = []
  for (const node of nodes) {
    result.push(node)
    if (node.children && node.children.length) {
      result.push(...flattenTree(node.children))
    }
  }
  return result
}

// ===== 新增子菜单 =====
function handleAppend(data: MenuTreeNode) {
  isEdit.value = false
  editingId.value = null
  parentMenu.value = data
  form.parentId = data.id
  form.name = ''
  form.menuType = 2
  form.icon = ''
  form.routePath = ''
  form.sortNo = 0
  dialogVisible.value = true
}

// ===== 新增顶级菜单 =====
function handleAddRoot() {
  isEdit.value = false
  editingId.value = null
  parentMenu.value = null
  form.parentId = 0
  form.name = ''
  form.menuType = 1
  form.icon = ''
  form.routePath = ''
  form.sortNo = 0
  dialogVisible.value = true
}

// ===== 编辑菜单 =====
function handleEdit(data: MenuTreeNode) {
  isEdit.value = true
  editingId.value = data.id
  parentMenu.value = null
  form.parentId = data.parentId
  form.name = data.name
  form.menuType = data.menuType
  form.icon = data.icon || ''
  form.routePath = data.routePath || ''
  form.sortNo = data.sortNo || 0
  dialogVisible.value = true
}

// ===== 保存 =====
async function handleSave() {
  if (!form.name.trim()) {
    ElMessage.warning('菜单名称不能为空')
    return
  }
  try {
    const req: MenuCreateRequest = {
      parentId: form.parentId,
      name: form.name,
      menuType: form.menuType,
      icon: form.icon || undefined,
      routePath: form.routePath || undefined,
      sortNo: form.sortNo || 0,
    }
    if (isEdit.value && editingId.value !== null) {
      await updateMenu(editingId.value, req)
      ElMessage.success('菜单更新成功')
    } else {
      await addMenu(req)
      ElMessage.success('菜单新增成功')
    }
    dialogVisible.value = false
    fetchTree()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '操作失败')
  }
}

// ===== 删除菜单 =====
async function handleDelete(data: MenuTreeNode) {
  try {
    await ElMessageBox.confirm(
      `确定删除菜单「${data.name}」？${data.children && data.children.length ? '（包含子菜单将一并删除）' : ''}`,
      '删除确认',
      { confirmButtonText: '确定删除', cancelButtonText: '取消', type: 'warning' },
    )
  } catch {
    return
  }
  try {
    await deleteMenu(data.id)
    ElMessage.success('菜单删除成功')
    fetchTree()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '删除失败')
  }
}

// ===== 切换启用/停用 =====
async function handleToggle(data: MenuTreeNode) {
  const action = data.isActive === 1 ? '停用' : '启用'
  try {
    await ElMessageBox.confirm(`确定${action}菜单「${data.name}」？`, '操作确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
  } catch {
    return
  }
  try {
    await toggleMenuStatus(data.id)
    ElMessage.success(`菜单已${action}`)
    fetchTree()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '操作失败')
  }
}

onMounted(() => {
  fetchTree()
})
</script>

<template>
  <div class="menu">
    <div class="menu-header">
      <el-button type="primary" @click="handleAddRoot">新增顶级菜单</el-button>
    </div>
    <div v-loading="loading" class="menu-list">
      <el-tree
        :data="treeData"
        node-key="id"
        :default-expanded-keys="defaultExpandedKeys"
        :expand-on-click-node="false"
        :props="{ label: 'name', children: 'children' }"
        highlight-current
      >
        <template #default="{ data }">
          <el-popover
            placement="right"
            trigger="hover"
            popper-class="menu-popover"
            :width="100"
          >
            <div class="button-list">
              <el-button link type="primary" @click="handleAppend(data)">新增</el-button>
              <el-button link type="primary" @click="handleEdit(data)">编辑</el-button>
              <el-button
                link
                :type="data.isActive === 1 ? 'warning' : 'success'"
                @click="handleToggle(data)"
              >
                {{ data.isActive === 1 ? '停用' : '启用' }}
              </el-button>
              <el-button link type="danger" @click="handleDelete(data)">删除</el-button>
            </div>
            <template #reference>
              <span class="tree-node-label">
                <el-tag v-if="data.menuType === 1" size="small" type="info" class="type-tag">目录</el-tag>
                <el-tag v-else-if="data.menuType === 2" size="small" type="success" class="type-tag">菜单</el-tag>
                <el-tag v-else size="small" type="warning" class="type-tag">按钮</el-tag>
                <span class="node-name">{{ data.name }}</span>
                <span v-if="data.routePath" class="route-path">{{ data.routePath }}</span>
                <el-tag v-if="data.isActive === 0" size="small" type="danger" class="status-tag">已停用</el-tag>
              </span>
            </template>
          </el-popover>
        </template>
      </el-tree>
    </div>

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      v-drag-dialog
      :title="isEdit ? '编辑菜单' : '新增菜单'"
      :close-on-click-modal="false"
    >
      <el-form :model="form" label-width="90px">
        <el-form-item label="上级菜单">
          <el-tree-select
            v-model="form.parentId"
            :data="treeData"
            :props="{ label: 'name', value: 'id', children: 'children' }"
            :render-after-expand="false"
            check-strictly
            placeholder="无（顶级菜单）"
            clearable
            style="width: 100%;"
          />
        </el-form-item>
        <el-form-item label="菜单名称" required>
          <el-input v-model="form.name" placeholder="请输入菜单名称" />
        </el-form-item>
        <el-form-item label="菜单类型" required>
          <el-radio-group v-model="form.menuType">
            <el-radio v-for="opt in menuTypeOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="图标">
          <el-input v-model="form.icon" placeholder="Element Plus 图标名（可选）" />
        </el-form-item>
        <el-form-item label="路由路径">
          <el-input v-model="form.routePath" placeholder="如 /settings/profile（可选）" />
        </el-form-item>
        <el-form-item label="排序号">
          <el-input-number v-model="form.sortNo" :min="0" :controls="false" style="width: 120px;" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.menu {
  display: flex;
  flex-direction: column;
}

.menu-header {
  margin: 24px 24px 0 24px;
}

.menu-list {
  flex: 1;
  overflow: auto;
  background-color: var(--color-white, #fff);
  margin: 8px 24px 0 24px;
  border-radius: 4px;
  padding: 8px;
  border: 1px solid var(--border-color-base, #dcdfe6);
  min-height: 400px;
}

.tree-node-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  cursor: pointer;
}

.node-name {
  margin: 0 2px;
}

.route-path {
  font-size: 12px;
  color: var(--color-text-secondary, #909399);
}

.type-tag {
  transform: scale(0.85);
  transform-origin: left center;
}

.status-tag {
  margin-left: 4px;
  transform: scale(0.85);
}
</style>

<style>
.menu-popover {
  min-width: 80px !important;
}

.menu-popover .button-list {
  display: grid;
}

.menu-popover .button-list .el-button {
  margin-left: 0;
  justify-content: flex-start;
}
</style>
