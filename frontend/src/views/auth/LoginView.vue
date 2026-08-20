<script setup lang="ts">
/**
 * 登录页 - M1
 */
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { login } from '@/api/auth'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const form = reactive({ username: '', password: '' })

async function handleLogin() {
  if (!form.username || !form.password) {
    message.warning('请输入用户名和密码')
    return
  }
  loading.value = true
  try {
    const res: any = await login(form)
    const data = res.data
    userStore.setToken(data.accessToken)
    if (data.refreshToken) userStore.setRefreshToken(data.refreshToken)
    // 获取用户信息
    message.success('登录成功')
    router.push('/project')
  } catch (e: any) {
    message.error(e?.response?.data?.message || '登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-view">
    <div class="login-card">
      <div class="login-header">
        <h2>关键字驱动测试平台</h2>
        <p>请登录您的账户</p>
      </div>
      <a-form layout="vertical" @finish="handleLogin">
        <a-form-item label="用户名" required>
          <a-input
            v-model:value="form.username"
            placeholder="请输入用户名"
            size="large"
            @press-enter="handleLogin"
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
        <a-form-item>
          <a-button
            type="primary"
            block
            size="large"
            :loading="loading"
            @click="handleLogin"
          >
            登录
          </a-button>
        </a-form-item>
      </a-form>
      <div class="login-footer">
        <span>默认账户: admin / admin123</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.login-view {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}
.login-card {
  width: 400px;
  padding: 40px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.15);
}
.login-header {
  text-align: center;
  margin-bottom: 32px;
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
  margin-top: 16px;
}
</style>
