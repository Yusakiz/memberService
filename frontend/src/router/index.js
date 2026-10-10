import { createRouter, createWebHashHistory } from 'vue-router'
import LoginView from '../views/LoginView.vue'
import MemberView from '../views/MemberView.vue'
import { ApiError, getMe } from '../api'

const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    { path: '/', redirect: '/login' },
    { path: '/login', component: LoginView },
    { path: '/members', component: MemberView },
    { path: '/:pathMatch(.*)*', redirect: '/login' },
  ],
})

router.beforeEach(async (to) => {
  if (to.path !== '/login' && to.path !== '/members') return true
  try {
    await getMe()
    return to.path === '/login' ? '/members' : true
  } catch (error) {
    if (error instanceof ApiError && error.status === 401) {
      return to.path === '/members' ? '/login' : true
    }
    return true
  }
})

export default router
