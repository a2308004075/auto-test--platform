<!--
 @author HXN
 @date 2026-08-20 15:34
 @description Action 编辑器视图
-->
<script setup lang="ts">
/**
 * Action 流程编辑器 - M7
 * 三栏布局：元素面板 | 画布 | 属性面板
 * 三 Tab：基础信息 / I/O 参数 / 节点编排器
 * 使用 @antv/x6 实现流程图画布
 * 对齐原型 action-editor.html
 */
import { ref, reactive, onMounted, onBeforeUnmount, computed, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAction, updateAction } from '@/api/action'
import { getKeywords } from '@/api/keyword'
import { getTools } from '@/api/tool'
import { usePermission } from '@/composables/usePermission'
import EditPageHeader from '@/components/EditPageHeader/index.vue'

const route = useRoute()
const router = useRouter()
const { hasPermission } = usePermission()
const projectId = computed(() => Number(route.params.id))
const actionId = computed(() => Number(route.params.actionId))

// ===== Tab 状态 =====
const activeTab = ref('basic')

// ===== 基础信息 =====
const actionName = ref('')
const actionDesc = ref('')

// ===== I/O 参数编辑 =====
interface ParamRow {
  name: string
  type: string
  required: boolean
  defaultValue: string
  description: string
}
const inputParams = ref<ParamRow[]>([])
const outputParams = ref<ParamRow[]>([])
const ioSubTab = ref<'input' | 'output'>('input')

function parseParams(raw?: string): ParamRow[] {
  if (!raw) return []
  try {
    const arr = JSON.parse(raw)
    return Array.isArray(arr) ? arr : []
  } catch {
    return []
  }
}

function serializeParams(rows: ParamRow[]): string {
  return JSON.stringify(rows)
}

// ===== 画布 =====
const containerRef = ref<HTMLDivElement>()
const saving = ref(false)
const loading = ref(false)
let graph: any = null
let graphInitialized = false

// 节点数据
const nodes = ref<any[]>([])
const selectedNode = ref<any>(null)
const selectedCellData = ref<any>(null)

// 缩放百分比
const zoomPercent = ref(100)

// 全屏状态
const isFullscreen = ref(false)

// 画布尺寸设置
const canvasSizeVisible = ref(false)
const canvasWidth = ref(1200)
const canvasHeight = ref(800)
const canvasPresets = [
  { label: '800 × 600', width: 800, height: 600 },
  { label: '1200 × 800', width: 1200, height: 800 },
  { label: '1600 × 1000', width: 1600, height: 1000 },
  { label: '2000 × 1200', width: 2000, height: 1200 },
]

// 右键插入元素菜单
const insertMenuVisible = ref(false)
const insertMenuPos = reactive({ x: 0, y: 0 })
const insertSearch = ref('')

// ===== 元素面板数据 =====
interface ElementCategory {
  title: string
  badge: number
  expanded: boolean
  search: string
  items: { name: string; type: string; shape: string }[]
}
const elementCategories = ref<ElementCategory[]>([
  { title: '接口关键字', badge: 0, expanded: true, search: '', items: [] },
  { title: '工具方法', badge: 0, expanded: false, search: '', items: [] },
  { title: 'Action关键字', badge: 0, expanded: false, search: '', items: [] },
  { title: '监听器', badge: 2, expanded: false, search: '', items: [
    { name: '执行前监听', type: 'LISTENER', shape: 'node-listener' },
    { name: '执行后监听', type: 'LISTENER', shape: 'node-listener' },
  ] },
  { title: '断言', badge: 3, expanded: false, search: '', items: [
    { name: '状态码等于200', type: 'ASSERT', shape: 'node-assert' },
    { name: '响应包含字段', type: 'ASSERT', shape: 'node-assert' },
    { name: '响应时间校验', type: 'ASSERT', shape: 'node-assert' },
  ] },
  { title: '逻辑判断', badge: 3, expanded: false, search: '', items: [
    { name: '状态是否为空', type: 'CONDITION', shape: 'node-logic' },
    { name: '返回值是否成功', type: 'CONDITION', shape: 'node-logic' },
    { name: '数量是否大于0', type: 'CONDITION', shape: 'node-logic' },
  ] },
])

// 节点类型配置
const nodeTypeConfig: Record<string, { color: string; icon: string; iconBg: string; label: string }> = {
  API_KEYWORD: { color: '#409eff', icon: 'K', iconBg: '#ecf5ff', label: '接口关键字' },
  TOOL_METHOD: { color: '#67c23a', icon: 'T', iconBg: '#f0f9eb', label: '工具方法' },
  ACTION: { color: '#e6a23c', icon: 'A', iconBg: '#fdf6ec', label: 'Action关键字' },
  CONDITION: { color: '#722ed1', icon: '◇', iconBg: '#f4ecff', label: '逻辑判断' },
  ASSERT: { color: '#f56c6c', icon: '断', iconBg: '#fef0f0', label: '断言' },
  LISTENER: { color: '#999', icon: '♪', iconBg: '#f5f5f5', label: '监听器' },
  START: { color: '#67c23a', icon: '▶', iconBg: '#f0f9eb', label: '开始' },
  END: { color: '#f56c6c', icon: '■', iconBg: '#fef0f0', label: '结束' },
}

// ===== 加载数据 =====
async function fetchAction() {
  loading.value = true
  try {
    const res: any = await getAction(projectId.value, actionId.value)
    const data = res.data
    actionName.value = data.name || ''
    actionDesc.value = data.description || ''
    try { nodes.value = data.nodes ? JSON.parse(data.nodes) : [] } catch { nodes.value = [] }
    inputParams.value = parseParams(data.inputParams)
    outputParams.value = parseParams(data.outputParams)
  } catch {
    ElMessage.error('加载 Action 失败')
  } finally {
    loading.value = false
  }
}

async function fetchElementData() {
  try {
    const [kwRes, toolRes, actionRes]: any[] = await Promise.all([
      getKeywords(projectId.value, { page: 1, pageSize: 100 }),
      getTools(projectId.value, { page: 1, pageSize: 100 }),
      // 使用动态 import 避免循环依赖
      import('@/api/action').then((m) => m.getActions(projectId.value, { page: 1, pageSize: 100 })),
    ])
    // 接口关键字
    const kwItems = (kwRes.data?.items || []).map((k: any) => ({
      name: k.name, type: 'API_KEYWORD', shape: 'node-api',
    }))
    elementCategories.value[0].items = kwItems
    elementCategories.value[0].badge = kwItems.length
    // 工具方法
    const toolItems = (toolRes.data?.items || []).map((t: any) => ({
      name: t.name, type: 'TOOL_METHOD', shape: 'node-tool',
    }))
    elementCategories.value[1].items = toolItems
    elementCategories.value[1].badge = toolItems.length
    // Action关键字
    const actionItems = (actionRes.data?.items || [])
      .filter((a: any) => a.id !== actionId.value)
      .map((a: any) => ({
        name: a.name, type: 'ACTION', shape: 'node-action',
      }))
    elementCategories.value[2].items = actionItems
    elementCategories.value[2].badge = actionItems.length
  } catch {
    // 忽略元素面板加载失败
  }
}

