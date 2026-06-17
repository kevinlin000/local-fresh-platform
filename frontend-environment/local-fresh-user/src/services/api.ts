import axios from 'axios'
import http from '@/services/http'

export interface ApiResponse<T> {
  code: number
  msg: string | null
  data: T
}

interface ApiErrorResponse {
  msg?: string | null
  message?: string | null
}

export interface PageResult<T> {
  total: number
  records: T[]
}

export async function unwrap<T>(request: Promise<{ data: ApiResponse<T> }>) {
  try {
    const response = await request
    if (response.data.code !== 1) {
      throw new Error(response.data.msg || '操作失敗')
    }
    return response.data.data
  } catch (error) {
    if (axios.isAxiosError<ApiErrorResponse>(error)) {
      const message = error.response?.data?.msg || error.response?.data?.message
      if (message) {
        throw new Error(message)
      }
    }
    throw error
  }
}

export { http }
