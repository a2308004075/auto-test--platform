<!--
 @author HXN
 @date 2026-08-20 15:34
 @description Action 编辑器视图
-->
<script setup lang="ts">
/**
 * Action 流程编辑器 - M7
 * 三栏布局：元素面板 | 画布 | 属性面板
 * 使用 @antv/x6 实现流程图画布
 */
import { ref, onMounted, onBeforeUnmount, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getAction, updateAction } from '@/api/action'
import { usePermission } from '@/composables/usePermission'

const route = useRoute()
const router = useRouter()
const { hasPermission } = usePermission()
const projectId = computed(() => Number(route.params.id))
const actionId = computed(() => Number(route.params.actionId))

const containerRef = ref<HTMLDivElement>()
const actionName = ref('')
const actionDesc = ref('')
const nodes = ref<any[]>([])
const selectedNode = ref<any>(null)
const saving = ref(false)
let graph: any = null

async function fetchAction() {
  try {
    const res: any = await getAction(projectId.value, actionId.value)
    const data = res.data
    actionName.value = data.name || ''
    actionDesc.value = data.description || ''
    try { nodes.value = data.nodes ? JSON.parse(data.nodes) : [] } catch { nodes.value = [] }
  } catch { ElMessage.error('加载 Action 失败') }
}

async function initGraph() {
  const { Graph } = await import('@antv/x6')
  if (!containerRef.value) return

  graph = new Graph({
    container: containerRef.value,
    width: containerRef.value.clientWidth,
    height: containerRef.value.clientHeight || 500,
    grid: { visible: true, size: 10, type: 'mesh' },
    selecting: { enabled: true, rubberband: true },
    connecting: { snap: true, allowBlank: false, highlight: true },
    mousewheel: { enabled: true, modifiers: ['ctrl'] },
    panning: { enabled: true },
  } as any)

  // 监听节点选中
  graph.on('cell:selected', ({ cell }: any) => {
    if (cell.isNode()) {
      const data = cell.getData() || {}
      const config = data.config ? (typeof data.config === 'string' ? JSON.parse(data.config) : data.config) : {}
      selectedNode.value = {
        id: cell.id,
        label: cell.getLabel(),
        type: data.nodeType || 'START',
        // 加载节点配置
        conditionExpression: config.expression || '',
        trueNext: config.trueNext || '',
        falseNext: config.falseNext || '',
        loopCount: config.count || 0,
        loopExpression: config.expression || '',
        nextNode: config.nextNode || '',
        refKeywordId: data.refKeywordId || null,
      }
    }
  })
  graph.on('cell:unselected', () => { selectedNode.value = null })

  // 加载已有节点
  renderNodes()
}

function renderNodes() {
  if (!graph) return
  graph.clearCells()
  const defaultNodes = [
    { nodeKey: 'start', nodeType: 'START', label: '开始', positionX: 200, positionY: 40 },
    { nodeKey: 'end', nodeType: 'END', label: '结束', positionX: 200, positionY: 400 },
  ]
  const allNodes = nodes.value.length ? nodes.value : defaultNodes
  allNodes.forEach((n: any) => {
    const color = getNodeColor(n.nodeType)
    graph.addNode({
      id: n.nodeKey,
      x: n.positionX || 200,
      y: n.positionY || 100,
      width: 120, height: 40,
      label: n.nodeKey,
      shape: 'rect',
      attrs: { body: { fill: color, stroke: '#333', rx: 6, ry: 6 }, label: { fill: '#fff', fontSize: 12 } },
      data: {
        nodeType: n.nodeType,
        config: n.config || null,
        refKeywordId: n.refKeywordId || null,
        refToolId: n.refToolId || null,
      },
      ports: { groups: { top: { position: 'top' }, bottom: { position: 'bottom' } }, items: [{ group: 'top' }, { group: 'bottom' }] },
    })
  })
}

function getNodeColor(type: string): string {
  const colors: Record<string, string> = { START: '#67c23a', END: '#f56c6c', API_KEYWORD: '#409eff', TOOL_METHOD: '#722ed1', CONDITION: '#e6a23c', LOOP: '#13c2c2' }
  return colors[type] || '#909399'
}

