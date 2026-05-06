import http from '@/services/http'

interface ApiResponse<T> {
  code: number
  msg: string
  data: T
}

export interface MemberLoginPayload {
  id: number
  openid: string
  name: string
  token: string
}

async function unwrap<T>(request: Promise<{ data: ApiResponse<T> }>) {
  const response = await request
  if (response.data.code !== 1) {
    throw new Error(response.data.msg || '操作失敗')
  }
  return response.data.data
}

export function mockLogin(code: string) {
  return unwrap<MemberLoginPayload>(
    http.post('/user/member/login', { code })
  )
}

export function googleOAuthLogin(code: string, redirectUri: string) {
  return unwrap<MemberLoginPayload>(
    http.post('/user/member/oauth/google', { code, redirectUri })
  )
}
