import http from '@/services/http'

export interface ApiResponse<T> {
  code: number
  msg: string | null
  data: T
}

export interface PageResult<T> {
  total: number
  records: T[]
}

export async function unwrap<T>(request: Promise<{ data: ApiResponse<T> }>) {
  const response = await request
  if (response.data.code !== 1) {
    throw new Error(response.data.msg || '操作失敗')
  }
  return response.data.data
}

export { http }
