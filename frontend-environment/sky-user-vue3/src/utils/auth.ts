export const AUTH_STORAGE_KEYS = {
  token: 'member-token',
  id: 'member-id',
  openid: 'member-openid',
  name: 'member-name',
  oauthState: 'google-oauth-state',
  postLoginRedirect: 'post-login-redirect'
} as const

export interface StoredMemberProfile {
  id: number | null
  openid: string
  name: string
}

export function loadStoredProfile(): StoredMemberProfile {
  return {
    id: Number(localStorage.getItem(AUTH_STORAGE_KEYS.id) ?? 0) || null,
    openid: localStorage.getItem(AUTH_STORAGE_KEYS.openid) ?? '',
    name: localStorage.getItem(AUTH_STORAGE_KEYS.name) ?? ''
  }
}

export function persistMemberSession(payload: {
  token: string
  id: number
  openid: string
  name: string
}) {
  localStorage.setItem(AUTH_STORAGE_KEYS.token, payload.token)
  localStorage.setItem(AUTH_STORAGE_KEYS.id, String(payload.id))
  localStorage.setItem(AUTH_STORAGE_KEYS.openid, payload.openid)
  localStorage.setItem(AUTH_STORAGE_KEYS.name, payload.name)
}

export function clearMemberSession() {
  localStorage.removeItem(AUTH_STORAGE_KEYS.token)
  localStorage.removeItem(AUTH_STORAGE_KEYS.id)
  localStorage.removeItem(AUTH_STORAGE_KEYS.openid)
  localStorage.removeItem(AUTH_STORAGE_KEYS.name)
}

export function generateOAuthState() {
  if (typeof crypto !== 'undefined' && 'randomUUID' in crypto) {
    return crypto.randomUUID()
  }
  return `${Date.now()}-${Math.random().toString(36).slice(2)}`
}

export function saveOAuthState(state: string) {
  sessionStorage.setItem(AUTH_STORAGE_KEYS.oauthState, state)
}

export function getOAuthState() {
  return sessionStorage.getItem(AUTH_STORAGE_KEYS.oauthState)
}

export function clearOAuthState() {
  sessionStorage.removeItem(AUTH_STORAGE_KEYS.oauthState)
}

export function savePostLoginRedirect(path: string) {
  sessionStorage.setItem(AUTH_STORAGE_KEYS.postLoginRedirect, path)
}

export function consumePostLoginRedirect() {
  const path = sessionStorage.getItem(AUTH_STORAGE_KEYS.postLoginRedirect)
  sessionStorage.removeItem(AUTH_STORAGE_KEYS.postLoginRedirect)
  return path
}

export function clearPostLoginRedirect() {
  sessionStorage.removeItem(AUTH_STORAGE_KEYS.postLoginRedirect)
}

export function clearAuthArtifacts() {
  clearMemberSession()
  clearOAuthState()
  clearPostLoginRedirect()
}
