<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ApiError, login, register } from '../api'

const router = useRouter()
const route = useRoute()
const mode = ref('login')
const username = ref('')
const nickname = ref('')
const password = ref('')
const confirmPassword = ref('')
const submitting = ref(false)
const error = ref('')
const fieldErrors = ref({})
const success = ref(typeof route.query.message === 'string' ? route.query.message : '')

const isLogin = computed(() => mode.value === 'login')

function switchMode(nextMode) {
  mode.value = nextMode
  password.value = ''
  confirmPassword.value = ''
  error.value = ''
  fieldErrors.value = {}
  success.value = ''
}

function normalizeUsername() {
  return username.value.trim().toLowerCase()
}

function validateRegistration() {
  const errors = {}
  const normalized = normalizeUsername()
  if (!/^[a-z0-9_]{4,20}$/.test(normalized)) {
    errors.username = '帳號限 4～20 個半形英文字母、數字或底線'
  }
  const trimmedNickname = nickname.value.trim()
  const nicknameLength = Array.from(trimmedNickname).length
  if (nicknameLength < 1 || nicknameLength > 30 || /[\u0000-\u001f\u007f-\u009f]/.test(trimmedNickname)) {
    errors.nickname = '暱稱去除頭尾空白後須為 1～30 個字，且不可包含換行或控制字元'
  }
  if (!/^[!-~]{8,64}$/.test(password.value)) {
    errors.password = '密碼限 8～64 個半形英數或符號，不含空白'
  }
  if (password.value !== confirmPassword.value) {
    errors.confirmPassword = '兩次輸入的密碼不一致'
  }
  fieldErrors.value = errors
  return Object.keys(errors).length === 0
}

async function submitLogin() {
  error.value = ''
  fieldErrors.value = {}
  if (!username.value.trim() || !password.value) {
    error.value = '請輸入帳號與密碼'
    return
  }
  submitting.value = true
  try {
    await login(normalizeUsername(), password.value)
    await router.replace('/members')
  } catch (caught) {
    error.value = caught instanceof ApiError ? caught.message : '無法連線到伺服器'
  } finally {
    submitting.value = false
  }
}

async function submitRegistration() {
  error.value = ''
  success.value = ''
  if (!validateRegistration()) return
  submitting.value = true
  try {
    const normalized = normalizeUsername()
    await register({
      username: normalized,
      nickname: nickname.value,
      password: password.value,
      confirmPassword: confirmPassword.value,
    })
    username.value = normalized
    switchMode('login')
    success.value = '註冊成功，請登入'
  } catch (caught) {
    if (caught instanceof ApiError) {
      error.value = caught.message
      fieldErrors.value = caught.fieldErrors || {}
    } else {
      error.value = '無法連線到伺服器'
    }
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <main class="page narrow-page">
    <h1>會員平台</h1>
    <div class="tabs" aria-label="選擇登入或註冊">
      <button type="button" :class="{ active: isLogin }" @click="switchMode('login')">登入</button>
      <button type="button" :class="{ active: !isLogin }" @click="switchMode('register')">註冊</button>
    </div>

    <p v-if="success" class="message success" role="status">{{ success }}</p>
    <p v-if="error" class="message error" role="alert">{{ error }}</p>

    <form v-if="isLogin" class="form" @submit.prevent="submitLogin">
      <label>帳號<input v-model="username" autocomplete="username" /></label>
      <label>密碼<input v-model="password" type="password" autocomplete="current-password" /></label>
      <button type="submit" :disabled="submitting">{{ submitting ? '登入中…' : '登入' }}</button>
    </form>

    <form v-else class="form" @submit.prevent="submitRegistration">
      <label>
        帳號
        <input v-model="username" autocomplete="username" aria-describedby="username-help" />
      </label>
      <small id="username-help">4～20 個英文字母、數字或底線，不區分大小寫</small>
      <p v-if="fieldErrors.username" class="field-error">{{ fieldErrors.username }}</p>
      <label>
        暱稱
        <input v-model="nickname" autocomplete="nickname" />
      </label>
      <small>可自行取名，不需要使用本名</small>
      <p v-if="fieldErrors.nickname" class="field-error">{{ fieldErrors.nickname }}</p>
      <label>密碼<input v-model="password" type="password" autocomplete="new-password" /></label>
      <p v-if="fieldErrors.password" class="field-error">{{ fieldErrors.password }}</p>
      <label>確認密碼<input v-model="confirmPassword" type="password" autocomplete="new-password" /></label>
      <p v-if="fieldErrors.confirmPassword" class="field-error">{{ fieldErrors.confirmPassword }}</p>
      <button type="submit" :disabled="submitting">{{ submitting ? '註冊中…' : '註冊' }}</button>
    </form>
  </main>
</template>
