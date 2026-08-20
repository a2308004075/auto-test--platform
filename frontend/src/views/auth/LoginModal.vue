<script setup lang="ts">
/**
 * 登录弹窗组件
 * 包含验证码图片和记住密码功能
 */
import { ref, reactive, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { login, getCaptcha } from '@/api/auth'
import { useUserStore } from '@/stores'

const props = defineProps<{ modelValue: boolean }>()
const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'success'): void
}>()

const userStore = useUserStore()

const loading = ref(false)
const captchaLoading = ref(false)
const captchaImage = ref('')
const form = reactive({
  username: '',
  password: '',
  captchaId: '',
  captchaCode: '',
  rememberPassword: false,
})

/**
 * 加载验证码图片
 */
async function loadCaptcha() {
  captchaLoading.value = true
  try {
    const res: any = await getCaptcha()
    captchaImage.value = res.data?.image || ''
    form.captchaId = res.data?.captchaId || ''
  } catch {
    ElMessage.error('获取验证码失败，请刷新重试')
  } finally {
    captchaLoading.value = false
  }
}

/**
 * 打开弹窗时初始化
 */
watch(() => props.modelValue, (val) => {
  if (val) {
    const remembered = userStore.loadRememberedCredentials()
    if (remembered) {
      form.username = remembered.username
      form.password = remembered.password
      form.rememberPassword = true
    } else {
      form.username = ''
      form.password = ''
      form.rememberPassword = false
    }
    form.captchaCode = ''
    loadCaptcha()
  }
})

/**
 * 提交登录
 */
async function handleLogin() {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  if (!form.captchaCode) {
    ElMessage.warning('请输入验证码')
    return
  }
  loading.value = true
  try {
    const res: any = await login({
      username: form.username,
      password: form.password,
      captchaId: form.captchaId,
      captchaCode: form.captchaCode,
    })
    const data = res.data
    userStore.setToken(data.accessToken)
    if (data.refreshToken) userStore.setRefreshToken(data.refreshToken)
    if (data.user) {
      userStore.setUserInfo({
        id: data.user.id,
        username: data.user.username,
        role: data.user.role,
      })
    }
    if (form.rememberPassword) {
      userStore.saveRememberedCredentials(form.username, form.password)
    } else {
      userStore.clearRememberedCredentials()
    }
    ElMessage.success('登录成功')
    emit('update:modelValue', false)
    emit('success')
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '登录失败')
    loadCaptcha()
    form.captchaCode = ''
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <el-dialog
    :model-value="modelValue"
    @update:model-value="(val: boolean) => emit('update:modelValue', val)"
    :show-close="true"
    width="420px"
    :close-on-click-modal="false"
    align-center
  >
    <div class="login-modal">
      <div class="login-header">
        <h2>项目管理平台</h2>
        <p>请登录您的账户</p>
      </div>
      <el-form label-position="top" @submit.prevent="handleLogin">
        <el-form-item label="用户名">
          <el-input
            v-model="form.username"
            placeholder="请输入用户名"
            size="large"
            clearable
          />
        </el-form-item>
        <el-form-item label="密码">
          <el-input
            v-model="form.password"
            type="password"
            show-password
            placeholder="请输入密码"
            size="large"
            @keyup.enter="handleLogin"
          />
        </el-form-item>
        <el-form-item label="验证码">
          <div style="display:flex;gap:8px;align-items:center;width:100%">
            <el-input
              v-model="form.captchaCode"
              placeholder="请输入验证码"
              size="large"
              style="flex:1"
              @keyup.enter="handleLogin"
            />
            <div
              class="captcha-image"
              @click="loadCaptcha"
              :title="captchaLoading ? '加载中...' : '点击刷新验证码'"
            >
              <el-icon v-if="captchaLoading" class="is-loading" :size="20"><Loading /></el-icon>
              <img v-else-if="captchaImage" :src="captchaImage" alt="验证码" style="height:40px;cursor:pointer;border-radius:4px" />
              <div v-else style="height:40px;line-height:40px;color:#909399;font-size:12px;text-align:center;width:120px;background:#f5f7fa;border-radius:4px">点击加载</div>
            </div>
          </div>
        </el-form-item>
        <el-form-item>
          <div style="display:flex;justify-content:space-between;align-items:center;width:100%">
            <el-checkbox v-model="form.rememberPassword">记住密码</el-checkbox>
            <el-button
              type="primary"
              size="large"
              :loading="loading"
              @click="handleLogin"
            >
              登录
            </el-button>
          </div>
        </el-form-item>
      </el-form>
      <div class="login-footer">
        <span>默认账户: admin / admin123</span>
      </div>
    </div>
  </el-dialog>
</template>

<script lang="ts">
import { Loading } from '@element-plus/icons-vue'
export default { components: { Loading } }
</script>

<style scoped>
.login-modal {
  padding: 8px 0;
}
.login-header {
  text-align: center;
  margin-bottom: 24px;
}
.login-header h2 {
  margin: 0 0 8px;
  color: #303133;
}
.login-header p {
  margin: 0;
  color: #909399;
}
.login-footer {
  text-align: center;
  color: #909399;
  font-size: 12px;
  margin-top: 8px;
}
.captcha-image {
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 120px;
}
</style>