function addNode(type: string) {
  if (!graph) return
  const key = `node_${Date.now() % 10000}`
  const label = type === 'API_KEYWORD' ? '接口关键字' : type === 'TOOL_METHOD' ? '工具方法' : type === 'CONDITION' ? '条件' : '循环'
  graph.addNode({
    id: key, x: 200, y: 200, width: 120, height: 40, label,
    shape: 'rect',
    attrs: { body: { fill: getNodeColor(type), stroke: '#333', rx: 6, ry: 6 }, label: { fill: '#fff', fontSize: 12 } },
    data: { nodeType: type },
    ports: { groups: { top: { position: 'top' }, bottom: { position: 'bottom' } }, items: [{ group: 'top' }, { group: 'bottom' }] },
  })
}

function deleteSelected() {
  if (!graph || !selectedNode.value) return
  const cell = graph.getCellById(selectedNode.value.id)
  if (cell) graph.removeCell(cell)
  selectedNode.value = null
}

/** 将属性面板的配置保存到 X6 节点的 data 中 */
function saveNodeConfig() {
  if (!graph || !selectedNode.value) return
  const cell = graph.getCellById(selectedNode.value.id)
  if (!cell) return

  const nodeType = selectedNode.value.type
  const config: Record<string, any> = {}

  if (nodeType === 'CONDITION') {
    config.expression = selectedNode.value.conditionExpression || ''
    config.trueNext = selectedNode.value.trueNext || ''
    config.falseNext = selectedNode.value.falseNext || ''
  } else if (nodeType === 'LOOP') {
    config.count = selectedNode.value.loopCount || 0
    config.expression = selectedNode.value.loopExpression || ''
    config.nextNode = selectedNode.value.nextNode || ''
  } else if (nodeType === 'API_KEYWORD' || nodeType === 'TOOL_METHOD') {
    config.nextNode = selectedNode.value.nextNode || ''
  }

  const existingData = cell.getData() || {}
  cell.setData({
    ...existingData,
    config: JSON.stringify(config),
    refKeywordId: selectedNode.value.refKeywordId || null,
  })
  ElMessage.success('节点配置已保存（点击顶部「保存」持久化）')
}

async function handleSave() {
  saving.value = true
  try {
    const graphNodes = graph.getNodes().map((n: any) => {
      const data = n.getData() || {}
      const node: Record<string, any> = {
        nodeKey: n.id,
        nodeType: data.nodeType || 'API_KEYWORD',
        positionX: Math.round(n.position().x),
        positionY: Math.round(n.position().y),
      }
      // 保存节点配置
      if (data.config) node.config = typeof data.config === 'string' ? data.config : JSON.stringify(data.config)
      if (data.refKeywordId) node.refKeywordId = data.refKeywordId
      if (data.refToolId) node.refToolId = data.refToolId
      return node
    })
    await updateAction(projectId.value, actionId.value, {
      name: actionName.value, description: actionDesc.value, nodes: graphNodes,
    })
    ElMessage.success('保存成功')
  } catch (e: any) { ElMessage.error(e?.response?.data?.message || '保存失败') } finally { saving.value = false }
}

onMounted(() => { fetchAction().then(initGraph) })
onBeforeUnmount(() => { graph?.dispose() })
</script>