function filteredItems(cat: ElementCategory) {
  const kw = cat.search.toLowerCase()
  if (!kw) return cat.items
  return cat.items.filter((i) => i.name.toLowerCase().includes(kw))
}

function toggleCategory(cat: ElementCategory) {
  cat.expanded = !cat.expanded
}

// ===== X6 画布初始化 =====
function getNodeMarkup(color: string, icon: string, iconBg: string) {
  return [
    { tagName: 'rect', selector: 'body' },
    {
      tagName: 'foreignObject',
      selector: 'fo',
      attrs: { width: 200, height: 54 },
      children: [{
        ns: 'http://www.w3.org/1999/xhtml',
        tagName: 'div',
        attrs: { 'data-node-root': '1', width: 200, height: 54 },
        style: `margin:0;padding:0;width:100%;height:100%;display:flex;align-items:center;gap:8px;box-sizing:border-box;font-family:inherit;border-radius:6px;overflow:hidden;border:1.5px solid transparent;cursor:pointer;`,
        children: [
          { tagName: 'div', style: `width:4px;align-self:stretch;background:${color};flex-shrink:0;` },
          {
            tagName: 'div',
            style: `width:24px;height:24px;border-radius:4px;display:flex;align-items:center;justify-content:center;flex-shrink:0;background:${iconBg};color:${color};font-size:12px;font-weight:700;`,
            textContent: icon,
          },
          { tagName: 'strong', style: 'font-size:13px;color:rgba(0,0,0,.85);white-space:nowrap;overflow:hidden;text-overflow:ellipsis;display:block;min-width:0;', selector: 'label', attrs: { 'data-label': '1' } },
        ],
      }],
    },
  ]
}

function registerNodes(Graph: any) {
  const portConfig = {
    groups: {
      top: { position: 'top', attrs: { circle: { r: 5, magnet: true, stroke: '#dcdfe6', fill: '#fff', strokeWidth: 1.5 } } },
      bottom: { position: 'bottom', attrs: { circle: { r: 5, magnet: true, stroke: '#dcdfe6', fill: '#fff', strokeWidth: 1.5 } } },
      left: { position: 'left', attrs: { circle: { r: 5, magnet: true, stroke: '#dcdfe6', fill: '#fff', strokeWidth: 1.5 } } },
      right: { position: 'right', attrs: { circle: { r: 5, magnet: true, stroke: '#dcdfe6', fill: '#fff', strokeWidth: 1.5 } } },
    },
    items: [{ group: 'top' }, { group: 'bottom' }, { group: 'left' }, { group: 'right' }],
  }

  // 注册方形节点
  const registerSquare = (name: string, color: string, icon: string, iconBg: string) => {
    Graph.registerNode(name, {
      inherit: 'rect',
      width: 200, height: 54,
      markup: getNodeMarkup(color, icon, iconBg),
      attrs: { body: { fill: iconBg, stroke: 'none', strokeWidth: 0, rx: 6, ry: 6 } },
      ports: portConfig,
    })
  }
  registerSquare('node-action', '#e6a23c', 'A', '#fdf6ec')
  registerSquare('node-api', '#409eff', 'K', '#ecf5ff')
  registerSquare('node-tool', '#67c23a', 'T', '#f0f9eb')
  registerSquare('node-listener', '#999', '♪', '#f5f5f5')

  // 注册菱形节点（断言）
  Graph.registerNode('node-assert', {
    inherit: 'polygon',
    width: 160, height: 80,
    markup: [
      { tagName: 'polygon', selector: 'body' },
      {
        tagName: 'foreignObject', selector: 'fo',
        attrs: { x: 10, y: 22, width: 140, height: 36 },
        children: [{
          ns: 'http://www.w3.org/1999/xhtml', tagName: 'div',
          attrs: { 'data-node-root': '1', width: 140, height: 36 },
          style: 'margin:0;padding:0;width:100%;height:100%;display:flex;align-items:center;justify-content:center;cursor:pointer;',
          children: [{
            tagName: 'div',
            style: 'width:100%;height:100%;clip-path:polygon(50% 0%,100% 50%,50% 100%,0% 50%);display:flex;align-items:center;justify-content:center;background:#fef0f0;',
            children: [{ tagName: 'span', style: 'font-size:11px;color:#f56c6c;font-weight:600;text-align:center;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;display:block;padding:0 4px;', selector: 'label', attrs: { 'data-label': '1' } }],
          }],
        }],
      },
    ],
    attrs: { body: { refPoints: '0.5,0 1,0.5 0.5,1 0,0.5', fill: '#fef0f0', stroke: 'none', strokeWidth: 0 } },
    ports: portConfig,
  })

  // 注册菱形节点（逻辑判断）- 有 yes/no 两个输出端口
  Graph.registerNode('node-logic', {
    inherit: 'polygon',
    width: 160, height: 80,
    markup: [
      { tagName: 'polygon', selector: 'body' },
      {
        tagName: 'foreignObject', selector: 'fo',
        attrs: { x: 10, y: 22, width: 140, height: 36 },
        children: [{
          ns: 'http://www.w3.org/1999/xhtml', tagName: 'div',
          attrs: { 'data-node-root': '1', width: 140, height: 36 },
          style: 'margin:0;padding:0;width:100%;height:100%;display:flex;align-items:center;justify-content:center;cursor:pointer;',
          children: [{
            tagName: 'div',
            style: 'width:100%;height:100%;clip-path:polygon(50% 0%,100% 50%,50% 100%,0% 50%);display:flex;align-items:center;justify-content:center;background:#f4ecff;',
            children: [{ tagName: 'span', style: 'font-size:11px;color:#722ed1;font-weight:600;text-align:center;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;display:block;padding:0 4px;', selector: 'label', attrs: { 'data-label': '1' } }],
          }],
        }],
      },
    ],
    attrs: { body: { refPoints: '0.5,0 1,0.5 0.5,1 0,0.5', fill: '#f4ecff', stroke: 'none', strokeWidth: 0 } },
    ports: {
      groups: {
        yes: { position: 'right', attrs: { circle: { r: 5, magnet: true, stroke: '#67c23a', fill: '#fff', strokeWidth: 1.5 } } },
        no: { position: 'bottom', attrs: { circle: { r: 5, magnet: true, stroke: '#f56c6c', fill: '#fff', strokeWidth: 1.5 } } },
      },
      items: [{ id: 'yes', group: 'yes' }, { id: 'no', group: 'no' }],
    },
  })
}

const GRID = 20
function snap(v: number) { return Math.round(v / GRID) * GRID }

