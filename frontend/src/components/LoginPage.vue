<script setup>
import { ref } from 'vue'
import { ApiError, login, register } from '../api'

defineProps({ message: { type: String, default: '' } })
const emit = defineEmits(['logged-in'])

const mode = ref('login')
const username = ref('')
const name = ref('')
const password = ref('')
const confirmPassword = ref('')
const error = ref('')
const success = ref('')
const submitting = ref(false)

function switchMode(next) {
  mode.value = next
  password.value = ''
  confirmPassword.value = ''
  error.value = ''
  success.value = ''
}

async function submit() {
  error.value = ''
  success.value = ''
  submitting.value = true
  try {
    if (mode.value === 'login') {
      await login(username.value.trim().toLowerCase(), password.value)
      emit('logged-in')
    } else {
      await register({
        username: username.value,
        name: name.value,
        password: password.value,
        confirmPassword: confirmPassword.value,
      })
      username.value = username.value.trim().toLowerCase()
      switchMode('login')
      success.value = '註冊成功，請登入'
    }
  } catch (caught) {
    error.value = caught instanceof ApiError ? caught.message : '無法連線到伺服器'
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <section class="login-box">
    <h1>簡易會員平台</h1>
    <div class="tabs">
      <button type="button" :class="{ secondary: mode !== 'login' }" @click="switchMode('login')">登入</button>
      <button type="button" :class="{ secondary: mode !== 'register' }" @click="switchMode('register')">註冊</button>
    </div>
    <p v-if="message" class="success">{{ message }}</p>
    <p v-if="success" class="success">{{ success }}</p>
    <p v-if="error" class="error">{{ error }}</p>

    <form @submit.prevent="submit">
      <label>帳號<input v-model="username" autocomplete="username" required /></label>
      <label v-if="mode === 'register'">會員姓名<input v-model="name" required /></label>
      <label>密碼<input v-model="password" type="password" :autocomplete="mode === 'login' ? 'current-password' : 'new-password'" required /></label>
      <label v-if="mode === 'register'">確認密碼<input v-model="confirmPassword" type="password" autocomplete="new-password" required /></label>
      <small v-if="mode === 'register'">帳號 4～20 字；密碼 8～64 個半形英數或符號</small>
      <button type="submit" :disabled="submitting">{{ submitting ? '處理中…' : mode === 'login' ? '登入' : '註冊' }}</button>
    </form>
  </section>
</template>
