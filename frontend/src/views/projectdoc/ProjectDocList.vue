<!--
 @author HXN
 @date 2026-08-30
 @description 项目文档列表视图
-->
<script setup lang="ts">
/**
 * 项目文档列表 - M13
 * 左侧分组面板 + 右侧高级搜索 + 表格 + 上传/编辑/替换/下载/删除
 * 布局对齐 ActionList.vue
 */
import { ref, reactive, onMounted, onBeforeUnmount, computed } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getProjectDocs, uploadProjectDoc, updateProjectDoc, replaceProjectDoc,
  deleteProjectDoc, downloadProjectDoc,
  getDocGroups, createDocGroup, updateDocGroup, deleteDocGroup,
} from '@/api/projectdoc'
import PageHeader from '@/components/PageHeader/index.vue'
import ProSearchCard from '@/components/ProSearchCard/index.vue'
import ProPagination from '@/components/ProPagination/index.vue'
import { usePermission } from '@/composables/usePermission'

const route = useRoute()
const { hasPermission } = usePermission()
const projectId = computed(() => Number(route.params.id))

// ===== 列表数据 =====
const loading = ref(false)
const list = ref<any[]>([])
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })

// ===== 搜索条件 =====
const search = reactive({ name: '' })

// ===== 分组 =====
const groups = ref<any[]>([])
const activeGroupId = ref<number>(0) // 0 = 全部
const filterText = ref('')
const groupMap = computed<Record<number, any>>(() => {
  const m: Record<number, any> = {}
  groups.value.forEach((g) => { m[g.id] = g })
  return m
})
// 分组树：全部(虚拟) + 系统分组(未分组等，排除全部) + 用户分组按 parentId 建树
const groupTree = computed(() => {
  const userGrps = groups.value.filter((g) => g.isSystem !== 1)
  const buildTree = (parentId: number | null): any[] =>
    userGrps
      .filter((g) => (g.parentId ?? null) === parentId)
      .map((g) => ({ ...g, children: buildTree(g.id) }))
  const systemGroups = groups.value
    .filter((g) => g.isSystem === 1 && g.name !== '全部')
    .map((g) => ({ ...g, children: [] }))
  return [
    { id: 0, name: '全部', isSystem: 1, docCount: pagination.total, children: [] },
    ...systemGroups,
    ...buildTree(null),
  ]
})
// 上传/编辑弹窗的分组下拉：未分组(id=0) + 用户分组树
const groupSelectOptions = computed(() => {
  const userGrps = groups.value.filter((g) => g.isSystem !== 1)
  const buildTree = (parentId: number | null): any[] =>
    userGrps
      .filter((g) => (g.parentId ?? null) === parentId)
      .map((g) => ({ id: g.id, name: g.name, children: buildTree(g.id) }))
  return [{ id: 0, name: '未分组', children: [] }, ...buildTree(null)]
})

const treeRef = ref()
function filterNode(value: string, data: any) {
  if (!value) return true
  return (data.name || '').includes(value)
}

function onGroupNodeClick(data: any) {
  selectGroup(data.id)
}

async function fetchGroups() {
  try {
    const res: any = await getDocGroups(projectId.value)
    groups.value = res.data || []
  } catch { groups.value = [] }
}

function selectGroup(id: number) {
  activeGroupId.value = id === activeGroupId.value ? 0 : id
  pagination.current = 1
  fetchList()
}

