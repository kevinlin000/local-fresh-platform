import { http, unwrap } from '@/services/api'

export interface Category {
  id: number
  type: number
  name: string
  sort: number
  status: number
}

export interface ProductSpec {
  id: number
  productId: number
  name: string
  value: string
}

export interface Product {
  id: number
  productName: string
  categoryId: number
  price: number
  image: string | null
  description: string | null
  status: number
  categoryName?: string
  productSpecs: ProductSpec[]
}

export interface ProductSearchParams {
  categoryId?: number | null
  productName?: string
}

export interface GiftBox {
  id: number
  categoryId: number
  boxName: string
  price: number
  description: string | null
  image: string | null
  status: number
}

export interface ProductItem {
  name: string
  copies: number
  image: string | null
  description: string | null
}

export function fetchCategories(type: number) {
  return unwrap<Category[]>(
    http.get('/user/category/list', {
      params: { type }
    })
  )
}

export function fetchProducts(params: ProductSearchParams = {}) {
  const requestParams: Record<string, string | number> = {}
  if (params.categoryId) {
    requestParams.categoryId = params.categoryId
  }
  if (params.productName?.trim()) {
    requestParams.productName = params.productName.trim()
  }

  return unwrap<Product[]>(
    http.get('/user/product/list', {
      params: requestParams
    })
  )
}

export function fetchProductsByCategory(categoryId: number) {
  return fetchProducts({ categoryId })
}

export function fetchProductDetail(id: number) {
  return unwrap<Product>(http.get(`/user/product/${id}`))
}

export function fetchGiftBoxesByCategory(categoryId: number) {
  return unwrap<GiftBox[]>(
    http.get('/user/giftbox/list', {
      params: { categoryId }
    })
  )
}

export function fetchGiftBoxProducts(id: number) {
  return unwrap<ProductItem[]>(http.get(`/user/giftbox/product/${id}`))
}
