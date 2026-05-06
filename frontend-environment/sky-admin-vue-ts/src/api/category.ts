import request from '@/utils/request';
/**
 *
 * 分類管理
 *
 **/

// 查詢分類列表介面
export const getCategoryPage = (params: any) => {
  return request({
    url: '/category/page',
    method: 'get',
    params
  });
};

// 刪除目前列的介面
export const deleCategory = (ids: string) => {
  return request({
    url: '/category',
    method: 'delete',
    params: { id:ids }
  });
};

// 修改介面
export const editCategory = (params: any) => {
  return request({
    url: '/category',
    method: 'put',
    data: { ...params }
  });
};

// 新增介面
export const addCategory = (params: any) => {
  return request({
    url: '/category',
    method: 'post',
    data: { ...params }
  });
};

// 修改---啟用停用介面
export const enableOrDisableEmployee = (params: any) => {
  return request({
    url: `/category/status/${params.status}`,
    method: 'post',
    params: { id:params.id }
  })
}

// 根據類型查詢分類：1为單品分類 2为直送箱分類
export const getCategoryByType = (params: any) => {
  return request({
    url: `/category/list`,
    method: 'get',
    params: params
  })
}