<template>
  <div class="action-editor">
    <div class="editor-header">
      <div style="display:flex;align-items:center;gap:12px">
        <el-button type="primary" link @click="router.back()">← 返回</el-button>
        <el-input v-model="actionName" style="width:200px" placeholder="Action 名称" />
        <el-button v-if="hasPermission('project:action:edit')" type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </div>
    </div>

    <div class="editor-body">
      <!-- 左侧元素面板 -->
      <div class="element-panel">
        <div class="panel-title">元素面板</div>
        <div class="node-item" style="background:#67c23a" @click="addNode('START')">开始</div>
        <div class="node-item" style="background:#409eff" @click="addNode('API_KEYWORD')">接口关键字</div>
        <div class="node-item" style="background:#722ed1" @click="addNode('TOOL_METHOD')">工具方法</div>
        <div class="node-item" style="background:#e6a23c" @click="addNode('CONDITION')">条件判断</div>
        <div class="node-item" style="background:#13c2c2" @click="addNode('LOOP')">循环</div>
        <div class="node-item" style="background:#f56c6c" @click="addNode('END')">结束</div>
        <el-divider />
        <el-button size="small" type="danger" @click="deleteSelected" :disabled="!selectedNode">删除选中</el-button>
      </div>

      <!-- 中间画布 -->
      <div class="canvas-area" ref="containerRef"></div>

      <!-- 右侧属性面板 -->
      <div class="property-panel">
        <div class="panel-title">属性面板</div>
        <div v-if="selectedNode">
          <p><strong>节点:</strong> {{ selectedNode.label }}</p>
          <p><strong>类型:</strong> {{ selectedNode.type }}</p>
          <p><strong>ID:</strong> {{ selectedNode.id }}</p>
          <el-divider />

          <!-- CONDITION 节点配置 -->
          <template v-if="selectedNode.type === 'CONDITION'">
            <el-form label-position="top" size="small">
              <el-form-item label="条件表达式 (Groovy)">
                <el-input v-model="selectedNode.conditionExpression" type="textarea" :rows="3"
                  placeholder="如: ${status} == 200" />
                <div style="color:#909399;font-size:11px;margin-top:4px">支持 ${var} 引用上下文变量</div>
              </el-form-item>
              <el-form-item label="为 true 时跳转节点">
                <el-input v-model="selectedNode.trueNext" placeholder="nodeKey" />
              </el-form-item>
              <el-form-item label="为 false 时跳转节点">
                <el-input v-model="selectedNode.falseNext" placeholder="nodeKey" />
              </el-form-item>
              <el-button size="small" type="primary" @click="saveNodeConfig">保存配置</el-button>
            </el-form>
          </template>

          <!-- LOOP 节点配置 -->
          <template v-else-if="selectedNode.type === 'LOOP'">
            <el-form label-position="top" size="small">
              <el-form-item label="循环次数">
                <el-input-number v-model="selectedNode.loopCount" :min="0" :max="1000" />
              </el-form-item>
              <el-form-item label="条件表达式（可选）">
                <el-input v-model="selectedNode.loopExpression" type="textarea" :rows="2"
                  placeholder="如: ${index} < 10" />
                <div style="color:#909399;font-size:11px;margin-top:4px">当循环次数为 0 时，使用条件表达式控制循环</div>
              </el-form-item>
              <el-form-item label="后续节点">
                <el-input v-model="selectedNode.nextNode" placeholder="nodeKey" />
              </el-form-item>
              <el-button size="small" type="primary" @click="saveNodeConfig">保存配置</el-button>
            </el-form>
          </template>

          <!-- API_KEYWORD / TOOL_METHOD 节点配置 -->
          <template v-else-if="selectedNode.type === 'API_KEYWORD' || selectedNode.type === 'TOOL_METHOD'">
            <el-form label-position="top" size="small">
              <el-form-item label="引用关键字 ID">
                <el-input-number v-model="selectedNode.refKeywordId" :min="0" />
              </el-form-item>
              <el-form-item label="后续节点">
                <el-input v-model="selectedNode.nextNode" placeholder="nodeKey" />
              </el-form-item>
              <el-button size="small" type="primary" @click="saveNodeConfig">保存配置</el-button>
            </el-form>
          </template>

          <p v-else style="color:#909399;font-size:12px">{{ selectedNode.type }} 类型节点无需额外配置</p>
        </div>
        <div v-else style="color:#909399;text-align:center;padding:40px">选中节点查看属性</div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.action-editor { height:calc(100vh - 120px); display:flex; flex-direction:column; margin:-24px; }
.editor-header { padding:12px 16px; border-bottom:1px solid #ebeef5; background:#fff; }
.editor-body { flex:1; display:flex; overflow:hidden; }
.element-panel { width:160px; background:#f5f7fa; border-right:1px solid #ebeef5; padding:12px; overflow-y:auto; }
.canvas-area { flex:1; background:#f5f7fa; }
.property-panel { width:240px; background:#f5f7fa; border-left:1px solid #ebeef5; padding:12px; overflow-y:auto; }
.panel-title { font-weight:600; margin-bottom:12px; font-size:14px; }
.node-item { padding:8px 12px; margin-bottom:8px; border-radius:6px; color:#fff; text-align:center; cursor:pointer; font-size:13px; }
.node-item:hover { opacity:0.85; }
</style>
