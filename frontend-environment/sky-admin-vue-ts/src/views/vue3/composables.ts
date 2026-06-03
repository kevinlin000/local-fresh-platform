import { reactive, ref } from 'vue'

export interface PageState {
  page: number
  pageSize: number
  total: number
}

export function usePage(defaultPageSize = 10) {
  return reactive<PageState>({
    page: 1,
    pageSize: defaultPageSize,
    total: 0
  })
}

export function useLoading() {
  const loading = ref(false)

  async function withLoading<T>(task: () => Promise<T>) {
    loading.value = true
    try {
      return await task()
    } finally {
      loading.value = false
    }
  }

  return { loading, withLoading }
}

export function readPage(response: any) {
  const data = response.data?.data || {}
  return {
    records: data.records || [],
    total: Number(data.total || 0)
  }
}
