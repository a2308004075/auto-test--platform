<!--
 @author HXN
 @date 2026-08-21 23:16
 @description 全局配置视图
-->
<script setup lang="ts">
/**
 * 全局配置页面（仅 ADMIN）
 * 对齐原型 docs/ui/settings/global-config.html
 * 包含：保留策略卡片 + 通知配置卡片（SMTP/Webhook）+ 测试发送弹窗
 */
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/PageHeader/index.vue'
import { getSettings, updateSetting, testSmtpSend, testWebhookSend, type GlobalConfigItem } from '@/api/settings'
import { usePermission } from '@/composables/usePermission'

const { hasPermission } = usePermission()

// ===== 配置数据 =====
const configLoading = ref(false)
const configMap = ref<Record<string, string>>({})

// 保留策略表单
const retentionForm = reactive({
  logRetentionDays: '30',
  reportRetentionDays: '90',
})

// 通知配置表单
const notificationForm = reactive({
  smtpHost: '',
  smtpPort: '587',
  smtpUsername: '',
  smtpPassword: '',
  smtpEncryption: '',
  webhookUrl: '',
  webhookSecret: '',
})

// 保存按钮 loading 状态
const retentionSaving = ref(false)
const notificationSaving = ref(false)

// 测试发送状态
const smtpTestStatus = ref('')
const webhookTestStatus = ref('')

// 测试发送弹窗
const testSendVisible = ref(false)
const testSendType = ref<'smtp' | 'webhook'>('smtp')
const testSendForm = reactive({
  recipient: '',
  content: '这是一条测试通知消息，如果您收到此消息说明通知配置正确。',
})
const testSendLoading = ref(false)

// ===== 加载配置 =====
async function fetchSettings() {
  configLoading.value = true
  try {
    const res: any = await getSettings()
    const list: GlobalConfigItem[] = res.data || []
    const map: Record<string, string> = {}
    for (const item of list) {
      map[item.configKey] = item.configValue
    }
    configMap.value = map
    // 映射到表单
    retentionForm.logRetentionDays = map['log.retention_days'] ?? '30'
    retentionForm.reportRetentionDays = map['report.retention_days'] ?? '90'
    notificationForm.smtpHost = map['notification.smtp.host'] ?? ''
    notificationForm.smtpPort = map['notification.smtp.port'] ?? '587'
    notificationForm.smtpUsername = map['notification.smtp.username'] ?? ''
    notificationForm.smtpPassword = map['notification.smtp.password'] ?? ''
    notificationForm.smtpEncryption = map['notification.smtp.encryption'] ?? ''
    notificationForm.webhookUrl = map['notification.webhook.url'] ?? ''
    notificationForm.webhookSecret = map['notification.webhook.secret'] ?? ''
  } catch {
    ElMessage.error('加载配置失败')
  } finally {
    configLoading.value = false
  }
}

// ===== 保存保留策略 =====
async function handleSaveRetention() {
  retentionSaving.value = true
  try {
    const logVal = parseInt(retentionForm.logRetentionDays, 10)
    const reportVal = parseInt(retentionForm.reportRetentionDays, 10)
    if (isNaN(logVal) || logVal < 0 || isNaN(reportVal) || reportVal < 0) {
      ElMessage.error('保留天数必须为非负整数')
      return
    }
    await Promise.all([
      updateSetting('log.retention_days', { configValue: retentionForm.logRetentionDays }),
      updateSetting('report.retention_days', { configValue: retentionForm.reportRetentionDays }),
    ])
    ElMessage.success('保留策略 保存成功')
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '保存失败')
  } finally {
    retentionSaving.value = false
  }
}

