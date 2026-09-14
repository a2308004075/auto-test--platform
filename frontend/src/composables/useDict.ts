/**
 * @description 通用字典加载器
 * 通过 getDictByType API 从后端动态获取字典选项，替代前端硬编码
 */
import { ref } from 'vue'
import { getDictByType, type DictListItem } from '@/api/dict'

export interface DictOption {
  label: string
  value: string
}

// 全局缓存：dictType -> DictOption[]
const cache = new Map<string, DictOption[]>()

/**
 * 使用字典
 * @param dictType 字典类型编码（如 http_method、priority）
 * @returns { options, loading } 选项列表（{ label, value } 格式）
 */
export function useDict(dictType: string) {
  const cached = cache.get(dictType)
  const options = ref<DictOption[]>(cached || [])
  const loading = ref(false)

  if (!cached) {
    loading.value = true
    getDictByType(dictType)
      .then((res: any) => {
        const list: DictListItem[] = res.data || []
        const opts: DictOption[] = list.map((d) => ({
          label: d.dictValueName,
          value: d.dictValue,
        }))
        cache.set(dictType, opts)
        options.value = opts
      })
      .catch(() => {
        options.value = []
      })
      .finally(() => {
        loading.value = false
      })
  }

  return { options, loading }
}
