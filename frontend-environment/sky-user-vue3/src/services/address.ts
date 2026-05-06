import { http, unwrap } from '@/services/api'

export interface ShippingAddress {
  id: number
  memberId?: number
  consignee: string
  phone: string
  sex?: string | null
  provinceCode?: string | null
  provinceName?: string | null
  cityCode?: string | null
  cityName?: string | null
  districtCode?: string | null
  districtName?: string | null
  detail: string
  label?: string | null
  isDefault?: number
}

export interface CreateAddressPayload {
  consignee: string
  phone: string
  sex?: string
  cityName: string
  districtName: string
  detail: string
  label?: string
  isDefault?: number
}

export function fetchAddressList() {
  return unwrap<ShippingAddress[]>(http.get('/user/shippingAddress/list'))
}

export function fetchDefaultAddress() {
  return unwrap<ShippingAddress>(http.get('/user/shippingAddress/default'))
}

export function createAddress(payload: CreateAddressPayload) {
  return unwrap<void>(http.post('/user/shippingAddress', payload))
}

export function setDefaultAddress(id: number) {
  return unwrap<void>(http.put('/user/shippingAddress/default', { id }))
}
