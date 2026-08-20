<script setup lang="ts">
/**
 * Action 流程编辑器 - M7
 * 三栏布局：元素面板 | 画布 | 属性面板
 * 使用 @antv/x6 实现流程图画布
 */
import { ref, onMounted, onBeforeUnmount, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { getAction, updateAction } from '@/api/action'

const route = useRoute()
const router = useRouter()
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
  } catch { message.error('加载 Action 失败') }
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
      selectedNode.value = { id: cell.id, label: cell.getLabel(), type: cell.getData()?.nodeType || 'START' }
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
      data: { nodeType: n.nodeType },
      ports: { groups: { top: { position: 'top' }, bottom: { position: 'bottom' } }, items: [{ group: 'top' }, { group: 'bottom' }] },
    })
  })
}

function getNodeColor(type: string): string {
  const colors: Record<string, string> = { START: '#52c41a', END: '#ff4d4f', API_KEYWORD: '#1890ff', TOOL_METHOD: '#722ed1', CONDITION: '#faad14', LOOP: '#13c2c2' }
  return colors[type] || '#666'
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

async function handleSave() {
  saving.value = true
  try {
    const graphNodes = graph.getNodes().map((n: any) => ({
      nodeKey: n.id, nodeType: n.getData()?.nodeType || 'API_KEYWORD',
      positionX: Math.round(n.position().x), positionY: Math.round(n.position().y),
    }))
    await updateAction(projectId.value, actionId.value, {
      name: actionName.value, description: actionDesc.value, nodes: graphNodes,
    })
    message.success('保存成功')
  } catch (e: any) { message.error(e?.response?.data?.message || '保存失败') } finally { saving.value = false }
}

onMounted(() => { fetchAction().then(initGraph) })
onBeforeUnmount(() => { graph?.dispose() })
</script>

<template>
  <div class="action-editor">
    <div class="editor-header">
      <div style="display:flex;align-items:center;gap:12px">
        <a @click="router.back()">← 返回</a>
        <a-input v-model:value="actionName" style="width:200px" placeholder="Action 名称" />
        <a-button type="primary" :loading="saving" @click="handleSave">保存</a-button>
      </div>
    </div>

    <div class="editor-body">
      <!-- 左侧元素面板 -->
      <div class="element-panel">
        <div class="panel-title">元素面板</div>
        <div class="node-item" style="background:#52c41a" @click="addNode('START')">开始</div>
        <div class="node-item" style="background:#1890ff" @click="addNode('API_KEYWORD')">接口关键字</div>
        <div class="node-item" style="background:#722ed1" @click="addNode('TOOL_METHOD')">工具方法</div>
        <div class="node-item" style="background:#faad14" @click="addNode('CONDITION')">条件判断</div>
        <div class="node-item" style="background:#13c2c2" @click="addNode('LOOP')">循环</div>
        <div class="node-item" style="background:#ff4d4f" @click="addNode('END')">结束</div>
        <a-divider />
        <a-button size="small" danger @click="deleteSelected" :disabled="!selectedNode">删除选中</a-button>
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
          <a-divider />
          <p style="color:#999;font-size:12px">节点属性配置待完善</p>
        </div>
        <div v-else style="color:#999;text-align:center;padding:40px">选中节点查看属性</div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.action-editor { height:calc(100vh - 120px); display:flex; flex-direction:column; margin:-24px; }
.editor-header { padding:12px 16px; border-bottom:1px solid #f0f0f0; background:#fff; }
.editor-body { flex:1; display:flex; overflow:hidden; }
.element-panel { width:160px; background:#fafafa; border-right:1px solid #f0f0f0; padding:12px; overflow-y:auto; }
.canvas-area { flex:1; background:#f5f5f5; }
.property-panel { width:240px; background:#fafafa; border-left:1px solid #f0f0f0; padding:12px; overflow-y:auto; }
.panel-title { font-weight:600; margin-bottom:12px; font-size:14px; }
.node-item { padding:8px 12px; margin-bottom:8px; border-radius:6px; color:#fff; text-align:center; cursor:pointer; font-size:13px; }
.node-item:hover { opacity:0.85; }
</style>
