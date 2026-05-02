import request from '@/utils/request'
/**
 *
 * 员工管理
 *
 **/
// 登录
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

// 获取员工信息列表
export const getEmployeeList = (params: any) =>
request({
  url: '/employee/page',
  method: 'get',
  params : params
})

// 更新员工账号的状态
export const enableOrDisable = (params: any) =>
request({
  url: `/employee/status/${params.status}`,
  method: 'post',
  params : {id: params.id}
})

// 添加员工信息
export const addEmployee = (params: any) =>
  request({
    url: '/employee',
    method: 'post',
    data : params
  })

// 根据id获取员工信息
export const getEmployeeById = (id: number) =>
  request({
    url: `/employee/${id}`,
    method: 'get'
  })

// 更新员工信息
export const updateEmployee = (params: any) =>
  request({
    url: '/employee',
    method: 'put',
    data : params
  })