async function initGraph() {
  if (graphInitialized) return
  const x6 = await import('@antv/x6')
  const { Graph } = x6

  if (!containerRef.value) return

  // 注册自定义节点
  registerNodes(Graph)

  // 创建画布
  graph = new Graph({
    container: containerRef.value,
    width: containerRef.value.clientWidth,
    height: containerRef.value.clientHeight || 500,
    grid: { visible: false },
    translating: { snap: { radius: GRID } },
    mousewheel: { enabled: true, modifiers: null },
    panning: { enabled: true, modifiers: 'space' },
    selecting: {
      enabled: true,
      multiple: true,
      rubberband: true,
    },
    connecting: {
      snap: true,
      router: 'manhattan',
      connector: { name: 'rounded', args: { radius: 8 } },
      allowBlank: false,
    },
  } as any)

  // 监听选中
  graph.on('cell:selected', ({ cell }: any) => {
    if (cell.isNode()) {
      const data = cell.getData() || {}
      selectedCellData.value = data
      selectedNode.value = {
        id: cell.id,
        type: data.nodeType || 'API_KEYWORD',
        label: data.label || '',
        config: data.config || {},
        refKeywordId: data.refKeywordId || null,
        refToolId: data.refToolId || null,
        saveAs: data.save_as || '',
        condition: data.condition || '',
        assertType: data.assertType || 'equal',
        actual: data.actual || '',
        expected: data.expected || '',
        description: data.description || '',
      }
    }
  })

  graph.on('cell:unselected', () => {
    selectedNode.value = null
    selectedCellData.value = null
  })

  graph.on('blank:click', () => {
    graph.cleanSelection()
    insertMenuVisible.value = false
  })

  // 画布右键 → 插入元素菜单
  graph.on('blank:contextmenu', (e: any) => {
    e.preventDefault()
    e.stopPropagation()
    insertMenuPos.x = e.clientX || e.e?.clientX || 0
    insertMenuPos.y = e.clientY || e.e?.clientY || 0
    insertSearch.value = ''
    insertMenuVisible.value = true
  })

  // 连线双击 → 编辑标签
  graph.on('edge:dblclick', (e: any) => {
    const edge = e.cell || (e.edge)
    if (!edge) return
    const currentLabel = edge.getLabelAt(0)?.attrs?.label?.text || ''
    ElMessageBox.prompt('设置连线标签（如：是、否）', '编辑连线标签', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputValue: currentLabel,
      inputPlaceholder: '如：是',
    }).then(({ value }) => {
      if (value) {
        edge.setLabels([{
          position: { distance: 0.5 },
          attrs: {
            label: {
              text: value,
              fill: '#606266',
              fontSize: 11,
              textAnchor: 'middle',
              textVerticalAnchor: 'middle',
            },
            rect: {
              fill: '#fff',
              stroke: '#dcdfe6',
              strokeWidth: 1,
              rx: 3,
              ry: 3,
              refWidth: 1,
              refHeight: 1,
              refX: -0.5,
              refY: -0.5,
            },
          },
        }])
      } else {
        edge.setLabels([])
      }
    }).catch(() => {})
  })

  // 缩放跟踪
  const updateZoom = () => {
    zoomPercent.value = Math.round(graph.zoom() * 100)
  }
  graph.on('scale', updateZoom)
  graph.on('zoom', updateZoom)

  // 新节点默认隐藏连接点
  graph.on('node:added', ({ node }: any) => {
    togglePorts(node, false)
  })

  // 键盘删除
  const keyHandler = (e: KeyboardEvent) => {
    const tag = (document.activeElement?.tagName) || ''
    if (tag === 'INPUT' || tag === 'TEXTAREA') return
    if ((e.key === 'Delete' || e.key === 'Backspace') && graph) {
      const sel = graph.getSelectedCells()
      if (sel.length > 0) {
        e.preventDefault()
        graph.removeCells(sel)
        ElMessage.info(`已删除 ${sel.length} 个元素`)
      }
    }
  }
  document.addEventListener('keydown', keyHandler)

  // 加载已有节点
  renderExistingNodes()

  graphInitialized = true
}

// 连接点显隐
function togglePorts(node: any, visible: boolean) {
  if (!node || !node.isNode || !node.isNode()) return
  try {
    const view = graph.findViewByCell(node)
    if (!view) return
    const el = view.el || view.container
    if (!el) return
    if (visible) el.classList.add('is-selected')
    else el.classList.remove('is-selected')
    const ports = el.querySelectorAll('[magnet="true"], circle[port]')
    ports.forEach((p: HTMLElement) => { p.style.display = visible ? '' : 'none' })
  } catch { /* ignore */ }
}

function setNodeLabel(node: any, text: string) {
  if (!node || !text) return
  const data = node.getData() || {}
  data.label = text
  node.setData(data, { silent: true })
  // 自适应宽度
  const maxChars = 20
  const displayText = text.length > maxChars ? text.substring(0, maxChars) + '…' : text
  setTimeout(() => {
    try {
      const view = graph.findViewByCell(node)
      const el = view?.el || view?.container
      if (el) {
        const labelEl = el.querySelector('[data-label]')
        if (labelEl) {
          labelEl.textContent = displayText
          labelEl.title = text
          // 节点宽度自适应：根据 label 内容计算所需宽度
          autoResizeNode(node, labelEl)
        }
      }
    } catch { /* ignore */ }
  }, 100)
}

// 节点宽度自适应
function autoResizeNode(node: any, labelEl: HTMLElement) {
  if (!node || !labelEl) return
  try {
    const minWidth = 120
    const maxWidth = 320
    const padding = 64 // icon + left-bar + margins
    // 测量 label 文本宽度（创建临时元素）
    const measureEl = document.createElement('span')
    measureEl.style.cssText = 'visibility:hidden;position:absolute;font-size:13px;font-weight:600;white-space:nowrap;'
    measureEl.textContent = labelEl.textContent || ''
    document.body.appendChild(measureEl)
    const textWidth = measureEl.getBoundingClientRect().width
    measureEl.remove()
    const targetWidth = Math.min(maxWidth, Math.max(minWidth, Math.ceil(textWidth + padding)))
    const currentSize = node.size()
    if (currentSize.width !== targetWidth) {
      node.resize(targetWidth, currentSize.height)
      // 同步 foreignObject 宽度
      const foEl = graph.findViewByCell(node)?.el?.querySelector('foreignObject')
      if (foEl) foEl.setAttribute('width', String(targetWidth))
      const rootEl = graph.findViewByCell(node)?.el?.querySelector('[data-node-root]')
      if (rootEl) {
        rootEl.setAttribute('width', String(targetWidth))
        ;(rootEl as HTMLElement).style.width = '100%'
      }
    }
  } catch { /* ignore */ }
}

