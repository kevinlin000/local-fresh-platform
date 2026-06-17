import request from '@/utils/request'
// 修改密碼
export const editPassword = (data: any) =>
  request({
    'url': '/employee/editPassword',
    'method': 'put',
    data
  })
  // 取得營業狀態
  export const getStatus = () =>
  request({
    'url': `/shop/status`,
    'method': 'get'
  })
    // 設定營業狀態
    export const setStatus = (data:any) =>
    request({
      'url': `/shop/`+data,
      'method': 'put',
      'data':data
    })