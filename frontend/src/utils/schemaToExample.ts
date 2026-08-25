/**
 * 根据 JSON Schema 生成示例值
 * ponytail: 简单启发式生成，不处理 oneOf/allOf/anyOf 等复杂 schema
 */
export function schemaToExample(schema: any): any {
  if (schema == null) return null

  // 优先使用显式示例或默认值
  if ('example' in schema) return schema.example
  if ('default' in schema) return schema.default
  if (schema.enum && Array.isArray(schema.enum) && schema.enum.length > 0) {
    return schema.enum[0]
  }

  const type = schema.type

  if (type === 'object') {
    const example: Record<string, any> = {}
    const properties = schema.properties || {}
    for (const key of Object.keys(properties)) {
      example[key] = schemaToExample(properties[key])
    }
    return example
  }

  if (type === 'array') {
    return []
  }

  if (type === 'string') return ''
  if (type === 'integer' || type === 'number') return 0
  if (type === 'boolean') return false

  return null
}

function typeLabel(schema: any): string {
  if (schema == null) return 'null'
  if (schema.type) return schema.type
  if (schema.enum) return 'enum'
  return 'any'
}

function propComment(schema: any): string {
  const type = typeLabel(schema)
  const desc = schema?.description || ''
  return desc ? `// ${type}，${desc}` : `// ${type}`
}

/**
 * 根据 JSON Schema 生成带字段描述的示例 JSON 字符串
 * 输出形如："field": "", // string，描述
 */
export function schemaToExampleString(schema: any, indent = 0): string {
  if (schema == null) return 'null'

  if ('example' in schema) return JSON.stringify(schema.example)
  if ('default' in schema) return JSON.stringify(schema.default)
  if (schema.enum && Array.isArray(schema.enum) && schema.enum.length > 0) {
    return JSON.stringify(schema.enum[0])
  }

  const type = schema.type

  if (type === 'object') {
    const properties = schema.properties || {}
    const keys = Object.keys(properties)
    if (keys.length === 0) return '{}'
    const prefix = ' '.repeat(indent)
    const inner = ' '.repeat(indent + 2)
    const lines = keys.map((key, idx) => {
      const prop = properties[key]
      const value = schemaToExampleString(prop, indent + 2)
      const comma = idx < keys.length - 1 ? ',' : ''
      return `${inner}"${key}": ${value}${comma} ${propComment(prop)}`
    })
    return `{\n${lines.join('\n')}\n${prefix}}`
  }

  if (type === 'array') {
    return '[]'
  }

  if (type === 'string') return '""'
  if (type === 'integer' || type === 'number') return '0'
  if (type === 'boolean') return 'false'

  return 'null'
}