function renderExistingNodes() {
  if (!graph || !nodes.value.length) return
  graph.clearCells()
  const shapeMap: Record<string, string> = {
    API_KEYWORD: 'node-api',
    TOOL_METHOD: 'node-tool',
    CONDITION: 'node-logic',
    ASSERT: 'node-assert',
    LISTENER: 'node-listener',
    ACTION: 'node-action',
  }
  nodes.value.forEach((n: any) => {
    const shape = shapeMap[n.nodeType] || 'node-api'
    const node = graph.addNode({
      shape,
      x: n.positionX || 200,
      y: n.positionY || 100,
      data: {
        nodeType: n.nodeType,
        label: n.nodeKey || n.label || '',
        config: n.config ? (typeof n.config === 'string' ? JSON.parse(n.config) : n.config) : {},
        refKeywordId: n.refKeywordId || null,
        refToolId: n.refToolId || null,
      },
    })
    setNodeLabel(node, n.nodeKey || n.label || n.nodeType)
  })

  // 重新绘制连线（如果有 config 中的连线信息）
  setTimeout(() => {
    try { graph.zoomToFit({ padding: 40, maxScale: 1 }) } catch { /* ignore */ }
    graph.getNodes().forEach((n: any) => togglePorts(n, false))
  }, 200)
}

// ===== 添加节点 =====
function addNode(type: string, shape: string, name: string) {
  if (!graph) return
  const container = containerRef.value
  if (!container) return
  const rect = container.getBoundingClientRect()
  const center = graph.clientToLocal(rect.left + rect.width / 2, rect.top + rect.height / 2)
  const node = graph.addNode({
    shape,
    x: snap(center.x - 100),
    y: snap(center.y - 27),
    data: { nodeType: type, label: name },
  })
  setNodeLabel(node, name)
  ElMessage.success(`已添加节点：${name}`)
}

// 双击添加
function onElementDblClick(item: { name: string; type: string; shape: string }) {
  addNode(item.type, item.shape, item.name)
}

// 拖拽添加
let dragGhost: HTMLElement | null = null
function onElementDragStart(e: DragEvent, item: { name: string; type: string; shape: string }) {
  if (!graph) return
  e.dataTransfer!.effectAllowed = 'copy'
  e.dataTransfer!.setData('text/plain', JSON.stringify(item))
  // 创建拖拽预览
  dragGhost = document.createElement('div')
  dragGhost.style.cssText = 'position:fixed;z-index:9999;opacity:.75;pointer-events:none;padding:8px 12px;background:#fff;border:1px solid #dcdfe6;border-radius:4px;font-size:13px;'
  dragGhost.textContent = item.name
  document.body.appendChild(dragGhost)
}

function onElementDragMove(e: DragEvent) {
  if (dragGhost) {
    dragGhost.style.left = (e.clientX - 40) + 'px'
    dragGhost.style.top = (e.clientY - 16) + 'px'
  }
}

function onElementDragEnd() {
  if (dragGhost) {
    dragGhost.remove()
    dragGhost = null
  }
}

function onCanvasDrop(e: DragEvent) {
  e.preventDefault()
  if (!graph || !containerRef.value) return
  try {
    const data = JSON.parse(e.dataTransfer!.getData('text/plain'))
    const rect = containerRef.value.getBoundingClientRect()
    if (e.clientX < rect.left || e.clientX > rect.right || e.clientY < rect.top || e.clientY > rect.bottom) return
    const local = graph.clientToLocal(e.clientX, e.clientY)
    const node = graph.addNode({
      shape: data.shape,
      x: snap(local.x - 100),
      y: snap(local.y - 27),
      data: { nodeType: data.type, label: data.name },
    })
    setNodeLabel(node, data.name)
  } catch { /* ignore */ }
}

function onCanvasDragOver(e: DragEvent) {
  e.preventDefault()
  if (e.dataTransfer) e.dataTransfer.dropEffect = 'copy'
}

// ===== 画布工具栏 =====
function zoomIn() { if (graph) graph.zoom(0.1) }
function zoomOut() { if (graph) graph.zoom(-0.1) }
function fitCanvas() {
  if (graph) {
    try { graph.zoomToFit({ padding: 40, maxScale: 1 }) } catch { /* ignore */ }
  }
}
function deleteSelected() {
  if (!graph) return
  const sel = graph.getSelectedCells()
  if (sel.length > 0) {
    graph.removeCells(sel)
    ElMessage.info(`已删除 ${sel.length} 个元素`)
    selectedNode.value = null
  }
}

// 一键美化（拓扑分层布局）
function autoLayout() {
  if (!graph) return
  const gNodes = graph.getNodes()
  const gEdges = graph.getEdges()
  if (gNodes.length === 0) { ElMessage.info('画布为空，无需美化'); return }

  // 构建邻接表与入度
  const outMap: Record<string, string[]> = {}
  const inDeg: Record<string, number> = {}
  const inMap: Record<string, string[]> = {}
  gNodes.forEach((n: any) => { outMap[n.id] = []; inDeg[n.id] = 0; inMap[n.id] = [] })
  gEdges.forEach((e: any) => {
    const src = e.getSourceCellId(), tgt = e.getTargetCellId()
    if (outMap[src]) { outMap[src].push(tgt); inDeg[tgt] = (inDeg[tgt] || 0) + 1 }
    if (inMap[tgt]) { inMap[tgt].push(src) }
  })

  const roots = gNodes.filter((n: any) => inDeg[n.id] === 0).map((n: any) => n.id)
  if (roots.length === 0) roots.push(gNodes[0].id)

  // 最长路径分层
  const level: Record<string, number> = {}
  gNodes.forEach((n: any) => { level[n.id] = 0 })
  const deg: Record<string, number> = {}
  gNodes.forEach((n: any) => { deg[n.id] = inDeg[n.id] })
  const queue = [...roots]
  while (queue.length > 0) {
    const nid = queue.shift()!
    ;(outMap[nid] || []).forEach((tid: string) => {
      level[tid] = Math.max(level[tid] || 0, (level[nid] || 0) + 1)
      deg[tid]--
      if (deg[tid] === 0) queue.push(tid)
    })
  }

  // 按层级分组
  const groups: Record<number, any[]> = {}
  gNodes.forEach((n: any) => {
    const lv = level[n.id] || 0
    if (!groups[lv]) groups[lv] = []
    groups[lv].push(n)
  })

  // 定位
  const gapX = 100, gapY = 100, padX = 80, padY = 40
  const sortedLevels = Object.keys(groups).map(Number).sort((a, b) => a - b)
  let currentY = padY
  sortedLevels.forEach((lv) => {
    const g = groups[lv]
    let x = padX
    let maxH = 0
    g.forEach((n: any) => {
      const size = n.size()
      n.position(snap(x), snap(currentY))
      x += size.width + gapX
      maxH = Math.max(maxH, size.height)
    })
    currentY += maxH + gapY
  })

  // 刷新边路由
  gEdges.forEach((edge: any) => {
    try { edge.setRouter('manhattan'); edge.setVertices([]) } catch { /* ignore */ }
  })

  try { graph.zoomToFit({ padding: 40, maxScale: 1 }) } catch { /* ignore */ }
  ElMessage.success('美化完成')
}

