import { createRouter, createWebHistory } from 'vue-router'
import { useMemberStore } from '@/stores/member'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/LoginView.vue'),
      meta: { public: true }
    },
    {
      path: '/',
      name: 'home',
      component: () => import('@/views/HomeView.vue'),
      meta: { public: true }
    },
    {
      path: '/product/:id',
      name: 'product-detail',
      component: () => import('@/views/ProductDetailView.vue'),
      meta: { public: true }
    },
    {
      path: '/cart',
      name: 'cart',
      component: () => import('@/views/CartView.vue'),
      meta: { public: true }
    },
    {
      path: '/orders',
      name: 'orders',
      component: () => import('@/views/OrdersView.vue'),
      meta: { public: true }
    },
    {
      path: '/groupBuy/:groupNo',
      name: 'group-buy',
      component: () => import('@/views/GroupBuyView.vue'),
      meta: { public: true }
    }
  ]
})

router.beforeEach((to) => {
  const memberStore = useMemberStore()
  if (to.meta.public) {
    return true
  }
  if (!memberStore.isLoggedIn) {
    return {
      path: '/login',
      query: { redirect: to.fullPath }
    }
  }
  return true
})

export default router
