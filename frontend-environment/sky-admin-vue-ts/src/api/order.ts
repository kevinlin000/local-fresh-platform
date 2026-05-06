import request from '@/utils/request'

// 查詢列表页介面
export const getOrderDetailPage = (params: any) => {
  return request({
    url: '/order/conditionSearch',
    method: 'get',
    params
  })
}

// 查看介面
export const queryOrderDetailById = (params: any) => {
  return request({
    url: `/order/details/${params.orderId}`,
    method: 'get'
  })
}

// 派送介面
export const deliveryOrder = (params: any) => {
  return request({
    url: `/order/delivery/${params.id}`,
    method: 'put' /*  */
  })
}
//完成介面
export const completeOrder = (params: any) => {
  return request({
    url: `/order/complete/${params.id}`,
    method: 'put' /*  */
  })
}

//訂單取消
export const orderCancel = (params: any) => {
  return request({
    url: '/order/cancel',
    method: 'put' /*  */,
    data: { ...params }
  })
}

//接單
export const orderAccept = (params: any) => {
  return request({
    url: '/order/confirm',
    method: 'put' /*  */,
    data: { ...params }
  })
}

//拒单
export const orderReject = (params: any) => {
  return request({
    url: '/order/rejection',
    method: 'put' /*  */,
    data: { ...params }
  })
}

//取得待處理，待派送，派送中数量
export const getOrderListBy = (params: any) => {
  return request({
    url: '/order/statistics',
    method: 'get' /*  */
  })
}
