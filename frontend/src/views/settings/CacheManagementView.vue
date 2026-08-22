<!--
 @author HXN
 @date 2026-08-22 13:28
 @description 缓存管理视图
-->
<script setup lang="ts">
/**
 * 缓存管理页面（仅 ADMIN）
 * 模糊搜索 + 精确查询 + 新增/删除
 * 对标 svc-manager-web Cache.vue
 */
import { ref, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import FlexQueryForm from '@/components/FlexQueryForm/index.vue'
import TableFit from '@/components/TableFit/index.vue'
import {
  getCacheByKey,
  searchCache,
  setCache,
  deleteCache,
  type CacheItem,
  type CacheSetRequest,
} from '@/api/cache'

// ===== 搜索 =====
const searchPattern = ref('')
const exactKey = ref('')
const loading = ref(false)
const cacheList = ref<CacheItem[]>([])

// ===== 弹窗 =====
const dialogVisible = ref(false)
const dialogLoading = ref(false)
const form = reactive<CacheSetRequest>({
  key: '',
  value: '',
  ttl: undefined,
})

// ===== 模糊搜索 =====
async function handleSearch() {
  if (!searchPattern.value.trim()) {
    ElMessage.warning('请输入搜索关键词')
    return
  }
  loading.value = true
  try {
    const res: any = await searchCache(searchPattern.value.trim())
    cacheList.value = res.data || []
    if (!cacheList.value.length) {
      ElMessage.info('未找到匹配的缓存项')
    }
  } catch {
    cacheList.value = []
  } finally {
    loading.value = false
  }
}

// ===== 精确查询 =====
async function handleExactQuery() {
  if (!exactKey.value.trim()) {
    ElMessage.warning('请输入完整的缓存 Key')
    return
  }
  loading.value = true
  try {
    const res: any = await getCacheByKey(exactKey.value.trim())
    if (res.data) {
      cacheList.value = [res.data]
    } else {
      cacheList.value = []
      ElMessage.info('未找到该缓存项')
    }
  } catch (e: any) {
    cacheList.value = []
    if (e?.response?.status !== 404) {
      ElMessage.error(e?.response?.data?.message || '查询失败')
    } else {
      ElMessage.info('未找到该缓存项')
    }
  } finally {
    loading.value = false
  }
}

// ===== 重置 =====
function handleReset() {
  searchPattern.value = ''
  exactKey.value = ''
  cacheList.value = []
}

// ===== 新增缓存 =====
function openAdd() {
  form.key = ''
  form.value = ''
  form.ttl = undefined
  dialogVisible.value = true
}

async function handleSave() {
  if (!form.key.trim() || !form.value.trim()) {
    ElMessage.warning('请填写完整的 Key 和 Value')
    return
  }
  dialogLoading.value = true
  try {
    await setCache({ ...form })
    ElMessage.success('缓存设置成功')
    dialogVisible.value = false
    if (searchPattern.value) {
      handleSearch()
    }
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '操作失败')
  } finally {
    dialogLoading.value = false
  }
}

// ===== 删除缓存 =====
async function handleDelete(row: CacheItem) {
  try {
    await ElMessageBox.confirm(
      `确定删除缓存「${row.key}」？`,
      '删除确认',
      { confirmButtonText: '确定删除', cancelButtonText: '取消', type: 'warning' },
    )
  } catch {
    return
  }
  try {
    await deleteCache(row.key)
    ElMessage.success('缓存删除成功')
    cacheList.value = cacheList.value.filter((c) => c.key !== row.key)
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '删除失败')
  }
}

// ===== 格式化 TTL =====
function formatTtl(ttl: number): string {
  if (ttl < 0) return '永不过期'
  if (ttl < 60) return `${ttl} 秒`
  if (ttl < 3600) return `${Math.floor(ttl / 60)} 分钟`
  if (ttl < 86400) return `${Math.floor(ttl / 3600)} 小时`
  return `${Math.floor(ttl / 86400)} 天`
}

// ===== 格式化 Value 显示 =====
function formatValue(val: string): string {
  if (!val) return ''
  if (val.length > 100) return val.substring(0, 100) + '...'
  return val
}
</script>

<template>
  <div class="cache">
    <!-- 查询区 -->
    <div class="top">
      <el-form>
        <FlexQueryForm>
          <el-form-item label="模糊搜索">
            <el-input
              v-model="searchPattern"
              clearable
              placeholder="Key 关键词（支持 * 通配符）"
              class="max-width-300"
              @keyup.enter="handleSearch"
            />
          </el-form-item>
          <el-form-item label="精确查询">
            <el-input
              v-model="exactKey"
              clearable
              placeholder="输入完整 Key"
              class="max-width-300"
              @keyup.enter="handleExactQuery"
            />
          </el-form-item>
          <template #button>
            <el-button type="primary" @click="handleSearch">搜索</el-button>
            <el-button @click="handleExactQuery">查询</el-button>
            <el-button @click="handleReset">重置</el-button>
          </template>
        </FlexQueryForm>
      </el-form>
    </div>

    <!-- 表格区 -->
    <div class="body">
      <div class="function">
        <el-button type="primary" @click="openAdd">设置缓存</el-button>
      </div>

      <TableFit>
        <template #default="{ maxHeight }">
          <el-table
            v-loading="loading"
            :data="cacheList"
            stripe
            :max-height="maxHeight"
            style="width: 100%;"
          >
            <el-table-column prop="key" label="Key" min-width="200" show-overflow-tooltip />
            <el-table-column prop="value" label="Value" min-width="250" show-overflow-tooltip>
              <template #default="{ row }">
                <el-tooltip
                  v-if="row.value && row.value.length > 100"
                  :content="row.value"
                  placement="top"
                >
                  <span>{{ formatValue(row.value) }}</span>
                </el-tooltip>
                <span v-else>{{ row.value }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="ttl" label="TTL" width="120" align="center">
              <template #default="{ row }">
                {{ formatTtl(row.ttl) }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="80" align="right" fixed="right">
              <template #default="{ row }">
                <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </template>
      </TableFit>
    </div>

    <!-- 设置缓存弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      v-drag-dialog
      title="设置缓存"
      :close-on-click-modal="false"
    >
      <el-form :model="form" label-width="120px">
        <el-form-item label="Key" required>
          <el-input v-model="form.key" placeholder="请输入缓存 Key" />
        </el-form-item>
        <el-form-item label="Value" required>
          <el-input v-model="form.value" type="textarea" :rows="3" placeholder="请输入缓存 Value" />
        </el-form-item>
        <el-form-item label="TTL（秒）">
          <el-input-number
            v-model="form.ttl"
            :min="-1"
            :controls="false"
            placeholder="-1 为永不过期"
            style="width: 200px;"
          />
          <span class="ttl-hint">-1 表示永不过期，留空则使用默认值</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="dialogLoading" @click="handleSave">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.cache {
  display: flex;
  flex-direction: column;
  min-height: calc(100vh - 120px);
}

.top {
  background-color: var(--color-white, #fff);
  padding: 18px;
  margin: 8px 24px;
  margin-bottom: 0;
}

.body {
  background-color: var(--color-white, #fff);
  padding: 18px;
  margin: 10px 24px 0 24px;
  flex: 1;
  display: flex;
  flex-direction: column;
}

.function {
  text-align: right;
  margin-bottom: 16px;
}

.max-width-300 {
  max-width: 300px;
}

.ttl-hint {
  margin-left: 8px;
  font-size: 12px;
  color: var(--color-text-secondary, #909399);
}
</style>
