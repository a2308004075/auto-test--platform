<!--
 @author HXN
 @date 2026-08-22 13:28
 @description 字典管理视图
-->
<script setup lang="ts">
/**
 * 字典管理页面（仅 ADMIN）
 * 搜索 + 分页表格 + 新增/编辑弹窗 + 批量删除
 * 对标 svc-manager-web Dictionary.vue
 */
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import FlexQueryForm from '@/components/FlexQueryForm/index.vue'
import TableFit from '@/components/TableFit/index.vue'
import {
  getDictPage,
  addDict,
  updateDict,
  batchDeleteDict,
  type DictListItem,
  type DictCreateRequest,
} from '@/api/dict'

// ===== 列表数据 =====
const loading = ref(false)
const dictList = ref<DictListItem[]>([])
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const selectionList = ref<DictListItem[]>([])

// 搜索条件
const searchType = ref('')
const searchTypeName = ref('')

// ===== 弹窗 =====
const dialogVisible = ref(false)
const isEdit = ref(false)
const editingId = ref<number | null>(null)
const dialogLoading = ref(false)

const form = reactive<DictCreateRequest>({
  dictType: '',
  dictTypeName: '',
  dictValue: '',
  dictValueName: '',
  sortNo: 0,
  remark: '',
})

// ===== 加载列表 =====
async function fetchList() {
  loading.value = true
  try {
    const res: any = await getDictPage({
      dictType: searchType.value || undefined,
      dictTypeName: searchTypeName.value || undefined,
      page: pagination.current,
      pageSize: pagination.pageSize,
    })
    dictList.value = res.data?.items || []
    pagination.total = res.data?.total || 0
  } catch {
    dictList.value = []
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.current = 1
  fetchList()
}

function handlePageChange(p: number) {
  pagination.current = p
  fetchList()
}

function handleSizeChange(size: number) {
  pagination.pageSize = size
  pagination.current = 1
  fetchList()
}

function handleSelectionChange(rows: DictListItem[]) {
  selectionList.value = rows
}

// ===== 新增 =====
function openAdd() {
  isEdit.value = false
  editingId.value = null
  form.dictType = ''
  form.dictTypeName = ''
  form.dictValue = ''
  form.dictValueName = ''
  form.sortNo = 0
  form.remark = ''
  dialogVisible.value = true
}

// ===== 编辑 =====
function openEdit(row: DictListItem) {
  isEdit.value = true
  editingId.value = row.id
  form.dictType = row.dictType
  form.dictTypeName = row.dictTypeName
  form.dictValue = row.dictValue
  form.dictValueName = row.dictValueName
  form.sortNo = row.sortNo
  form.remark = row.remark || ''
  dialogVisible.value = true
}

// ===== 保存 =====
async function handleSave() {
  if (
    !form.dictType.trim() ||
    !form.dictTypeName.trim() ||
    !form.dictValue.trim() ||
    !form.dictValueName.trim()
  ) {
    ElMessage.warning('请填写完整字典信息')
    return
  }
  dialogLoading.value = true
  try {
    if (isEdit.value && editingId.value !== null) {
      await updateDict(editingId.value, { ...form })
      ElMessage.success('字典更新成功')
    } else {
      await addDict({ ...form })
      ElMessage.success('字典新增成功')
    }
    dialogVisible.value = false
    fetchList()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '操作失败')
  } finally {
    dialogLoading.value = false
  }
}

// ===== 批量删除 =====
async function handleBatchDelete() {
  if (!selectionList.value.length) return
  try {
    await ElMessageBox.confirm(
      `确定删除选中的 ${selectionList.value.length} 条字典记录？`,
      '批量删除确认',
      { confirmButtonText: '确定删除', cancelButtonText: '取消', type: 'warning' },
    )
  } catch {
    return
  }
  try {
    await batchDeleteDict(selectionList.value.map((r) => r.id))
    ElMessage.success('删除成功')
    pagination.current = 1
    fetchList()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '删除失败')
  }
}