async function fetchList() {
  loading.value = true
  try {
    const res: any = await getProjectDocs(projectId.value, {
      keyword: search.name || undefined,
      groupId: activeGroupId.value || undefined,
      page: pagination.current,
      pageSize: pagination.pageSize,
    })
    list.value = res.data?.items || []
    pagination.total = res.data?.total || 0
  } catch {
    list.value = []
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.current = 1
  fetchList()
}

function handleReset() {
  search.name = ''
  handleSearch()
}

// ===== 文件大小/类型格式化 =====
function formatSize(size?: number): string {
  if (size == null) return '--'
  if (size < 1024) return `${size} B`
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`
  if (size < 1024 * 1024 * 1024) return `${(size / 1024 / 1024).toFixed(1)} MB`
  return `${(size / 1024 / 1024 / 1024).toFixed(2)} GB`
}

function fileExt(fileName?: string): string {
  if (!fileName) return '--'
  const idx = fileName.lastIndexOf('.')
  if (idx < 0 || idx === fileName.length - 1) return '--'
  return fileName.substring(idx + 1).toUpperCase()
}

// ===== 上传 =====
const uploadVisible = ref(false)
const uploading = ref(false)
const uploadForm = reactive({ groupId: 0, docName: '', description: '' })
const uploadFile = ref<File | null>(null)
const uploadRef = ref()

function openUpload() {
  Object.assign(uploadForm, {
    groupId: activeGroupId.value > 0 ? activeGroupId.value : 0,
    docName: '',
    description: '',
  })
  uploadFile.value = null
  uploadVisible.value = true
}

function onUploadFileChange(file: any, fileList: any[]) {
  if (fileList.length > 1) fileList.splice(0, 1)
  uploadFile.value = file.raw
  if (!uploadForm.docName) uploadForm.docName = file.name
}

function onUploadFileRemove() {
  uploadFile.value = null
}

async function handleUploadSubmit() {
  if (!uploadFile.value) { ElMessage.warning('请选择要上传的文件'); return }
  uploading.value = true
  try {
    const form = new FormData()
    form.append('file', uploadFile.value)
    form.append('groupId', String(uploadForm.groupId ?? 0))
    form.append('docName', uploadForm.docName || uploadFile.value.name)
    if (uploadForm.description) form.append('description', uploadForm.description)
    await uploadProjectDoc(projectId.value, form)
    ElMessage.success('上传成功')
    uploadVisible.value = false
    fetchGroups()
    fetchList()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '上传失败')
  } finally {
    uploading.value = false
  }
}

// ===== 编辑（重命名/描述/移动分组） =====
const editVisible = ref(false)
const editForm = reactive({ docName: '', description: '', groupId: 0 })
const editingDocId = ref(0)

function handleEdit(row: any) {
  editingDocId.value = row.id
  Object.assign(editForm, {
    docName: row.docName,
    description: row.description || '',
    groupId: row.groupId ?? 0,
  })
  editVisible.value = true
}

async function handleEditSubmit() {
  if (!editForm.docName) { ElMessage.warning('请输入文档名称'); return }
  try {
    await updateProjectDoc(editingDocId.value, {
      docName: editForm.docName,
      description: editForm.description,
      groupId: editForm.groupId,
    })
    ElMessage.success('更新成功')
    editVisible.value = false
    fetchGroups()
    fetchList()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '更新失败')
  }
}

// ===== 替换文件 =====
const replaceInputRef = ref<HTMLInputElement>()
const replacingDoc = ref<any>(null)

function handleReplace(row: any) {
  replacingDoc.value = row
  replaceInputRef.value?.click()
}

async function onReplaceFileChange(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = '' // 允许重复选择同一文件
  if (!file || !replacingDoc.value) return
  const target = replacingDoc.value
  replacingDoc.value = null
  try {
    const form = new FormData()
    form.append('file', file)
    await replaceProjectDoc(target.id, form)
    ElMessage.success(`已替换「${target.docName}」`)
    fetchList()
  } catch (err: any) {
    ElMessage.error(err?.response?.data?.message || '替换失败')
  }
}

// ===== 下载 =====
async function handleDownload(row: any) {
  try {
    const blob = await downloadProjectDoc(row.id)
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    // 下载名 = docName（无扩展名时补 fileName 的扩展名）
    let name = row.docName || row.fileName
    const docExt = name.lastIndexOf('.') > 0 ? name.substring(name.lastIndexOf('.')) : ''
    const fileExt = row.fileName && row.fileName.lastIndexOf('.') > 0
      ? row.fileName.substring(row.fileName.lastIndexOf('.')) : ''
    if (!docExt && fileExt) name += fileExt
    a.download = name
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    URL.revokeObjectURL(url)
  } catch (e: any) {
    ElMessage.error(e?.message || '下载失败')
  }
}

// ===== 删除 =====
function handleDelete(row: any) {
  ElMessageBox.confirm(
    `确定删除文档「${row.docName}」？删除后磁盘文件将一并清除，无法恢复。`,
    '删除文档',
    { confirmButtonText: '确认删除', cancelButtonText: '取消', type: 'warning' },
  )
    .then(async () => {
      await deleteProjectDoc(row.id)
      ElMessage.success(`已删除文档「${row.docName}」`)
      fetchGroups()
      fetchList()
    })
    .catch(() => {})
}

// ===== 右键菜单 =====
const contextMenuVisible = ref(false)
const contextMenuPos = reactive({ x: 0, y: 0 })
const contextGroup = ref<any>(null)

function handleNodeContextmenu(e: MouseEvent, data: any) {
  e.preventDefault()
  e.stopPropagation()
  // 系统分组不可操作，不弹菜单
  if (data.isSystem === 1) return
  contextGroup.value = data
  contextMenuPos.x = e.clientX
  contextMenuPos.y = e.clientY
  contextMenuVisible.value = true
}

function handleBlankContextmenu(e: MouseEvent) {
  e.preventDefault()
  contextGroup.value = null
  contextMenuPos.x = e.clientX
  contextMenuPos.y = e.clientY
  contextMenuVisible.value = true
}

function closeContextMenu() {
  contextMenuVisible.value = false
  contextGroup.value = null
}

function contextCreateGroup() {
  openCreateGroup()
  closeContextMenu()
}

function contextCreateChild() {
  if (contextGroup.value) openCreateGroup(contextGroup.value.id)
  closeContextMenu()
}

function contextEdit() {
  if (contextGroup.value) openEditGroup(contextGroup.value)
  closeContextMenu()
}

function contextDelete() {
  if (contextGroup.value) handleDeleteGroup(contextGroup.value)
  closeContextMenu()
}

// ===== 分组 CRUD =====
const groupModalVisible = ref(false)
const editingGroupId = ref<number>(0)
const groupForm = reactive({ name: '', description: '', parentId: null as number | null })

function openCreateGroup(parentId?: number | null) {
  editingGroupId.value = 0
  Object.assign(groupForm, { name: '', description: '', parentId: parentId ?? null })
  groupModalVisible.value = true
}
function openEditGroup(g: any) {
  if (g.isSystem === 1) { ElMessage.info('系统分组不可编辑'); return }
  editingGroupId.value = g.id
  Object.assign(groupForm, { name: g.name, description: g.description || '', parentId: g.parentId ?? null })
  groupModalVisible.value = true
}
async function handleGroupSubmit() {
  if (!groupForm.name) { ElMessage.warning('请输入分组名称'); return }
  try {
    if (editingGroupId.value) {
      await updateDocGroup(projectId.value, editingGroupId.value, groupForm)
      ElMessage.success('更新成功')
    } else {
      await createDocGroup(projectId.value, groupForm)
      ElMessage.success('创建成功')
    }
    groupModalVisible.value = false
    fetchGroups()
  } catch (e: any) { ElMessage.error(e?.response?.data?.message || '操作失败') }
}
function handleDeleteGroup(g: any) {
  if (g.isSystem === 1) { ElMessage.info('系统分组不可删除'); return }
  ElMessageBox.confirm(
    `确定删除分组「${g.name}」？其子分组将一并删除，分组内文档归入未分组。`,
    '确认删除',
    { type: 'warning' },
  )
    .then(async () => { await deleteDocGroup(projectId.value, g.id); ElMessage.success('删除成功'); fetchGroups() })
    .catch(() => {})
}

// ===== 生命周期 =====
function onDocClick() { closeContextMenu() }
onMounted(() => {
  fetchGroups()
  fetchList()
  document.addEventListener('click', onDocClick)
})
onBeforeUnmount(() => {
  document.removeEventListener('click', onDocClick)
})
</script>

<template>
  <div>
    <PageHeader title="项目文档">
      <el-button
        v-if="hasPermission('project:doc:upload')"
        type="primary"
        @click="openUpload"
      >
        + 上传文档
      </el-button>
    </PageHeader>

    <div class="doc-layout">
      <!-- 左侧分组 -->
      <div class="group-panel" @contextmenu="handleBlankContextmenu">
        <div class="group-head">
          <span class="group-title">分组</span>
        </div>
        <div class="tree-search">
          <el-input v-model="filterText" size="small" placeholder="搜索分组..." clearable @input="(v: string) => treeRef?.filter(v)" />
        </div>
        <div class="group-tree">
          <el-tree
            ref="treeRef"
            :data="groupTree"
            node-key="id"
            :props="{ label: 'name', children: 'children' }"
            :default-expand-all="true"
            :expand-on-click-node="false"
            :filter-node-method="filterNode"
            @node-click="onGroupNodeClick"
          >
            <template #default="{ data }">
              <div
                :class="['group-tree-node', { active: activeGroupId === data.id }]"
                @contextmenu.stop="handleNodeContextmenu($event, data)"
              >
                <span class="group-name">{{ data.name }}</span>
                <span class="group-count">{{ data.docCount ?? 0 }}</span>
                <span v-if="data.isSystem === 1" class="group-lock" title="系统默认分组">🔒</span>
              </div>
            </template>
          </el-tree>
        </div>
      </div>

      <!-- 右侧内容 -->
      <div class="doc-content">
        <ProSearchCard :loading="loading" @search="handleSearch" @reset="handleReset">
          <div class="pro-search-field">
            <span class="pro-search-label">文档名</span>
            <el-input
              v-model="search.name"
              placeholder="输入文档名或文件名"
              clearable
              style="width: 220px"
              @keyup.enter="handleSearch"
            />
          </div>
        </ProSearchCard>

        <el-table
          :data="list"
          v-loading="loading"
          border
          stripe
          style="width: 100%"
        >
          <el-table-column
            prop="docName"
            label="文档名"
            min-width="200"
            show-overflow-tooltip
          >
            <template #default="{ row }">
              <span class="doc-name" :title="row.fileName">{{ row.docName }}</span>
            </template>
          </el-table-column>
          <el-table-column
            label="分组"
            width="140"
            show-overflow-tooltip
          >
            <template #default="{ row }">
              <span v-if="row.groupId">{{ groupMap[row.groupId]?.name || '未知分组' }}</span>
              <span v-else style="color: #c0c4cc">未分组</span>
            </template>
          </el-table-column>
          <el-table-column
            label="大小"
            width="100"
            align="right"
          >
            <template #default="{ row }">{{ formatSize(row.fileSize) }}</template>
          </el-table-column>
          <el-table-column
            label="类型"
            width="90"
            align="center"
          >
            <template #default="{ row }">{{ fileExt(row.fileName) }}</template>
          </el-table-column>
          <el-table-column
            prop="description"
            label="描述"
            min-width="160"
            show-overflow-tooltip
          >
            <template #default="{ row }">
              <span v-if="row.description">{{ row.description }}</span>
              <span v-else style="color: #c0c4cc">--</span>
            </template>
          </el-table-column>
          <el-table-column
            label="上传时间"
            width="160"
          >
            <template #default="{ row }">{{
              row.createdAt?.substring(0, 16)?.replace('T', ' ')
            }}</template>
          </el-table-column>
          <el-table-column
            label="操作"
            width="240"
            fixed="right"
          >
            <template #default="{ row }">
              <el-button
                type="primary"
                link
                size="small"
                @click="handleDownload(row)"
              >
                下载
              </el-button>
              <el-button
                v-if="hasPermission('project:doc:edit')"
                type="primary"
                link
                size="small"
                @click="handleEdit(row)"
              >
                编辑
              </el-button>
              <el-button
                v-if="hasPermission('project:doc:edit')"
                type="primary"
                link
                size="small"
                @click="handleReplace(row)"
              >
                替换
              </el-button>
              <el-button
                v-if="hasPermission('project:doc:delete')"
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
            <div style="padding: 48px 20px; color: #909399">
              <div>暂无文档，点击右上角「上传文档」添加</div>
            </div>
          </template>
        </el-table>

        <ProPagination
          v-model:current-page="pagination.current"
          v-model:page-size="pagination.pageSize"
          :total="pagination.total"
          @change="(p: number) => { pagination.current = p; fetchList() }"
        />
      </div>
    </div>

    <!-- 上传文档弹窗 -->
    <el-dialog v-model="uploadVisible" title="上传文档" width="520px" :close-on-click-modal="false">
      <el-form label-position="top">
        <el-form-item label="文件" required>
          <el-upload
            ref="uploadRef"
            drag
            :auto-upload="false"
            :limit="1"
            :on-change="onUploadFileChange"
            :on-remove="onUploadFileRemove"
            class="doc-upload"
          >
            <div class="el-upload__text">
              将文件拖到此处，或<em>点击选择文件</em>
            </div>
            <template #tip>
              <div class="el-upload__tip">文件类型与大小不限</div>
            </template>
          </el-upload>
        </el-form-item>
        <el-form-item label="文档名">
          <el-input v-model="uploadForm.docName" placeholder="默认取文件名" maxlength="200" />
        </el-form-item>
        <el-form-item label="分组">
          <el-tree-select
            v-model="uploadForm.groupId"
            :data="groupSelectOptions"
            node-key="id"
            check-strictly
            :props="{ label: 'name', children: 'children' }"
            placeholder="选择分组"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="uploadForm.description" type="textarea" :rows="2" maxlength="500" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="uploadVisible = false">取消</el-button>
        <el-button type="primary" :loading="uploading" @click="handleUploadSubmit">上传</el-button>
      </template>
    </el-dialog>

    <!-- 编辑文档弹窗 -->
    <el-dialog v-model="editVisible" title="编辑文档" width="460px">
      <el-form label-position="top">
        <el-form-item label="文档名" required>
          <el-input v-model="editForm.docName" maxlength="200" />
        </el-form-item>
        <el-form-item label="分组">
          <el-tree-select
            v-model="editForm.groupId"
            :data="groupSelectOptions"
            node-key="id"
            check-strictly
            :props="{ label: 'name', children: 'children' }"
            placeholder="选择分组"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="editForm.description" type="textarea" :rows="2" maxlength="500" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" @click="handleEditSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 替换文件隐藏 input -->
    <input
      ref="replaceInputRef"
      type="file"
      style="display: none"
      @change="onReplaceFileChange"
    />

    <!-- 分组新建/编辑弹窗 -->
    <el-dialog v-model="groupModalVisible" :title="editingGroupId ? '编辑分组' : '新建分组'" width="460px">
      <el-form label-position="top">
        <el-form-item label="分组名称" required>
          <el-input v-model="groupForm.name" placeholder="如：设计文档" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="groupForm.description" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="groupModalVisible = false">取消</el-button>
        <el-button type="primary" @click="handleGroupSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 右键上下文菜单 -->
    <Teleport to="body">
      <div
        v-if="contextMenuVisible"
        class="context-menu"
        :style="{ left: contextMenuPos.x + 'px', top: contextMenuPos.y + 'px' }"
        @click.stop
      >
        <!-- 空白区域右键：仅显示"新建分组" -->
        <template v-if="!contextGroup">
          <div
            v-if="hasPermission('project:doc:group')"
            class="context-menu-item"
            @click="contextCreateGroup"
          >
            新建分组
          </div>
        </template>
        <!-- 用户分组右键 -->
        <template v-else>
          <div
            v-if="hasPermission('project:doc:group')"
            class="context-menu-item"
            @click="contextCreateChild"
          >
            新建子分组
          </div>
          <div v-if="hasPermission('project:doc:group')" class="context-menu-divider" />
          <div
            v-if="hasPermission('project:doc:group')"
            class="context-menu-item"
            @click="contextEdit"
          >
            编辑
          </div>
          <div
            v-if="hasPermission('project:doc:group')"
            class="context-menu-item danger"
            @click="contextDelete"
          >
            删除
          </div>
        </template>
      </div>
    </Teleport>
  </div>
</template>

<style scoped>
.doc-layout {
  display: flex;
  gap: 16px;
  align-items: flex-start;
}
.group-panel {
  width: 220px;
  flex-shrink: 0;
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  padding: 12px;
}
.group-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.group-title {
  font-weight: 600;
  font-size: 14px;
  color: #303133;
}
.tree-search {
  margin: 8px 0;
}
.tree-search :deep(.el-input__wrapper) {
  box-shadow: 0 0 0 1px #dcdfe6 inset;
  border-radius: 4px;
}
.group-tree {
  max-height: 560px;
  overflow-y: auto;
}
.group-tree :deep(.el-tree-node__content) {
  height: auto;
  padding: 2px 0;
}
.group-tree-node {
  display: flex;
  align-items: center;
  flex: 1;
  padding: 2px 4px;
  border-radius: 4px;
  font-size: 13px;
  gap: 6px;
  width: 100%;
}
.group-tree-node:hover {
  background: #f5f7fa;
}
.group-tree-node.active {
  background: #ecf5ff;
  color: #409eff;
  font-weight: 500;
}
.group-name {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.group-count {
  font-size: 12px;
  color: #909399;
  flex-shrink: 0;
}
.group-lock {
  font-size: 10px;
  color: #c0c4cc;
  flex-shrink: 0;
  margin-left: 2px;
}
.doc-content {
  flex: 1;
  min-width: 0;
}
.doc-name {
  color: #303133;
}

/* 上传弹窗拖拽区 */
.doc-upload {
  width: 100%;
}
.doc-upload :deep(.el-upload) {
  width: 100%;
}
.doc-upload :deep(.el-upload-dragger) {
  width: 100%;
  padding: 20px 0;
}

/* 右键上下文菜单 */
.context-menu {
  position: fixed;
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  padding: 4px 0;
  min-width: 130px;
  z-index: 9999;
}
.context-menu-item {
  padding: 7px 14px;
  font-size: 13px;
  color: #303133;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 8px;
  transition: background 0.15s;
}
.context-menu-item:hover {
  background: #f5f7fa;
}
.context-menu-item.danger {
  color: #f56c6c;
}
.context-menu-item.danger:hover {
  background: #fef0f0;
}
.context-menu-divider {
  height: 1px;
  background: #ebeef5;
  margin: 4px 0;
}
</style>