function clearAll() {
  if (!graph) return
  if (graph.getNodes().length === 0 && graph.getEdges().length === 0) {
    ElMessage.info('画布已为空')
    return
  }
  if (!confirm('确定要清空画布中的所有节点和连线吗？此操作不可撤销。')) return
  graph.clearCells()
  selectedNode.value = null
  ElMessage.info('画布已清空')
}

function toggleFullscreen() {
  const orchEl = containerRef.value?.closest('.orchestrator')
  if (!orchEl) return
  isFullscreen.value = !isFullscreen.value
  orchEl.classList.toggle('orchestrator-fullscreen')
  setTimeout(() => { if (graph) graph.resize() }, 60)
}

// ===== 画布尺寸设置 =====
function applyCanvasSize() {
  if (!graph || !containerRef.value) return
  const w = Math.max(400, canvasWidth.value)
  const h = Math.max(300, canvasHeight.value)
  graph.resize(w, h)
  containerRef.value.style.width = w + 'px'
  containerRef.value.style.height = h + 'px'
  canvasSizeVisible.value = false
  ElMessage.success(`画布尺寸已设置为 ${w} × ${h}`)
}

function applyPreset(preset: { width: number; height: number }) {
  canvasWidth.value = preset.width
  canvasHeight.value = preset.height
}

// ===== 右键插入元素 =====
const insertFilteredItems = computed(() => {
  const kw = insertSearch.value.toLowerCase()
  const results: { name: string; type: string; shape: string; category: string }[] = []
  for (const cat of elementCategories.value) {
    for (const item of cat.items) {
      if (!kw || item.name.toLowerCase().includes(kw)) {
        results.push({ ...item, category: cat.title })
      }
    }
  }
  return results
})

function insertNodeFromMenu(item: { name: string; type: string; shape: string }) {
  if (!graph) return
  insertMenuVisible.value = false
  // 将右键位置转换为画布坐标
  const local = graph.clientToLocal(insertMenuPos.x, insertMenuPos.y)
  const node = graph.addNode({
    shape: item.shape,
    x: snap(local.x - 100),
    y: snap(local.y - 27),
    data: { nodeType: item.type, label: item.name },
  })
  setNodeLabel(node, item.name)
  ElMessage.success(`已添加节点：${item.name}`)
}

// ===== 属性面板保存 =====
function saveNodeConfig() {
  if (!graph || !selectedNode.value) return
  const cell = graph.getCellById(selectedNode.value.id)
  if (!cell) return
  const type = selectedNode.value.type
  const config: Record<string, any> = {}

  if (type === 'CONDITION') {
    config.condition = selectedNode.value.condition || ''
  } else if (type === 'ASSERT') {
    config.assertType = selectedNode.value.assertType || 'equal'
    config.actual = selectedNode.value.actual || ''
    config.expected = selectedNode.value.expected || ''
    config.description = selectedNode.value.description || ''
  } else if (type === 'API_KEYWORD' || type === 'TOOL_METHOD' || type === 'ACTION') {
    config.save_as = selectedNode.value.saveAs || ''
    if (type === 'API_KEYWORD') config.refKeywordId = selectedNode.value.refKeywordId || null
    if (type === 'TOOL_METHOD') config.refToolId = selectedNode.value.refToolId || null
  }

  const existingData = cell.getData() || {}
  cell.setData({
    ...existingData,
    config: JSON.stringify(config),
    refKeywordId: selectedNode.value.refKeywordId || null,
    refToolId: selectedNode.value.refToolId || null,
  })

  // 更新节点标签
  let newLabel = selectedNode.value.label
  if (type === 'CONDITION') newLabel = selectedNode.value.condition || newLabel
  else if (type === 'ASSERT') newLabel = selectedNode.value.description || newLabel
  setNodeLabel(cell, newLabel)

  ElMessage.success('节点属性已保存')
}

