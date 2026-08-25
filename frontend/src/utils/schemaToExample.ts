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
