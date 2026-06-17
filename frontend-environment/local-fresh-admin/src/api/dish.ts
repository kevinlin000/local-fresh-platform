import request from '@/utils/request'
/**
 *
 * 單品管理
 *
 **/
// 查詢列表介面
export const getDishPage = (params: any) => {
  return request({
    url: '/product/page',
    method: 'get',
    params
  })
}

// 刪除介面
export const deleteDish = (ids: string) => {
  return request({
    url: '/product',
    method: 'delete',
    params: { ids }
  })
}

// 修改介面
export const editDish = (params: any) => {
  return request({
    url: '/product',
    method: 'put',
    data: { ...params }
  })
}

// 新增介面
export const addDish = (params: any) => {
  return request({
    url: '/product',
    method: 'post',
    data: { ...params }
  })
}

// 查詢詳情
export const queryDishById = (id: string | (string | null)[]) => {
  return request({
    url: `/product/${id}`,
    method: 'get'
  })
}

// 查詢單品庫存異動紀錄
export const queryDishInventoryLogs = (id: string | number) => {
  return request({
    url: `/product/${id}/inventory-logs`,
    method: 'get'
  })
}

// 手動調整單品庫存
export const adjustDishInventory = (id: string | number, data: any) => {
  return request({
    url: `/product/${id}/inventory`,
    method: 'patch',
    data
  })
}

// 取得單品分類列表
export const getCategoryList = (params: any) => {
  return request({
    url: '/category/list',
    method: 'get',
    params
  })
}

// 查單品列表的介面
export const queryDishList = (params: any) => {
  return request({
    url: '/product/list',
    method: 'get',
    params
  })
}

// 檔案down预览
export const commonDownload = (params: any) => {
  return request({
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8'
    },
    url: '/common/download',
    method: 'get',
    params
  })
}

// 起售停售---批次起售停售介面
export const dishStatusByStatus = (params: any) => {
  return request({
    url: `/product/status/${params.status}`,
    method: 'post',
    params: { id: params.id }
  })
}

//單品分類資料查詢
export const dishCategoryList = (params: any) => {
  return request({
    url: `/category/list`,
    method: 'get',
    params: { ...params }
  })
}
