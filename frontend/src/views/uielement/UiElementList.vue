<!--
 @author HXN
 @date 2026-08-30
 @description 界面元素列表视图
-->
<script setup lang="ts">
/**
 * 界面元素 - 前端源码交互元素解析与 XPath 管理
 * 左侧文件树（仓库 → 目录 → 文件），右侧选中文件的元素列表
 */
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete } from '@element-plus/icons-vue'
import { getRepositories } from '@/api/repository'
import {
  getUiElementFiles,
  getUiElements,
  importUiElements,
  deleteUiElementFile,
  deleteUiElementRepository,
} from '@/api/uielement'
import { useProjectStore } from '@/stores/modules/project'
import { usePermission } from '@/composables/usePermission'

const route = useRoute()
const { hasPermission } = usePermission()
const projectStore = useProjectStore()
const projectId = computed(() => Number(route.params.id))

// ===== 文件树 =====
const treeLoading = ref(false)
const fileTree = ref<any[]>([])

/** 为树节点补充唯一 nodeKey（nodeType:repositoryId:path） */
function stampNodeKey(nodes: any[]) {
  nodes.forEach((n) => {
    n.nodeKey = `${n.nodeType}:${n.repositoryId}:${n.path}`
    if (n.children?.length) {
      stampNodeKey(n.children)
    }
  })
}

async function fetchTree() {
  treeLoading.value = true
  try {
    const res: any = await getUiElementFiles(projectId.value)
    const data = res.data || []
    stampNodeKey(data)
    fileTree.value = data
  } catch {
    fileTree.value = []
  } finally {
    treeLoading.value = false
  }
}

// ===== 当前选中文件与元素列表 =====
const selectedFile = ref<{ repositoryId: number; repositoryName: string; filePath: string } | null>(null)
const elementsLoading = ref(false)
const elements = ref<any[]>([])
const tagFilter = ref('')
const keyword = ref('')

/** 标签筛选选项（从当前文件元素中提取去重） */
const tagOptions = computed(() => {
  const tags = new Set<string>()
  elements.value.forEach((e) => tags.add(e.elementTag))
  return Array.from(tags).sort()
})

/** 本地过滤：标签类型 + 关键字（ID/name/文本/placeholder/XPath） */
const filteredElements = computed(() => {
  return elements.value.filter((e) => {
    if (tagFilter.value && e.elementTag !== tagFilter.value) return false
    if (keyword.value) {
      const kw = keyword.value.toLowerCase()
      const hit = [e.elementId, e.elementName, e.elementText, e.elementPlaceholder, e.smartXPath]
        .some((v) => v && String(v).toLowerCase().includes(kw))
      if (!hit) return false
    }
    return true
  })
})

function handleNodeClick(data: any) {
  if (data.nodeType !== 'FILE') return
  selectedFile.value = {
    repositoryId: data.repositoryId,
    repositoryName: data.repositoryName,
    filePath: data.path,
  }
  fetchElements()
}

async function fetchElements() {
  if (!selectedFile.value) return
  elementsLoading.value = true
  tagFilter.value = ''
  keyword.value = ''
  try {
    const res: any = await getUiElements(
      projectId.value,
      selectedFile.value.repositoryId,
      selectedFile.value.filePath,
    )
    elements.value = res.data || []
  } catch {
    elements.value = []
  } finally {
    elementsLoading.value = false
  }
}

// ===== 导入界面元素 =====
const importVisible = ref(false)
const importLoading = ref(false)
const repoLoading = ref(false)
const repoOptions = ref<any[]>([])
const selectedRepoId = ref<number | null>(null)

async function openImport() {
  importVisible.value = true
  selectedRepoId.value = null
  repoLoading.value = true
  try {
    const res: any = await getRepositories(projectId.value)
    // 仅展示已成功拉取代码的仓库
    repoOptions.value = (res.data || []).filter((r: any) => r.lastPullStatus === 'SUCCESS')
  } catch {
    repoOptions.value = []
  } finally {
    repoLoading.value = false
  }
}

function handleImport() {
  if (!selectedRepoId.value) {
    ElMessage.warning('请选择要导入的仓库')
    return
  }
  importLoading.value = true
  importUiElements(projectId.value, selectedRepoId.value)
    .then((res: any) => {
      const d = res.data || {}
      ElMessage.success(`导入完成：${d.message || '解析成功'}`)
      importVisible.value = false
      selectedFile.value = null
      elements.value = []
      fetchTree()
    })
    .catch((e: any) => {
      ElMessage.error(e?.response?.data?.message || '导入失败')
    })
    .finally(() => {
      importLoading.value = false
    })
}

