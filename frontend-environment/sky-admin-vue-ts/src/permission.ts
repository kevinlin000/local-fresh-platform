import router from '@/router'
import { getToken } from '@/utils/cookies'

router.beforeEach(to => {
  if (to.meta.public) {
    return true
  }
  if (!getToken()) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  return true
})
