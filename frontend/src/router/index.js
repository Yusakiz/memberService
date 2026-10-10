import { createRouter, createWebHashHistory } from 'vue-router'
import LoginView from '../views/LoginView.vue'
import MemberView from '../views/MemberView.vue'

const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    { path: '/', redirect: '/login' },
    { path: '/login', component: LoginView },
    { path: '/members', component: MemberView },
    { path: '/:pathMatch(.*)*', redirect: '/login' },
  ],
})

export default router
