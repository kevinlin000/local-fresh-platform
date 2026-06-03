import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import AdminLayout from '@/views/vue3/AdminLayout.vue'

export const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/vue3/LoginView.vue'),
    meta: { public: true, title: '登入' }
  },
  {
    path: '/',
    component: AdminLayout,
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/vue3/DashboardView.vue'),
        meta: { title: '工作台' }
      },
      {
        path: 'orders',
        name: 'Orders',
        component: () => import('@/views/vue3/OrdersView.vue'),
        meta: { title: '訂單管理' }
      },
      {
        path: 'products',
        name: 'Products',
        component: () => import('@/views/vue3/ProductsView.vue'),
        meta: { title: '單品管理' }
      },
      {
        path: 'gift-boxes',
        name: 'GiftBoxes',
        component: () => import('@/views/vue3/GiftBoxesView.vue'),
        meta: { title: '直送箱管理' }
      },
      {
        path: 'categories',
        name: 'Categories',
        component: () => import('@/views/vue3/CategoriesView.vue'),
        meta: { title: '分類管理' }
      },
      {
        path: 'employees',
        name: 'Employees',
        component: () => import('@/views/vue3/EmployeesView.vue'),
        meta: { title: '員工管理' }
      },
      {
        path: 'reports',
        name: 'Reports',
        component: () => import('@/views/vue3/PlaceholderView.vue'),
        meta: { title: '資料統計', planned: true }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/dashboard'
  }
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes
})

export default router
