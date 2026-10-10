<script setup>
import { reactive, ref } from 'vue'
import { ApiError, deleteMember, logout, searchMembers, updateMember } from '../api'

const props = defineProps({ member: { type: Object, required: true } })
const emit = defineEmits(['member-updated', 'show-login'])

const search = reactive({ field: 'username', keyword: '', results: null })
const profile = reactive({ username: props.member.username, name: props.member.name })
const error = ref('')
const success = ref('')
const busy = ref(false)

async function run(action) {
  error.value = ''
  success.value = ''
  busy.value = true
  try {
    await action()
  } catch (caught) {
    if (caught instanceof ApiError && caught.status === 401) {
      emit('show-login', '登入狀態已失效，請重新登入')
    } else {
      error.value = caught instanceof ApiError ? caught.message : '操作失敗'
    }
  } finally {
    busy.value = false
  }
}

function submitSearch() {
  run(async () => { search.results = await searchMembers(search.field, search.keyword.trim()) })
}

function saveProfile() {
  run(async () => {
    const result = await updateMember(profile)
    if (result.reLoginRequired) {
      emit('show-login', '帳號已修改，請使用新帳號重新登入')
    } else {
      emit('member-updated', result.member)
      success.value = '會員資料已更新'
    }
  })
}

function signOut() {
  run(async () => { await logout(); emit('show-login') })
}

function removeAccount() {
  if (!window.confirm('確定要永久刪除自己的帳號嗎？')) return
  run(async () => { await deleteMember(); emit('show-login', '帳號已刪除') })
}
</script>

<template>
  <header class="welcome">
    <h1>歡迎，{{ member.name }}</h1>
    <button type="button" :disabled="busy" @click="signOut">登出</button>
  </header>
  <p v-if="error" class="error">{{ error }}</p>
  <p v-if="success" class="success">{{ success }}</p>

  <section>
    <h2>查詢會員</h2>
    <form class="search-form" @submit.prevent="submitSearch">
      <select v-model="search.field"><option value="username">帳號</option><option value="name">姓名</option></select>
      <input v-model="search.keyword" placeholder="輸入關鍵字" required />
      <button :disabled="busy">查詢</button>
    </form>
    <p v-if="search.results?.length === 0">查無符合的會員</p>
    <table v-else-if="search.results?.length">
      <thead><tr><th>帳號</th><th>姓名</th></tr></thead>
      <tbody><tr v-for="item in search.results" :key="item.username"><td>{{ item.username }}</td><td>{{ item.name }}</td></tr></tbody>
    </table>
  </section>

  <section>
    <h2>修改我的資料</h2>
    <form @submit.prevent="saveProfile">
      <label>帳號<input v-model="profile.username" required /></label>
      <label>會員姓名<input v-model="profile.name" required /></label>
      <button :disabled="busy">儲存</button>
    </form>
  </section>

  <section class="danger-zone">
    <h2>刪除帳號</h2>
    <button type="button" class="danger" :disabled="busy" @click="removeAccount">永久刪除自己的帳號</button>
  </section>
</template>
