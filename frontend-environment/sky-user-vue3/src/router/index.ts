import { createRouter, createWebHistory } from 'vue-router'
import { useMemberStore } from '@/stores/member'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/LoginView.vue'),
      meta: { public: true, hideChrome: true }
    },
    {
      path: '/oauth/callback',
      name: 'oauth-callback',
      component: () => import('@/views/OAuthCallbackView.vue'),
      meta: { public: true, hideChrome: true }
    },
    {
      path: '/',
      name: 'home',
      component: () => import('@/views/HomeView.vue')
    },
    {
      path: '/product/:id',
      name: 'product-detail',
      component: () => import('@/views/ProductDetailView.vue')
    },
    {
      path: '/cart',
      name: 'cart',
      component: () => import('@/views/CartView.vue')
    },
    {
      path: '/orders',
      name: 'orders',
      component: () => import('@/views/OrdersView.vue')
    },
    {
      path: '/addresses',
      name: 'addresses',
      component: () => import('@/views/AddressView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/groupBuy/:groupNo',
      name: 'group-buy',
      component: () => import('@/views/GroupBuyView.vue')
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
