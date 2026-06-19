import request from '@/utils/request'

export const getPaymentEventPage = (params: any) => {
  return request({
    url: '/paymentEvents/page',
    method: 'get',
    params
  })
}
