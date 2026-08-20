<script setup lang="ts">
/**
 * 项目列表页 - M2
 */
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import { getProjects, createProject, updateProject, deleteProject } from '@/api/project'
import { useProjectStore } from '@/stores/project'

const router = useRouter()
const projectStore = useProjectStore()

const loading = ref(false)
const list = ref<any[]>([])
const keyword = ref('')
const pagination = reactive({ current: 1, pageSize: 12, total: 0 })
const modalVisible = ref(false)
const editingId = ref('')
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
  projectStore.setCurrentProject(project.id, project.name)
  router.push(`/project/${project.id}/dashboard`)
}

function openCreate() {
  editingId.value = ''
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
  if (!form.name) { message.warning('请输入项目名称'); return }
  try {
    if (editingId.value) {
      await updateProject(editingId.value, form)
      message.success('更新成功')
    } else {
      await createProject(form)
      message.success('创建成功')
    }
    modalVisible.value = false
    fetchList()
  } catch (e: any) { message.error(e?.response?.data?.message || '操作失败') }
}

function handleDelete(project: any) {
  Modal.confirm({
    title: '确认删除',
    content: `确定删除项目「${project.name}」？此操作不可恢复。`,
    okType: 'danger',
    onOk: async () => {
      await deleteProject(project.id)
      message.success('删除成功')
      fetchList()
    },
  })
}

function handleSearch() { pagination.current = 1; fetchList() }
onMounted(fetchList)
</script>

<template>
  <div>
    <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:24px">
      <h2 style="margin:0">项目管理</h2>
      <div style="display:flex;gap:8px">
        <a-input-search
          v-model:value="keyword"
          placeholder="搜索项目"
          style="width:240px"
          allow-clear
          @search="handleSearch"
        />
        <a-button type="primary" @click="openCreate">新建项目</a-button>
      </div>
    </div>

    <a-spin :spinning="loading">
      <a-row :gutter="[16, 16]">
        <a-col :span="6" v-for="project in list" :key="project.id">
          <a-card hoverable @click="enterProject(project)">
            <template #title>
              <div style="display:flex;justify-content:space-between;align-items:center">
                <span>{{ project.name }}</span>
                <a-tag :color="project.isActive === 1 ? 'green' : 'default'">
                  {{ project.isActive === 1 ? '启用' : '停用' }}
                </a-tag>
              </div>
            </template>
            <template #extra>
              <a-dropdown @click.stop>
                <a @click.stop>···</a>
                <template #overlay>
                  <a-menu>
                    <a-menu-item @click.stop="openEdit(project)">编辑</a-menu-item>
                    <a-menu-item @click.stop="handleDelete(project)" danger>删除</a-menu-item>
                  </a-menu>
                </template>
              </a-dropdown>
            </template>
            <p style="color:#666;min-height:40px">{{ project.description || '暂无描述' }}</p>
            <div style="color:#999;font-size:12px">
              创建: {{ project.createdAt?.substring(0, 10) }}
            </div>
          </a-card>
        </a-col>
      </a-row>
      <div v-if="!list.length && !loading" style="text-align:center;padding:60px;color:#999">
        暂无项目，点击「新建项目」开始
      </div>
    </a-spin>

    <div style="text-align:right;margin-top:16px" v-if="pagination.total > pagination.pageSize">
      <a-pagination
        v-model:current="pagination.current"
        :total="pagination.total"
        :page-size="pagination.pageSize"
        @change="fetchList"
      />
    </div>

    <!-- 新建/编辑弹窗 -->
    <a-modal
      v-model:open="modalVisible"
      :title="editingId ? '编辑项目' : '新建项目'"
      @ok="handleSubmit"
    >
      <a-form layout="vertical" style="margin-top:16px">
        <a-form-item label="项目名称" required>
          <a-input v-model:value="form.name" placeholder="请输入项目名称" />
        </a-form-item>
        <a-form-item label="描述">
          <a-textarea v-model:value="form.description" :rows="3" placeholder="项目描述（可选）" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>