// ===== 删除（文件级 / 仓库级） =====
function handleDeleteNode(data: any) {
  if (data.nodeType === 'FILE') {
    ElMessageBox.confirm(
      `确定删除文件「${data.name}」的 ${data.elementCount} 个界面元素？`,
      '确认删除',
      { type: 'warning', confirmButtonText: '确认删除', cancelButtonText: '取消' },
    )
      .then(async () => {
        await deleteUiElementFile(projectId.value, data.repositoryId, data.path)
        ElMessage.success('删除成功')
        if (selectedFile.value?.filePath === data.path) {
          selectedFile.value = null
          elements.value = []
        }
        fetchTree()
      })
      .catch(() => {})
  } else if (data.nodeType === 'REPO') {
    ElMessageBox.confirm(
      `确定删除仓库「${data.name}」的全部界面元素？`,
      '确认删除',
      { type: 'warning', confirmButtonText: '确认删除', cancelButtonText: '取消' },
    )
      .then(async () => {
        await deleteUiElementRepository(projectId.value, data.repositoryId)
        ElMessage.success('删除成功')
        selectedFile.value = null
        elements.value = []
        fetchTree()
      })
      .catch(() => {})
  }
}

// ===== 复制 XPath =====
async function copyText(text: string, tip: string) {
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success(tip)
  } catch {
    ElMessage.error('复制失败，请手动复制')
  }
}

onMounted(fetchTree)
</script>

