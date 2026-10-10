<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  ApiError,
  changePassword,
  deleteAccount,
  getMe,
  logout,
  searchMembers,
  updateProfile,
} from '../api'

const router = useRouter()
const member = ref(null)
const loading = ref(true)
const pageError = ref('')
const actionMessage = ref('')

const search = reactive({ field: 'username', keyword: '', page: 0, result: null, loading: false, error: '' })
const profile = reactive({ username: '', nickname: '', loading: false, error: '', fieldErrors: {} })
const password = reactive({ current: '', next: '', confirm: '', loading: false, error: '', fieldErrors: {} })
const deleting = ref(false)
const loggingOut = ref(false)

onMounted(loadMember)

async function loadMember() {
  loading.value = true
  try {
    member.value = await getMe()
    profile.username = member.value.username
    profile.nickname = member.value.nickname
  } catch (error) {
    await handleProtectedError(error)
  } finally {
    loading.value = false
  }
}

async function handleProtectedError(error) {
  if (error instanceof ApiError && error.status === 401) {
    await router.replace({ path: '/login', query: { message: '登入狀態已失效，請重新登入' } })
    return true
  }
  if (error instanceof ApiError && error.status === 403) {
    pageError.value = '安全驗證已失效，請重新整理頁面後再試'
    return true
  }
  return false
}

async function submitSearch(targetPage = 0) {
  search.error = ''
  const keyword = search.keyword.trim()
  const length = Array.from(keyword).length
  if (length < 1 || length > 30) {
    search.error = '查詢關鍵字須為 1～30 個字'
    return
  }
  search.loading = true
  try {
    search.result = await searchMembers(search.field, keyword, targetPage)
    search.page = targetPage
  } catch (error) {
    if (!(await handleProtectedError(error))) {
      search.error = error instanceof ApiError ? error.message : '查詢失敗，請稍後再試'
    }
  } finally {
    search.loading = false
  }
}

function validateProfile() {
  const errors = {}
  const username = profile.username.trim().toLowerCase()
  if (!/^[a-z0-9_]{4,20}$/.test(username)) {
    errors.username = '帳號限 4～20 個半形英文字母、數字或底線'
  }
  const nickname = profile.nickname.trim()
  if (Array.from(nickname).length < 1 || Array.from(nickname).length > 30
      || /[\u0000-\u001f\u007f-\u009f]/.test(nickname)) {
    errors.nickname = '暱稱去除頭尾空白後須為 1～30 個字，且不可包含換行或控制字元'
  }
  profile.fieldErrors = errors
  return Object.keys(errors).length === 0
}

async function saveProfile() {
  profile.error = ''
  actionMessage.value = ''
  if (!validateProfile()) return
  profile.loading = true
  try {
    const result = await updateProfile({ username: profile.username, nickname: profile.nickname })
    if (result.reLoginRequired) {
      await router.replace({ path: '/login', query: { message: '帳號已修改，請使用新帳號重新登入' } })
      return
    }
    member.value = result.member
    profile.username = result.member.username
    profile.nickname = result.member.nickname
    actionMessage.value = '個人資料已更新'
  } catch (error) {
    if (!(await handleProtectedError(error))) {
      profile.error = error instanceof ApiError ? error.message : '儲存失敗，請稍後再試'
      profile.fieldErrors = error.fieldErrors || {}
    }
  } finally {
    profile.loading = false
  }
}

function validatePassword() {
  const errors = {}
  if (!password.current) errors.currentPassword = '請輸入目前密碼'
  if (!/^[!-~]{8,64}$/.test(password.next)) {
    errors.newPassword = '密碼限 8～64 個半形英數或符號，不含空白'
  }
  if (password.next !== password.confirm) errors.confirmPassword = '兩次輸入的新密碼不一致'
  password.fieldErrors = errors
  return Object.keys(errors).length === 0
}

async function savePassword() {
  password.error = ''
  if (!validatePassword()) return
  password.loading = true
  try {
    await changePassword({
      currentPassword: password.current,
      newPassword: password.next,
      confirmPassword: password.confirm,
    })
    await router.replace({ path: '/login', query: { message: '密碼已修改，請重新登入' } })
  } catch (error) {
    if (!(await handleProtectedError(error))) {
      password.error = error instanceof ApiError ? error.message : '修改密碼失敗，請稍後再試'
      password.fieldErrors = error.fieldErrors || {}
    }
  } finally {
    password.loading = false
  }
}

async function signOut() {
  loggingOut.value = true
  pageError.value = ''
  try {
    await logout()
    await router.replace('/login')
  } catch (error) {
    pageError.value = error instanceof ApiError ? error.message : '登出失敗，請稍後再試'
  } finally {
    loggingOut.value = false
  }
}

