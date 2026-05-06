import request from '@/utils/request'
/**
 *
 * 員工管理
 *
 **/
// 登入
export const login = (data: any) =>
  request({
    'url': '/employee/login',
    'method': 'post',
    data: data
  })

  // 退出
 export const userLogout = (params: any) =>
 request({
   'url': '/employee/logout',
   'method': 'post',
   params
 })

// 取得員工資訊列表
export const getEmployeeList = (params: any) =>
request({
  url: '/employee/page',
  method: 'get',
  params : params
})

// 更新員工帳號的狀態
export const enableOrDisable = (params: any) =>
request({
  url: `/employee/status/${params.status}`,
  method: 'post',
  params : {id: params.id}
})

// 添加員工資訊
export const addEmployee = (params: any) =>
  request({
    url: '/employee',
    method: 'post',
    data : params
  })

// 根據id取得員工資訊
export const getEmployeeById = (id: number) =>
  request({
    url: `/employee/${id}`,
    method: 'get'
  })

// 更新員工資訊
export const updateEmployee = (params: any) =>
  request({
    url: '/employee',
    method: 'put',
    data : params
  })