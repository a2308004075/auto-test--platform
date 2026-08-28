/**
 * JSON 格式化通用工具（保留行内 // 注释，检测语法错误）
 */
import { ElMessage } from 'element-plus'

/**
 * 剥离行内 // 注释，返回纯 JSON 文本与注释映射
 */
function splitComments(text: string): { jsonText: string; jsonLines: string[]; comments: Map<number, string> } {
  const lines = text.split('\n')
  const jsonLines: string[] = []
  const comments: Map<number, string> = new Map()
  lines.forEach((line, idx) => {
    const extracted = extractTrailingLineComment(line)
    if (extracted) {
      comments.set(idx, extracted.comment)
      jsonLines.push(extracted.jsonPart)
    } else {
      jsonLines.push(line)
    }
  })
  return { jsonText: jsonLines.join('\n'), jsonLines, comments }
}

/**
 * 检测 JSON 语法错误（忽略行内 // 注释），合法返回 null
 */
export function jsonSyntaxError(text: string): string | null {
  try {
    JSON.parse(splitComments(text).jsonText)
    return null
  } catch (e: any) {
    return e?.message || 'JSON 解析失败'
  }
}

/**
 * 格式化 JSON，保留行内 // 注释；语法错误返回 null（供需要自定义提示的调用方）
 */
export function tryFormatJson(text: string): string | null {
  const { jsonText, jsonLines, comments } = splitComments(text)

  let formatted: string
  try {
    formatted = JSON.stringify(JSON.parse(jsonText), null, 2)
  } catch {
    return null
  }

  const formattedLines = formatted.split('\n')
  const used = new Set<number>()
  const result = formattedLines.map((fLine) => {
    const keyMatch = fLine.match(/"([^"]+)"\s*:/)
    if (keyMatch) {
      const key = keyMatch[1]
      for (const [idx, line] of jsonLines.entries()) {
        if (used.has(idx)) continue
        if (line.includes(`"${key}":`)) {
          const comment = comments.get(idx)
          if (comment) {
            used.add(idx)
            // 对齐：保证注释前至少一个空格
            return fLine.replace(/\s*$/, '') + ' ' + comment.trimStart()
          }
        }
      }
    }
    return fLine
  })

  comments.forEach((comment, idx) => {
    if (!used.has(idx)) result.push(comment)
  })
  return result.join('\n')
}

/**
 * 格式化 JSON（适合直接绑定到按钮）：语法错误时弹警告并原样返回
 */
export function formatJson(text: string): string {
  const formatted = tryFormatJson(text)
  if (formatted === null) {
    ElMessage.warning(`JSON 语法错误：${jsonSyntaxError(text)}`)
    return text
  }
  return formatted
}

function extractTrailingLineComment(line: string): { jsonPart: string; comment: string } | null {
  let inString = false
  let escape = false
  for (let i = 0; i < line.length; i++) {
    const ch = line[i]
    if (escape) {
      escape = false
      continue
    }
    if (ch === '\\') {
      escape = true
      continue
    }
    if (ch === '"') {
      inString = !inString
      continue
    }
    if (!inString && ch === '/' && line[i + 1] === '/') {
      return { jsonPart: line.slice(0, i), comment: line.slice(i) }
    }
  }
  return null
}
