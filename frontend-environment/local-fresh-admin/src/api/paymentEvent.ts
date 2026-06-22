import request from '@/utils/request'

export const getPaymentEventPage = (params: any) => {
  return request({
    url: '/paymentEvents/page',
    method: 'get',
    params
  })
}

export const getPendingPaymentRequests = (params: any) => {
  return request({
    url: '/paymentEvents/pendingRequests',
    method: 'get',
    params
  })
}
