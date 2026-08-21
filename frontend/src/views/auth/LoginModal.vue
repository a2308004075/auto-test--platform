<script setup lang="ts">
/**
 * 登录弹窗组件
 * 包含验证码图片
 */
import { ref, reactive, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { User, Lock, View, Hide, CircleCheck, Loading } from '@element-plus/icons-vue'
import { login, getCaptcha } from '@/api/auth'
import { useUserStore } from '@/stores'

const props = defineProps<{ modelValue: boolean }>()
const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'success'): void
}>()

const userStore = useUserStore()

const loading = ref(false)
const showPassword = ref(false)
const captchaLoading = ref(false)
const captchaImage = ref('')
const form = reactive({
  username: '',
  password: '',
  captchaId: '',
  captchaCode: '',
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
    form.username = ''
    form.password = ''
    form.captchaCode = ''
    showPassword.value = false
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
    class="login-dialog"
  >
    <div class="login-modal">
      <h2 class="login-title">账号登录</h2>
      <el-form @submit.prevent="handleLogin">
        <el-form-item>
          <el-input
            v-model="form.username"
            placeholder="请输入账号"
            size="large"
            clearable
          >
            <template #prefix>
              <el-icon><User /></el-icon>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item>
          <el-input
            v-model="form.password"
            :type="showPassword ? 'text' : 'password'"
            placeholder="请输入密码"
            size="large"
            @keyup.enter="handleLogin"
          >
            <template #prefix>
              <el-icon><Lock /></el-icon>
            </template>
            <template #suffix>
              <el-icon class="eye-icon" @click.stop="showPassword = !showPassword">
                <View v-if="showPassword" />
                <Hide v-else />
              </el-icon>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item>
          <div class="captcha-row">
            <el-input
              v-model="form.captchaCode"
              placeholder="请输入安全验证码"
              size="large"
              class="captcha-input"
              @keyup.enter="handleLogin"
            >
              <template #prefix>
                <el-icon><CircleCheck /></el-icon>
              </template>
            </el-input>
            <div
              class="captcha-image"
              @click="loadCaptcha"
              :title="captchaLoading ? '加载中...' : '点击刷新验证码'"
            >
              <el-icon v-if="captchaLoading" class="is-loading" :size="20"><Loading /></el-icon>
              <img v-else-if="captchaImage" :src="captchaImage" alt="验证码" />
              <div v-else class="captcha-placeholder">点击加载</div>
            </div>
          </div>
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            size="large"
            :loading="loading"
            style="width: 100%"
            @click="handleLogin"
          >
            登录
          </el-button>
        </el-form-item>
      </el-form>
    </div>
  </el-dialog>
</template>

<style scoped>
.login-modal {
  padding: 8px 0;
}
.login-title {
  margin: 0 0 32px;
  text-align: left;
  color: #303133;
  font-size: 24px;
  font-weight: 500;
}
.eye-icon {
  cursor: pointer;
  color: #909399;
}
.captcha-row {
  display: flex;
  gap: 8px;
  align-items: center;
  width: 100%;
}
.captcha-input {
  flex: 1;
}
.captcha-image {
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 120px;
  height: 40px;
  cursor: pointer;
  border-radius: 4px;
  overflow: hidden;
}
.captcha-image img {
  height: 40px;
  border-radius: 4px;
}
.captcha-placeholder {
  height: 40px;
  line-height: 40px;
  color: #909399;
  font-size: 12px;
  text-align: center;
  width: 120px;
  background: #f5f7fa;
  border-radius: 4px;
}
</style>