// ===== 保存 Action =====
async function handleSave() {
  saving.value = true
  try {
    const graphNodes = graph ? graph.getNodes().map((n: any) => {
      const data = n.getData() || {}
      const pos = n.position()
      const node: Record<string, any> = {
        nodeKey: data.label || n.id,
        nodeType: data.nodeType || 'API_KEYWORD',
        positionX: Math.round(pos.x),
        positionY: Math.round(pos.y),
      }
      if (data.config) node.config = typeof data.config === 'string' ? data.config : JSON.stringify(data.config)
      if (data.refKeywordId) node.refKeywordId = data.refKeywordId
      if (data.refToolId) node.refToolId = data.refToolId
      return node
    }) : []

    await updateAction(projectId.value, actionId.value, {
      name: actionName.value,
      description: actionDesc.value,
      nodes: graphNodes,
      inputParams: serializeParams(inputParams.value),
      outputParams: serializeParams(outputParams.value),
    })
    ElMessage.success('保存成功')
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

// ===== 跳转调试 =====
function gotoDebug() {
  router.push(`/project/${projectId.value}/actions/${actionId.value}/debug`)
}

// ===== Tab 切换时延迟初始化画布 =====
async function onTabChange(name: string) {
  activeTab.value = name
  if (name === 'orchestrator') {
    await nextTick()
    if (!graphInitialized) {
      try {
        await initGraph()
      } catch (err) {
        console.error('画布初始化失败:', err)
        ElMessage.error('流程图画布加载失败')
      }
    } else if (graph) {
      setTimeout(() => graph.resize(), 60)
    }
  }
}

// document click 关闭右键插入菜单
function onDocClickCloseInsert() {
  insertMenuVisible.value = false
}

onMounted(() => {
  fetchAction()
  fetchElementData()
  document.addEventListener('click', onDocClickCloseInsert)
})

onBeforeUnmount(() => {
  document.removeEventListener('click', onDocClickCloseInsert)
  if (graph) {
    graph.dispose()
    graph = null
    graphInitialized = false
  }
})
</script>

<template>
  <div v-loading="loading" class="action-editor">
    <!-- 编辑器头部 -->
    <EditPageHeader title="编辑 Action关键字">
      <el-button @click="gotoDebug">调试</el-button>
      <el-button
        v-if="hasPermission('project:action:edit')"
        type="primary"
        :loading="saving"
        @click="handleSave"
      >
        保存
      </el-button>
      <el-button @click="router.back()">取消</el-button>
    </EditPageHeader>

    <!-- Tab 页签 -->
    <el-tabs v-model="activeTab" @tab-change="(n: string) => onTabChange(n)">
      <!-- Tab: 基础信息 -->
      <el-tab-pane label="基础信息" name="basic">
        <el-card>
          <el-form label-position="top" style="max-width: 800px">
            <el-row :gutter="16">
              <el-col :span="12">
                <el-form-item label="名称" required>
                  <el-input v-model="actionName" placeholder="请输入 Action 名称" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="描述">
                  <el-input v-model="actionDesc" placeholder="请输入描述" />
                </el-form-item>
              </el-col>
            </el-row>
          </el-form>
        </el-card>
      </el-tab-pane>

      <!-- Tab: I/O 参数 -->
      <el-tab-pane label="I/O 参数" name="params">
        <el-card>
          <div style="display: flex; gap: 0; margin-bottom: 12px">
            <el-button
              :type="ioSubTab === 'input' ? 'primary' : 'default'"
              @click="ioSubTab = 'input'"
            >
              输入参数
            </el-button>
            <el-button
              :type="ioSubTab === 'output' ? 'primary' : 'default'"
              @click="ioSubTab = 'output'"
            >
              输出参数
            </el-button>
          </div>

          <el-table
            v-if="ioSubTab === 'input'"
            :data="inputParams"
            size="small"
            border
            style="width: 100%"
          >
            <el-table-column label="名称" width="160">
              <template #default="{ row }">
                <el-input v-model="row.name" size="small" placeholder="参数名" />
              </template>
            </el-table-column>
            <el-table-column label="类型" width="100">
              <template #default="{ row }">
                <el-select v-model="row.type" size="small" style="width: 100%">
                  <el-option value="string" label="string" />
                  <el-option value="int" label="int" />
                  <el-option value="bool" label="bool" />
                  <el-option value="float" label="float" />
                  <el-option value="json" label="json" />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="必填" width="80" align="center">
              <template #default="{ row }">
                <el-switch v-model="row.required" size="small" />
              </template>
            </el-table-column>
            <el-table-column label="默认值" width="140">
              <template #default="{ row }">
                <el-input v-model="row.defaultValue" size="small" placeholder="无" />
              </template>
            </el-table-column>
            <el-table-column label="说明">
              <template #default="{ row }">
                <el-input v-model="row.description" size="small" placeholder="说明" />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="60" align="center">
              <template #default="{ $index }">
                <el-button
                  link
                  size="small"
                  type="danger"
                  @click="inputParams.splice($index, 1)"
                >
                  ×
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <el-table
            v-else
            :data="outputParams"
            size="small"
            border
            style="width: 100%"
          >
            <el-table-column label="名称" width="160">
              <template #default="{ row }">
                <el-input v-model="row.name" size="small" placeholder="参数名" />
              </template>
            </el-table-column>
            <el-table-column label="类型" width="100">
              <template #default="{ row }">
                <el-select v-model="row.type" size="small" style="width: 100%">
                  <el-option value="string" label="string" />
                  <el-option value="int" label="int" />
                  <el-option value="bool" label="bool" />
                  <el-option value="float" label="float" />
                  <el-option value="json" label="json" />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="必填" width="80" align="center">
              <template #default="{ row }">
                <el-switch v-model="row.required" size="small" />
              </template>
            </el-table-column>
            <el-table-column label="默认值" width="140">
              <template #default="{ row }">
                <el-input v-model="row.defaultValue" size="small" placeholder="无" />
              </template>
            </el-table-column>
            <el-table-column label="说明">
              <template #default="{ row }">
                <el-input v-model="row.description" size="small" placeholder="说明" />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="60" align="center">
              <template #default="{ $index }">
                <el-button
                  link
                  size="small"
                  type="danger"
                  @click="outputParams.splice($index, 1)"
                >
                  ×
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <el-button
            size="small"
            style="margin-top: 8px"
            @click="(ioSubTab === 'input' ? inputParams : outputParams).push({ name: '', type: 'string', required: false, defaultValue: '', description: '' })"
          >
            + 添加参数
          </el-button>
        </el-card>
      </el-tab-pane>

      <!-- Tab: 节点编排器 -->
      <el-tab-pane label="节点编排器" name="orchestrator">
        <div class="orchestrator" @drop="onCanvasDrop" @dragover="onCanvasDragOver">
          <!-- 左侧：元素面板 -->
          <div class="node-panel">
            <div class="panel-title">元素类型</div>
            <div
              v-for="(cat, idx) in elementCategories"
              :key="idx"
              class="el-section"
              :class="{ 'el-open': cat.expanded }"
            >
              <div class="el-header" @click="toggleCategory(cat)">
                <span class="el-arrow" :class="{ expanded: cat.expanded }">▸</span>
                <span>{{ cat.title }}</span>
                <span class="el-badge">{{ cat.badge }}</span>
              </div>
              <div v-show="cat.expanded" class="el-body">
                <input
                  v-model="cat.search"
                  class="el-search"
                  type="text"
                  :placeholder="`搜索${cat.title}…`"
                />
                <div class="el-items">
                  <div
                    v-for="(item, i) in filteredItems(cat)"
                    :key="i"
                    class="node-drag"
                    draggable="true"
                    @dblclick="onElementDblClick(item)"
                    @dragstart="(e) => onElementDragStart(e, item)"
                    @drag="onElementDragMove"
                    @dragend="onElementDragEnd"
                  >
                    <div
                      class="nd-icon"
                      :style="{ background: nodeTypeConfig[item.type]?.iconBg, color: nodeTypeConfig[item.type]?.color }"
                    >
                      {{ nodeTypeConfig[item.type]?.icon }}
                    </div>
                    {{ item.name }}
                  </div>
                  <div
                    v-if="filteredItems(cat).length === 0"
                    style="font-size: 12px; color: #c0c4cc; text-align: center; padding: 8px"
                  >
                    无匹配项
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 中间：画布 -->
          <div class="canvas-wrap">
            <!-- 工具栏 -->
            <div class="canvas-toolbar">
              <button class="tb-btn" title="放大" @click="zoomIn">+</button>
              <button class="tb-btn" title="缩小" @click="zoomOut">−</button>
              <span class="tb-label">{{ zoomPercent }}%</span>
              <button class="tb-btn" title="适应画布" @click="fitCanvas">⊡</button>
              <div class="tb-sep"></div>
              <button class="tb-btn danger" title="删除选中" @click="deleteSelected">✕</button>
              <div class="tb-sep"></div>
              <button class="tb-btn" title="一键美化" @click="autoLayout">⊞</button>
              <button class="tb-btn" title="一键清空" @click="clearAll">⌀</button>
              <div class="tb-sep"></div>
              <button
                class="tb-btn"
                :class="{ active: isFullscreen }"
                title="全屏编辑"
                @click="toggleFullscreen"
              >
                ⛶
              </button>
              <div class="tb-sep"></div>
              <button class="tb-btn" title="画布尺寸" @click="canvasSizeVisible = true">⤢</button>
            </div>
            <div ref="containerRef" class="flow-graph"></div>
          </div>

          <!-- 右侧：属性面板 -->
          <div class="prop-panel">
            <template v-if="selectedNode">
              <!-- API_KEYWORD -->
              <template v-if="selectedNode.type === 'API_KEYWORD'">
                <h4>节点属性：接口关键字</h4>
                <el-form label-position="top" size="small">
                  <el-form-item label="接口关键字">
                    <el-input v-model="selectedNode.label" />
                  </el-form-item>
                  <el-form-item label="引用关键字 ID">
                    <el-input-number v-model="selectedNode.refKeywordId" :min="0" style="width: 100%" />
                  </el-form-item>
                  <el-form-item label="save_as 变量名">
                    <el-input v-model="selectedNode.saveAs" placeholder="如：api_result" />
                  </el-form-item>
                  <el-button size="small" type="primary" @click="saveNodeConfig">保存</el-button>
                </el-form>
              </template>

              <!-- TOOL_METHOD -->
              <template v-else-if="selectedNode.type === 'TOOL_METHOD'">
                <h4>节点属性：工具方法</h4>
                <el-form label-position="top" size="small">
                  <el-form-item label="工具方法">
                    <el-input v-model="selectedNode.label" />
                  </el-form-item>
                  <el-form-item label="引用工具方法 ID">
                    <el-input-number v-model="selectedNode.refToolId" :min="0" style="width: 100%" />
                  </el-form-item>
                  <el-form-item label="save_as 变量名">
                    <el-input v-model="selectedNode.saveAs" placeholder="如：tool_result" />
                  </el-form-item>
                  <el-button size="small" type="primary" @click="saveNodeConfig">保存</el-button>
                </el-form>
              </template>

              <!-- ACTION -->
              <template v-else-if="selectedNode.type === 'ACTION'">
                <h4>节点属性：Action关键字</h4>
                <el-form label-position="top" size="small">
                  <el-form-item label="Action关键字">
                    <el-input v-model="selectedNode.label" />
                  </el-form-item>
                  <el-form-item label="save_as 变量名">
                    <el-input v-model="selectedNode.saveAs" placeholder="如：action_result" />
                  </el-form-item>
                  <el-button size="small" type="primary" @click="saveNodeConfig">保存</el-button>
                </el-form>
              </template>

              <!-- CONDITION -->
              <template v-else-if="selectedNode.type === 'CONDITION'">
                <h4>节点属性：逻辑判断</h4>
                <el-form label-position="top" size="small">
                  <el-form-item label="条件表达式（Python 语法）">
                    <el-input
                      v-model="selectedNode.condition"
                      type="textarea"
                      :rows="3"
                      placeholder="如：status == 'empty'"
                      style="font-family: monospace"
                    />
                    <div style="color: #909399; font-size: 11px; margin-top: 4px">
                      支持 Python 语法：==, !=, &gt;, &lt;, and, or, not, in, len() 等
                    </div>
                  </el-form-item>
                  <div style="margin-top: 12px; padding: 10px; background: #fafafa; border-radius: 4px; font-size: 12px">
                    <div style="display: flex; align-items: center; gap: 6px; margin-bottom: 6px">
                      <span style="display: inline-block; width: 8px; height: 8px; border-radius: 50%; background: #67c23a"></span>
                      <strong>是（满足条件）</strong>
                      <span style="color: #909399">→ 右侧分支</span>
                    </div>
                    <div style="display: flex; align-items: center; gap: 6px">
                      <span style="display: inline-block; width: 8px; height: 8px; border-radius: 50%; background: #f56c6c"></span>
                      <strong>否（不满足条件）</strong>
                      <span style="color: #909399">→ 下方分支</span>
                    </div>
                  </div>
                  <el-button size="small" type="primary" @click="saveNodeConfig" style="margin-top: 12px">保存</el-button>
                </el-form>
              </template>

              <!-- ASSERT -->
              <template v-else-if="selectedNode.type === 'ASSERT'">
                <h4>节点属性：断言</h4>
                <el-form label-position="top" size="small">
                  <el-form-item label="断言类型">
                    <el-select v-model="selectedNode.assertType" style="width: 100%">
                      <el-option value="equal" label="equal（等于）" />
                      <el-option value="not_equal" label="not_equal（不等于）" />
                      <el-option value="include" label="include（包含）" />
                      <el-option value="not_include" label="不包含" />
                      <el-option value="true" label="true（为真）" />
                      <el-option value="not_true" label="not_true（为假）" />
                    </el-select>
                  </el-form-item>
                  <el-form-item label="实际值表达式">
                    <el-input
                      v-model="selectedNode.actual"
                      placeholder="如：${response.status_code}"
                      style="font-family: monospace"
                    />
                  </el-form-item>
                  <el-form-item label="预期值">
                    <el-input v-model="selectedNode.expected" placeholder="如：200" />
                  </el-form-item>
                  <el-form-item label="描述（可选）">
                    <el-input v-model="selectedNode.description" placeholder="断言描述" />
                  </el-form-item>
                  <p style="font-size: 11px; color: #909399; line-height: 1.6">
                    断言节点在执行时对上游节点的结果进行校验，校验失败则标记该节点为失败状态
                  </p>
                  <el-button size="small" type="primary" @click="saveNodeConfig">保存</el-button>
                </el-form>
              </template>

              <!-- LISTENER -->
              <template v-else-if="selectedNode.type === 'LISTENER'">
                <h4>节点属性：监听器</h4>
                <p style="font-size: 12px; color: #909399">
                  监听器功能待定义，敬请期待。
                </p>
              </template>

              <!-- 默认 -->
              <template v-else>
                <h4>节点属性</h4>
                <p style="color: #909399; font-size: 12px">
                  {{ selectedNode.type }} 类型节点无需额外配置
                </p>
              </template>
            </template>

            <!-- 空状态 -->
            <div v-else class="prop-empty">
              <div class="pe-icon">⊞</div>
              <p>点击流程图中的节点查看和编辑属性</p>
              <p style="font-size: 11px; margin-top: 4px">从左侧拖拽或双击元素添加到画布</p>
            </div>
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 画布尺寸设置弹窗 -->
    <el-dialog
      v-model="canvasSizeVisible"
      title="画布尺寸设置"
      width="420px"
      append-to-body
    >
      <el-form label-width="80px" size="small">
        <el-form-item label="宽度">
          <el-input-number v-model="canvasWidth" :min="400" :max="5000" :step="50" style="width: 100%" />
        </el-form-item>
        <el-form-item label="高度">
          <el-input-number v-model="canvasHeight" :min="300" :max="5000" :step="50" style="width: 100%" />
        </el-form-item>
        <el-form-item label="预设">
          <div style="display: flex; flex-wrap: wrap; gap: 8px">
            <el-button
              v-for="preset in canvasPresets"
              :key="preset.label"
              size="small"
              @click="applyPreset(preset)"
            >
              {{ preset.label }}
            </el-button>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="canvasSizeVisible = false">取消</el-button>
        <el-button type="primary" @click="applyCanvasSize">应用</el-button>
      </template>
    </el-dialog>

    <!-- 右键插入元素菜单 -->
    <Teleport to="body">
      <div
        v-if="insertMenuVisible"
        class="insert-context-menu"
        :style="{ left: insertMenuPos.x + 'px', top: insertMenuPos.y + 'px' }"
        @click.stop
        @contextmenu.prevent
      >
        <div class="insert-search-bar">
          <input
            v-model="insertSearch"
            class="insert-search-input"
            type="text"
            placeholder="搜索可插入元素…"
            autofocus
          />
        </div>
        <div class="insert-items-list">
          <div
            v-for="(item, idx) in insertFilteredItems"
            :key="idx"
            class="insert-item"
            @click="insertNodeFromMenu(item)"
          >
            <div
              class="insert-item-icon"
              :style="{
                background: nodeTypeConfig[item.type]?.iconBg,
                color: nodeTypeConfig[item.type]?.color,
              }"
            >
              {{ nodeTypeConfig[item.type]?.icon }}
            </div>
            <div class="insert-item-text">
              <span class="insert-item-name">{{ item.name }}</span>
              <span class="insert-item-cat">{{ item.category }}</span>
            </div>
          </div>
          <div
            v-if="insertFilteredItems.length === 0"
            class="insert-empty"
          >
            无匹配元素
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<style scoped>
.action-editor {
  margin: -24px;
}
.edit-header {
  padding: 12px 16px;
  border-bottom: 1px solid #ebeef5;
  background: #fff;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

/* 节点编排器布局 */
.orchestrator {
  display: flex;
  height: 640px;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  overflow: hidden;
  position: relative;
  min-height: 360px;
  max-height: 85vh;
}
.orchestrator-fullscreen {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw !important;
  height: 100vh !important;
  z-index: 10000;
  border-radius: 0;
  border: none;
}

/* 左侧元素面板 */
.node-panel {
  width: 180px;
  background: #fafafa;
  border-right: 1px solid #ebeef5;
  padding: 10px;
  overflow-y: auto;
  flex-shrink: 0;
}
.panel-title {
  font-size: 12px;
  color: #909399;
  margin-bottom: 8px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}
.el-section {
  margin-bottom: 2px;
}
.el-header {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 8px;
  cursor: pointer;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
  color: #303133;
  user-select: none;
  transition: background 0.15s;
}
.el-header:hover {
  background: #f0f0f0;
}
.el-arrow {
  font-size: 9px;
  color: #c0c4cc;
  transition: transform 0.2s;
  display: inline-block;
  width: 10px;
  text-align: center;
}
.el-arrow.expanded {
  transform: rotate(90deg);
}
.el-badge {
  margin-left: auto;
  font-size: 10px;
  color: #909399;
  background: #f0f0f0;
  border-radius: 8px;
  padding: 0 5px;
  min-width: 16px;
  text-align: center;
  line-height: 16px;
}
.el-body {
  padding: 0 2px 4px;
}
.el-search {
  width: 100%;
  height: 24px;
  border: 1px solid #dcdfe6;
  border-radius: 3px;
  padding: 0 6px;
  font-size: 11px;
  margin-bottom: 4px;
  outline: none;
  box-sizing: border-box;
  background: #fff;
}
.el-search:focus {
  border-color: #409eff;
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.1);
}
.el-items {
  max-height: 150px;
  overflow-y: auto;
}
.node-drag {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  margin-bottom: 6px;
  cursor: grab;
  font-size: 13px;
  transition: all 0.15s;
  user-select: none;
}
.node-drag:hover {
  border-color: #409eff;
  box-shadow: 0 1px 4px rgba(64, 158, 255, 0.15);
}
.node-drag:active {
  cursor: grabbing;
}
.nd-icon {
  width: 22px;
  height: 22px;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  flex-shrink: 0;
  font-weight: 700;
}

/* 中间画布 */
.canvas-wrap {
  flex: 1;
  display: flex;
  flex-direction: column;
  position: relative;
  background: #fff;
  overflow: hidden;
}
.canvas-toolbar {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 8px;
  border-bottom: 1px solid #ebeef5;
  background: #fafafa;
  flex-shrink: 0;
}
.tb-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  background: #fff;
  cursor: pointer;
  font-size: 14px;
  color: #606266;
  transition: all 0.15s;
}
.tb-btn:hover {
  border-color: #409eff;
  color: #409eff;
}
.tb-btn.danger:hover {
  border-color: #f56c6c;
  color: #f56c6c;
}
.tb-btn.active {
  background: #409eff;
  color: #fff;
  border-color: #409eff;
}
.tb-sep {
  width: 1px;
  height: 18px;
  background: #dcdfe6;
  margin: 0 4px;
}
.tb-label {
  font-size: 12px;
  color: #606266;
  min-width: 40px;
  text-align: center;
}
.flow-graph {
  flex: 1;
  overflow: hidden;
  background-color: #fff;
  background-image: linear-gradient(rgba(0, 0, 0, 0.07) 1px, transparent 1px),
    linear-gradient(90deg, rgba(0, 0, 0, 0.07) 1px, transparent 1px);
  background-size: 20px 20px;
}

