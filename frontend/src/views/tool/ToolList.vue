<!--
 @author HXN
 @date 2026-08-20 15:34
 @description 工具方法列表视图
-->
<script setup lang="ts">
/**
 * 工具方法列表 - M6
 */
import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getTools, createTool, updateTool, deleteTool, testTool } from '@/api/tool'

const route = useRoute()
const projectId = computed(() => Number(route.params.id))

const loading = ref(false)
const list = ref<any[]>([])
const category = ref('')
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const modalVisible = ref(false)
const testVisible = ref(false)
const testResult = ref<any>(null)
const testLoading = ref(false)
const editingId = ref<number>(0)
const form = reactive({ name: '', category: 'CUSTOM', description: '', code: 'return "Hello"', returnType: 'String', paramDefinitions: '[]' })
const testInput = ref('{}')
const currentTestId = ref<number>(0)

async function fetchList() {
  loading.value = true
  try {
    const res: any = await getTools(projectId.value, {
      category: category.value || undefined, page: pagination.current, pageSize: pagination.pageSize,
    })
    list.value = res.data?.items || []
    pagination.total = res.data?.total || 0
  } catch { list.value = [] } finally { loading.value = false }
}

function openCreate() {
  editingId.value = 0
  Object.assign(form, { name: '', category: 'CUSTOM', description: '', code: 'return "Hello"', returnType: 'String', paramDefinitions: '[]' })
  modalVisible.value = true
}

function openEdit(record: any) {
  editingId.value = record.id
  Object.assign(form, { name: record.name, category: record.category, description: record.description || '', code: record.code || '', returnType: record.returnType || '', paramDefinitions: record.paramDefinitions || '[]' })
  modalVisible.value = true
}

async function handleSubmit() {
  if (!form.name || !form.code) { ElMessage.warning('请填写必填项'); return }
  try {
    if (editingId.value) {
      await updateTool(projectId.value, editingId.value, form)
      ElMessage.success('更新成功')
    } else {
      await createTool(projectId.value, { ...form, projectId: projectId.value })
      ElMessage.success('创建成功')
    }
    modalVisible.value = false; fetchList()
  } catch (e: any) { ElMessage.error(e?.response?.data?.message || '操作失败') }
}

function openTest(record: any) {
  currentTestId.value = record.id
  testInput.value = record.testInput || '{}'
  testResult.value = null
  testVisible.value = true
}

async function handleTest() {
  testLoading.value = true
  try {
    const res: any = await testTool(projectId.value, currentTestId.value, { testInput: testInput.value })
    testResult.value = res.data
  } catch (e: any) { testResult.value = { success: 0, error: e?.message } } finally { testLoading.value = false }
}

function handleDelete(record: any) {
  ElMessageBox.confirm(`确定删除工具「${record.name}」？`, '确认删除', { type: 'warning' })
    .then(async () => { await deleteTool(projectId.value, record.id); ElMessage.success('删除成功'); fetchList() })
    .catch(() => {})
}

onMounted(fetchList)
</script>

<template>
  <div>
    <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:16px">
      <h2>工具方法</h2>
      <div style="display:flex;gap:8px">
        <el-radio-group v-model="category" size="default" @change="fetchList">
          <el-radio-button value="">全部</el-radio-button>
          <el-radio-button value="BUILTIN">内置</el-radio-button>
          <el-radio-button value="CUSTOM">自定义</el-radio-button>
        </el-radio-group>
        <el-button type="primary" @click="openCreate">新建工具</el-button>
      </div>
    </div>
    <el-table v-loading="loading" :data="list" row-key="id" border style="width:100%">
      <el-table-column prop="name" label="名称" width="200" />
      <el-table-column label="分类" width="100">
        <template #default="{ row }">
          <el-tag :type="row.category === 'BUILTIN' ? '' : 'success'" size="small">{{ row.category === 'BUILTIN' ? '内置' : '自定义' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="returnType" label="返回类型" width="100" />
      <el-table-column prop="description" label="描述" show-overflow-tooltip />
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link size="small" @click="openTest(row)">测试</el-button>
          <el-button type="primary" link size="small" @click="openEdit(row)">编辑</el-button>
          <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <div style="display:flex;justify-content:flex-end;margin-top:16px">
      <el-pagination background layout="total, prev, pager, next" :total="pagination.total"
        :page-size="pagination.pageSize" :current-page="pagination.current"
        @current-change="(p: number) => { pagination.current = p; fetchList() }" />
    </div>

    <!-- 新建/编辑弹窗 -->
    <el-dialog v-model="modalVisible" :title="editingId ? '编辑工具' : '新建工具'" width="640px">
      <el-form label-position="top">
        <el-form-item label="名称" required>
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" />
        </el-form-item>
        <el-form-item label="Groovy 代码" required>
          <el-input v-model="form.code" type="textarea" :rows="8" style="font-family:monospace" />
        </el-form-item>
        <el-form-item label="返回类型">
          <el-input v-model="form.returnType" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="modalVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 测试弹窗 -->
    <el-dialog v-model="testVisible" title="在线测试" width="560px">
      <el-form-item label="输入参数 (JSON)">
        <el-input v-model="testInput" type="textarea" :rows="4" style="font-family:monospace" />
      </el-form-item>
      <div v-if="testResult" style="margin-top:12px">
        <el-alert :type="testResult.success === 1 ? 'success' : 'error'" :title="testResult.success === 1 ? '执行成功' : '执行失败'"
          :description="testResult.output || testResult.error" show-icon :closable="false" />
        <div v-if="testResult.executionTimeMs" style="color:#909399;margin-top:4px">耗时: {{ testResult.executionTimeMs }}ms</div>
      </div>
      <template #footer>
        <el-button @click="testVisible = false">关闭</el-button>
        <el-button type="primary" :loading="testLoading" @click="handleTest">执行</el-button>
      </template>
    </el-dialog>
  </div>
</template>
