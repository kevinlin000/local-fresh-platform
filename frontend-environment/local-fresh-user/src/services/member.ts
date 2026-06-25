import { http, unwrap } from '@/services/api'

export interface MemberLoginPayload {
  id: number
  openid: string
  name: string
  token: string
}

export interface MemberPasswordLoginRequest {
  email: string
  password: string
}

export interface MemberRegisterRequest extends MemberPasswordLoginRequest {
  name: string
  phone?: string
}

export function mockLogin(code: string) {
  return unwrap<MemberLoginPayload>(
    http.post('/user/member/login', { code })
  )
}

export function passwordLogin(payload: MemberPasswordLoginRequest) {
  return unwrap<MemberLoginPayload>(
    http.post('/user/member/password-login', payload)
  )
}

export function register(payload: MemberRegisterRequest) {
  return unwrap<MemberLoginPayload>(
    http.post('/user/member/register', payload)
  )
}

export function googleOAuthLogin(code: string, redirectUri: string) {
  return unwrap<MemberLoginPayload>(
    http.post('/user/member/oauth/google', { code, redirectUri })
  )
}
