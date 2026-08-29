<template>
  <div class="login-page">
    <!-- 全屏背景图片 -->
    <div class="login-bg">
      <img
        src="/logo/login-page.jpg"
        alt="英威腾 AI 智能体平台"
        class="bg-image"
      />
      <div class="bg-overlay"></div>
    </div>

    <!-- 浮动登录卡片（靠右侧） -->
    <div class="login-panel">
      <div class="login-center">
        <div class="login-logo">
          <img src="/logo/login-logo.png" alt="英威腾 AI 智能体平台" class="logo-image" />
          <h1 class="logo-title">英威腾 AI Platform</h1>
          <p class="logo-subtitle">新一代的智能体平台</p>
        </div>

        <form class="login-form" @submit.prevent="handleLogin">
          <div class="input-wrap">
            <input
              v-model="form.username"
              type="text"
              class="form-input"
              :placeholder="t('login.placeholders.username')"
              :aria-label="t('login.fields.username')"
              autocomplete="username"
              required
            />
          </div>

          <div class="input-wrap">
            <input
              v-model="form.password"
              :type="showPassword ? 'text' : 'password'"
              class="form-input form-input--has-eye"
              :placeholder="t('login.placeholders.password')"
              :aria-label="t('login.fields.password')"
              autocomplete="current-password"
              required
            />
            <button type="button" class="eye-btn" @click="showPassword = !showPassword">
              <svg v-if="!showPassword" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/>
                <circle cx="12" cy="12" r="3"/>
              </svg>
              <svg v-else width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"/>
                <line x1="1" y1="1" x2="23" y2="23"/>
              </svg>
            </button>
          </div>

          <div v-if="errorMsg" class="error-msg">{{ errorMsg }}</div>

          <div class="login-options">
            <label class="remember-me">
              <input type="checkbox" v-model="rememberMe" />
              <span>{{ t('login.rememberMe') }}</span>
            </label>
          </div>

          <button type="submit" class="login-btn" :disabled="loading">
            <span v-if="!loading">{{ t('login.signIn') }}</span>
            <span v-else class="loading-dots">
              <span></span><span></span><span></span>
            </span>
          </button>

          <div class="login-links">
            <button type="button" class="link-btn" @click="showForgotHint = !showForgotHint">
              {{ t('login.forgotPassword') }}
            </button>
          </div>
          <div v-if="showForgotHint" class="forgot-hint">
            {{ t('login.forgotHint') }}
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { authApi } from '@/api/index'
import { useWorkspaceStore } from '@/stores/useWorkspaceStore'
import { useSystemSettingsStore } from '@/stores/useSystemSettingsStore'

const router = useRouter()
const { t } = useI18n()
const workspaceStore = useWorkspaceStore()
const systemSettingsStore = useSystemSettingsStore()
const loading = ref(false)
const showPassword = ref(false)
const errorMsg = ref('')
const rememberMe = ref(false)
const showForgotHint = ref(false)
const form = reactive({ username: '', password: '' })

const REMEMBER_KEY = 'invt_remembered_username'

onMounted(() => {
  const saved = localStorage.getItem(REMEMBER_KEY)
  if (saved) {
    form.username = saved
    rememberMe.value = true
  }
})

async function handleLogin() {
  if (!form.username || !form.password) return
  loading.value = true
  errorMsg.value = ''
  try {
    const res: any = await authApi.login(form)
    const data = res.data || res
    localStorage.setItem('token', data.token)
    localStorage.setItem('userId', String(data.id || '1'))
    localStorage.setItem('username', data.username || form.username)
    localStorage.setItem('role', data.role || 'user')
    // Remember username
    if (rememberMe.value) {
      localStorage.setItem(REMEMBER_KEY, form.username)
    } else {
      localStorage.removeItem(REMEMBER_KEY)
    }
    systemSettingsStore.load()
    try {
      await workspaceStore.fetchWorkspaces()
    } catch {
    }
    const target = workspaceStore.can('view:dashboard') ? '/dashboard' : '/chat'
    router.push(target)
  } catch (e: any) {
    errorMsg.value = typeof e === 'string' ? e : t('login.failed')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
/* ===== 全屏背景图，图片覆盖整个浏览器 ===== */
.login-page {
  position: relative;
  min-height: 100vh;
  width: 100%;
  overflow: hidden;
  background: #0d1b2a;
}

/* 背景层：铺满整个视口 */
.login-bg {
  position: absolute;
  inset: 0;
  overflow: hidden;
}

.bg-image {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center;
  display: block;
}

/* 淡淡的压暗遮罩，让右侧登录卡片更清晰，不影响看图 */
.bg-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(
    to right,
    rgba(6, 18, 32, 0) 0%,
    rgba(6, 18, 32, 0.05) 55%,
    rgba(6, 18, 32, 0.12) 100%
  );
}

/* ===== 浮动登录卡片（右侧偏上区域） ===== */
.login-panel {
  position: absolute;
  top: 22%;
  right: 22%;
  transform: translateY(-50%);
  width: 400px;
  max-width: calc(100vw - 48px);
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.5);
  border-radius: 20px;
  padding: 40px 36px;
  box-shadow: 0 24px 60px rgba(0, 0, 0, 0.28);
  animation: fadeUp 0.6s ease-out both;
  z-index: 2;
}