// ===== 重置搜索 =====
function handleReset() {
  searchType.value = ''
  searchTypeName.value = ''
  pagination.current = 1
  fetchList()
}

onMounted(() => {
  fetchList()
})
</script>

<template>
  <div class="dictionary">
    <!-- 查询区 -->
    <div class="form">
      <el-form inline>
        <FlexQueryForm>
          <el-form-item label="字典名称">
            <el-input
              v-model="searchType"
              clearable
              placeholder="请输入字典名称"
              style="width: 180px;"
              @keyup.enter="handleSearch"
            />
          </el-form-item>
          <el-form-item label="字典描述">
            <el-input
              v-model="searchTypeName"
              clearable
              placeholder="请输入字典描述"
              style="width: 180px;"
              @keyup.enter="handleSearch"
            />
          </el-form-item>
          <template #button>
            <el-button type="primary" @click="handleSearch">搜索</el-button>
            <el-button @click="handleReset">重置</el-button>
          </template>
        </FlexQueryForm>
      </el-form>
    </div>

    <!-- 表格区 -->
    <div class="table-card">
      <div class="button-list">
        <el-button type="primary" @click="openAdd">新增字典</el-button>
        <el-button type="danger" :disabled="!selectionList.length" @click="handleBatchDelete">
          批量删除
        </el-button>
      </div>

      <TableFit>
        <template #default="{ maxHeight }">
          <el-table
            v-loading="loading"
            :data="dictList"
            stripe
            :max-height="maxHeight"
            style="width: 100%;"
            @selection-change="handleSelectionChange"
          >
            <el-table-column type="selection" width="45" />
            <el-table-column prop="dictType" label="字典名称" min-width="120" />
            <el-table-column prop="dictTypeName" label="字典描述" min-width="120" />
            <el-table-column prop="dictValue" label="字典键值" min-width="100" />
            <el-table-column prop="dictValueName" label="字典键值描述" min-width="120" />
            <el-table-column label="操作" width="80" align="right" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
              </template>
            </el-table-column>
          </el-table>
        </template>
      </TableFit>

      <div class="pagination">
        <el-pagination
          v-model:current-page="pagination.current"
          v-model:page-size="pagination.pageSize"
          :total="pagination.total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </div>

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      v-drag-dialog
      :title="isEdit ? '编辑字典' : '新增字典'"
      :close-on-click-modal="false"
    >
      <el-form :model="form" label-width="auto">
        <el-form-item label="字典名称" required>
          <el-input v-model="form.dictType" placeholder="如：project_status" class="input" />
        </el-form-item>
        <el-form-item label="字典描述" required>
          <el-input v-model="form.dictTypeName" placeholder="如：项目状态" class="input" />
        </el-form-item>
        <el-form-item label="字典键值" required>
          <el-input v-model="form.dictValue" placeholder="如：active" class="input" />
        </el-form-item>
        <el-form-item label="字典键值描述" required>
          <el-input v-model="form.dictValueName" placeholder="如：启用" class="input" />
        </el-form-item>
        <el-form-item label="排序号">
          <el-input-number v-model="form.sortNo" :min="0" :controls="false" style="width: 120px;" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="备注（可选）" class="input" />
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
.dictionary {
  display: flex;
  flex-direction: column;
  min-height: calc(100vh - 120px);
}

.form {
  background-color: var(--color-white, #fff);
  padding: 18px;
  margin: 8px 24px;
  margin-bottom: 0;
}

.table-card {
  background-color: var(--color-white, #fff);
  padding: 18px;
  margin: 10px 24px 0 24px;
  flex: 1;
  display: flex;
  flex-direction: column;
}

.button-list {
  text-align: right;
  margin-bottom: 16px;
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.input {
  max-width: 480px;
}

.pagination {
  margin-top: 16px;
  text-align: right;
}
</style>
