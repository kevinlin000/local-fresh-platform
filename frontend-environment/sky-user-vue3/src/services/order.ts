import { http, unwrap, type PageResult } from '@/services/api'

export interface OrderDetail {
  id: number
  orderId: number
  productId: number | null
  giftBoxId: number | null
  name: string
  image: string | null
  productSpec: string | null
  number: number
  amount: number
}

export interface OrderRecord {
  id: number
  number: string
  status: number
  userId: number
  addressBookId: number
  orderTime: string
  checkoutTime: string | null
  payMethod: number | null
  payStatus: number
  amount: number
  remark: string | null
  phone: string
  address: string
  consignee: string
  cancelReason: string | null
  estimatedDeliveryTime: string | null
  deliveryStatus: number | null
  packAmount: number
  tablewareNumber: number
  tablewareStatus: number | null
  orderDishes?: string | null
  orderDetailList: OrderDetail[]
}

export interface SubmitOrderPayload {
  addressBookId: number
  payMethod: number
  remark?: string
  estimatedDeliveryTime?: string | null
  deliveryStatus: number
  tablewareNumber: number
  tablewareStatus: number
  packAmount: number
  amount: number
}

export interface OrderSubmitResult {
  id: number
  orderNumber: string
  orderAmount: number
  orderTime: string
}

export function submitOrder(payload: SubmitOrderPayload) {
  return unwrap<OrderSubmitResult>(http.post('/user/order/submit', payload))
}

export function fetchOrderHistory(params: {
  page: number
  pageSize: number
  status?: number
}) {
  return unwrap<PageResult<OrderRecord>>(
    http.get('/user/order/historyOrders', {
      params
    })
  )
}