// ===== 保存通知配置 =====
async function handleSaveNotification() {
  notificationSaving.value = true
  try {
    const portVal = parseInt(notificationForm.smtpPort, 10)
    if (isNaN(portVal) || portVal < 0 || portVal > 65535) {
      ElMessage.error('端口号必须在 0 ~ 65535 之间')
      return
    }
    await Promise.all([
      updateSetting('notification.smtp.host', { configValue: notificationForm.smtpHost }),
      updateSetting('notification.smtp.port', { configValue: notificationForm.smtpPort }),
      updateSetting('notification.smtp.username', { configValue: notificationForm.smtpUsername }),
      updateSetting('notification.smtp.password', { configValue: notificationForm.smtpPassword }),
      updateSetting('notification.smtp.encryption', { configValue: notificationForm.smtpEncryption }),
      updateSetting('notification.webhook.url', { configValue: notificationForm.webhookUrl }),
      updateSetting('notification.webhook.secret', { configValue: notificationForm.webhookSecret }),
    ])
    ElMessage.success('通知配置 保存成功')
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '保存失败')
  } finally {
    notificationSaving.value = false
  }
}

// ===== 测试发送 =====
function openTestSend(type: 'smtp' | 'webhook') {
  testSendType.value = type
  testSendForm.recipient = ''
  testSendVisible.value = true
}

async function handleConfirmTestSend() {
  if (testSendType.value === 'smtp' && !testSendForm.recipient.trim()) {
    ElMessage.error('请输入收件人邮箱地址')
    return
  }
  testSendLoading.value = true
  try {
    if (testSendType.value === 'smtp') {
      await testSmtpSend({ recipient: testSendForm.recipient, content: testSendForm.content })
      smtpTestStatus.value = '✓ 上次发送成功'
    } else {
      await testWebhookSend({ recipient: testSendForm.recipient || undefined, content: testSendForm.content })
      webhookTestStatus.value = '✓ 上次发送成功'
    }
    testSendVisible.value = false
    ElMessage.success((testSendType.value === 'smtp' ? '测试邮件' : 'Webhook 通知') + ' 发送成功')
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '发送失败')
  } finally {
    testSendLoading.value = false
  }
}

onMounted(() => { fetchSettings() })
</script>

