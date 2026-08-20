<script setup lang="ts">
/**
 * 项目列表页 - M2
 */
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getProjects, createProject, updateProject, deleteProject } from '@/api/project'
import { useProjectStore, useUserStore } from '@/stores'

const router = useRouter()
const projectStore = useProjectStore()
const userStore = useUserStore()

const loading = ref(false)
const list = ref<any[]>([])
const keyword = ref('')
const pagination = reactive({ current: 1, pageSize: 12, total: 0 })
const modalVisible = ref(false)
const editingId = ref<number>(0)
const form = reactive({ name: '', description: '' })

async function fetchList() {
  loading.value = true
  try {
    const res: any = await getProjects({ keyword: keyword.value, page: pagination.current, pageSize: pagination.pageSize })
    list.value = res.data?.items || []
    pagination.total = res.data?.total || 0
  } catch { list.value = [] } finally { loading.value = false }
}

function enterProject(project: any) {
  if (!userStore.isLoggedIn) {
    ElMessage.info('请先登录后再进入项目')
    return
  }
  projectStore.setCurrentProject(project.id, project.name)
  router.push(`/project/${project.id}/dashboard`)
}

function openCreate() {
  if (!userStore.isLoggedIn) {
    ElMessage.info('请先登录后再创建项目')
    return
  }
  editingId.value = 0
  form.name = ''
  form.description = ''
  modalVisible.value = true
}

function openEdit(project: any) {
  editingId.value = project.id
  form.name = project.name
  form.description = project.description || ''
  modalVisible.value = true
}

async function handleSubmit() {
  if (!form.name) { ElMessage.warning('请输入项目名称'); return }
  try {
    if (editingId.value) {
      await updateProject(editingId.value, form)
      ElMessage.success('更新成功')
    } else {
      await createProject(form)
      ElMessage.success('创建成功')
    }
    modalVisible.value = false
    fetchList()
  } catch (e: any) { ElMessage.error(e?.response?.data?.message || '操作失败') }
}

function handleDelete(project: any) {
  ElMessageBox.confirm(
    `确定删除项目「${project.name}」？此操作不可恢复。`,
    '确认删除',
    { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
  ).then(async () => {
    await deleteProject(project.id)
    ElMessage.success('删除成功')
    fetchList()
  }).catch(() => {})
}

function handleSearch() { pagination.current = 1; fetchList() }
onMounted(fetchList)
</script>

<template>
  <div>
    <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:20px">
      <h2 style="margin:0">项目管理</h2>
      <div style="display:flex;gap:8px">
        <el-input
          v-model="keyword"
          placeholder="搜索项目"
          style="width:240px"
          clearable
          @keyup.enter="handleSearch"
        >
          <template #append>
            <el-button @click="handleSearch">搜索</el-button>
          </template>
        </el-input>
        <el-button v-if="userStore.isLoggedIn" type="primary" @click="openCreate">新建项目</el-button>
      </div>
    </div>

    <div v-loading="loading">
      <el-row :gutter="16">
        <el-col :span="6" v-for="project in list" :key="project.id" style="margin-bottom:16px">
          <el-card shadow="hover" class="project-card" @click="enterProject(project)">
            <template #header>
              <div style="display:flex;justify-content:space-between;align-items:center">
                <span style="font-weight:500">{{ project.name }}</span>
                <el-tag :type="project.isActive === 1 ? 'success' : 'info'" size="small">
                  {{ project.isActive === 1 ? '启用' : '停用' }}
                </el-tag>
              </div>
            </template>
            <div class="card-actions" v-if="userStore.isLoggedIn" @click.stop>
              <el-dropdown trigger="click">
                <span class="el-dropdown-link" @click.stop>···</span>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item @click.stop="openEdit(project)">编辑</el-dropdown-item>
                    <el-dropdown-item @click.stop="handleDelete(project)" divided style="color:#f56c6c">删除</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
            <p style="color:#606266;min-height:40px;margin:0">{{ project.description || '暂无描述' }}</p>
            <div style="color:#909399;font-size:12px;margin-top:12px">
              创建: {{ project.createdAt?.substring(0, 10) }}
            </div>
          </el-card>
        </el-col>
      </el-row>
      <el-empty v-if="!list.length && !loading" :description="`暂无项目${userStore.isLoggedIn ? '，点击「新建项目」开始' : ''}`" />
    </div>

    <div style="text-align:right;margin-top:16px" v-if="pagination.total > pagination.pageSize">
      <el-pagination
        v-model:current-page="pagination.current"
        :page-size="pagination.pageSize"
        :total="pagination.total"
        layout="prev, pager, next"
        @current-change="fetchList"
      />
    </div>

    <!-- 新建/编辑弹窗 -->
    <el-dialog
      v-model="modalVisible"
      :title="editingId ? '编辑项目' : '新建项目'"
      width="500px"
    >
      <el-form label-position="top" style="margin-top:8px">
        <el-form-item label="项目名称">
          <el-input v-model="form.name" placeholder="请输入项目名称" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="项目描述（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="modalVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.project-card {
  cursor: pointer;
  transition: transform 0.2s;
  position: relative;
}
.project-card:hover {
  transform: translateY(-2px);
}
.card-actions {
  position: absolute;
  top: 12px;
  right: 12px;
}
.el-dropdown-link {
  cursor: pointer;
  font-size: 18px;
  color: #909399;
  padding: 4px 8px;
}
.el-dropdown-link:hover {
  color: #409eff;
}
</style>
