<script setup lang="ts">
/**
 * 登录弹窗组件
 * 包含验证码图片和记住密码功能
 */
import { ref, reactive, watch } from 'vue'
import { message } from 'ant-design-vue'
import { login, getCaptcha } from '@/api/auth'
import { useUserStore } from '@/stores/user'

const props = defineProps<{ open: boolean }>()
const emit = defineEmits<{
  (e: 'update:open', value: boolean): void
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
    message.error('获取验证码失败，请刷新重试')
  } finally {
    captchaLoading.value = false
  }
}

/**
 * 打开弹窗时初始化
 */
watch(() => props.open, (val) => {
  if (val) {
    // 加载记住的凭据
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
    message.warning('请输入用户名和密码')
    return
  }
  if (!form.captchaCode) {
    message.warning('请输入验证码')
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
    // 记住密码
    if (form.rememberPassword) {
      userStore.saveRememberedCredentials(form.username, form.password)
    } else {
      userStore.clearRememberedCredentials()
    }
    message.success('登录成功')
    emit('update:open', false)
    emit('success')
  } catch (e: any) {
    message.error(e?.response?.data?.message || '登录失败')
    // 登录失败后刷新验证码
    loadCaptcha()
    form.captchaCode = ''
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <a-modal
    :open="open"
    @update:open="(val: boolean) => emit('update:open', val)"
    :footer="null"
    :width="420"
    :mask-closable="false"
    centered
  >
    <div class="login-modal">
      <div class="login-header">
        <h2>项目管理平台</h2>
        <p>请登录您的账户</p>
      </div>
      <a-form layout="vertical" @finish="handleLogin">
        <a-form-item label="用户名" required>
          <a-input
            v-model:value="form.username"
            placeholder="请输入用户名"
            size="large"
            allow-clear
          />
        </a-form-item>
        <a-form-item label="密码" required>
          <a-input-password
            v-model:value="form.password"
            placeholder="请输入密码"
            size="large"
            @press-enter="handleLogin"
          />
        </a-form-item>
        <a-form-item label="验证码" required>
          <div style="display:flex;gap:8px;align-items:center">
            <a-input
              v-model:value="form.captchaCode"
              placeholder="请输入验证码"
              size="large"
              style="flex:1"
              @press-enter="handleLogin"
            />
            <div
              class="captcha-image"
              @click="loadCaptcha"
              :title="captchaLoading ? '加载中...' : '点击刷新验证码'"
            >
              <a-spin v-if="captchaLoading" size="small" />
              <img v-else-if="captchaImage" :src="captchaImage" alt="验证码" style="height:40px;cursor:pointer;border-radius:4px" />
              <div v-else style="height:40px;line-height:40px;color:#999;font-size:12px;text-align:center;width:120px;background:#f5f5f5;border-radius:4px">点击加载</div>
            </div>
          </div>
        </a-form-item>
        <a-form-item>
          <div style="display:flex;justify-content:space-between;align-items:center">
            <a-checkbox v-model:checked="form.rememberPassword">记录密码</a-checkbox>
            <a-button
              type="primary"
              size="large"
              :loading="loading"
              @click="handleLogin"
            >
              登录
            </a-button>
          </div>
        </a-form-item>
      </a-form>
      <div class="login-footer">
        <span>默认账户: admin / admin123</span>
      </div>
    </div>
  </a-modal>
</template>

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
  color: #333;
}
.login-header p {
  margin: 0;
  color: #999;
}
.login-footer {
  text-align: center;
  color: #999;
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