<template>
  <div>
    <!-- 页面头部 -->
    <div class="page-header">
      <h2>界面元素</h2>
      <el-button v-if="hasPermission('project:ui:import')" type="primary" @click="openImport">
        + 导入界面元素
      </el-button>
    </div>

    <!-- 项目上下文栏 -->
    <div class="ui-project-bar">
      <span>&#x1F4CB;</span>
      <span>当前项目：<span class="project-name">{{ projectStore.currentProjectName }}</span></span>
      <span class="bar-sep">|</span>
      <span>从已拉取的前端源码中解析交互元素 XPath，供 UI 自动化功能调用</span>
    </div>

    <!-- 主体：左侧文件树 + 右侧元素表格 -->
    <div class="ui-main">
      <!-- 左侧文件树 -->
      <div class="ui-tree-panel" v-loading="treeLoading">
        <div v-if="fileTree.length" class="tree-body">
          <el-tree
            :data="fileTree"
            node-key="nodeKey"
            :props="{ label: 'name', children: 'children' }"
            default-expand-all
            :expand-on-click-node="false"
            highlight-current
            @node-click="handleNodeClick"
          >
            <template #default="{ data }">
              <div class="tree-node">
                <span class="tree-label" :class="{ 'tree-repo': data.nodeType === 'REPO' }">
                  {{ data.name }}
                </span>
                <span v-if="data.nodeType === 'FILE'" class="tree-count">{{ data.elementCount }}</span>
                <el-icon
                  v-if="hasPermission('project:ui:delete') && data.nodeType !== 'DIR'"
                  class="tree-delete"
                  title="删除"
                  @click.stop="handleDeleteNode(data)"
                >
                  <Delete />
                </el-icon>
              </div>
            </template>
          </el-tree>
        </div>
        <div v-else class="empty-text">
          暂无界面元素数据，点击「导入界面元素」开始导入
        </div>
      </div>

      <!-- 右侧元素列表 -->
      <div class="ui-table-panel">
        <template v-if="selectedFile">
          <!-- 工具栏 -->
          <div class="ui-toolbar">
            <div class="file-info">
              <span class="file-path">{{ selectedFile.filePath }}</span>
              <el-tag size="small" type="info">{{ selectedFile.repositoryName }}</el-tag>
              <span class="element-total">共 {{ filteredElements.length }}/{{ elements.length }} 个元素</span>
            </div>
            <div class="ui-filters">
              <el-select v-model="tagFilter" clearable placeholder="标签筛选" size="small" style="width: 110px">
                <el-option v-for="t in tagOptions" :key="t" :label="t" :value="t" />
              </el-select>
              <el-input
                v-model="keyword"
                clearable
                placeholder="搜索 ID / 文本 / XPath"
                size="small"
                style="width: 200px"
              />
            </div>
          </div>

          <!-- 元素表格 -->
          <el-table v-loading="elementsLoading" :data="filteredElements" row-key="id" style="width: 100%">
            <el-table-column prop="sortNo" label="#" width="50" />
            <el-table-column prop="elementTag" label="标签" width="90">
              <template #default="{ row }">
                <el-tag size="small">{{ row.elementTag }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="elementId" label="ID" width="130" show-overflow-tooltip>
              <template #default="{ row }">{{ row.elementId || '-' }}</template>
            </el-table-column>
            <el-table-column prop="elementName" label="name" width="130" show-overflow-tooltip>
              <template #default="{ row }">{{ row.elementName || '-' }}</template>
            </el-table-column>
            <el-table-column prop="elementText" label="文本" min-width="120" show-overflow-tooltip>
              <template #default="{ row }">{{ row.elementText || '-' }}</template>
            </el-table-column>
            <el-table-column prop="elementPlaceholder" label="placeholder" min-width="120" show-overflow-tooltip>
              <template #default="{ row }">{{ row.elementPlaceholder || '-' }}</template>
            </el-table-column>
            <el-table-column prop="smartXPath" label="智能 XPath" min-width="280" show-overflow-tooltip>
              <template #default="{ row }">
                <span class="xpath-text">{{ row.smartXPath }}</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="150" fixed="right">
              <template #default="{ row }">
                <el-button
                  type="primary"
                  link
                  size="small"
                  @click="copyText(row.smartXPath, '已复制智能 XPath')"
                >
                  复制XPath
                </el-button>
                <el-button
                  v-if="row.absoluteXPath"
                  type="primary"
                  link
                  size="small"
                  @click="copyText(row.absoluteXPath, '已复制绝对 XPath')"
                >
                  绝对
                </el-button>
              </template>
            </el-table-column>
            <template #empty>
              <div class="empty-text">该文件无匹配的界面元素</div>
            </template>
          </el-table>
        </template>

        <div v-else class="empty-text empty-select">
          请在左侧选择源码文件查看界面元素
        </div>
      </div>
    </div>

    <!-- 导入界面元素弹窗 -->
    <el-dialog v-model="importVisible" title="导入界面元素" width="560px">
      <el-form label-position="top">
        <el-form-item label="选择仓库" required>
          <el-select
            v-model="selectedRepoId"
            placeholder="选择已成功拉取的仓库"
            style="width: 100%"
            :loading="repoLoading"
          >
            <el-option v-for="repo in repoOptions" :key="repo.id" :label="repo.name" :value="repo.id">
              <div class="repo-option">
                <span>{{ repo.name }}</span>
                <span class="repo-branch">{{ repo.branch || '默认分支' }}</span>
              </div>
            </el-option>
          </el-select>
        </el-form-item>
      </el-form>
      <el-alert
        type="info"
        :closable="false"
        show-icon
        title="解析仓库内 .vue / .html 源码，提取原生交互元素与 Element UI 组件（el-input / el-button / el-select 等），支持 Vue 绑定语法（v-model / :attr）解析并生成智能 XPath"
        description="重复导入将覆盖该仓库已有元素数据；XPath 以源码书写结构为准，无法感知 v-if / v-for 等运行时动态结构"
      />
      <div v-if="repoOptions.length === 0 && !repoLoading" class="no-repo-tip">
        暂无已成功拉取的仓库，请先在【源代码】中拉取仓库代码
      </div>
      <template #footer>
        <el-button @click="importVisible = false">取消</el-button>
        <el-button type="primary" :loading="importLoading" @click="handleImport">开始导入</el-button>
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

.ui-project-bar {
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

.ui-project-bar .project-name {
  font-weight: 600;
  color: #409eff;
}

.ui-project-bar .bar-sep {
  color: rgba(0, 0, 0, 0.25);
}

.ui-main {
  display: flex;
  gap: 16px;
  align-items: stretch;
}

/* ===== 左侧文件树 ===== */
.ui-tree-panel {
  width: 280px;
  flex-shrink: 0;
  background: #fff;
  border-radius: 8px;
  border: 1px solid #f0f0f0;
  padding: 12px 8px;
  min-height: 400px;
  max-height: calc(100vh - 220px);
  overflow: auto;
}

.tree-body :deep(.el-tree) {
  --el-tree-node-content-height: 30px;
  background: transparent;
}

.tree-node {
  display: flex;
  align-items: center;
  gap: 6px;
  width: 100%;
  font-size: 13px;
  min-width: 0;
}

.tree-label {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: rgba(0, 0, 0, 0.75);
}

.tree-repo {
  font-weight: 600;
  color: rgba(0, 0, 0, 0.9);
}

.tree-count {
  flex-shrink: 0;
  min-width: 20px;
  text-align: center;
  font-size: 11px;
  line-height: 18px;
  padding: 0 5px;
  border-radius: 9px;
  background: #f0f2f5;
  color: rgba(0, 0, 0, 0.45);
}

.tree-delete {
  flex-shrink: 0;
  color: rgba(0, 0, 0, 0.25);
  cursor: pointer;
  opacity: 1;
  transition: opacity 0.2s;
}

.tree-delete:hover {
  color: #f56c6c;
}

/* ===== 右侧元素列表 ===== */
.ui-table-panel {
  flex: 1;
  min-width: 0;
  background: #fff;
  border-radius: 8px;
  border: 1px solid #f0f0f0;
  padding: 16px;
}

.ui-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}

.file-info {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  flex-wrap: wrap;
}

.file-path {
  font-family: Consolas, Monaco, monospace;
  font-size: 13px;
  color: rgba(0, 0, 0, 0.8);
  word-break: break-all;
}

.element-total {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.45);
}

.ui-filters {
  display: flex;
  align-items: center;
  gap: 8px;
}

.xpath-text {
  font-family: Consolas, Monaco, monospace;
  font-size: 12px;
  color: #606266;
}

.empty-text {
  padding: 32px 0;
  text-align: center;
  color: rgba(0, 0, 0, 0.25);
  font-size: 13px;
}

.empty-select {
  padding: 120px 0;
}

/* ===== 导入弹窗 ===== */
.repo-option {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.repo-branch {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.35);
}

.no-repo-tip {
  margin-top: 10px;
  font-size: 13px;
  color: #e6a23c;
}
</style>