async function removeAccount() {
  if (!window.confirm('確定要永久刪除帳號嗎？此操作無法復原。')) return
  deleting.value = true
  pageError.value = ''
  try {
    await deleteAccount()
    await router.replace({ path: '/login', query: { message: '帳號已刪除' } })
  } catch (error) {
    if (!(await handleProtectedError(error))) {
      pageError.value = error instanceof ApiError ? error.message : '刪除失敗，請稍後再試'
    }
  } finally {
    deleting.value = false
  }
}
</script>

<template>
  <main class="page">
    <p v-if="loading">載入中…</p>
    <template v-else-if="member">
      <header class="welcome">
        <h1>歡迎，{{ member.nickname }}</h1>
        <button type="button" :disabled="loggingOut" @click="signOut">
          {{ loggingOut ? '登出中…' : '登出' }}
        </button>
      </header>

      <p v-if="pageError" class="message error" role="alert">{{ pageError }}</p>
      <p v-if="actionMessage" class="message success" role="status">{{ actionMessage }}</p>

      <section>
        <h2>會員查詢</h2>
        <form class="inline-form" @submit.prevent="submitSearch(0)">
          <label>查詢類型
            <select v-model="search.field" @change="search.result = null">
              <option value="username">帳號</option>
              <option value="nickname">暱稱</option>
            </select>
          </label>
          <label>關鍵字<input v-model="search.keyword" maxlength="60" /></label>
          <button type="submit" :disabled="search.loading">{{ search.loading ? '查詢中…' : '查詢' }}</button>
        </form>
        <p v-if="search.error" class="field-error">{{ search.error }}</p>
        <template v-if="search.result">
          <p v-if="search.result.items.length === 0">查無符合的會員</p>
          <div v-else class="table-wrap">
            <table>
              <thead><tr><th>帳號</th><th>暱稱</th></tr></thead>
              <tbody>
                <tr v-for="item in search.result.items" :key="item.username">
                  <td>{{ item.username }}</td><td>{{ item.nickname }}</td>
                </tr>
              </tbody>
            </table>
          </div>
          <div class="pagination">
            <button type="button" :disabled="search.loading || search.page === 0" @click="submitSearch(search.page - 1)">上一頁</button>
            <span>第 {{ search.page + 1 }} 頁</span>
            <button type="button" :disabled="search.loading || !search.result.hasNext" @click="submitSearch(search.page + 1)">下一頁</button>
          </div>
        </template>
      </section>

      <section>
        <h2>我的資料</h2>
        <form class="form" @submit.prevent="saveProfile">
          <label>帳號<input v-model="profile.username" autocomplete="username" /></label>
          <p v-if="profile.fieldErrors.username" class="field-error">{{ profile.fieldErrors.username }}</p>
          <label>暱稱<input v-model="profile.nickname" autocomplete="nickname" /></label>
          <p v-if="profile.fieldErrors.nickname" class="field-error">{{ profile.fieldErrors.nickname }}</p>
          <p v-if="profile.error" class="field-error">{{ profile.error }}</p>
          <button type="submit" :disabled="profile.loading">{{ profile.loading ? '儲存中…' : '儲存' }}</button>
        </form>
      </section>

      <section>
        <h2>修改密碼</h2>
        <form class="form" @submit.prevent="savePassword">
          <label>目前密碼<input v-model="password.current" type="password" autocomplete="current-password" /></label>
          <p v-if="password.fieldErrors.currentPassword" class="field-error">{{ password.fieldErrors.currentPassword }}</p>
          <label>新密碼<input v-model="password.next" type="password" autocomplete="new-password" /></label>
          <p v-if="password.fieldErrors.newPassword" class="field-error">{{ password.fieldErrors.newPassword }}</p>
          <label>確認新密碼<input v-model="password.confirm" type="password" autocomplete="new-password" /></label>
          <p v-if="password.fieldErrors.confirmPassword" class="field-error">{{ password.fieldErrors.confirmPassword }}</p>
          <p v-if="password.error" class="field-error">{{ password.error }}</p>
          <button type="submit" :disabled="password.loading">{{ password.loading ? '修改中…' : '修改密碼' }}</button>
        </form>
      </section>

      <section class="danger-zone">
        <h2>刪除帳號</h2>
        <p>刪除後所有會員資料將永久移除，無法復原。</p>
        <button type="button" class="danger" :disabled="deleting" @click="removeAccount">
          {{ deleting ? '刪除中…' : '永久刪除帳號' }}
        </button>
      </section>
    </template>
  </main>
</template>
