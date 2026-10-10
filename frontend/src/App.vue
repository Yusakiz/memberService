<script setup>
import { onMounted, ref } from 'vue'
import { ApiError, getMe } from './api'
import LoginPage from './components/LoginPage.vue'
import MemberPage from './components/MemberPage.vue'

const member = ref(null)
const loading = ref(true)
const message = ref('')

onMounted(loadMember)

async function loadMember() {
  try {
    member.value = await getMe()
  } catch (error) {
    if (!(error instanceof ApiError && error.status === 401)) {
      message.value = '無法連線到後端服務'
    }
    member.value = null
  } finally {
    loading.value = false
  }
}

function showLogin(text = '') {
  member.value = null
  message.value = text
}
</script>

<template>
  <main class="page">
    <p v-if="loading">載入中…</p>
    <LoginPage v-else-if="!member" :message="message" @logged-in="loadMember" />
    <MemberPage
      v-else
      :member="member"
      @member-updated="member = $event"
      @show-login="showLogin"
    />
  </main>
</template>
