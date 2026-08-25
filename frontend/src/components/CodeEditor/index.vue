<!--
 @author HXN
 @date 2026-08-23
 @description 代码编辑器组件（Groovy 语法高亮）
-->
<script setup lang="ts">
/**
 * 代码编辑器
 * 基于 textarea + pre 实现的轻量级代码编辑器
 * 支持语法高亮、行号、Tab 缩进、滚动同步
 */
import { ref, computed, watch, nextTick } from 'vue'

interface Props {
  modelValue: string
  language?: string
  minHeight?: number
  placeholder?: string
}

const props = withDefaults(defineProps<Props>(), {
  language: 'groovy',
  minHeight: 240,
  placeholder: '请输入代码...',
})

const emit = defineEmits<{
  (e: 'update:modelValue', v: string): void
}>()

const editorRef = ref<HTMLTextAreaElement | null>(null)
const gutterRef = ref<HTMLDivElement | null>(null)
const highlightRef = ref<HTMLPreElement | null>(null)

const code = computed({
  get: () => props.modelValue,
  set: (v: string) => emit('update:modelValue', v),
})

// ===== Groovy 关键字 =====
const GROOVY_KEYWORDS = [
  'as', 'assert', 'break', 'case', 'catch', 'class', 'const', 'continue',
  'def', 'default', 'do', 'else', 'enum', 'extends', 'false', 'finally',
  'for', 'goto', 'if', 'implements', 'import', 'in', 'instanceof',
  'interface', 'new', 'null', 'package', 'return', 'super', 'switch',
  'this', 'throw', 'throws', 'trait', 'true', 'try', 'while',
]

const GROOVY_TYPES = [
  'void', 'int', 'long', 'short', 'byte', 'float', 'double', 'boolean',
  'char', 'String', 'Object', 'Integer', 'Long', 'List', 'Map', 'Set',
  'ArrayList', 'HashMap', 'BigDecimal', 'BigInteger', 'Boolean',
]

const GROOVY_BUILTINS = [
  'println', 'print', 'printf', 'log', 'request', 'response', 'context',
  'vars', 'steps', 'assert', 'sleep', 'System', 'Math', 'Collections',
  'Arrays', 'Random', 'StringBuilder', 'Date', 'Time', 'UUID',
]

// ===== JavaScript 关键字 =====
const JS_KEYWORDS = [
  'break', 'case', 'catch', 'class', 'const', 'continue', 'debugger',
  'default', 'delete', 'do', 'else', 'export', 'extends', 'finally',
  'for', 'function', 'if', 'import', 'in', 'instanceof', 'new', 'of',
  'return', 'super', 'switch', 'this', 'throw', 'try', 'typeof', 'var',
  'let', 'void', 'while', 'with', 'yield', 'async', 'await', 'static',
]

const JS_LITERALS = [
  'true', 'false', 'null', 'undefined', 'NaN', 'Infinity',
]

const JS_BUILTINS = [
  'Math', 'JSON', 'Object', 'Array', 'String', 'Number', 'Boolean',
  'Date', 'RegExp', 'Map', 'Set', 'Promise', 'console', 'Symbol',
  'Error', 'TypeError', 'RangeError',
]

function esc(s: string): string {
  return s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
}

/**
 * Groovy 语法高亮
 * 逐行逐字符解析，输出带 span 标签的 HTML
 */
