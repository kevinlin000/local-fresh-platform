import { http, unwrap } from '@/services/api'

export interface CartItem {
  id: number
  name: string
  userId: number
  productId: number | null
  giftBoxId: number | null
  productSpec: string | null
  number: number
  amount: number
  image: string | null
  createTime: string
}

export interface CartPayload {
  productId?: number
  giftBoxId?: number
  productSpec?: string
}

export function addToCart(payload: CartPayload) {
  return unwrap<void>(http.post('/user/cart/add', payload))
}

export function subFromCart(payload: CartPayload) {
  return unwrap<void>(http.post('/user/cart/sub', payload))
}

export function fetchCartList() {
  return unwrap<CartItem[]>(http.get('/user/cart/list'))
}

export function cleanCart() {
  return unwrap<void>(http.delete('/user/cart/clean'))
}