.login-center {
  display: flex;
  flex-direction: column;
  gap: 24px;
  width: 100%;
}

.login-logo {
  text-align: center;
}

.logo-image {
  display: block;
  margin: 0 auto 14px;
  width: 108px;
  height: auto;
  object-fit: contain;
}

.logo-title {
  font-size: 25px;
  font-weight: 800;
  color: #0d1b2a;
  margin: 0;
  letter-spacing: -0.02em;
}

.logo-subtitle {
  font-size: 14px;
  color: #6B7280;
  margin: 4px 0 0;
  letter-spacing: 0.02em;
}

.login-form {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.input-wrap {
  position: relative;
  display: flex;
  align-items: center;
}

.form-input {
  width: 100%;
  padding: 13px 16px;
  border: 1.8px solid #E2E8F0;
  border-radius: 12px;
  font-size: 15px;
  color: #1A1A2E;
  background: #F8FAFC;
  outline: none;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
  transition: border-color 0.2s, box-shadow 0.2s, background 0.2s;
}

.form-input:-webkit-autofill,
.form-input:-webkit-autofill:hover,
.form-input:-webkit-autofill:focus {
  -webkit-box-shadow: 0 0 0 30px #F8FAFC inset !important;
  -webkit-text-fill-color: #1A1A2E !important;
  transition: background-color 5000s ease-in-out 0s;
}

.form-input--has-eye {
  padding-right: 44px;
}

.form-input:focus {
  border-color: #007FCC;
  background: #ffffff;
  box-shadow: 0 0 0 3px rgba(0, 127, 204, 0.12);
}

.eye-btn {
  position: absolute;
  right: 12px;
  width: 28px;
  height: 28px;
  border: none;
  background: none;
  cursor: pointer;
  color: #9CA3AF;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
}

.eye-btn:hover {
  color: #007FCC;
}

.error-msg {
  padding: 10px 14px;
  background: #FFF1F0;
  border: 1px solid #FF4D4F;
  border-radius: 10px;
  font-size: 13px;
  color: #D4380D;
}

.login-btn {
  width: 100%;
  padding: 12px;
  background: linear-gradient(135deg, #007FCC, #005999);
  color: white;
  border: none;
  border-radius: 12px;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s;
  margin-top: 4px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.login-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 8px 20px rgba(0, 127, 204, 0.35);
}

.login-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.login-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.remember-me {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #6B7280;
  cursor: pointer;
  user-select: none;
}

.remember-me input[type="checkbox"] {
  width: 15px;
  height: 15px;
  accent-color: #007FCC;
  cursor: pointer;
}

.login-links {
  text-align: center;
}

.link-btn {
  background: none;
  border: none;
  color: #007FCC;
  font-size: 13px;
  cursor: pointer;
  padding: 4px 8px;
  text-decoration: none;
}

.link-btn:hover {
  text-decoration: underline;
  color: #005999;
}

.forgot-hint {
  margin-top: 4px;
  padding: 10px 14px;
  background: #FEF3C7;
  border: 1px solid #F59E0B;
  border-radius: 10px;
  font-size: 13px;
  color: #92400E;
  text-align: center;
  line-height: 1.5;
}

.loading-dots {
  display: flex;
  gap: 5px;
  align-items: center;
}

.loading-dots span {
  width: 6px;
  height: 6px;
  background: white;
  border-radius: 50%;
  animation: bounce 1.2s infinite;
}

.loading-dots span:nth-child(2) { animation-delay: 0.2s; }
.loading-dots span:nth-child(3) { animation-delay: 0.4s; }

@keyframes bounce {
  0%, 60%, 100% { transform: translateY(0); }
  30% { transform: translateY(-5px); }
}

@keyframes fadeUp {
  from { opacity: 0; }
  to { opacity: 1; transform: translateY(0); }
}

/* ===== 小屏适配：登录卡片水平居中、垂直偏上 ===== */
@media (max-width: 640px) {
  .login-panel {
    left: 50%;
    right: auto;
    top: 12%;
    transform: translateX(-50%);
    width: calc(100vw - 40px);
    padding: 32px 24px;
  }
}
</style>