function highlightGroovy(code: string): string {
  const lines = code.split('\n')
  const out: string[] = []

  for (const line of lines) {
    let result = ''
    let i = 0
    while (i < line.length) {
      // 单行注释
      if (line[i] === '/' && line[i + 1] === '/') {
        result += `<span style="color:#6a9955;">${esc(line.substring(i))}</span>`
        i = line.length
        continue
      }
      // 块注释
      if (line[i] === '/' && line[i + 1] === '*') {
        const end = line.indexOf('*/', i + 2)
        if (end !== -1) {
          result += `<span style="color:#6a9955;">${esc(line.substring(i, end + 2))}</span>`
          i = end + 2
        } else {
          result += `<span style="color:#6a9955;">${esc(line.substring(i))}</span>`
          i = line.length
        }
        continue
      }
      // 字符串（双引号，支持插值 ${...}）
      if (line[i] === '"') {
        let j = i + 1
        while (j < line.length && line[j] !== '"') {
          if (line[j] === '\\') j++
          j++
        }
        j = Math.min(j + 1, line.length)
        result += `<span style="color:#ce9178;">${esc(line.substring(i, j))}</span>`
        i = j
        continue
      }
      // 字符串（单引号）
      if (line[i] === "'") {
        let j = i + 1
        while (j < line.length && line[j] !== "'") {
          if (line[j] === '\\') j++
          j++
        }
        j = Math.min(j + 1, line.length)
        result += `<span style="color:#ce9178;">${esc(line.substring(i, j))}</span>`
        i = j
        continue
      }
      // 三引号字符串
      if (line.substring(i, i + 3) === '"""') {
        const end = line.indexOf('"""', i + 3)
        if (end !== -1) {
          result += `<span style="color:#ce9178;">${esc(line.substring(i, end + 3))}</span>`
          i = end + 3
        } else {
          result += `<span style="color:#ce9178;">${esc(line.substring(i))}</span>`
          i = line.length
        }
        continue
      }
      // 注解 @
      if (line[i] === '@' && (i === 0 || /\s/.test(line[i - 1]))) {
        const m = line.substring(i).match(/^@\w+/)
        if (m) {
          result += `<span style="color:#dcdcaa;">${esc(m[0])}</span>`
          i += m[0].length
          continue
        }
      }
      // 数字
      if (/\d/.test(line[i]) && (i === 0 || /[\s=+\-*/%<>!,(:]/.test(line[i - 1]))) {
        const nm = line.substring(i).match(/^\d+\.?\d*[fLl]?/)
        if (nm) {
          result += `<span style="color:#b5cea8;">${esc(nm[0])}</span>`
          i += nm[0].length
          continue
        }
      }
      // 标识符
      if (/[a-zA-Z_]/.test(line[i])) {
        const wm = line.substring(i).match(/^[a-zA-Z_]\w*/)
        if (wm) {
          const w = wm[0]
          if (GROOVY_KEYWORDS.includes(w)) {
            result += `<span style="color:#569cd6;">${esc(w)}</span>`
          } else if (GROOVY_TYPES.includes(w)) {
            result += `<span style="color:#4ec9b0;">${esc(w)}</span>`
          } else if (GROOVY_BUILTINS.includes(w)) {
            result += `<span style="color:#dcdcaa;">${esc(w)}</span>`
          } else if (i > 0 && line[i - 1] === '.') {
            result += `<span style="color:#d4d4d4;">${esc(w)}</span>`
          } else {
            const after = line.substring(i + w.length)
            if (/^\s*\(/.test(after)) {
              result += `<span style="color:#dcdcaa;">${esc(w)}</span>`
            } else {
              result += `<span style="color:#9cdcfe;">${esc(w)}</span>`
            }
          }
          i += w.length
          continue
        }
      }
      result += esc(line[i])
      i++
    }
    out.push(result)
  }
  return out.join('\n')
}

/**
 * JavaScript 语法高亮
 */
function highlightJs(code: string): string {
  const lines = code.split('\n')
  const out: string[] = []
  for (const line of lines) {
    let result = ''
    let i = 0
    while (i < line.length) {
      // 单行注释
      if (line[i] === '/' && line[i + 1] === '/') {
        result += `<span style="color:#6a9955;">${esc(line.substring(i))}</span>`
        i = line.length
        continue
      }
      // 块注释
      if (line[i] === '/' && line[i + 1] === '*') {
        const end = line.indexOf('*/', i + 2)
        if (end !== -1) {
          result += `<span style="color:#6a9955;">${esc(line.substring(i, end + 2))}</span>`
          i = end + 2
        } else {
          result += `<span style="color:#6a9955;">${esc(line.substring(i))}</span>`
          i = line.length
        }
        continue
      }
      // 模板字符串（反引号）
      if (line[i] === '`') {
        let j = i + 1
        while (j < line.length && line[j] !== '`') {
          if (line[j] === '\\') j++
          j++
        }
        j = Math.min(j + 1, line.length)
        result += `<span style="color:#ce9178;">${esc(line.substring(i, j))}</span>`
        i = j
        continue
      }
      // 字符串（双引号）
      if (line[i] === '"') {
        let j = i + 1
        while (j < line.length && line[j] !== '"') {
          if (line[j] === '\\') j++
          j++
        }
        j = Math.min(j + 1, line.length)
        result += `<span style="color:#ce9178;">${esc(line.substring(i, j))}</span>`
        i = j
        continue
      }
      // 字符串（单引号）
      if (line[i] === "'") {
        let j = i + 1
        while (j < line.length && line[j] !== "'") {
          if (line[j] === '\\') j++
          j++
        }
        j = Math.min(j + 1, line.length)
        result += `<span style="color:#ce9178;">${esc(line.substring(i, j))}</span>`
        i = j
        continue
      }
      // 数字
      if (/\d/.test(line[i]) && (i === 0 || /[\s=+\-*/%<>!,(:]/.test(line[i - 1]))) {
        const nm = line.substring(i).match(/^\d+\.?\d*[eE]?[+-]?\d*/)
        if (nm) {
          result += `<span style="color:#b5cea8;">${esc(nm[0])}</span>`
          i += nm[0].length
          continue
        }
      }
      // 标识符
      if (/[a-zA-Z_$]/.test(line[i])) {
        const wm = line.substring(i).match(/^[a-zA-Z_$][\w$]*/)
        if (wm) {
          const w = wm[0]
          if (JS_KEYWORDS.includes(w) || JS_LITERALS.includes(w)) {
            result += `<span style="color:#569cd6;">${esc(w)}</span>`
          } else if (JS_BUILTINS.includes(w)) {
            result += `<span style="color:#4ec9b0;">${esc(w)}</span>`
          } else if (i > 0 && line[i - 1] === '.') {
            result += `<span style="color:#d4d4d4;">${esc(w)}</span>`
          } else {
            const after = line.substring(i + w.length)
            if (/^\s*\(/.test(after)) {
              result += `<span style="color:#dcdcaa;">${esc(w)}</span>`
            } else {
              result += `<span style="color:#9cdcfe;">${esc(w)}</span>`
            }
          }
          i += w.length
          continue
        }
      }
      result += esc(line[i])
      i++
    }
    out.push(result)
  }
  return out.join('\n')
}

function highlightPlain(code: string): string {
  return esc(code)
}

const highlightedCode = computed(() => {
  const lang = props.language === 'json' ? 'javascript' : props.language
  const fn = lang === 'javascript' ? highlightJs : lang === 'text' ? highlightPlain : highlightGroovy
  return fn(code.value) + '\n'
})

const lineNumbers = computed(() => {
  const n = code.value.split('\n').length
  return Array.from({ length: n }, (_, i) => i + 1).join('\n')
})

// ===== 滚动同步 =====
function onScroll() {
  if (!editorRef.value || !highlightRef.value || !gutterRef.value) return
  highlightRef.value.scrollTop = editorRef.value.scrollTop
  highlightRef.value.scrollLeft = editorRef.value.scrollLeft
  gutterRef.value.scrollTop = editorRef.value.scrollTop
}

// ===== Tab 键支持 =====
function onKeydown(e: KeyboardEvent) {
  if (e.key === 'Tab') {
    e.preventDefault()
    const el = editorRef.value
    if (!el) return
    const s = el.selectionStart
    const end = el.selectionEnd
    const newVal = code.value.substring(0, s) + '    ' + code.value.substring(end)
    code.value = newVal
    nextTick(() => {
      el.selectionStart = el.selectionEnd = s + 4
    })
  }
}

// 暴露方法给父组件
defineExpose({
  focus: () => editorRef.value?.focus(),
  getEditor: () => editorRef.value,
})

watch(() => props.modelValue, () => {
  nextTick(onScroll)
})
</script>

<template>
  <div class="code-editor-wrap" :style="{ minHeight: minHeight + 'px' }">
    <div ref="gutterRef" class="code-gutter">{{ lineNumbers }}</div>
    <div class="code-editor-container">
      <pre ref="highlightRef" class="code-highlight" v-html="highlightedCode"></pre>
      <textarea
        ref="editorRef"
        v-model="code"
        class="code-textarea"
        spellcheck="false"
        :placeholder="placeholder"
        @scroll="onScroll"
        @keydown="onKeydown"
      ></textarea>
    </div>
  </div>
</template>

<style scoped>
.code-editor-wrap {
  display: flex;
  background: #1e1e1e;
  border-radius: 0 0 6px 6px;
  overflow: hidden;
  height: 100%;
}
.code-gutter {
  padding: 16px 0;
  text-align: right;
  user-select: none;
  color: #858585;
  font: 13px/1.6 Consolas, 'Courier New', monospace;
  min-width: 40px;
  background: #1e1e1e;
  border-right: 1px solid #333;
  white-space: pre;
  overflow: hidden;
}
.code-editor-container {
  position: relative;
  flex: 1;
  min-height: inherit;
}
.code-highlight {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  margin: 0;
  padding: 16px 16px 16px 12px;
  font: 13px/1.6 Consolas, 'Courier New', monospace;
  white-space: pre;
  overflow: auto;
  pointer-events: none;
  color: #d4d4d4;
  tab-size: 4;
}
.code-textarea {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  min-height: inherit;
  padding: 16px 16px 16px 12px;
  background: transparent;
  color: transparent;
  caret-color: #d4d4d4;
  border: none;
  outline: none;
  resize: none;
  font: 13px/1.6 Consolas, 'Courier New', monospace;
  tab-size: 4;
  white-space: pre;
  overflow: auto;
  z-index: 1;
}
.code-textarea::placeholder {
  color: #666;
}
</style>
