import request from '@/utils/request'

export const getOperationLogPage = (params: any) => {
  return request({
    url: '/operationLogs/page',
    method: 'get',
    params
  })
}
