<!--
 @author HXN
 @date 2026-09-01
 @description Action 关键字新建视图
-->
<script setup lang="ts">
/**
 * Action 关键字新建 - M7
 * 独立新建页：填写名称/描述/所属分组后创建，成功后进入编辑器继续配置 I/O 参数与节点编排
 * 对齐关键字/工具方法模块的新建交互（列表页跳转 new 路由）
 */
defineOptions({ name: 'ActionCreate' })
import { ref, reactive, onMounted, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { createAction, getActionGroups } from '@/api/action'
import { usePermission } from '@/composables/usePermission'
import EditPageHeader from '@/components/EditPageHeader/index.vue'

const route = useRoute()
const router = useRouter()
const { hasPermission } = usePermission()
const projectId = computed(() => Number(route.params.id))

// ===== 表单 =====
const formRef = ref<FormInstance>()
const form = reactive({
  name: '',
  description: '',
  groupId: null as number | null,
})
const rules = reactive<FormRules>({
  name: [
    { required: true, message: '请输入 Action 名称', trigger: 'blur' },
    { max: 100, message: '名称长度不能超过 100 个字符', trigger: 'blur' },
  ],
  description: [
    { max: 500, message: '描述长度不能超过 500 个字符', trigger: 'blur' },
  ],
})

// ===== 分组树（仅用户分组，系统分组不参与选择） =====
const groups = ref<any[]>([])
const groupTreeOptions = computed(() => {
  const userGrps = groups.value.filter((g) => g.isSystem !== 1)
  const buildTree = (parentId: number | null): any[] =>
    userGrps
      .filter((g) => (g.parentId ?? null) === parentId)
      .map((g) => ({ id: g.id, name: g.name, children: buildTree(g.id) }))
  return buildTree(null)
})

async function fetchGroups() {
  if (!projectId.value) return
  try {
    const res: any = await getActionGroups(projectId.value)
    groups.value = res.data || []
  } catch { groups.value = [] }
}

// 列表页跳转时若选中了用户分组，通过 query.groupId 带入作为默认分组
function applyQueryGroup() {
  const qid = Number(route.query.groupId)
  form.groupId = qid && groups.value.some((g) => g.id === qid && g.isSystem !== 1) ? qid : null
}

// ===== 提交 =====
const saving = ref(false)
function handleSubmit() {
  formRef.value?.validate(async (valid) => {
    if (!valid) return
    saving.value = true
    try {
      const res: any = await createAction(projectId.value, {
        projectId: projectId.value,
        name: form.name.trim(),
        description: form.description,
        nodes: [],
        groupId: form.groupId || undefined,
      })
      ElMessage.success('创建成功')
      router.push(`/project/${projectId.value}/actions/${res.data.id}/edit`)
    } catch (e: any) {
      ElMessage.error(e?.response?.data?.message || '创建失败')
    } finally {
      saving.value = false
    }
  })
}

function handleCancel() {
  router.push(`/project/${projectId.value}/actions`)
}

// ===== 页面初始化（重复进入时重置表单，避免 KeepAlive 残留） =====
function initPage() {
  formRef.value?.clearValidate()
  Object.assign(form, { name: '', description: '', groupId: null })
  fetchGroups().then(applyQueryGroup)
}

watch(() => route.fullPath, () => {
  if (route.path.endsWith('/actions/new')) initPage()
})

onMounted(initPage)
</script>

<template>
  <div class="action-create">
    <EditPageHeader title="新建 Action关键字">
      <el-button
        v-if="hasPermission('project:action:add')"
        type="primary"
        :loading="saving"
        @click="handleSubmit"
      >
        保存
      </el-button>
      <el-button @click="handleCancel">取消</el-button>
    </EditPageHeader>

    <div class="create-form">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" style="max-width: 800px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="名称" prop="name">
              <el-input v-model="form.name" placeholder="请输入 Action 名称" maxlength="100" show-word-limit />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="所属分组" prop="groupId">
              <el-tree-select
                v-model="form.groupId"
                :data="groupTreeOptions"
                :props="{ label: 'name', value: 'id', children: 'children' }"
                check-strictly
                clearable
                placeholder="不选则归入未分组"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="描述" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            placeholder="请输入描述"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
        <div class="form-tip">
          创建后将进入编辑器，可在其中配置 I/O 参数与节点编排。
        </div>
      </el-form>
    </div>
  </div>
</template>

<style scoped>
.create-form {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  padding: 20px 24px;
}
.form-tip {
  font-size: 12px;
  color: #909399;
  line-height: 1.6;
}
</style>