<template>
  <div class="global-config-view" v-loading="configLoading">
    <PageHeader title="全局配置" />

    <!-- 保留策略 -->
    <div class="config-card">
      <div class="config-card-header">保留策略</div>
      <div class="config-card-body">
        <div class="config-form-grid">
          <div class="config-form-group">
            <label class="config-form-label">日志保留天数</label>
            <div class="config-input-with-suffix">
              <el-input-number v-model="retentionForm.logRetentionDays" :min="0" :controls="false" style="width: 120px;" />
              <span class="config-suffix">天</span>
            </div>
          </div>
          <div class="config-form-group">
            <label class="config-form-label">报告保留天数</label>
            <div class="config-input-with-suffix">
              <el-input-number v-model="retentionForm.reportRetentionDays" :min="0" :controls="false" style="width: 120px;" />
              <span class="config-suffix">天</span>
            </div>
          </div>
        </div>
        <div class="config-save-row">
          <el-button v-if="hasPermission('system:config:save')" type="primary" :loading="retentionSaving" @click="handleSaveRetention">保存</el-button>
        </div>
      </div>
    </div>

    <!-- 通知配置 -->
    <div class="config-card">
      <div class="config-card-header">通知配置</div>
      <div class="config-card-body">
        <!-- SMTP 邮件通知 -->
        <h4 class="config-section-subtitle">邮件通知 (SMTP)</h4>
        <div class="config-form-grid">
          <div class="config-form-group">
            <label class="config-form-label">SMTP 服务器地址</label>
            <el-input v-model="notificationForm.smtpHost" placeholder="smtp.example.com" />
          </div>
          <div class="config-form-group">
            <label class="config-form-label">端口</label>
            <el-input-number v-model="notificationForm.smtpPort" :min="0" :max="65535" :controls="false" style="width: 120px;" />
          </div>
          <div class="config-form-group">
            <label class="config-form-label">账号</label>
            <el-input v-model="notificationForm.smtpUsername" placeholder="notify@example.com" />
          </div>
          <div class="config-form-group">
            <label class="config-form-label">密码</label>
            <el-input v-model="notificationForm.smtpPassword" type="password" show-password placeholder="****" />
          </div>
          <div class="config-form-group">
            <label class="config-form-label">加密方式</label>
            <el-select v-model="notificationForm.smtpEncryption" placeholder="请选择" style="width: 100%;">
              <el-option label="TLS" value="tls" />
              <el-option label="SSL" value="ssl" />
              <el-option label="无" value="none" />
            </el-select>
          </div>
        </div>
        <div class="config-test-row">
          <el-button v-if="hasPermission('system:config:test')" size="small" @click="openTestSend('smtp')">测试发送</el-button>
          <span v-if="smtpTestStatus" class="config-test-status" style="color: var(--el-color-success);">{{ smtpTestStatus }}</span>
        </div>

        <div class="config-divider"></div>

        <!-- Webhook 通知 -->
        <h4 class="config-section-subtitle">Webhook 通知</h4>
        <div class="config-form-grid">
          <div class="config-form-group">
            <label class="config-form-label">Webhook URL</label>
            <el-input v-model="notificationForm.webhookUrl" placeholder="https://hooks.example.com/notify" />
          </div>
          <div class="config-form-group">
            <label class="config-form-label">密钥</label>
            <el-input v-model="notificationForm.webhookSecret" type="password" show-password placeholder="Webhook Secret" />
          </div>
        </div>
        <div class="config-test-row">
          <el-button v-if="hasPermission('system:config:test')" size="small" @click="openTestSend('webhook')">测试发送</el-button>
          <span v-if="webhookTestStatus" class="config-test-status" style="color: var(--el-color-success);">{{ webhookTestStatus }}</span>
        </div>

        <div class="config-save-row">
          <el-button v-if="hasPermission('system:config:save')" type="primary" :loading="notificationSaving" @click="handleSaveNotification">保存</el-button>
        </div>
      </div>
    </div>

    <!-- 测试发送弹窗 -->
    <el-dialog v-model="testSendVisible" :title="testSendType === 'smtp' ? '测试邮件发送' : '测试 Webhook 发送'" width="440px">
      <el-form label-position="top">
        <el-form-item label="接收地址">
          <el-input v-model="testSendForm.recipient" :placeholder="testSendType === 'smtp' ? '请输入收件人邮箱地址' : '请输入 Webhook 回调地址（可选，留空使用配置值）'" />
        </el-form-item>
        <el-form-item label="通知内容">
          <el-input v-model="testSendForm.content" type="textarea" :rows="3" resize="vertical" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="testSendVisible = false">取消</el-button>
        <el-button type="primary" :loading="testSendLoading" @click="handleConfirmTestSend">发送</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.global-config-view {
  width: 100%;
}

/* 卡片 */
.config-card {
  background: #fff;
  border-radius: 6px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.03), 0 1px 6px -1px rgba(0, 0, 0, 0.02), 0 2px 4px rgba(0, 0, 0, 0.02);
  border: 1px solid #f0f0f0;
  margin-bottom: 16px;
}
.config-card-header {
  padding: 16px 20px;
  border-bottom: 1px solid #f0f0f0;
  font-size: 15px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.88);
}
.config-card-body {
  padding: 20px;
}

/* 表单网格 */
.config-form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0 24px;
}
.config-form-group {
  margin-bottom: 16px;
}
.config-form-label {
  display: block;
  font-size: 14px;
  font-weight: 500;
  color: rgba(0, 0, 0, 0.88);
  margin-bottom: 6px;
}

/* 输入框 + 后缀 */
.config-input-with-suffix {
  display: flex;
  align-items: center;
  gap: 8px;
}
.config-suffix {
  font-size: 13px;
  color: rgba(0, 0, 0, 0.45);
}

/* 子标题 */
.config-section-subtitle {
  font-size: 13px;
  font-weight: 600;
  margin: 0 0 10px;
  color: rgba(0, 0, 0, 0.88);
}

/* 测试发送行 */
.config-test-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 12px;
}
.config-test-status {
  font-size: 13px;
}

/* 分隔线 */
.config-divider {
  border-top: 1px solid #f0f0f0;
  margin: 20px 0;
}

/* 保存按钮行 */
.config-save-row {
  margin-top: 16px;
}
</style>
