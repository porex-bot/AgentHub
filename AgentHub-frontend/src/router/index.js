import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'

const routes = [
  {
    path: '/',
    name: 'home',
    component: HomeView,
  },
  {
    path: '/love',
    name: 'love',
    component: () => import('../views/ChatAppView.vue'),
  },
  {
    path: '/task',
    name: 'task',
    component: () => import('../views/ChatAppView.vue'),
  },
  {
    path: '/report',
    name: 'report',
    component: () => import('../views/ChatAppView.vue'),
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'not-found',
    component: () => import('../views/NotFoundView.vue'),
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

export default router
