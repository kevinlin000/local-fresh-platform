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
        meta: { title: '商品管理' }
      },
      {
        path: 'operation-logs',
        name: 'OperationLogs',
        component: () => import('@/views/vue3/OperationLogsView.vue'),
        meta: { title: '操作紀錄' }
      },
      {
        path: 'payment-events',
        name: 'PaymentEvents',
        component: () => import('@/views/vue3/PaymentEventsView.vue'),
        meta: { title: '付款事件' }
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
        component: () => import('@/views/vue3/ReportsView.vue'),
        meta: { title: '資料統計' }
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