/* 连接点默认隐藏，选中节点时显示 */
:deep(.flow-port),
:deep([magnet="true"]) {
  display: none;
}
:deep(.is-selected .flow-port),
:deep(.is-selected [magnet="true"]) {
  display: inline;
}

/* 右侧属性面板 */
.prop-panel {
  width: 280px;
  border-left: 1px solid #ebeef5;
  background: #fff;
  overflow-y: auto;
  padding: 12px;
  flex-shrink: 0;
}
.prop-panel h4 {
  font-size: 13px;
  font-weight: 600;
  margin-bottom: 10px;
  color: #303133;
}
.prop-empty {
  text-align: center;
  padding: 60px 16px;
  color: #c0c4cc;
  font-size: 13px;
}
.prop-empty .pe-icon {
  font-size: 28px;
  margin-bottom: 8px;
  opacity: 0.4;
}
</style>

<!-- 右键插入菜单样式（Teleport 到 body，需非 scoped） -->
<style>
.insert-context-menu {
  position: fixed;
  z-index: 10000;
  width: 260px;
  max-height: 360px;
  background: #fff;
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}
.insert-search-bar {
  padding: 8px;
  border-bottom: 1px solid #ebeef5;
}
.insert-search-input {
  width: 100%;
  height: 30px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  padding: 0 8px;
  font-size: 12px;
  outline: none;
  box-sizing: border-box;
  background: #fff;
}
.insert-search-input:focus {
  border-color: #409eff;
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.1);
}
.insert-items-list {
  flex: 1;
  overflow-y: auto;
  padding: 4px;
}
.insert-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 8px;
  border-radius: 4px;
  cursor: pointer;
  transition: background 0.15s;
}
.insert-item:hover {
  background: #f5f7fa;
}
.insert-item-icon {
  width: 22px;
  height: 22px;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  font-weight: 700;
  flex-shrink: 0;
}
.insert-item-text {
  display: flex;
  flex-direction: column;
  min-width: 0;
}
.insert-item-name {
  font-size: 12px;
  color: #303133;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.insert-item-cat {
  font-size: 10px;
  color: #c0c4cc;
}
.insert-empty {
  text-align: center;
  padding: 24px 8px;
  font-size: 12px;
  color: #c0c4cc;
}
</style>
